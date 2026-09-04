"""Baseline top-1/top-5 on the 150 test clips, computed from the cached features.

No librosa, no torchcrepe -- this only exercises the fusion arithmetic, and tells us the
cache is the same thing the reported 0.48/0.82 was measured on.
"""
import json, sys
from pathlib import Path
import numpy as np, torch

D = Path("/Users/neerajaabhyankar/Repos/icm-shruti-analysis/raag-identifier/best-model-09-01")
sys.path.insert(0, str(D))
from raag_fusion import RaagIdentifier, melody_branch            # noqa: E402
from raag_fusion.identifier import _softmax                      # noqa: E402

model = RaagIdentifier.load(device="cpu")
cfg, raags = model.config, model.raags
index = [r for r in json.loads((D / "cache/index.json").read_text()) if r["split"] == "test"]
print(f"{len(index)} test clips, {len(raags)} raags")

def stem(clip_id):
    raag, name = clip_id.split("/")
    return f"{raag}__{name.rsplit('.', 1)[0]}.npy"

top1 = top5 = 0
rows = []
for r in index:
    s = stem(r["clip_id"])
    cqt = np.load(D / "cache/cqt" / s).astype(np.float32)[None]
    hist = np.load(D / "cache/melody" / s).astype(np.float64)
    with torch.no_grad():
        logits = model.net(torch.from_numpy(cqt)[None].float())
    p_cqt = _softmax(logits[0].numpy(), cfg["temperature_cqt"])
    p_mel = _softmax(model.linear.scores(hist), cfg["temperature_melody"])
    w = cfg["melody_weight"]
    p = (1 - w) * p_cqt + w * p_mel
    order = np.argsort(-p)
    gold = raags.index(r["raag"])
    top1 += order[0] == gold
    top5 += gold in order[:5]
    rows.append((r["clip_id"], raags[order[0]], r["raag"], float(p[order[0]])))

n = len(index)
print(f"top1 {top1/n:.4f}  top5 {top5/n:.4f}   ({top1}/{n}, {top5}/{n})")
json.dump(rows, open(Path(__file__).parent / "baseline_cache_rows.json", "w"), indent=1)
