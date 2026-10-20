"""at2aw.py AT JAR AW_EXTRA -- writes the Fabric access widener from the NeoForge access transformer (field descriptors
from the Minecraft jar via javap), then the Fabric-only lines from AW_EXTRA. Prints the result."""
import re,subprocess,sys
at,jar,extra=sys.argv[1:4]
out=['accessWidener v2 official','# generated from neoforge/src/main/resources/META-INF/accesstransformer.cfg by tools/port/at2aw.py']
def javap(cls):
    return subprocess.run(['javap','-s','-p','-cp',jar,cls],capture_output=True,text=True).stdout
for line in open(at):
    line=line.strip()
    if not line or line.startswith('#'):
        if line: out.append(line)
        continue
    mod,rest=line.split(' ',1)
    parts=rest.split(' ')
    cls=parts[0]; internal=cls.replace('.','/')
    if len(parts)==1:
        out.append(f'accessible class {internal}'); continue
    member=parts[1]
    m=re.match(r'([<>\w$]+)(\(.*)',member)
    if m:
        name,desc=m.groups()
        out.append(f'accessible method {internal} {name} {desc}')
        if mod.endswith('-f'): out.append(f'extendable method {internal} {name} {desc}')
    else:
        txt=javap(cls)
        mm=re.search(r' '+re.escape(member)+r';\n\s+descriptor: (\S+)',txt)
        if not mm:
            print(f'missing field {cls}.{member} (dropped)', file=sys.stderr); continue
        out.append(f'accessible field {internal} {member} {mm.group(1)}')
        if mod.endswith('-f'): out.append(f'mutable field {internal} {member} {mm.group(1)}')
out.append('# Fabric only')
out+= [l.rstrip('\n') for l in open(extra) if l.strip()]
print('\n'.join(out))
