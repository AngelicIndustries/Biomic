#!/usr/bin/env python3
"""Builds the Nether supplement of Biomic's pinned modern decoration graph from Regions Unexplored's generated data.

Usage: tools/ru_nether_decoration.py [RU_DATA] [OUT]

RU_DATA defaults to the pinned RU build's datapack (run/reference/regions-unexplored/source/build/resources/
neoforge211/data, revision f5dfe4ee, the graph's own). The supplement holds RU's four Nether biomes with only RU's own
features in their lists (Tartarus gives every Nether biome its ores, lava and glowstone), the closure of the placed and
configured features those reference, and their global order per decoration step, which the graph appends after the
overworld's so no overworld feature changes index. The pinned graph's tables must already hold every tag the features
name. Prints the file's SHA-256 for ModernDecorationGraph.NETHER_SHA256.
"""
import hashlib
import json
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA = sys.argv[1] if len(sys.argv) > 1 else os.path.join(
    ROOT, 'run/reference/regions-unexplored/source/build/resources/neoforge211/data')
OUT = sys.argv[2] if len(sys.argv) > 2 else os.path.join(
    ROOT, 'src/main/resources/assets/biomic/ru_modern_parity/resolved-nether-decoration.json')
MAIN = os.path.join(ROOT, 'src/main/resources/assets/biomic/ru_modern_parity/resolved-modern-decoration.json')
BIOMES = ['blackstone_basin', 'glistering_meadow', 'infernal_holt', 'mycotoxic_undergrowth']
OWN = 'regions_unexplored:'


def path(kind, identifier):
    namespace, name = identifier.split(':', 1)
    return os.path.join(DATA, namespace, 'worldgen', kind, name + '.json')


def load(kind, identifier):
    with open(path(kind, identifier)) as f:
        return json.load(f)


def ids(node):
    """Every namespaced string in a JSON tree."""
    if isinstance(node, str):
        if ':' in node and ' ' not in node and not node.startswith('#'):
            yield node
    elif isinstance(node, dict):
        for value in node.values():
            yield from ids(value)
    elif isinstance(node, list):
        for value in node:
            yield from ids(value)


def tags(node):
    if isinstance(node, str):
        if node.startswith('#'):
            yield node[1:]
    elif isinstance(node, dict):
        for key, value in node.items():
            if key == 'tag' and isinstance(value, str):
                yield value.lstrip('#')
            yield from tags(value)
    elif isinstance(node, list):
        for value in node:
            yield from tags(value)


def main():
    with open(MAIN) as f:
        pinned = json.load(f)
    biomes, lists = {}, []
    for name in BIOMES:
        biome = load('biome', OWN + name)
        biome['features'] = [[feature for feature in step if feature.startswith(OWN)] for step in biome['features']]
        biomes[OWN + name] = biome
        lists.append(biome['features'])
    placed, configured, missing = {}, {}, set()
    pending = [feature for steps in lists for step in steps for feature in step]
    while pending:
        identifier = pending.pop()
        if identifier in placed or identifier in configured:
            continue
        found = False
        for kind, table, pinned_table in (('placed_feature', placed, 'placed_features'),
                                          ('configured_feature', configured, 'configured_features')):
            if identifier.startswith(OWN) and os.path.exists(path(kind, identifier)):
                table[identifier] = load(kind, identifier)
                pending.extend(ids(table[identifier]))
                found = True
            elif identifier in pinned[pinned_table]:
                found = True  # the overworld graph already has it
        if not found and (identifier.startswith(OWN) and '/' in identifier):
            missing.add(identifier)
    if missing:
        sys.exit('missing definitions: ' + ', '.join(sorted(missing)))
    unknown = sorted({tag for table in (placed, configured) for value in table.values() for tag in tags(value)}
                     - set(pinned['block_tags']) - set(pinned['biome_tags']) - set(pinned['fluid_tags']))
    if unknown:
        sys.exit('tags the pinned graph lacks: ' + ', '.join(unknown))
    # Global order per step: each biome's list keeps its order; features first seen earlier come first (FeatureSorter
    # merges the dimension's biomes the same way when their lists agree, as these do).
    steps = []
    for step in range(11):
        order = []
        for features in lists:
            if step >= len(features):
                continue
            previous = -1
            for feature in features[step]:
                if feature in order:
                    previous = order.index(feature)
                else:
                    order.insert(previous + 1, feature)
                    previous += 1
        steps.append(order)
    for features in lists:
        for step, order in enumerate(steps):
            positions = [order.index(feature) for feature in (features[step] if step < len(features) else [])]
            if positions != sorted(positions):
                sys.exit('the biomes disagree on the order of step %d' % step)
    out = {
        'format': 'biomic-modern-decoration-nether-supplement-v1',
        'revision': pinned['revision'],
        'minecraft': pinned['minecraft'],
        'nether_biomes': biomes,
        'placed_features': dict(sorted(placed.items())),
        'configured_features': dict(sorted(configured.items())),
        'features_per_step': steps,
    }
    text = json.dumps(out, indent=1, sort_keys=False) + '\n'
    with open(OUT, 'w') as f:
        f.write(text)
    print(OUT)
    print('biomes %d, placed %d, configured %d, ordered %d' % (len(biomes), len(placed), len(configured),
                                                               sum(len(step) for step in steps)))
    print('sha256', hashlib.sha256(text.encode()).hexdigest())


if __name__ == '__main__':
    main()
