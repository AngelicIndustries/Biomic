import sys, re, subprocess, collections
JFR=__import__("os").environ.get("JFR","jfr")
CATS=[
 ("terra-interpreter(Skylands/Tartarus TerraPack)", re.compile(r'TerraPack|TerraV2|\.internal\.terra\.|com\.dfsek|Terrascript|TerraScript')),
 ("ultimate-caves", re.compile(r'internal\.ultimatecaves')),
 ("yung-caves", re.compile(r'internal\.yungbettercaves')),
 ("yung-mineshafts", re.compile(r'yungbettermineshafts')),
 ("regional-caves/cave-ecology", re.compile(r'world\.cave\.|RegionalCave|HostedCave|CaveComposition')),
 ("abyssal/alexscaves", re.compile(r'Abyssal|alexscaves')),
 ("floating-islands", re.compile(r'FloatingIsland|HostedIsland')),
 ("skylands(other)", re.compile(r'world\.skylands\.')),
 ("tartarus(other)", re.compile(r'world\.tartarus\.')),
 ("origen-terrain/features", re.compile(r'Origen')),
 ("modern(RU) engine", re.compile(r'world\.modern\.')),
 ("bop/flora compat", re.compile(r'compat\.(biome|flora)')),
 ("lighting", re.compile(r'func_150809_p|generateSkylightMap|func_147451_t|updateLightByType|func_147463_c|relightBlock|Relight|Lighting|propagateSkylight|func_76594_o')),
 ("chunk IO/NBT", re.compile(r'AnvilChunkLoader|RegionFile|CompressedStreamTools|NBTTagCompound\.write')),
 ("network/packets", re.compile(r'S26PacketMapChunkBulk|S21PacketChunkData|Deflater|NetworkManager')),
]
def run(f, depth=96):
    p=subprocess.Popen([JFR,"print","--events","jdk.ExecutionSample","--stack-depth",str(depth),f],stdout=subprocess.PIPE,text=True,errors='replace')
    total=0; bythread=collections.Counter(); cat=collections.Counter(); catthread=collections.Counter(); excl=collections.Counter()
    frames=[]; thread=None
    def flush():
        nonlocal total
        if not frames: return
        total+=1
        t=thread or '?'
        t=re.sub(r'[ #-]*\d+$','',t)
        bythread[t]+=1
        stack="\n".join(frames)
        first=None
        for name,rx in CATS:
            if rx.search(stack):
                cat[name]+=1
                if first is None: first=name
        # exclusive: attribute to category of the deepest (closest to top) matching frame
        ex=None
        for fr in frames:
            for name,rx in CATS:
                if rx.search(fr): ex=name; break
            if ex: break
        excl[ex or ('biomic-other' if 'industries.angelic' in stack else 'non-biomic')]+=1
    for line in p.stdout:
        s=line.strip()
        if s.startswith('jdk.ExecutionSample'):
            flush(); frames=[]; thread=None
        elif s.startswith('sampledThread ='):
            m=re.search(r'"([^"]*)"',s); thread=m.group(1) if m else s
        elif s and (s[0].isalpha()) and ('(' in s) and 'line:' in s:
            frames.append(s)
    flush()
    print(f"== {f.split('/')[-3:]}  samples={total}")
    print(" threads:", ", ".join(f"{k}:{v*100//total}%" for k,v in bythread.most_common(8)))
    print(" inclusive (any frame):")
    for k,v in cat.most_common(): print(f"   {v*100/total:5.1f}%  {k}")
    print(" exclusive (top-most matching frame):")
    for k,v in excl.most_common(): print(f"   {v*100/total:5.1f}%  {k}")
for f in sys.argv[1:]: run(f)
