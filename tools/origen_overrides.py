#!/usr/bin/env python3
"""Writes src/main/resources/assets/biomic/origen/palette-overrides.json: Biomic's quiet skins for Origen.

The imported terrain-density.json stays as upstream. This script derives replacement palettes from it:
- hand-made replacements for the worst per-block mixes (terrain study, proposal 3.1),
- and three mechanical rules for every other palette that has no air or water in it (those carve rivers and pools):
  a dithering warp (white or gaussian noise) becomes a smooth one, a layer chosen per block by white noise becomes its
  most common material, and translucent ice becomes packed ice.
Run from the repository root after changing it; the JSON is what ships.
"""
import copy
import json

SOURCE = 'src/main/resources/assets/biomic/origen/terrain-density.json'
TARGET = 'src/main/resources/assets/biomic/origen/palette-overrides.json'

GRAINY = {'WHITE_NOISE', 'POSITIVE_WHITE_NOISE', 'GAUSSIAN'}
STRUCTURAL = {'minecraft:air', 'minecraft:water', 'minecraft:lava'}
# Ice that floats on water, left translucent.
KEEP_ICE = {'FROZEN_OCEAN'}
# Palettes whose noise is not a surface: bedrock's ragged floor.
UNTOUCHED = {'BEDROCK'}


def mat(name):
    return name if ':' in name else 'minecraft:' + name


def layer(depth, *materials, sampler=None):
    out = {'layers': depth, 'materials': [{'material': mat(m), 'weight': w} for m, w in materials]}
    if sampler:
        out['sampler'] = sampler
    return out


def patches(freq, salt, octaves=2):
    return {'type': 'FBM', 'octaves': octaves, 'sampler': {'type': 'OPEN_SIMPLEX_2', 'frequency': freq, 'salt': salt}}


def veins(freq, salt, vein_freq, vein_salt, floor):
    """Ordered ramp on smooth patches, with thin veins of the first material where a larger noise crosses zero."""
    return {'type': 'EXPRESSION', 'dimensions': 3,
            'expression': f'if(abs(v(x, z)) < 0.02, -1, max(n(x, y, z), {floor}))',
            'samplers': {'v': {'dimensions': 2, **patches(vein_freq, vein_salt)},
                         'n': {'dimensions': 3, **patches(freq, salt)}}}


def strata(source):
    """Origen's own rocky-slant banding (cellular bands drifting with a slow height offset)."""
    return copy.deepcopy(source['ROCKY_SLANT']['layers'][0]['sampler'])


def kinds(node, acc):
    if isinstance(node, dict):
        if 'type' in node:
            acc.add(node['type'])
        for value in node.values():
            kinds(value, acc)
    elif isinstance(node, list):
        for value in node:
            kinds(value, acc)
    return acc


def smooth_warps(node, salt):
    """Every domain warp driven by white or gaussian noise becomes a smooth 16-block wiggle of three blocks."""
    if isinstance(node, dict):
        if node.get('type') == 'DOMAIN_WARP' and node.get('warp', {}).get('type') in GRAINY:
            node['warp'] = {'type': 'OPEN_SIMPLEX_2', 'frequency': 0.06, 'salt': salt}
            node['amplitude'] = 3
        for value in node.values():
            smooth_warps(value, salt + 1)
    elif isinstance(node, list):
        for value in node:
            smooth_warps(value, salt + 1)


def mechanical(name, palette):
    """The three rules; returns the palette changed, or None when nothing applied."""
    if name in UNTOUCHED:
        return None
    if any(m['material'] in STRUCTURAL for layer_ in palette['layers'] for m in layer_['materials']):
        return None
    out = copy.deepcopy(palette)
    smooth_warps(out, 9100 + sum(map(ord, name)))
    for layer_ in out['layers']:
        sampler = layer_.get('sampler', out['sampler'])
        materials = layer_['materials']
        if sampler.get('type') in GRAINY and len({m['material'] for m in materials}) > 1:
            weights = {}
            for m in materials:
                weights[m['material']] = weights.get(m['material'], 0) + m['weight']
            top = max(weights, key=lambda k: (weights[k], -list(weights).index(k)))
            layer_['materials'] = [{'material': top, 'weight': 1}]
            layer_.pop('sampler', None)
        if name not in KEEP_ICE:
            for m in layer_['materials']:
                if m['material'] == 'minecraft:ice':
                    m['material'] = 'minecraft:packed_ice'
    return out if out != palette else None


def main():
    source = json.load(open(SOURCE))
    palettes = source['palettes']
    out = {}
    for name, palette in palettes.items():
        changed = mechanical(name, palette)
        if changed is not None:
            out[name] = changed

    rocky = strata(palettes)
    stone_strata = {'sampler': {'type': 'CONSTANT', 'value': 0},
                    'layers': [layer(1, ('stone', 5), ('tuff', 2), sampler=rocky)]}
    hand = {
        # Stonegate Seas: moss and grass on the flat tops, over stone; the faces are stone with tuff bands.
        'MOSS_CAPS': {'sampler': patches(0.025, 9201),
                      'layers': [layer(1, ('moss_block', 6), ('grass_block', 4)), layer(1, ('stone', 1))]},
        'STONE_STRATA': stone_strata,
        'GREEN_STONE_MIX': copy.deepcopy(stone_strata),
        'DARK_STONE_MIX': copy.deepcopy(stone_strata),
        # Ice cliffs: packed ice and snow in patches with thin blue-ice veins, no mud and no translucent ice.
        'ICE_STONE_MIX_DARK': {'sampler': {'type': 'CONSTANT', 'value': 0}, 'layers': [
            layer(2, ('blue_ice', 1), ('packed_ice', 10), ('snow_block', 10), sampler=veins(0.04, 9202, 0.015, 9203, -0.8)),
            layer(1, ('packed_ice', 1))]},
        'ICE_STONE_MIX': {'sampler': {'type': 'CONSTANT', 'value': 0}, 'layers': [
            layer(2, ('blue_ice', 1), ('snow_block', 11), ('packed_ice', 8), sampler=veins(0.04, 9204, 0.015, 9205, -0.8)),
            layer(1, ('packed_ice', 1))]},
        'ICE_STONE_MIX_LIGHT': {'sampler': {'type': 'CONSTANT', 'value': 0}, 'layers': [
            layer(2, ('blue_ice', 1), ('snow_block', 11), ('packed_ice', 8), sampler=veins(0.04, 9206, 0.015, 9207, -0.8)),
            layer(1, ('packed_ice', 1))]},
        # Calcite slopes: calcite with a rare large diorite patch, not white-noise grain.
        'CALCITE_DIORITE_MIX': {'sampler': patches(0.015, 9208),
                                'layers': [layer(2, ('calcite', 3), ('diorite', 1)), layer(1, ('stone', 1))]},
    }
    # Shapes and polish (terrain study 3.5 and 3.6).
    rocky = palettes['ROCKY_SLANT']['layers'][0]['sampler']
    hand.update({
        # Frozen seas: an ice sheet broken by thin open leads and a few open bays, not round floes.
        'FROZEN_OCEAN': {'sampler': {'type': 'CONSTANT', 'value': 0}, 'layers': [
            layer(1, ('water', 1), ('ice', 1), sampler={
                'type': 'EXPRESSION', 'dimensions': 2,
                'expression': 'if(bays(x, z) < -0.45, -1, if(abs(leads(x, z)) < 0.035, -1, 1))',
                'samplers': {'bays': {'dimensions': 2, **patches(0.004, 9301)},
                             'leads': {'dimensions': 2, **patches(1 / 64, 9302)}}}),
            layer(1, ('water', 1))]},
        # Badlands Balconies' low ground: red terracotta and red sand in patches, not leopard rings.
        'WOODED_BADLANDS_LOW': {'sampler': patches(0.03, 9303), 'layers': [
            layer(1, ('red_terracotta', 1), ('red_sand', 1)), layer(1, ('red_sand', 1)), layer(1, ('stone', 1))]},
        # Banded slopes: three materials in lightness order (diorite and mossy cobble were the loud extremes).
        'ROCKY_SLANT': {'sampler': {'type': 'CONSTANT', 'value': 0},
                        'layers': [layer(1, ('tuff', 3), ('stone', 2), ('andesite', 2), sampler=copy.deepcopy(rocky))]},
        'ROCKY_SLANT_DARK': {'sampler': {'type': 'CONSTANT', 'value': 0},
                             'layers': [layer(1, ('blackstone', 3), ('deepslate', 3), ('stone', 2), sampler=copy.deepcopy(rocky))]},
        # The deep dark sea floor: gravel, andesite and basalt in its cells (was seven materials).
        'OCEAN_DARK_FLOOR_DEEP': {'sampler': copy.deepcopy(palettes['OCEAN_DARK_FLOOR_DEEP']['sampler']), 'layers': [
            layer(5, ('gravel', 5), ('andesite', 3), ('basalt', 3)), layer(1, ('deepslate', 1))]},
    })
    # Fern Fissures, Biomic's temperate canyon (biome study): Origen's canyon landform with grass rims, grey strata
    # walls in the canyons' own banding and a floor of grass, moss and coarse dirt in patches.
    strata_bands = copy.deepcopy(palettes['TERRACOTTA_CANYON_HIGH']['sampler'])
    grey_strata = [('stone', 2), ('andesite', 1), ('tuff', 2), ('stone', 2), ('andesite', 1), ('deepslate', 1),
                   ('stone', 2), ('tuff', 1)]
    hand.update({
        'FERN_CANYON_HIGH': {'sampler': copy.deepcopy(strata_bands),
                             'layers': [layer(1, ('grass_block', 1)), layer(40, *grey_strata)]},
        'FERN_CANYON': {'sampler': copy.deepcopy(strata_bands), 'layers': [layer(40, *grey_strata)]},
        'FERN_CANYON_FLOOR': {'sampler': copy.deepcopy(palettes['SAND_CANYON']['sampler']), 'layers': [
            layer(3, ('grass_block', 5), ('moss_block', 2), ('coarse_dirt', 2), ('stone', 1))]},
    })
    out.update(hand)

    profiles = source['profiles']
    stonegate = copy.deepcopy(profiles['STONEGATE_SEAS']['surface'])
    for band in stonegate['palette']:
        if band['palette'] == 'GREEN_STONE_MIX':
            band['palette'] = 'MOSS_CAPS'
    stonegate['slant'][0]['threshold'] = 0.45
    for band in stonegate['slant'][0]['palette']:
        if band['palette'] == 'GREEN_STONE_MIX':
            band['palette'] = 'STONE_STRATA'
    black_forest = copy.deepcopy(profiles['BLACK_FOREST']['surface'])
    # Every terrace riser showed a grass, dirt and stone layer cake: steep faces are rock.
    black_forest['slant'] = [{'threshold': 0.45, 'palette': [
        {'max_y': 319, 'palette': 'STONE_STRATA'}] + [copy.deepcopy(b) for b in black_forest['palette'][1:]]}]
    black_forest['slant_depth'] = 4

    fern = copy.deepcopy(profiles['SANDY_SPLITS'])
    fern['surface']['palette'][0]['palette'] = 'FERN_CANYON_HIGH'
    fern['surface']['palette'][1]['palette'] = 'FERN_CANYON_FLOOR'
    fern['surface']['slant'][0]['palette'][0]['palette'] = 'FERN_CANYON_HIGH'
    fern['surface']['slant'][0]['palette'][1]['palette'] = 'FERN_CANYON'
    for key in ('palette_owner', 'slant_owner'):
        fern['surface'][key] = 'FERN_FISSURES'
    for key in ('palette_source', 'slant_source'):
        fern['surface'][key] = 'biomic: tools/origen_overrides.py'
    fern['features'].update({'flora': ['GRASS'], 'trees': ['SPARSE_TEMPERATE_TREE_PATCHES'],
                             'landforms': ['MOSSY_BOULDERS', 'LARGE_LEAF_VINES_CANYON']})

    result = {
        'about': 'Biomic changes to Origen (CC-BY-4.0, by Rearth), applied when the manifest loads; the imported data '
                 'stays as upstream. Generated by tools/origen_overrides.py.',
        'palettes': out,
        'surfaces': {'STONEGATE_SEAS': stonegate, 'BLACK_FOREST': black_forest},
        # New profiles (appended to OrigenBiomeProfile): the temperate canyon.
        'profiles': {'FERN_FISSURES': fern},
        # Andesite lumps stuck on stone cliffs: stone lumps only.
        'feature_structures': {'TEXTURED_STONE_SLANT': [{'structure': 'stone_wall_boulder', 'weight': 1}]},
        # Tilted plateaus: neighbouring cells' planes blend over 28 blocks instead of meeting in a sheer wall (review,
        # 2026-10-24: "the giant wall here looks unnatural"). Base -> cellular sampler -> blend width in blocks.
        'cellular_edges': {'EQ_TILTED_PLATEAU': {'biomeTilt': 28}},
        # Features added to an existing profile. Sandy Splits' mesa tops are grass with only cacti (which need sand)
        # in its lists; grass and sparse trees grow there now, as on modern wooded badlands (review, 2026-10-24:
        # "kinda weird to not have more vegetation").
        'profile_features': {'SANDY_SPLITS': {'flora': ['GRASS'], 'trees': ['SPARSE_TEMPERATE_TREE_PATCHES']}},
        # Rock deposits stay in caves but never show on faces open to the sky.
        'ores_hidden_from_sky': ['ANDESITE_DEPOSIT', 'DIORITE_DEPOSIT', 'GRANITE_DEPOSIT', 'TUFF_DEPOSIT',
                                 'GRAVEL_DEPOSIT', 'DIRT_DEPOSIT', 'DRIPSTONE_DEPOSIT'],
    }
    with open(TARGET, 'w') as f:
        json.dump(result, f, indent=1, sort_keys=True)
        f.write('\n')
    print(f'{len(out)} palettes ({len(hand)} by hand), {TARGET}')


if __name__ == '__main__':
    main()
