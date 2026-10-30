"""jfrstages.py FILE.jfr [THREAD] [ANCHOR,...]: attribute samples to the callee directly below each anchor method."""
import sys,re,subprocess,collections,os
JFR=os.environ.get("JFR","jfr"); f=sys.argv[1]; thread=sys.argv[2] if len(sys.argv)>2 else "Server thread"
anchors=(sys.argv[3] if len(sys.argv)>3 else "ChunkProviderBiomic.provideChunk,ChunkProviderBiomic.populate").split(',')
p=subprocess.Popen([JFR,"print","--events","jdk.ExecutionSample","--stack-depth","200",f],stdout=subprocess.PIPE,text=True,errors='replace')
frames=[];th=None;total=0;stages=collections.Counter()
def short(fr): return re.sub(r'\(.*','',fr).replace('industries.angelic.biomic.','').split('.',)[-2:]
def flush():
    global total
    if not frames or th is None or thread not in th: return
    total+=1
    names=['.'.join(short(x)) for x in frames]
    for i,n in enumerate(names):
        for a in anchors:
            if n.endswith(a.split('.')[-2]+'.'+a.split('.')[-1]) or n==a.split('.',1)[-1]:
                callee=names[i-1] if i>0 else '(self)'
                stages[a.split('.')[-1]+' > '+callee]+=1; return
    stages['(outside anchors)']+=1
for line in p.stdout:
    s=line.strip()
    if s.startswith('jdk.ExecutionSample'): flush(); frames=[]; th=None
    elif s.startswith('sampledThread ='): m=re.search(r'"([^"]*)"',s); th=m.group(1) if m else s
    elif s and s[0].isalpha() and '(' in s and 'line:' in s: frames.append(s)
flush()
print(f"{thread}: {total} samples")
for k,v in stages.most_common(30): print(f"  {v*100/total:5.1f}%  {k}")
