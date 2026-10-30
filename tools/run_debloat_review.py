#!/usr/bin/env python3
"""Start a fresh biomic server plus a visible client with the newest debloat dev jar.

Usage: tools/run-debloat-review.sh [--mods pack|standalone] [--height 512|256] [--seed N]
       [--jar path/to/biomic-dev.jar] [--profile]

--mods pack (the default) is the pack's mod set: the template plus ArchaicFix, Village Names, HarvestCraft, Harvest
the Nether and Adventura Arcana Core (the newest dev jar in ../AdventuraArcanaCore/build/libs). --mods standalone is
Biomic, Et Futurum Requiem and their libraries only, Biomic on its default config; --height 256 also drops Hohen Angst
(docs/biomic-aac-split.md).
--profile records JFR on server and client (server.jfr, client.jfr) and the client frame-time log (frames.txt).
--archaicfix, --villagenames and --harvest add ArchaicFix, Village Names, or HarvestCraft and Harvest the Nether to a
standalone run (the pack has them).
--npcs turns villagers and animals on (the template has them off, and the server removes them every tick).
--view-distance N sets the server's view distance and the client's render distance (vanilla clamps the server to 20;
Hodgepodge lifts it).
--connect has the client join the server on start; --client-jvm=-Dname=value passes an extra client JVM argument.

Each run gets its own session under run/debloat-review/<timestamp>/ (server world, logs, client dir). The server
stops when the client window closes. Connect to "localhost:25611" from the multiplayer menu; ReviewBot is op.
"""
import argparse
import glob
import json
import os
import re
import shutil
import subprocess
import sys
import time
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEMPLATE = os.path.join(ROOT, 'run', 'debloat-template')
AAC = os.path.join(os.path.dirname(ROOT), 'AdventuraArcanaCore')
# Content mods outside Biomic's required set, and AAC: the standalone profile leaves them out.
OPTIONAL = re.compile(r'growthcraft|plantmegapack|Natura|Mantle|saltymod|netherlicious|biomes-o-plenty|VillageNames'
                      r'|harvestcraft|harvest-the-nether|adventuraarcanacore', re.I)
HOHEN_ANGST = re.compile(r'hohenangst', re.I)
# Biomic's own configs: a standalone run takes its defaults.
BIOMIC_CONFIGS = ('biomic.cfg', 'biomic-structures.json')


def newest_dev_jar():
    jars = [j for j in glob.glob(os.path.join(ROOT, 'build', 'libs', 'biomic-*-dev.jar')) if 'preshadow' not in j]
    if not jars:
        sys.exit('No biomic-*-dev.jar in build/libs; run: VERSION=0.0.1-debloat ./gradlew assemble -x test')
    return max(jars, key=os.path.getmtime)


def archaicfix_jar():
    jars = glob.glob(os.path.expanduser('~/.gradle/caches/*/transforms/*/transformed/archaicfix-*-deobf.jar'))
    if not jars:
        sys.exit('No archaicfix-*-deobf.jar in the Gradle transform cache')
    return max(jars, key=os.path.getmtime)


def village_names_jar():
    jars = glob.glob(os.path.expanduser(
        '~/.gradle/caches/modules-2/files-2.1/com.github.GTNewHorizons/VillageNames/*/*/VillageNames-*-dev.jar'))
    if not jars:
        sys.exit('No VillageNames-*-dev.jar in the Gradle cache')
    return max(jars, key=os.path.getmtime)


def harvest_jars():
    pam = glob.glob(os.path.expanduser(
        '~/.gradle/caches/modules-2/files-2.1/com.github.GTNewHorizons/harvestcraft/1.3.13-GTNH/*/harvestcraft-1.3.13-GTNH-dev.jar'))
    nether = glob.glob(os.path.expanduser(
        '~/.gradle/caches/*/transforms/*/transformed/pams-harvest-the-nether-231262-2241397-deobf.jar'))
    if not pam or not nether:
        sys.exit('HarvestCraft 1.3.13-GTNH dev jar or the deobfuscated Harvest the Nether jar is missing; '
                 'run: ./gradlew --offline compileJava')
    return [max(pam, key=os.path.getmtime), max(nether, key=os.path.getmtime)]


def aac_jar():
    # A dev jar has its reobfuscated twin beside it: "x-0.1.0-dev.jar" is the release jar of version 0.1.0-dev when
    # "x-0.1.0.jar" is missing.
    jars = [j for j in glob.glob(os.path.join(AAC, 'build', 'libs', 'adventuraarcanacore-*-dev.jar'))
            if os.path.exists(j[:-len('-dev.jar')] + '.jar')]
    if not jars:
        sys.exit(f'No adventuraarcanacore-*-dev.jar in {AAC}/build/libs; run there: ./gradlew build')
    return max(jars, key=os.path.getmtime)


def with_jar(command, extra):
    i = command.index('-cp') + 1
    if extra in command[i].split(':'):
        return command
    return command[:i] + [command[i] + ':' + extra] + command[i + 1:]


def with_mods(command, mods, height=512, aac=None):
    """The template's command with the pack's mod set (AAC's newest dev jar unless {@code aac} names one), or the
    standalone one (see the module doc)."""
    if mods == 'pack':
        for extra in [archaicfix_jar(), village_names_jar(), *harvest_jars(), aac or aac_jar()]:
            command = with_jar(command, extra)
        return command
    i = command.index('-cp') + 1
    jars = [j for j in command[i].split(':') if not OPTIONAL.search(os.path.basename(j))
            and not (height == 256 and HOHEN_ANGST.search(os.path.basename(j)))]
    return command[:i] + [':'.join(jars)] + command[i + 1:]


def with_hohenangst(command, jar):
    """The command with {@code jar} in place of the template's Hohen Angst (none when the profile dropped it)."""
    i = command.index('-cp') + 1
    jars = [jar if HOHEN_ANGST.search(os.path.basename(j)) else j for j in command[i].split(':')]
    return command[:i] + [':'.join(jars)] + command[i + 1:]


def drop_biomic_configs(config_dir):
    for name in BIOMIC_CONFIGS:
        path = os.path.join(config_dir, name)
        if os.path.exists(path):
            os.remove(path)


def with_archaicfix(command):
    return with_jar(command, archaicfix_jar())


# The template froze FentLib-era jars; FentLib became AngelLib (Angelic Industries), and these mods moved with it.
REBRANDED = {
    'FentLib-0.0.55-dev.jar': 'industries.angelic.angellib/AngelLib/1.0.0',
    'WawelAuth-1.0.5-dev.jar': 'industries.angelic.wawelauth/WawelAuth/996a655-snapshot',
    'vrap-1.0.1-dev.jar': 'industries.angelic.vrap/vrap/1.0.2',
    'VintageResourcify-1.0.5-dev.jar': 'industries.angelic.vintageresourcify/VintageResourcify/1.0.6',
}


def rebranded(path):
    for old, coordinates in REBRANDED.items():
        if path.endswith(old):
            jars = glob.glob(os.path.expanduser(f'~/.gradle/caches/modules-2/files-2.1/{coordinates}/*/*-dev.jar'))
            if not jars:
                sys.exit(f'No {coordinates} dev jar in the Gradle cache; build Biomic once (./gradlew assemble)')
            return jars[0]
    return path


def retarget(command, old_session, session, jar):
    out = []
    for arg in command:
        arg = arg.replace(os.path.join(old_session, 'server', 'normal-client'), os.path.join(session, 'client'))
        arg = arg.replace(old_session, session)
        if '-cp' in out[-1:]:
            arg = ':'.join(jar if p.endswith('biomic-dev.jar') else rebranded(p) for p in arg.split(':'))
        out.append(arg)
    return out


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--mods', choices=['pack', 'standalone'], default='pack')
    parser.add_argument('--height', type=int, choices=[512, 256], default=512)
    parser.add_argument('--aac', default=None, help="AAC's dev jar for the pack (default: the newest in its build/libs)")
    parser.add_argument('--hohenangst', default=None, help="a Hohen Angst dev jar in place of the template's")
    parser.add_argument('--seed', default=None)
    parser.add_argument('--jar', default=None)
    parser.add_argument('--server-only', action='store_true', help='start, wait for Done, then stop (smoke test)')
    parser.add_argument('--profile', action='store_true', help='JFR on both sides plus the client frame-time log')
    parser.add_argument('--archaicfix', action='store_true', help='add ArchaicFix (Phosphor lighting) to both sides')
    parser.add_argument('--villagenames', action='store_true', help='add Village Names to both sides')
    parser.add_argument('--npcs', action='store_true', help='villagers and animals on (spawn-npcs, spawn-animals)')
    parser.add_argument('--harvest', action='store_true', help='add HarvestCraft and Harvest the Nether to both sides')
    parser.add_argument('--view-distance', type=int, default=None, help='server view and client render distance')
    parser.add_argument('--connect', action='store_true', help='the client joins the server on start')
    parser.add_argument('--client-jvm', action='append', default=[], help='an extra client JVM argument (repeatable)')
    args = parser.parse_args()
    if args.mods == 'pack' and args.height != 512:
        sys.exit('the pack is 512 high (Hohen Angst is part of the pack)')
    source_jar = os.path.abspath(args.jar) if args.jar else newest_dev_jar()
    launch = json.load(open(os.path.join(TEMPLATE, 'launch.json')))
    session = os.path.join(ROOT, 'run', 'debloat-review', time.strftime('%Y%m%dT%H%M%S'))
    os.makedirs(session)
    # Snapshot the jar: a rebuild during the session must never be seen by the running game.
    jar = os.path.join(session, 'biomic-dev.jar')
    shutil.copy2(source_jar, jar)
    try:
        with zipfile.ZipFile(jar) as archive:
            archive.getinfo('mcmod.info')
    except (zipfile.BadZipFile, KeyError):
        sys.exit(f'{source_jar} is incomplete (a build may be running); retry in a minute')
    shutil.copytree(os.path.join(TEMPLATE, 'server'), os.path.join(session, 'server'))
    shutil.copytree(os.path.join(TEMPLATE, 'client'), os.path.join(session, 'client'))
    os.makedirs(os.path.join(session, 'tmp'), exist_ok=True)
    if args.mods == 'standalone':
        drop_biomic_configs(os.path.join(session, 'server', 'config'))
        drop_biomic_configs(os.path.join(session, 'client', 'config'))
    overrides = {}
    if args.seed is not None:
        overrides['level-seed'] = args.seed
    if args.npcs:
        overrides['spawn-npcs'] = overrides['spawn-animals'] = 'true'
    if args.view_distance is not None:
        overrides['view-distance'] = str(args.view_distance)
        options = os.path.join(session, 'client', 'options.txt')
        lines = [f'renderDistance:{args.view_distance}' if l.startswith('renderDistance:') else l
                 for l in open(options).read().splitlines()]
        open(options, 'w').write('\n'.join(lines) + '\n')
    if overrides:
        path = os.path.join(session, 'server', 'server.properties')
        lines = [f'{l.split("=", 1)[0]}={overrides[l.split("=", 1)[0]]}' if l.split('=', 1)[0] in overrides else l
                 for l in open(path).read().splitlines()]
        open(path, 'w').write('\n'.join(lines) + '\n')
    server_cmd = retarget(launch['server'], launch['old_session'], session, jar)
    client_cmd = retarget(launch['client'], launch['old_session'], session, jar)
    aac = os.path.abspath(args.aac) if args.aac else None
    server_cmd = with_mods(server_cmd, args.mods, args.height, aac)
    client_cmd = with_mods(client_cmd, args.mods, args.height, aac)
    if args.hohenangst:
        server_cmd = with_hohenangst(server_cmd, os.path.abspath(args.hohenangst))
        client_cmd = with_hohenangst(client_cmd, os.path.abspath(args.hohenangst))
    if args.archaicfix:
        server_cmd = with_archaicfix(server_cmd)
        client_cmd = with_archaicfix(client_cmd)
    if args.villagenames:
        server_cmd = with_jar(server_cmd, village_names_jar())
        client_cmd = with_jar(client_cmd, village_names_jar())
    if args.harvest:
        for extra in harvest_jars():
            server_cmd = with_jar(server_cmd, extra)
            client_cmd = with_jar(client_cmd, extra)
    if args.profile:
        def profiled(cmd, name, extra=()):
            java = next(i for i, c in enumerate(cmd) if c.endswith('/bin/java'))
            jfr = f'-XX:StartFlightRecording=filename={os.path.join(session, name)},settings=profile,dumponexit=true'
            return cmd[:java + 1] + [jfr, *extra] + cmd[java + 1:]
        server_cmd = profiled(server_cmd, 'server.jfr')
        client_cmd = profiled(client_cmd, 'client.jfr', [f'-Dbiomic.frameLog={os.path.join(session, "frames.txt")}'])
    if args.client_jvm:
        java = next(i for i, c in enumerate(client_cmd) if c.endswith('/bin/java'))
        client_cmd = client_cmd[:java + 1] + args.client_jvm + client_cmd[java + 1:]
    if args.connect:
        client_cmd = client_cmd + ['--server', 'localhost', '--port', '25611']
    print(f'session: {session}\njar: {source_jar}', flush=True)

    server_log = open(os.path.join(session, 'server.log'), 'w')
    server = subprocess.Popen(server_cmd, cwd=os.path.join(session, 'server'), stdin=subprocess.PIPE,
                              stdout=server_log, stderr=subprocess.STDOUT, text=True)
    deadline = time.time() + 600
    while time.time() < deadline:
        if server.poll() is not None:
            sys.exit(f'Server exited early; see {session}/server.log')
        if 'Done (' in open(os.path.join(session, 'server.log'), errors='replace').read():
            break
        time.sleep(2)
    else:
        server.kill()
        sys.exit('Server did not start within 10 minutes')
    if args.server_only:
        print('server ready; stopping (server-only smoke test)', flush=True)
        server.stdin.write('stop\n')
        server.stdin.flush()
        server.wait(timeout=120)
        return
    print('server ready on localhost:25611; starting client', flush=True)
    with open(os.path.join(session, 'client.log'), 'w') as client_log:
        client = subprocess.Popen(client_cmd, cwd=os.path.join(session, 'client'), stdout=client_log,
                                  stderr=subprocess.STDOUT)
        client.wait()
    print('client closed; stopping server', flush=True)
    try:
        server.stdin.write('stop\n')
        server.stdin.flush()
        server.wait(timeout=120)
    except Exception:
        server.kill()
    print(f'logs: {session}/server.log, {session}/client.log')


if __name__ == '__main__':
    main()
