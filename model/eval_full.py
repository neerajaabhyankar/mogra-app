"""All 150 test clips, entire front end portable: no librosa, no soxr, no torchcrepe.

librosa is still used to *decode* the mp3, because decoding is the one job Android does
natively and there is nothing to prove about it.
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
from portable import frontend, melody                            # noqa: E402
from portable.tonic import anchor_fmin                           # noqa: E402

model = RaagIdentifier.load(device="cpu")
cfg, raags = model.config, model.raags
w = cfg["melody_weight"]
index = [r for r in json.loads((D / "cache/index.json").read_text()) if r["split"] == "test"]

kernels = {}
hits = {"ref": [0, 0], "port": [0, 0]}
rows, t0 = [], time.time()
for i, r in enumerate(index, 1):
    raag, name = r["clip_id"].split("/")
    stem = f"{raag}__{name.rsplit('.', 1)[0]}.npy"
    gold = raags.index(r["raag"])
    tonic = r["tonic_hz"]

    y, sr = audio.load(AUDIO / r["clip_id"])
    win = audio.windows(np.asarray(y, dtype=np.float32), sr)[0]

    y22 = frontend.resample(win, sr, frontend.SR_CQT)
    y22 = (y22 / (float(np.max(np.abs(y22))) or 1.0)).astype(np.float32)
    y22 = audio.fit_length(y22, int(round(frontend.SR_CQT * 20.0)))
    fmin = anchor_fmin(tonic)
    key = round(fmin, 6)
    if key not in kernels:
        kernels[key] = frontend.cqt_kernel(fmin)
    port_cqt = frontend.features(y22, tonic, kernel=kernels[key])

    y16 = frontend.resample(win, sr, melody.SR)
    f0, voiced = melody.f0_track(y16)
    port_hist = melody.histogram(f0, voiced, tonic)

    ref_cqt = np.load(D / "cache/cqt" / stem).astype(np.float32)[None]
    ref_hist = np.load(D / "cache/melody" / stem).astype(np.float64)

    out = {}
    for tag, (x, h) in (("ref", (ref_cqt, ref_hist)), ("port", (port_cqt, port_hist))):
        with torch.no_grad():
            logits = model.net(torch.from_numpy(x)[None].float())
        p = ((1 - w) * _softmax(logits[0].numpy(), cfg["temperature_cqt"])
             + w * _softmax(model.linear.scores(h), cfg["temperature_melody"]))
        order = np.argsort(-p)
        hits[tag][0] += order[0] == gold
        hits[tag][1] += gold in order[:5]
        out[tag] = [raags[order[0]], float(p[order[0]]), int(np.where(order == gold)[0][0])]
    rows.append({"clip": r["clip_id"], "gold": r["raag"], **out})
    if i % 25 == 0:
        print(f"  {i}/150  {time.time()-t0:.0f}s", flush=True)

n = len(index)
print()
for tag, label in (("ref", "librosa + torchcrepe"), ("port", "portable front end")):
    t1, t5 = hits[tag]
    print(f"{label:<24s} top1 {t1/n:.4f}  top5 {t5/n:.4f}   ({t1}/{n}, {t5}/{n})")
same1 = sum(r["ref"][0] == r["port"][0] for r in rows)
rank = np.array([[r["ref"][2], r["port"][2]] for r in rows])
print(f"top-1 label agreement: {same1}/{n} ({same1/n:.1%})")
print(f"gold-rank identical:   {(rank[:,0]==rank[:,1]).sum()}/{n}")
json.dump(rows, open(HERE / "eval_full.json", "w"), indent=1)
