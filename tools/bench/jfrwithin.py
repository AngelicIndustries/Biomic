"""jfrwithin.py FILE.jfr THREAD ANCHOR [N]: inclusive method shares among samples that pass through ANCHOR."""
import sys,re,subprocess,collections,os
JFR=os.environ.get("JFR","jfr"); f,thread,anchor=sys.argv[1:4]; N=int(sys.argv[4]) if len(sys.argv)>4 else 40
p=subprocess.Popen([JFR,"print","--events","jdk.ExecutionSample","--stack-depth","200",f],stdout=subprocess.PIPE,text=True,errors='replace')
frames=[];th=None;total=0;inside=0;incl=collections.Counter();leaf=collections.Counter()
def name(fr):
    m=re.match(r'([\w.$]+)\.([\w$<>]+)\(',fr); return (m.group(1).split('.')[-1]+'.'+m.group(2)) if m else fr
def flush():
    global total,inside
    if not frames or th is None or thread not in th: return
    total+=1
    names=[name(x) for x in frames]
    if anchor not in names: return
    inside+=1; i=names.index(anchor)
    leaf[names[0]]+=1
    for n in set(names[:i]): incl[n]+=1
for line in p.stdout:
    s=line.strip()
    if s.startswith('jdk.ExecutionSample'): flush(); frames=[]; th=None
    elif s.startswith('sampledThread ='): m=re.search(r'"([^"]*)"',s); th=m.group(1) if m else s
    elif s and s[0].isalpha() and '(' in s and 'line:' in s: frames.append(s)
flush()
print(f"{anchor}: {inside}/{total} samples ({inside*100/max(total,1):.1f}% of thread)")
print("-- inclusive (share of anchor time)")
for k,v in incl.most_common(N): print(f"  {v*100/max(inside,1):5.1f}%  {k}")
print("-- leaf")
for k,v in leaf.most_common(12): print(f"  {v*100/max(inside,1):5.1f}%  {k}")
