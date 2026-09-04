"""Export both networks to TorchScript for PyTorch Mobile, and check they still agree.

TorchScript rather than ONNX because it can be verified here and now -- onnx and
onnxruntime are not installed in this venv, and I am not installing into it uninvited.
The nets are plain conv stacks, so an ONNX or TFLite route stays open.
"""
import sys, time
from pathlib import Path
import numpy as np, torch

HERE = Path(__file__).resolve().parent
D = Path("/Users/neerajaabhyankar/Repos/icm-shruti-analysis/raag-identifier/best-model-09-01")
OUT = HERE / "exported"; OUT.mkdir(exist_ok=True)
sys.path.insert(0, str(HERE)); sys.path.insert(0, str(D))
from raag_fusion import RaagIdentifier                           # noqa: E402
from portable import melody                                      # noqa: E402

torch.manual_seed(0)
rng = np.random.default_rng(0)

# ---- branch 1: the CQT net -------------------------------------------------
model = RaagIdentifier.load(device="cpu")
net = model.net.eval()
x = torch.from_numpy(rng.random((1, 1, 144, 431)).astype(np.float32))
with torch.no_grad():
    want = net(x)
ts = torch.jit.trace(net, x, strict=False)
ts = torch.jit.freeze(ts)
with torch.no_grad():
    got = ts(x)
print(f"cqt_net    max|d| = {(want - got).abs().max().item():.3e}")
ts._save_for_lite_interpreter(str(OUT / "cqt_net.ptl"))

# ---- branch 2: CREPE tiny --------------------------------------------------
crepe = melody._model("cpu")
f = torch.from_numpy(rng.standard_normal((64, 1024)).astype(np.float32))
with torch.no_grad():
    want_c = crepe(f)
tsc = torch.jit.freeze(torch.jit.trace(crepe, f, strict=False))
with torch.no_grad():
    got_c = tsc(f)
    # a different batch size, to prove the trace is not shape-locked
    f2 = torch.from_numpy(rng.standard_normal((501, 1024)).astype(np.float32))
    d2 = (crepe(f2) - tsc(f2)).abs().max().item()
print(f"crepe_tiny max|d| = {(want_c - got_c).abs().max().item():.3e}  "
      f"(batch 501: {d2:.3e})")
tsc._save_for_lite_interpreter(str(OUT / "crepe_tiny.ptl"))

# ---- what one 20 s window costs, single-threaded ---------------------------
torch.set_num_threads(1)
frames = torch.from_numpy(rng.standard_normal((2001, 1024)).astype(np.float32))
with torch.no_grad():
    t0 = time.time(); tsc(frames); t_crepe = time.time() - t0
    t0 = time.time(); ts(x); t_cqt = time.time() - t0
print(f"\nsingle-threaded, one 20 s window:  crepe {t_crepe:.2f}s   cqt net {t_cqt:.2f}s")
for p in sorted(OUT.iterdir()):
    print(f"  {p.name:<16s} {p.stat().st_size/1e6:.2f} MB")
