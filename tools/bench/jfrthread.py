import sys, re, subprocess, collections
JFR=__import__("os").environ.get("JFR","jfr")
f=sys.argv[1]; want=sys.argv[2]
p=subprocess.Popen([JFR,"print","--events","jdk.ExecutionSample","--stack-depth","120",f],stdout=subprocess.PIPE,text=True,errors='replace')
frames=[];thread=None;total=0
leaf=collections.Counter(); incl=collections.Counter()
def flush():
    global total
    if not frames or thread is None or want not in thread: return
    total+=1
    leaf[re.sub(r'\(.*','',frames[0])]+=1
    seen=set()
    for fr in frames:
        m=re.match(r'([\w.$]+)\.([\w$<>]+)\(',fr)
        if not m: continue
        cls=m.group(1)
        if 'industries.angelic' in cls or 'yung' in cls.lower():
            k=cls.split('.')[-1].split('$')[0]+'.'+m.group(2)
            if k not in seen: seen.add(k); incl[k]+=1
for line in p.stdout:
    s=line.strip()
    if s.startswith('jdk.ExecutionSample'):
        flush(); frames=[]; thread=None
    elif s.startswith('sampledThread ='):
        m=re.search(r'"([^"]*)"',s); thread=m.group(1) if m else s
    elif s and s[0].isalpha() and '(' in s and 'line:' in s:
        frames.append(s)
flush()
print(f"thread~{want}: {total} samples")
print("-- top leaf methods"); [print(f"{v*100/total:5.1f}% {k}") for k,v in leaf.most_common(18)]
print("-- inclusive biomic methods"); [print(f"{v*100/total:5.1f}% {k}") for k,v in incl.most_common(45)]
