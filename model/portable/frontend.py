"""A numpy-only replacement for the librosa half of the raag identifier's front end.

Nothing here imports librosa, soxr or torchcrepe. Everything is either plain numpy or a
handful of arithmetic that maps onto what an Android build can actually run: one FIR
resampler, one FFT per frame, one sparse matrix multiply.

The reference it is chasing is `raag_fusion.cqt_branch.features`, which is

    librosa.cqt(y, sr=22050, fmin=Sa/2 folded, n_bins=144, bins_per_octave=36, hop=1024)
    librosa.amplitude_to_db(ref=max) -> float16 -> (db + 80) / 80

librosa computes that transform octave by octave, halving the sample rate as it goes,
because that is cheaper. This computes all 144 bins in a single pass at 22.05 kHz. The two
are the same transform on paper; the difference is that librosa's version also carries the
ringing of four soxr resampling stages, and this one does not.
"""

import numpy as np

SR_CQT = 22050
BINS_PER_OCTAVE = 36
OCTAVES = 4
N_BINS = BINS_PER_OCTAVE * OCTAVES
HOP = 1024
N_FRAMES = 431
TOP_DB = 80.0
SPARSITY = 0.01


# ----------------------------------------------------------------- resampling

def _kaiser_sinc(taps, cutoff, beta, up):
    """Windowed-sinc low-pass designed at the interpolated rate `up * sr_in`."""
    n = np.arange(taps, dtype=np.float64) - (taps - 1) / 2.0
    h = 2.0 * cutoff * np.sinc(2.0 * cutoff * n) * np.kaiser(taps, beta)
    return (h / h.sum() * up).astype(np.float64)


def resample(y, sr_in, sr_out, half_width=32, beta=14.769656459379492):
    """Rational polyphase resampling -- the same shape of filter soxr_hq uses.

    Written as a polyphase bank rather than zero-stuff-and-convolve. 48 kHz to 22.05 kHz
    is up=147 / down=320, and the naive form makes a 65-million-sample intermediate and
    a trillion multiply-adds; the polyphase form is ~139 taps per output sample. That is
    also the only version that could run on a phone.
    """
    y = np.asarray(y, dtype=np.float64)
    if int(sr_in) == int(sr_out):
        return y.astype(np.float32)

    g = np.gcd(int(sr_in), int(sr_out))
    up, down = int(sr_out) // g, int(sr_in) // g
    cutoff = 0.5 / max(up, down) * 0.99          # 1 % guard below the new Nyquist

    taps = 2 * half_width * max(up, down) + 1
    h = _kaiser_sinc(taps, cutoff, beta, up)

    # h[phase::up] is the sub-filter for outputs landing on that phase
    per = int(np.ceil(taps / up))
    hp = np.zeros((up, per), dtype=np.float64)
    for p_ in range(up):
        col = h[p_::up]
        hp[p_, :len(col)] = col

    n_out = int(np.ceil(len(y) * up / down))
    centre = (taps - 1) // 2
    m = np.arange(n_out)
    phase = (m * down) % up
    base = (m * down) // up + (centre // up)

    x = np.concatenate([np.zeros(per, dtype=np.float64), y, np.zeros(per, dtype=np.float64)])
    out = np.empty(n_out, dtype=np.float64)
    j = np.arange(per)
    for p_ in range(up):
        sel = np.flatnonzero(phase == p_)
        if not sel.size:
            continue
        idx = (base[sel][:, None] - j[None, :]) + per
        np.clip(idx, 0, len(x) - 1, out=idx)
        out[sel] = (x[idx] * hp[p_][None, :]).sum(axis=1)
    return out.astype(np.float32)


# ----------------------------------------------------------------- the CQT

def _wavelet_lengths(freqs, sr, bins_per_octave):
    alpha = (2.0 ** (2.0 / bins_per_octave) - 1) / (2.0 ** (2.0 / bins_per_octave) + 1)
    Q = 1.0 / alpha
    return Q * sr / np.asarray(freqs, dtype=np.float64)


def _hann(n):
    """Periodic Hann, which is what scipy.signal.get_window gives librosa."""
    return 0.5 - 0.5 * np.cos(2.0 * np.pi * np.arange(n) / n)


def _sparsify_rows(x, quantile=SPARSITY):
    """Zero the smallest entries of each row that together hold < `quantile` of its L1.

    librosa does this to the filter bank before using it; keeping it means this matches
    librosa rather than matching an idealised CQT librosa never computes. It is also what
    makes the matrix cheap enough to multiply on a phone.
    """
    out = np.zeros_like(x)
    for i, row in enumerate(x):
        mag = np.abs(row)
        order = np.argsort(mag)[::-1]
        keep = np.cumsum(mag[order]) < (1.0 - quantile) * mag.sum()
        keep[0] = True
        idx = order[keep]
        out[i, idx] = row[idx]
    return out


def cqt_kernel(fmin, sr=SR_CQT, n_bins=N_BINS, bins_per_octave=BINS_PER_OCTAVE):
    """The frequency-domain filter bank: (n_bins, n_fft//2+1) complex, plus n_fft, lengths.

    Depends only on the tonic, so an app that keeps Sa fixed between recordings builds
    this once.
    """
    freqs = fmin * 2.0 ** (np.arange(n_bins) / bins_per_octave)
    lengths = _wavelet_lengths(freqs, sr, bins_per_octave)
    n_fft = int(2 ** np.ceil(np.log2(lengths.max())))

    basis = np.zeros((n_bins, n_fft), dtype=np.complex128)
    for k, (ilen, f) in enumerate(zip(lengths, freqs)):
        lo, hi = -(int(ilen) // 2), int(ilen) // 2      # librosa's -ilen//2 on a float
        t = np.arange(lo, hi, dtype=np.float64)
        sig = np.exp(1j * t * 2.0 * np.pi * f / sr) * _hann(len(t))
        sig /= np.abs(sig).sum()                        # norm=1
        start = (n_fft - len(sig)) // 2                 # pad_center
        basis[k, start:start + len(sig)] = sig

    basis *= lengths[:, None] / float(n_fft)
    fft_basis = np.fft.fft(basis, n=n_fft, axis=1)[:, : n_fft // 2 + 1]
    return _sparsify_rows(fft_basis).astype(np.complex64), n_fft, lengths


def _stft_ones(y, n_fft, hop):
    """STFT with a rectangular window and centred, zero-padded frames -- librosa's
    `__cqt_response` calls stft(window="ones", pad_mode="constant")."""
    y = np.pad(np.asarray(y, dtype=np.float64), n_fft // 2, mode="constant")
    n_frames = 1 + (len(y) - n_fft) // hop
    idx = np.arange(n_fft)[None, :] + hop * np.arange(n_frames)[:, None]
    return np.fft.rfft(y[idx], axis=1).T.astype(np.complex64)


def cqt(y22050, fmin, sr=SR_CQT, hop=HOP, kernel=None):
    """(n_bins, n_frames) complex CQT, scaled the way librosa's `scale=True` scales it."""
    fft_basis, n_fft, lengths = kernel if kernel is not None else cqt_kernel(fmin, sr=sr)
    D = _stft_ones(y22050, n_fft, hop)
    return (fft_basis @ D) / np.sqrt(lengths)[:, None]


def amplitude_to_db(S, top_db=TOP_DB):
    """librosa.amplitude_to_db(S, ref=np.max), amin=1e-10 on the power."""
    power = np.abs(S) ** 2
    ref = power.max()
    db = 10.0 * np.log10(np.maximum(1e-10, power)) - 10.0 * np.log10(max(1e-10, ref))
    return np.maximum(db, db.max() - top_db)


def features(y22050, tonic_hz, kernel=None):
    """20 s of peak-normalised 22.05 kHz audio -> (1, 144, 431) float32."""
    from .tonic import anchor_fmin

    C = amplitude_to_db(cqt(y22050, anchor_fmin(tonic_hz), kernel=kernel))
    C = C.astype(np.float16).astype(np.float32)
    if C.shape[1] >= N_FRAMES:
        C = C[:, :N_FRAMES]
    else:
        C = np.pad(C, ((0, 0), (0, N_FRAMES - C.shape[1])), constant_values=C.min())
    return ((C + 80.0) / 80.0)[None]
