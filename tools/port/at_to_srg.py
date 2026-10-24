"""Forge 1.20.1 access transformer: Mojang names -> SRG names (run from the repo root; running it again changes nothing).

Legacy Forge applies the AT to SRG-named code, at dev time (ModDevGradle legacyforge) and in production, so an entry
written with a Mojang name is silently ignored. Entries keep the Mojang name as a comment.
"""
import pathlib
import re

AT = pathlib.Path('forge/src/main/resources/META-INF/accesstransformer.cfg')
SRG = pathlib.Path('common/build/moddev/artifacts/intermediateToNamed.srg')

fields, methods = {}, {}
for line in SRG.read_text().splitlines():
    if line.startswith('FD: '):
        srg, named = line[4:].split(' ')[:2]
        cls, name = named.rsplit('/', 1)
        fields[(cls, name)] = srg.rsplit('/', 1)[1]
    elif line.startswith('MD: '):
        parts = line[4:].split(' ')
        srg, named, desc = parts[0], parts[2], parts[3]
        cls, name = named.rsplit('/', 1)
        methods[(cls, name, desc)] = srg.rsplit('/', 1)[1]

out, missing = [], []
for line in AT.read_text().splitlines():
    m = re.match(r'^(\S+) (\S+) ([\w$<>]+)(\(\S*)?$', line.strip())
    if not m or line.lstrip().startswith('#'):
        out.append(line)
        continue
    access, cls, name, desc = m.groups()
    owner = cls.replace('.', '/')
    if desc is not None:
        srg = name if name == '<init>' else methods.get((owner, name, desc))
        if srg is None:
            missing.append(line)
            out.append(line)
            continue
        out.append(f'{access} {cls} {srg}{desc}' + ('' if srg == name else f' # {name}'))
    else:
        srg = fields.get((owner, name))
        if srg is None:
            missing.append(line)
            out.append(line)
            continue
        out.append(f'{access} {cls} {srg} # {name}')
AT.write_text('\n'.join(out) + '\n')
print(len(missing), 'not found:', *missing, sep='\n  ')
