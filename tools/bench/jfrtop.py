import sys, re, subprocess, collections
JFR=__import__("os").environ.get("JFR","jfr")
f=sys.argv[1]
p=subprocess.Popen([JFR,"print","--events","jdk.ExecutionSample","--stack-depth","1",f],stdout=subprocess.PIPE,text=True,errors='replace')
top=collections.Counter(); total=0
for line in p.stdout:
    s=line.strip()
    if s and s[0].isalpha() and '(' in s and 'line:' in s:
        total+=1
        m=re.match(r'([\w.$]+)\.([\w$<>]+)\(',s)
        cls=m.group(1) if m else s
        if re.search(r'internal\.terra\.noise|Seismic|samplers|OpenSimplex|Perlin|FastNoise|Simplex|Cellular|NoiseFunction|FBm|Fractal',cls): k='noise sampler math'
        elif re.search(r'paralithic|Expression|Paralithic',cls): k='expression evaluation'
        elif re.search(r'TerraPack|TerraV2|Terrascript|terrascript|dfsek',cls): k='Terra runtime/interpreter bookkeeping'
        elif re.search(r'fastutil|java\.util|jdk\.internal|java\.lang',cls): k='collections/JDK (maps, bitsets, copies)'
        elif 'industries.angelic' in cls: k='other biomic'
        else: k='other'
        top[k]+=1
for k,v in top.most_common(): print(f"{v*100/total:5.1f}% {k}")
