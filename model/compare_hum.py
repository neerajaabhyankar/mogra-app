"""Portable YIN tonic vs librosa.pyin, on synthetic hums with known answers."""
import sys
from pathlib import Path
import numpy as np

HERE = Path(__file__).resolve().parent
D = Path("/Users/neerajaabhyankar/Repos/icm-shruti-analysis/raag-identifier/best-model-09-01")
sys.path.insert(0, str(HERE)); sys.path.insert(0, str(D))
from raag_fusion import tonic as ref_tonic                       # noqa: E402
from portable import hum                                         # noqa: E402

SR = 22050
rng = np.random.default_rng(0)


def synth(f, seconds=5.0, sr=SR, vibrato=0.008, scoop=True, harmonics=(1, .5, .3, .18, .1),
          noise=0.004):
    t = np.arange(int(seconds * sr)) / sr
    f_t = f * (1 + vibrato * np.sin(2 * np.pi * 5.2 * t))
    if scoop:                       # a real hum arrives from below and falls off at the end
        f_t = f_t * (1 - 0.06 * np.exp(-t / 0.25) - 0.04 * np.exp(-(t[-1] - t) / 0.2))
    phase = 2 * np.pi * np.cumsum(f_t) / sr
    y = sum(a * np.sin(k * phase) for k, a in enumerate(harmonics, 1))
    env = np.minimum(1.0, np.minimum(t / 0.15, (t[-1] - t) / 0.15))
    return (y * env + noise * rng.standard_normal(len(t))).astype(np.float32)


print(f"{'true Hz':>9s} {'note':>5s} {'portable':>9s} {'cents':>7s} {'librosa pyin':>13s} {'cents':>7s}")
for f, note in [(110.0, "A2"), (130.81, "C3"), (138.59, "C#3"), (146.83, "D3"),
                (196.0, "G3"), (220.0, "A3"), (261.63, "C4"), (293.66, "D4"), (329.63, "E4")]:
    y = synth(f)
    mine = hum.from_hum(y, SR)
    ref = ref_tonic.from_hum(y, SR)
    c_mine = 1200 * np.log2(mine / f)
    c_ref = 1200 * np.log2(ref / f)
    print(f"{f:9.2f} {note:>5s} {mine:9.2f} {c_mine:+7.1f} {ref:13.2f} {c_ref:+7.1f}")
