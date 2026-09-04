"""Golden vectors so the Kotlin transcription can be checked against the Python it came from.

The input signal is generated, not shipped: the same deterministic formula is written in
both languages, so only the outputs need to cross over.
"""
import json, sys
from pathlib import Path
import numpy as np

HERE = Path(__file__).resolve().parent
OUT = HERE.parent / "app/src/test/resources/golden"
OUT.mkdir(parents=True, exist_ok=True)
sys.path.insert(0, str(HERE))
from portable import frontend, melody, hum                       # noqa: E402
from portable.tonic import anchor_fmin                           # noqa: E402

SR_IN = 48000
TONIC = 146.83


def signal(n, sr=SR_IN):
    """Three partials plus a little LCG noise. Mirrored exactly in Goldens.kt."""
    t = np.arange(n)
    y = (0.6 * np.sin(2 * np.pi * 220.0 * t / sr)
         + 0.3 * np.sin(2 * np.pi * 440.0 * t / sr)
         + 0.1 * np.sin(2 * np.pi * 660.0 * t / sr))
    s = 12345
    noise = np.empty(n)
    for i in range(n):
        s = (s * 1103515245 + 12345) & 0x7FFFFFFF
        noise[i] = s / 0x7FFFFFFF * 2.0 - 1.0
    return (y + 0.02 * noise).astype(np.float32)


def dump(name, a):
    a = np.asarray(a, dtype="<f4")
    (OUT / name).write_bytes(a.tobytes())
    print(f"  {name:<28s} {a.size} floats")


one = signal(SR_IN)
dump("resample_48_22050.bin", frontend.resample(one, SR_IN, 22050))
dump("resample_48_16000.bin", frontend.resample(one, SR_IN, 16000))

twenty = signal(SR_IN * 20)
y22 = frontend.resample(twenty, SR_IN, 22050)
y22 = (y22 / (float(np.max(np.abs(y22))) or 1.0)).astype(np.float32)
y22 = frontend.fit_length(y22, 441000) if hasattr(frontend, "fit_length") else y22[:441000]
dump("cqt_features.bin", frontend.features(y22, TONIC).ravel())

# a deterministic pitch track, so the histogram is tested without running CREPE
n = 500
f0 = TONIC * 2.0 ** ((np.arange(n) % 12) / 12.0) * (1 + 0.01 * np.sin(np.arange(n) / 7.0))
voiced = (np.arange(n) % 5) != 0
dump("histogram.bin", melody.histogram(f0.astype(np.float32), voiced, TONIC))

# YIN on a held hum
h = hum.__dict__
sr = 22050
t = np.arange(int(4.0 * sr)) / sr
f = 146.83 * (1 + 0.008 * np.sin(2 * np.pi * 5.2 * t))
phase = 2 * np.pi * np.cumsum(f) / sr
held = sum(a * np.sin(k * phase) for k, a in enumerate((1, .5, .3, .18, .1), 1)).astype(np.float32)
meta = {
    "tonic_hz": TONIC,
    "anchor_fmin": anchor_fmin(TONIC),
    "hum_expected_hz": float(hum.from_hum(held, sr)),
    "f16_cases": [float(np.float16(np.float32(v))) for v in
                  [0.0, 1.0, -1.0, 0.1, -0.1, 79.9, -79.9, 1e-5, 65504.0, 0.33333333]],
}
(OUT / "meta.json").write_text(json.dumps(meta, indent=1))
print(" ", json.dumps(meta))

# --- pieces of the CQT, so a mismatch can be localised rather than guessed at ----------
kernel = frontend.cqt_kernel(anchor_fmin(TONIC))
fft_basis, n_fft, lengths = kernel
K = 52                                    # the bin the end-to-end test disagreed on
row = np.asarray(fft_basis[K])
dump("kernel_row52.bin", np.stack([row.real, row.imag], axis=1).ravel())
dump("kernel_lengths.bin", lengths)
D = frontend._stft_ones(y22, n_fft, frontend.HOP)
col = np.asarray(D[:, 0])
dump("stft_frame0.bin", np.stack([col.real, col.imag], axis=1).ravel())
meta = json.loads((OUT / "meta.json").read_text())
meta["n_fft"] = int(n_fft)
meta["kernel_nnz_row52"] = int(np.count_nonzero(row))
(OUT / "meta.json").write_text(json.dumps(meta, indent=1))
print("  n_fft", n_fft, "nnz(row52)", meta["kernel_nnz_row52"])
