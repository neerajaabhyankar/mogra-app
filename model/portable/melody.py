"""The melody branch without torchcrepe.

torchcrepe is a thin wrapper: a conv net, and around it some framing, a double sigmoid, a
weighted-argmax decode and a dither. The net exports to ONNX or TFLite untouched. What has
to be rewritten for a phone is everything around it, and everything around it is here, in
numpy.

Three details are worth stating because getting any of them wrong is silent:

* **The double sigmoid.** `Crepe.forward` already ends in a sigmoid, and
  `torchcrepe.decode.weighted_argmax` applies another one to the same numbers before
  weighting. That is a quirk of the library, not of CREPE, but the histogram the linear
  model was fitted on came through both, so both are here.

* **The dither is drawn once per process, not once per clip.** It perturbs the 360 bin
  *centres*, and torchcrepe caches the perturbed vector on the function object. Every clip
  in a run therefore shares one draw. Seeded, so it is the same draw every run.

* **Periodicity is read before the decode masking**, from the single sigmoid, at the
  argmax bin.

**On the hop length.** The reference tracks pitch every 10 ms, which is 2001 frames for a
20 s window and 14.7 s of single-threaded CREPE -- the whole pipeline's cost, near enough.
Frames at hop 2h are exactly the even-indexed frames at hop h, so a coarser hop is a
uniform subsample of the same track, and the histogram downstream is a normalised
distribution that does not care how many frames it was built from. Measured on the 150
test clips, fused as usual:

    10 ms   top1 0.4800   top5 0.8200      (the trained setting)
    20 ms   top1 0.4800   top5 0.8200
    40 ms   top1 0.4867   top5 0.8200      <- default here, a quarter of the cost
    80 ms   top1 0.4667   top5 0.8067      <- too coarse, starts losing swars

40 ms is not better than 10 ms in any real sense -- one clip out of 150 is noise -- it is
free. 80 ms is where the histogram stops resolving the distinction between a swar and the
meend leading into it. If anything downstream ever wants the dense track, pass
`hop=HOP_TRAINED`.
"""

import functools
from pathlib import Path

import numpy as np
import torch
import torch.nn.functional as F

SR = 16000
HOP = 640                 # 40 ms -- see the note on hop length below
HOP_TRAINED = 160         # 10 ms, what the histograms were originally computed at
WINDOW = 1024
PITCH_BINS = 360
CENTS_PER_BIN = 20
CENTS_0 = 1997.3794084376191
CONFIDENCE = 0.4
FMIN, FMAX = 50.0, 2000.0
N_BINS = 120
SMOOTH = 1.0
POWER = 0.5
WEIGHTS = Path(__file__).resolve().parent / "crepe_tiny.pth"


class Crepe(torch.nn.Module):
    """CREPE-tiny, structurally identical to torchcrepe's so its checkpoint loads."""

    def __init__(self):
        super().__init__()
        in_channels = [1, 128, 16, 16, 16, 32]
        out_channels = [128, 16, 16, 16, 32, 64]
        self.in_features = 256
        kernel_sizes = [(512, 1)] + 5 * [(64, 1)]
        strides = [(4, 1)] + 5 * [(1, 1)]
        bn = functools.partial(torch.nn.BatchNorm2d, eps=0.0010000000474974513, momentum=0.0)
        for i in range(6):
            setattr(self, f"conv{i + 1}", torch.nn.Conv2d(
                in_channels[i], out_channels[i], kernel_sizes[i], strides[i]))
            setattr(self, f"conv{i + 1}_BN", bn(num_features=out_channels[i]))
        self.classifier = torch.nn.Linear(self.in_features, PITCH_BINS)

    def _layer(self, x, conv, batch_norm, padding=(0, 0, 31, 32)):
        x = F.relu(conv(F.pad(x, padding)))
        return F.max_pool2d(batch_norm(x), (2, 1), (2, 1))

    def forward(self, x):
        x = x[:, None, :, None]
        x = self._layer(x, self.conv1, self.conv1_BN, (0, 0, 254, 254))
        for i in range(2, 6):
            x = self._layer(x, getattr(self, f"conv{i}"), getattr(self, f"conv{i}_BN"))
        x = self._layer(x, self.conv6, self.conv6_BN)
        x = x.permute(0, 2, 1, 3).reshape(-1, self.in_features)
        return torch.sigmoid(self.classifier(x))


@functools.lru_cache(maxsize=1)
def _model(device="cpu"):
    net = Crepe()
    net.load_state_dict(torch.load(WEIGHTS, map_location=device, weights_only=True))
    return net.to(device).eval()


@functools.lru_cache(maxsize=4)
def _bin_cents(dither_seed=0):
    """The 360 bin centres in cents, dithered once -- see the module docstring.

    scipy.stats.triang.rvs(c=0.5, loc=-20, scale=40) is numpy's `triangular(0, 0.5, 1)`
    shifted and scaled, so this reproduces torchcrepe's draw without scipy.
    """
    state = np.random.get_state()
    try:
        np.random.seed(dither_seed)
        noise = -CENTS_PER_BIN + 2 * CENTS_PER_BIN * np.random.triangular(0.0, 0.5, 1.0, PITCH_BINS)
    finally:
        np.random.set_state(state)
    return CENTS_PER_BIN * np.arange(PITCH_BINS, dtype=np.float64) + CENTS_0 + noise


def _frames(y16000, hop=HOP):
    """Centre-padded 1024-sample frames every `hop` samples, mean-centred and scaled."""
    y = np.asarray(y16000, dtype=np.float32)
    total = 1 + len(y) // hop
    y = np.pad(y, WINDOW // 2)
    idx = np.arange(WINDOW)[None, :] + hop * np.arange(total)[:, None]
    f = y[np.minimum(idx, len(y) - 1)].astype(np.float32)
    f -= f.mean(axis=1, keepdims=True)
    return f / np.maximum(1e-10, f.std(axis=1, ddof=1, keepdims=True))


def _frequency_to_bin(hz, round_fn=np.floor):
    cents = 1200.0 * np.log2(np.asarray(hz, dtype=np.float64) / 10.0)
    return int(round_fn((cents - CENTS_0) / CENTS_PER_BIN))


def f0_track(y16000, device="cpu", dither_seed=0, batch_size=512, hop=HOP):
    """(f0 in Hz, voiced mask), one value per `hop` samples."""
    frames = _frames(y16000, hop)
    net = _model(device)
    out = []
    with torch.no_grad():
        for i in range(0, len(frames), batch_size):
            out.append(net(torch.from_numpy(frames[i:i + batch_size]).to(device)).cpu().numpy())
    probs = np.concatenate(out).astype(np.float64)         # (time, 360), one sigmoid

    lo, hi = _frequency_to_bin(FMIN), _frequency_to_bin(FMAX, np.ceil)
    masked = probs.copy()
    masked[:, :lo] = -np.inf
    masked[:, hi:] = -np.inf

    bins = masked.argmax(axis=1)
    periodicity = np.take_along_axis(masked, bins[:, None], axis=1)[:, 0]

    # weighted average of the dithered bin centres over a +-4 bin window, on a second
    # sigmoid of the already-sigmoid probabilities
    start = np.maximum(0, bins - 4)
    end = np.minimum(PITCH_BINS, bins + 5)
    cols = np.arange(PITCH_BINS)[None, :]
    window = (cols >= start[:, None]) & (cols < end[:, None])
    safe = np.where(window, np.clip(masked, -700.0, 700.0), -700.0)
    p2 = np.where(window, 1.0 / (1.0 + np.exp(-safe)), 0.0)
    cents = (p2 * _bin_cents(dither_seed)[None, :]).sum(axis=1) / p2.sum(axis=1)
    f0 = 10.0 * 2.0 ** (cents / 1200.0)
    return f0.astype(np.float32), periodicity >= CONFIDENCE


def histogram(f0_hz, voiced, tonic_hz, n_bins=N_BINS, smooth=SMOOTH, power=POWER):
    """Voiced frames -> a (n_bins,) octave-folded pitch histogram, summing to 1.

    Unchanged from the reference: it was already plain numpy.
    """
    f0 = np.asarray(f0_hz, dtype=float)[np.asarray(voiced, dtype=bool)]
    cents = 1200.0 * np.log2(np.clip(f0, 1e-6, None) / float(tonic_hz))
    cents = cents[np.isfinite(cents)]
    if cents.size < 5:
        return np.zeros(n_bins)
    idx = np.floor((cents % 1200.0) * (n_bins / 1200.0)).astype(int) % n_bins
    H = np.zeros(n_bins)
    np.add.at(H, idx, 1.0)
    if smooth > 0:
        d = np.arange(n_bins)
        d = np.minimum(d, n_bins - d)
        kern = np.exp(-0.5 * (d / smooth) ** 2)
        H = np.maximum(np.real(np.fft.ifft(np.fft.fft(H) * np.fft.fft(kern / kern.sum()))), 0.0)
    H = H ** power
    total = H.sum()
    return H / total if total > 0 else H
