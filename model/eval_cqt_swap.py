"""All 150 test clips with the portable CQT in place of librosa's.

The melody branch is left on its cached (torchcrepe) histogram, so whatever moves here is
the CQT swap and nothing else.
"""
import json, sys, time
from pathlib import Path
import numpy as np, torch

HERE = Path(__file__).resolve().parent
D = Path("/Users/neerajaabhyankar/Repos/icm-shruti-analysis/raag-identifier/best-model-09-01")
AUDIO = Path("/Users/neerajaabhyankar/Repos/icm-shruti-analysis/raag-identifier/hindustani-raag-small-v1")
sys.path.insert(0, str(HERE)); sys.path.insert(0, str(D))
from raag_fusion import RaagIdentifier, audio                    # noqa: E402
from raag_fusion.identifier import _softmax                      # noqa: E402
from portable import frontend                                    # noqa: E402
from portable.tonic import anchor_fmin                           # noqa: E402

model = RaagIdentifier.load(device="cpu")
cfg, raags, w = model.config, model.raags, None
w = cfg["melody_weight"]
index = [r for r in json.loads((D / "cache/index.json").read_text()) if r["split"] == "test"]

kernels, hits = {}, {"ref": [0, 0], "port": [0, 0]}
rows, t0 = [], time.time()
for i, r in enumerate(index, 1):
    raag, name = r["clip_id"].split("/")
    stem = f"{raag}__{name.rsplit('.', 1)[0]}.npy"
    gold = raags.index(r["raag"])

    y, sr = audio.load(AUDIO / r["clip_id"])
    win = audio.windows(np.asarray(y, dtype=np.float32), sr)[0]
    y22 = frontend.resample(win, sr, frontend.SR_CQT)
    y22 = (y22 / (float(np.max(np.abs(y22))) or 1.0)).astype(np.float32)
    y22 = audio.fit_length(y22, int(round(frontend.SR_CQT * 20.0)))

    fmin = anchor_fmin(r["tonic_hz"])
    key = round(fmin, 6)
    if key not in kernels:
        kernels[key] = frontend.cqt_kernel(fmin)
    port_cqt = frontend.features(y22, r["tonic_hz"], kernel=kernels[key])
    ref_cqt = np.load(D / "cache/cqt" / stem).astype(np.float32)[None]
    hist = np.load(D / "cache/melody" / stem).astype(np.float64)
    p_mel = _softmax(model.linear.scores(hist), cfg["temperature_melody"])

    out = {}
    for tag, x in (("ref", ref_cqt), ("port", port_cqt)):
        with torch.no_grad():
            logits = model.net(torch.from_numpy(x)[None].float())
        p = (1 - w) * _softmax(logits[0].numpy(), cfg["temperature_cqt"]) + w * p_mel
        order = np.argsort(-p)
        hits[tag][0] += order[0] == gold
        hits[tag][1] += gold in order[:5]
        out[tag] = (raags[order[0]], float(p[order[0]]), int(np.where(order == gold)[0][0]))
    rows.append({"clip": r["clip_id"], "gold": r["raag"], **out})
    if i % 30 == 0:
        print(f"  {i}/150  {time.time() - t0:.0f}s", flush=True)

n = len(index)
for tag, label in (("ref", "librosa CQT (reference)"), ("port", "portable CQT")):
    t1, t5 = hits[tag]
    print(f"{label:<26s} top1 {t1/n:.4f}  top5 {t5/n:.4f}   ({t1}/{n}, {t5}/{n})")
same = sum(r["ref"][0] == r["port"][0] for r in rows)
print(f"top-1 label agreement between the two front ends: {same}/{n} ({same/n:.1%})")
json.dump(rows, open(HERE / "eval_cqt_swap.json", "w"), indent=1)
