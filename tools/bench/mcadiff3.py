"""mcadiff3.py WORLD_A WORLD_B [REGION_DIR]: compare populated chunks by block name+metadata (independent of numeric
IDs); REGION_DIR defaults to region (DIM-1/region for the Nether)."""
import collections, os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from mcworld import chunks, block_names, section_cells

SUB = sys.argv[3] if len(sys.argv) > 3 else 'region'
A, B = chunks(sys.argv[1], SUB), chunks(sys.argv[2], SUB)
na, nb = block_names(sys.argv[1]), block_names(sys.argv[2])
both = [k for k in A if k in B and A[k]['Level'].get('TerrainPopulated') and B[k]['Level'].get('TerrainPopulated')]
diff_chunks = diff_cells = 0; kinds = collections.Counter(); examples = []
for k in both:
    sa = {s['Y']: s for s in A[k]['Level']['Sections']}; sb = {s['Y']: s for s in B[k]['Level']['Sections']}
    n = 0
    for y in set(sa) | set(sb):
        ca = section_cells(sa[y], na) if y in sa else [('minecraft:air', 0)] * 4096
        cb = section_cells(sb[y], nb) if y in sb else [('minecraft:air', 0)] * 4096
        for i in range(4096):
            if ca[i] != cb[i]:
                n += 1; kinds[(ca[i][0], cb[i][0])] += 1
                if len(examples) < 10:
                    examples.append((k[0] * 16 + (i & 15), y * 16 + (i >> 8), k[1] * 16 + ((i >> 4) & 15), ca[i], cb[i]))
    if n: diff_chunks += 1; diff_cells += n
print(f'chunks {len(both)}, differing chunks {diff_chunks}, differing cells {diff_cells}')
for (a, b), v in kinds.most_common(15): print(f'  {v:6d}  {a} -> {b}')
for e in examples: print('  ', e)
