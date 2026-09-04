"""CREPE at 10 ms costs 14.7 s per 20 s window single-threaded. Does it need 10 ms?

Frames at hop 2h are exactly the even-indexed frames at hop h -- same padding, same
window, centres at multiples of the hop. So one pass at 10 ms gives the 20/40/80 ms
tracks for free by subsampling, and the histogram is the only thing downstream of them.
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
cfg, raags, w = model.config, model.raags, None
w = cfg["melody_weight"]
index = [r for r in json.loads((D / "cache/index.json").read_text()) if r["split"] == "test"]
STRIDES = [1, 2, 4, 8]

kernels = {}
hits = {s: [0, 0] for s in STRIDES}
hits["ref"] = [0, 0]
t0 = time.time()
for i, r in enumerate(index, 1):
    raag, name = r["clip_id"].split("/")
    stem = f"{raag}__{name.rsplit('.', 1)[0]}.npy"
    gold, tonic = raags.index(r["raag"]), r["tonic_hz"]

    y, sr = audio.load(AUDIO / r["clip_id"])
    win = audio.windows(np.asarray(y, dtype=np.float32), sr)[0]

    y22 = frontend.resample(win, sr, frontend.SR_CQT)
    y22 = (y22 / (float(np.max(np.abs(y22))) or 1.0)).astype(np.float32)
    y22 = audio.fit_length(y22, int(round(frontend.SR_CQT * 20.0)))
    key = round(anchor_fmin(tonic), 6)
    if key not in kernels:
        kernels[key] = frontend.cqt_kernel(anchor_fmin(tonic))
    cqt_x = frontend.features(y22, tonic, kernel=kernels[key])
    with torch.no_grad():
        p_cqt = _softmax(model.net(torch.from_numpy(cqt_x)[None].float())[0].numpy(),
                         cfg["temperature_cqt"])

    f0, voiced = melody.f0_track(frontend.resample(win, sr, melody.SR))
    for s in STRIDES:
        h = melody.histogram(f0[::s], voiced[::s], tonic)
        p = (1 - w) * p_cqt + w * _softmax(model.linear.scores(h), cfg["temperature_melody"])
        order = np.argsort(-p)
        hits[s][0] += order[0] == gold
        hits[s][1] += gold in order[:5]

    ref_hist = np.load(D / "cache/melody" / stem).astype(np.float64)
    ref_cqt = np.load(D / "cache/cqt" / stem).astype(np.float32)[None]
    with torch.no_grad():
        pr = ((1 - w) * _softmax(model.net(torch.from_numpy(ref_cqt)[None].float())[0].numpy(),
                                 cfg["temperature_cqt"])
              + w * _softmax(model.linear.scores(ref_hist), cfg["temperature_melody"]))
    order = np.argsort(-pr)
    hits["ref"][0] += order[0] == gold
    hits["ref"][1] += gold in order[:5]
    if i % 30 == 0:
        print(f"  {i}/150  {time.time()-t0:.0f}s", flush=True)

n = len(index)
print()
t1, t5 = hits["ref"]
print(f"{'librosa + torchcrepe, 10 ms':<32s} top1 {t1/n:.4f}  top5 {t5/n:.4f}")
for s in STRIDES:
    t1, t5 = hits[s]
    print(f"{'portable, ' + str(10*s) + ' ms hop':<32s} top1 {t1/n:.4f}  top5 {t5/n:.4f}"
          f"   crepe cost /{s}")
