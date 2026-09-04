"""Sa arithmetic -- a straight copy of raag_fusion.tonic's octave folding, no librosa."""
import numpy as np


def canonical(tonic_hz, lo_hz=110.0):
    f = float(tonic_hz)
    while f < lo_hz:
        f *= 2.0
    while f >= 2.0 * lo_hz:
        f /= 2.0
    return f


def anchor_fmin(tonic_hz, octaves_below=1, lo_hz=110.0):
    return canonical(tonic_hz, lo_hz) / (2.0 ** octaves_below)
