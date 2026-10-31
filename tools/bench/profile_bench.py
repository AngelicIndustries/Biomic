#!/usr/bin/env python3
"""Generate benchmark windows with one mod profile and keep the world (docs/biomic-aac-split.md).

  pack        the pack's mod set: the template server plus ArchaicFix, Village Names, HarvestCraft, Harvest the
              Nether and Adventura Arcana Core, with the template's configs
  standalone  Biomic, Et Futurum Requiem and their libraries only, Biomic on its default config (the dev stack keeps
              the template's, WawelAuth needs one); --height 256 also drops Hohen Angst

Usage: tools/bench/profile_bench.py --profile pack|standalone --out DIR [--height 512|256] [--order grid|shuffled|dh]
       [--windows standard|order|"CX,CZ;CX,CZ"] [--radius 12] [--jar JAR] [--aac JAR] [--hohenangst JAR] [--archaicfix] [--port 25712] [--cpus 8-13]
       [--jvm -Dname=value ...]

The benchmark always generates its default window (chunk 256,-256) besides the listed centres: "standard" is 17
windows, "order" (the determinism check's) 4. DIR gets server/world, server.log, result.txt and manifest.json (the
revision, the jar's hash, the profile and every jar on the classpath).
"""
import argparse
import hashlib
import json
import os
import shutil
import subprocess
import sys
import time

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
sys.path.insert(0, os.path.join(ROOT, 'tools'))
import run_debloat_review as review  # noqa: E402

TEMPLATE = os.path.join(ROOT, 'run', 'debloat-template')
WINDOWS = {
    'standard': '-1200,-1200;-1200,-400;-1200,400;-1200,1200;-400,-1200;-400,-400;-400,400;-400,1200;'
                '400,-1200;400,-400;400,400;400,1200;1200,-1200;1200,-400;1200,400;1200,1200',
    'order': '-400,400;400,-400;1200,400',
}


def classpath(command):
    return command[command.index('-cp') + 1].split(':')


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--profile', choices=['pack', 'standalone'], required=True)
    parser.add_argument('--out', required=True)
    parser.add_argument('--height', choices=['512', '256'], default='512')
    parser.add_argument('--order', choices=['grid', 'shuffled', 'dh'], default='grid')
    parser.add_argument('--windows', default='standard')
    parser.add_argument('--radius', type=int, default=12)
    parser.add_argument('--jar', default=None)
    parser.add_argument('--archaicfix', action='store_true', help='add ArchaicFix (Phosphor lighting) to a standalone run')
    parser.add_argument('--hohenangst', default=None, help="a Hohen Angst dev jar in place of the template's")
    parser.add_argument('--aac', default=None, help="AAC's dev jar for the pack (default: the newest in its build/libs)")
    parser.add_argument('--port', default='25712')
    parser.add_argument('--cpus', default='8-13')
    parser.add_argument('--timeout', type=int, default=3600)
    parser.add_argument('--jvm', action='append', default=[], help='an extra JVM argument (repeatable)')
    args = parser.parse_args()
    if args.profile == 'pack' and args.height != '512':
        sys.exit('the pack profile is 512 high (Hohen Angst is part of the pack)')

    out = os.path.abspath(args.out)
    if os.path.exists(out):
        sys.exit(f'{out} exists; choose a new directory')
    os.makedirs(os.path.join(out, 'tmp'))
    source_jar = os.path.abspath(args.jar) if args.jar else review.newest_dev_jar()
    jar = os.path.join(out, 'biomic-dev.jar')
    shutil.copy2(source_jar, jar)

    server = os.path.join(out, 'server')
    os.makedirs(server)
    for name in ('server.properties', 'eula.txt'):
        shutil.copy2(os.path.join(TEMPLATE, 'server', name), server)
    shutil.copytree(os.path.join(TEMPLATE, 'server', 'config'), os.path.join(server, 'config'))
    if args.profile == 'standalone':
        review.drop_biomic_configs(os.path.join(server, 'config'))

    launch = json.load(open(os.path.join(TEMPLATE, 'launch.json')))
    command = review.retarget(launch['server'], launch['old_session'], out, jar)
    command = review.with_mods(command, args.profile, int(args.height), os.path.abspath(args.aac) if args.aac else None)
    if args.hohenangst:
        command = review.with_hohenangst(command, os.path.abspath(args.hohenangst))
    if args.archaicfix:
        command = review.with_archaicfix(command)

    windows = WINDOWS.get(args.windows, args.windows)
    java = next(i for i, c in enumerate(command) if c.endswith('/bin/java'))
    bench = ['-Dbiomic.worldgenBenchmark=true', f'-Dbiomic.worldgenBenchmarkResult={os.path.join(out, "result.txt")}',
             f'-Dbiomic.worldgenBenchmarkRadius={args.radius}', f'-Dbiomic.worldgenBenchmarkCenters={windows}',
             f'-Dbiomic.worldgenBenchmarkOrder={args.order}', '-Dbiomic.strictTerrainOwnership=true', *args.jvm]
    command = ['taskset', '--cpu-list', args.cpus] + command[:java + 1] + bench + command[java + 1:]
    port = command.index('--port') + 1
    command[port] = args.port

    revision = subprocess.run(['git', '-C', ROOT, 'rev-parse', 'HEAD'], capture_output=True, text=True).stdout.strip()
    dirty = bool(subprocess.run(['git', '-C', ROOT, 'status', '--porcelain', '--', 'src'], capture_output=True,
                                text=True).stdout.strip())
    manifest = {
        'profile': args.profile, 'height': int(args.height), 'order': args.order, 'windows': windows,
        'radius': args.radius, 'jvm': args.jvm, 'revision': revision, 'source_dirty': dirty, 'jar': source_jar,
        'jar_sha256': hashlib.sha256(open(jar, 'rb').read()).hexdigest(),
        'started': time.strftime('%Y-%m-%dT%H:%M:%S'), 'classpath': [os.path.basename(j) for j in classpath(command)],
    }
    with open(os.path.join(out, 'server.log'), 'w') as log:
        code = subprocess.run(command, cwd=server, stdin=subprocess.DEVNULL, stdout=log, stderr=subprocess.STDOUT,
                              timeout=args.timeout).returncode
    result = open(os.path.join(out, 'result.txt')).read() if os.path.exists(os.path.join(out, 'result.txt')) else ''
    manifest.update(exit=code, finished=time.strftime('%Y-%m-%dT%H:%M:%S'), result=result.splitlines()[:3])
    json.dump(manifest, open(os.path.join(out, 'manifest.json'), 'w'), indent=2)
    print(f'{args.profile} {args.height} {args.order}: exit {code}; ' + ' | '.join(result.splitlines()[:3]))
    sys.exit(0 if code == 0 and result.startswith('PASS') else 1)


if __name__ == '__main__':
    main()
