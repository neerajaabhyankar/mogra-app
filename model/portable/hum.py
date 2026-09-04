"""Reading Sa out of a few seconds of held hum, without librosa.pyin.

The reference is `raag_fusion.tonic.from_hum`: pYIN, keep the voiced frames, take the
median. pYIN is YIN plus an HMM over pitch candidates, and the HMM is there to hold a
melody together through leaps and octave errors. A held Sa has no leaps. So this is plain
YIN with a voicing threshold and the same median, which is the part that actually does the
work -- a hum starts and ends with a scoop, and the median ignores both instead of
averaging them in.

The octave check at the end is the one thing YIN needs that pYIN's HMM was giving for
free: YIN's cumulative-mean normalisation can settle on a period twice the true one when
the second harmonic is strong, which for a hummed Sa is a real risk. If half the median
period explains the frame nearly as well, the higher octave wins.
"""

import numpy as np

FRAME = 2048
HOP = 256
FMIN, FMAX = 60.0, 600.0
THRESHOLD = 0.15          # YIN's absolute threshold on the normalised difference
VOICED_MAX = 0.45         # frames whose best d' is worse than this are called unvoiced


def _difference(frame, tau_max):
    """YIN's cumulative mean normalised difference function, via autocorrelation."""
    n = len(frame)
    size = 1 << int(np.ceil(np.log2(2 * n)))
    F = np.fft.rfft(frame, size)
    acf = np.fft.irfft(F * np.conj(F), size)[:tau_max]

    power = np.concatenate([[0.0], np.cumsum(frame ** 2)])
    # d(tau) = r(0) + r_tau(0) - 2 * acf(tau), with both energy terms windowed
    tau = np.arange(tau_max)
    left = power[n] - power[tau]
    right = power[n - tau] if tau_max <= n else np.array([power[n - t] for t in tau])
    d = left + right - 2 * acf

    cum = np.cumsum(d[1:])
    dprime = np.ones(tau_max)
    nz = cum > 0
    dprime[1:][nz] = d[1:][nz] * np.arange(1, tau_max)[nz] / cum[nz]
    return dprime


def _parabolic(d, tau):
    """Sub-sample refinement of the dip at `tau`."""
    if tau <= 0 or tau >= len(d) - 1:
        return float(tau)
    a, b, c = d[tau - 1], d[tau], d[tau + 1]
    denom = a - 2 * b + c
    return float(tau) if denom == 0 else float(tau) + 0.5 * (a - c) / denom


def f0_yin(y, sr, fmin=FMIN, fmax=FMAX, frame=FRAME, hop=HOP, threshold=THRESHOLD):
    """(f0 per frame in Hz, voiced mask) -- NaN where unvoiced."""
    y = np.asarray(y, dtype=np.float64)
    tau_min = max(2, int(np.floor(sr / fmax)))
    tau_max = min(frame - 1, int(np.ceil(sr / fmin)) + 1)
    if len(y) < frame:
        y = np.pad(y, (0, frame - len(y)))

    f0, ok = [], []
    for start in range(0, len(y) - frame + 1, hop):
        d = _difference(y[start:start + frame], tau_max)
        search = d[tau_min:]
        below = np.flatnonzero(search < threshold)
        if below.size:                      # first dip under the threshold, as YIN does
            tau = tau_min + int(below[0])
            while tau + 1 < tau_max and d[tau + 1] < d[tau]:
                tau += 1
        else:
            tau = tau_min + int(np.argmin(search))
        f0.append(sr / _parabolic(d, tau))
        ok.append(d[tau] < VOICED_MAX)
    return np.asarray(f0), np.asarray(ok, dtype=bool)


def from_hum(y, sr, fmin_hz=FMIN, fmax_hz=FMAX):
    """A few seconds of a held Sa -> its frequency in Hz."""
    f0, voiced = f0_yin(y, sr, fmin=fmin_hz, fmax=fmax_hz)
    f0 = f0[voiced & np.isfinite(f0)]
    if f0.size < 10:
        raise ValueError("could not hear a steady pitch -- hum one note, louder, for ~5 s")

    med = float(np.median(f0))
    # octave check: if a third of the frames sit within 5 % of half the median period,
    # YIN has locked onto a subharmonic and the true Sa is the octave above
    if fmax_hz >= 2 * med and np.mean(np.abs(f0 / (2 * med) - 1.0) < 0.05) > 0.33:
        return 2 * med
    return med
