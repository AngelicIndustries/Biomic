"""Minimal 1.7.10 world reader: region chunks, NBT, block-ID registry and section block names."""
import glob, gzip, hashlib, io, os, struct, zlib


def _tag(f, t):
    if t == 1: return struct.unpack('>b', f.read(1))[0]
    if t == 2: return struct.unpack('>h', f.read(2))[0]
    if t == 3: return struct.unpack('>i', f.read(4))[0]
    if t == 4: return struct.unpack('>q', f.read(8))[0]
    if t == 5: return struct.unpack('>f', f.read(4))[0]
    if t == 6: return struct.unpack('>d', f.read(8))[0]
    if t == 7: return f.read(struct.unpack('>i', f.read(4))[0])
    if t == 8: return f.read(struct.unpack('>H', f.read(2))[0]).decode('utf-8', 'replace')
    if t == 9:
        et = f.read(1)[0]; n = struct.unpack('>i', f.read(4))[0]
        return [_tag(f, et) for _ in range(n)]
    if t == 10:
        d = {}
        while True:
            tt = f.read(1)[0]
            if tt == 0: return d
            name = f.read(struct.unpack('>H', f.read(2))[0]).decode('utf-8', 'replace')
            d[name] = _tag(f, tt)
    if t == 11: return f.read(4 * struct.unpack('>i', f.read(4))[0])
    if t == 12: return f.read(8 * struct.unpack('>i', f.read(4))[0])
    raise ValueError(t)


def nbt(data):
    f = io.BytesIO(data); t = f.read(1)[0]; f.read(struct.unpack('>H', f.read(2))[0])
    return _tag(f, t)


def chunks(world, sub='region'):
    """{(chunkX, chunkZ): root compound} for every chunk in world/region (or another dimension's, e.g. DIM-1/region)."""
    out = {}
    for path in glob.glob(os.path.join(world, sub, '*.mca')):
        data = open(path, 'rb').read()
        for i in range(1024):
            offset = struct.unpack('>I', b'\0' + data[i * 4:i * 4 + 3])[0]
            if not offset: continue
            o = offset * 4096; length = struct.unpack('>i', data[o:o + 4])[0]
            raw = data[o + 5:o + 4 + length]
            raw = zlib.decompress(raw) if data[o + 4] == 2 else gzip.decompress(raw)
            root = nbt(raw); level = root['Level']
            out[(level['xPos'], level['zPos'])] = root
    return out


def block_names(world):
    """numeric block id -> registry name, from level.dat's FML ItemData."""
    root = nbt(gzip.decompress(open(os.path.join(world, 'level.dat'), 'rb').read()))
    return {e['V']: e['K'][1:] for e in root.get('FML', {}).get('ItemData', []) if e['K'].startswith('\x01')}


def section_cells(section, names):
    """4096 (name, meta) tuples in y,z,x order for one section."""
    blocks, add, data = section['Blocks'], section.get('Add'), section['Data']
    out = []
    for i in range(4096):
        high = (add[i >> 1] >> ((i & 1) * 4)) & 15 if add is not None else 0
        out.append((names.get(blocks[i] | high << 8, f'#{blocks[i] | high << 8}'), (data[i >> 1] >> ((i & 1) * 4)) & 15))
    return out
