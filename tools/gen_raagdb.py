#!/usr/bin/env python3
"""Build the raag database asset from libmogra's copy of the tanarang data.

    python3 tools/gen_raagdb.py

Reads ~/Repos/libmogra/libmogra/raagdb/tanarang.json (read-only; libmogra is not touched)
and writes app/src/main/assets/raagdb.json — the same shape the web raagfinder uses, so the
two stay in step:

    swarOrder  the twelve swars in Swar-enum order, S=0 .. N=11
    raags      every raag, in alphabetical order of its display name
    bySwar     "g,m,P,..." -> [indices into raags], the exact-set search index

The set for a raag is the swars of its aaroha and avaroha together, saptak marks stripped,
de-duplicated and sorted — which is what `libmogra.raagfinder.parse.index_by_set` does.
"""

import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SOURCE = Path.home() / "Repos/libmogra/libmogra/raagdb/tanarang.json"
OUT = ROOT / "app/src/main/assets/raagdb.json"

SWAR_ORDER = ["S", "r", "R", "g", "G", "m", "M", "P", "d", "D", "n", "N"]
RANK = {s: i for i, s in enumerate(SWAR_ORDER)}

# the attributes shown in the table, in the order tanarang.com shows them
FIELDS = ["aaroha", "avaroha", "mukhyanga", "aarohi_nyas", "avarohi_nyas",
          "vaadi", "samvaadi", "thaat", "prahar"]


def base_swar(token):
    """"`S" -> "S", ",,D" -> "D". The saptak mark is a prefix; the swar is the last char."""
    token = token.strip()
    return token[-1] if token else ""


def swar_set(entry):
    swars = [base_swar(t) for t in entry.get("aaroha", []) + entry.get("avaroha", [])]
    swars = [s for s in swars if s in RANK]
    return sorted(set(swars), key=lambda s: RANK[s])


def main():
    if not SOURCE.exists():
        print(f"cannot find {SOURCE}", file=sys.stderr)
        return 1
    raw = json.loads(SOURCE.read_text())

    raags = []
    for key in sorted(raw, key=lambda k: raw[k].get("name", k).lower()):
        entry = raw[key]
        fields = {}
        for f in FIELDS:
            value = entry.get(f)
            if value in (None, "", []):
                continue
            if f == "mukhyanga":
                # a list of phrases; keep the phrases apart, drop the empty trailing one
                phrases = [" ".join(p) for p in value if p]
                if not phrases:
                    continue
                fields[f] = phrases
            elif isinstance(value, list):
                fields[f] = [" ".join(value)]
            else:
                fields[f] = [str(value)]
        raags.append({
            "key": key,
            "name": entry.get("name", key.title()),
            "swars": swar_set(entry),
            "fields": fields,
        })

    by_swar = {}
    for i, r in enumerate(raags):
        by_swar.setdefault(",".join(r["swars"]), []).append(i)

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(
        {"source": "tanarang.com, via libmogra", "swarOrder": SWAR_ORDER,
         "raags": raags, "bySwar": by_swar},
        ensure_ascii=False, separators=(",", ":")) + "\n")

    sizes = sorted(len(v) for v in by_swar.values())
    print(f"{OUT.relative_to(ROOT)}: {len(raags)} raags, {len(by_swar)} distinct swar sets, "
          f"{OUT.stat().st_size // 1024} KB")
    print(f"  largest set of raags sharing one scale: {sizes[-1]}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
