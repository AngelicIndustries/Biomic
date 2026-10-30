#!/usr/bin/env python3
"""Compare two worlds' populated chunks by block name and metadata, independent of numeric ids.

Usage: tools/bench/worlddiff.py WORLD_A WORLD_B [--region DIM-1/region] [--rename OLD=NEW ...] [--examples 12]
       [--chunks]

--rename maps a block name of WORLD_A to the name it has in WORLD_B (a block that moved to another mod), so a move
that changes nothing else compares equal. Exit status 0 when no populated cell differs.
"""
import argparse
import collections
import glob
import gzip
import os
import struct
import sys
import zlib

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcworld import block_names, nbt  # noqa: E402


def populated(world, sub):
    """{(cx, cz): {section y: (block ids, metadata)}} of every populated chunk."""
    out = {}
    for path in glob.glob(os.path.join(world, sub, '*.mca')):
        data = open(path, 'rb').read()
        for i in range(1024):
            offset = struct.unpack('>I', b'\0' + data[i * 4:i * 4 + 3])[0]
            if not offset:
                continue
            o = offset * 4096
            length = struct.unpack('>i', data[o:o + 4])[0]
            raw = data[o + 5:o + 4 + length]
            raw = zlib.decompress(raw) if data[o + 4] == 2 else gzip.decompress(raw)
            level = nbt(raw)['Level']
            if not level.get('TerrainPopulated'):
                continue
            sections = {}
            for s in level['Sections']:
                ids = np.frombuffer(s['Blocks'], dtype=np.uint8).astype(np.int32)
                if 'Add' in s:
                    add = np.frombuffer(s['Add'], dtype=np.uint8)
                    high = np.empty(4096, np.int32)
                    high[0::2], high[1::2] = add & 15, add >> 4
                    ids |= high << 8
                packed = np.frombuffer(s['Data'], dtype=np.uint8)
                meta = np.empty(4096, np.int32)
                meta[0::2], meta[1::2] = packed & 15, packed >> 4
                sections[s['Y']] = (ids, meta)
            out[(level['xPos'], level['zPos'])] = sections
    return out


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('a')
    parser.add_argument('b')
    parser.add_argument('--region', default='region')
    parser.add_argument('--rename', nargs='*', default=[])
    parser.add_argument('--examples', type=int, default=12)
    parser.add_argument('--chunks', action='store_true', help='list each differing chunk with its count')
    args = parser.parse_args()
    rename = dict(r.split('=', 1) for r in args.rename)

    names_a, names_b = block_names(args.a), block_names(args.b)
    vocabulary = {}

    def index(name):
        return vocabulary.setdefault(name, len(vocabulary))

    def translate(names, renames):
        size = max(names, default=0) + 1
        table = np.full(max(size, 4096), index('minecraft:air'), np.int32)
        for numeric, name in names.items():
            table[numeric] = index(renames.get(name, name))
        table[0] = index('minecraft:air')
        return table

    table_a, table_b = translate(names_a, rename), translate(names_b, {})
    by_index = None
    a, b = populated(args.a, args.region), populated(args.b, args.region)
    common = sorted(set(a) & set(b))
    differing_chunks = differing_cells = 0
    kinds = collections.Counter()
    examples = []
    per_chunk = []
    empty = (np.zeros(4096, np.int32), np.zeros(4096, np.int32))
    for key in common:
        sa, sb = a[key], b[key]
        changed = 0
        for y in sorted(set(sa) | set(sb)):
            ida, ma = sa.get(y, empty)
            idb, mb = sb.get(y, empty)
            na, nb = table_a[ida], table_b[idb]
            cells = np.nonzero((na != nb) | (ma != mb))[0]
            if not len(cells):
                continue
            changed += len(cells)
            if by_index is None:
                by_index = {v: k for k, v in vocabulary.items()}
            for c in cells[:64]:
                pair = (by_index[int(na[c])], by_index[int(nb[c])])
                kinds[pair] += 1
                if len(examples) < args.examples:
                    x, z = key[0] * 16 + (c & 15), key[1] * 16 + ((c >> 4) & 15)
                    examples.append((x, y * 16 + (c >> 8), z, pair[0], int(ma[c]), pair[1], int(mb[c])))
        if changed:
            per_chunk.append((key, changed))
            differing_chunks += 1
            differing_cells += changed
    print(f'populated chunks in both {len(common)} (only A {len(set(a) - set(b))}, only B {len(set(b) - set(a))}); '
          f'differing chunks {differing_chunks}, differing cells {differing_cells}')
    for (before, after), n in kinds.most_common(12):
        print(f'  {n:7d}  {before} -> {after}')
    for e in examples:
        print('   ', tuple(int(v) if isinstance(v, (int, np.integer)) else v for v in e))
    if args.chunks:
        for (cx, cz), n in per_chunk:
            print(f'  chunk {cx},{cz} (blocks {cx * 16},{cz * 16}): {n}')
    sys.exit(0 if differing_cells == 0 else 1)


if __name__ == '__main__':
    main()
