"""Portable CQT vs the librosa features the network was trained on."""
import json, sys, time
from pathlib import Path
import numpy as np

HERE = Path(__file__).resolve().parent
D = Path("/Users/neerajaabhyankar/Repos/icm-shruti-analysis/raag-identifier/best-model-09-01")
AUDIO = Path("/Users/neerajaabhyankar/Repos/icm-shruti-analysis/raag-identifier/hindustani-raag-small-v1")
sys.path.insert(0, str(HERE)); sys.path.insert(0, str(D))
from raag_fusion import audio                                    # noqa: E402
from portable import frontend                                    # noqa: E402

index = [r for r in json.loads((D / "cache/index.json").read_text()) if r["split"] == "test"]
n = int(sys.argv[1]) if len(sys.argv) > 1 else 6

for r in index[::max(1, len(index) // n)][:n]:
    raag, name = r["clip_id"].split("/")
    stem = f"{raag}__{name.rsplit('.', 1)[0]}.npy"
    y, sr = audio.load(AUDIO / r["clip_id"])          # decode only; librosa here is I/O
    w = audio.windows(np.asarray(y, dtype=np.float32), sr)[0]

    t0 = time.time()
    y22 = frontend.resample(w, sr, frontend.SR_CQT)
    peak = float(np.max(np.abs(y22))) or 1.0
    y22 = (y22 / peak).astype(np.float32)
    y22 = audio.fit_length(y22, int(round(frontend.SR_CQT * 20.0)))
    mine = frontend.features(y22, r["tonic_hz"])[0]
    dt = time.time() - t0

    ref = np.load(D / "cache/cqt" / stem).astype(np.float32)
    d = np.abs(mine - ref)
    corr = np.corrcoef(mine.ravel(), ref.ravel())[0, 1]
    print(f"{r['clip_id']:<50s} max|d|={d.max():.4f} mean|d|={d.mean():.4f} "
          f"corr={corr:.5f}  {dt:.1f}s")
