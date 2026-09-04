"""Portable melody branch vs the cached torchcrepe histograms."""
import json, sys, time
from pathlib import Path
import numpy as np

HERE = Path(__file__).resolve().parent
D = Path("/Users/neerajaabhyankar/Repos/icm-shruti-analysis/raag-identifier/best-model-09-01")
AUDIO = Path("/Users/neerajaabhyankar/Repos/icm-shruti-analysis/raag-identifier/hindustani-raag-small-v1")
sys.path.insert(0, str(HERE)); sys.path.insert(0, str(D))
from raag_fusion import audio                                    # noqa: E402
from portable import frontend, melody                            # noqa: E402

index = [r for r in json.loads((D / "cache/index.json").read_text()) if r["split"] == "test"]
n = int(sys.argv[1]) if len(sys.argv) > 1 else 4
for r in index[::max(1, len(index) // n)][:n]:
    raag, name = r["clip_id"].split("/")
    y, sr = audio.load(AUDIO / r["clip_id"])
    w = audio.windows(np.asarray(y, dtype=np.float32), sr)[0]
    t0 = time.time()
    y16 = frontend.resample(w, sr, melody.SR)
    f0, voiced = melody.f0_track(y16)
    mine = melody.histogram(f0, voiced, r["tonic_hz"])
    dt = time.time() - t0
    ref = np.load(D / "cache/melody" / f"{raag}__{name.rsplit('.', 1)[0]}.npy").astype(np.float64)
    print(f"{r['clip_id']:<50s} max|d|={np.abs(mine-ref).max():.4f} "
          f"L1={np.abs(mine-ref).sum():.4f} corr={np.corrcoef(mine,ref)[0,1]:.5f} "
          f"voiced={voiced.mean():.2f}  {dt:.1f}s")
