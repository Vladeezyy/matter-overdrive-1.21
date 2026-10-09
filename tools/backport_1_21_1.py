#!/usr/bin/env python3
"""Turns the 1.21.10 resources written by gen_resources.py into the Minecraft 1.21.1 formats (branch 1.21.1).

    python3 -I tools/backport_1_21_1.py <1.7.10 assets/mo> src/main/resources <minecraft 1.21.1 client resources jar>

(the jar: build/moddev/artifacts/neoforge-21.1.*-client-extra-aka-minecraft-resources.jar, after a build)

Run it right after gen_resources.py. What changes for 1.21.1:
- client item definitions (assets/*/items/*.json, 1.21.4+) become item models with overrides; their constant layer tints
  go to assets/matteroverdrive/item_tints.json (read by client/BarrelProperty.java);
- recipe ingredients are objects ({"item": ...} / {"tag": ...}) instead of strings (1.21.2+);
- spawn eggs use the vanilla two-colour template (1.21.5+ eggs have their own textures);
- the failed pig / cow textures keep the 1.7.10 64x32 layout (the 1.21.5+ models are 64x64);
- structure NBT files get the 1.21.1 data version; equipment assets (1.21.2+) are dropped;
- vanilla items / blocks newer than 1.21.1 leave the matter data map and become optional tag entries.
"""
import gzip
import io
import json
import shutil
import struct
import sys
import zipfile
from pathlib import Path

MOD = "matteroverdrive"
DATA_VERSION_1_21_1 = 3955
# select property matteroverdrive:barrel -> (WeaponBarrelItem.Type ordinal + 1) / 10
BARREL = {"damage": 0.1, "fire": 0.2, "explosion": 0.3, "heal": 0.4}

ref = Path(sys.argv[1])
res = Path(sys.argv[2])
with zipfile.ZipFile(sys.argv[3]) as jar:
    names = jar.namelist()
# every 1.21.1 item has an item model, every block a blockstate
VANILLA = {
    "item": {"minecraft:" + n[len("assets/minecraft/models/item/"):-5] for n in names if n.startswith("assets/minecraft/models/item/")},
    "block": {"minecraft:" + n[len("assets/minecraft/blockstates/"):-5] for n in names if n.startswith("assets/minecraft/blockstates/")},
}
A = res / "assets" / MOD
D = res / "data" / MOD


def read(p):
    return json.loads(p.read_text(encoding="utf-8"))


def write(p, obj):
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def model_file(model_id):
    ns, path = model_id.split(":", 1) if ":" in model_id else ("minecraft", model_id)
    return res / "assets" / ns / "models" / f"{path}.json"


# --- client item definitions -> item models with overrides ------------------------------------------------
tints = {}


def leaf(node):
    """A minecraft:model node: its model id (and records nothing else)."""
    assert node["type"] == "minecraft:model", node
    return node["model"]


def overrides_of(item, d):
    """(base model, [(predicate, model)]) of a client item definition."""
    t = d["type"]
    if t == "minecraft:model":
        return d["model"], []
    if t == "minecraft:select" and d["property"] == f"{MOD}:barrel":
        return leaf(d["fallback"]), [({f"{MOD}:barrel": BARREL[c["when"]]}, leaf(c["model"])) for c in d["cases"]]
    if t == "minecraft:select" and d["property"] == "minecraft:component" and d["component"] == f"{MOD}:security_type":
        return leaf(d["fallback"]), [({f"{MOD}:security_type": c["when"] / 10}, leaf(c["model"])) for c in d["cases"] if c["when"] > 0]
    if t == "minecraft:condition" and d["property"] == "minecraft:has_component" and d["component"] == f"{MOD}:scanner_link":
        return leaf(d["on_false"]), [({f"{MOD}:linked": 1}, leaf(d["on_true"]))]
    if t == "minecraft:range_dispatch" and d["property"] == "minecraft:custom_model_data":
        return leaf(d["fallback"]), [({"custom_model_data": e["threshold"]}, leaf(e["model"])) for e in d["entries"]]
    raise SystemExit(f"{item}: unsupported client item definition {t} {d.get('property')}")


for f in sorted((A / "items").glob("*.json")):
    item = f.stem
    d = read(f)["model"]
    if d.get("tints"):
        tints[f"{MOD}:{item}"] = [t["value"] if t["type"] == "minecraft:constant" else -1 for t in d["tints"]]
    base, overrides = overrides_of(item, d)
    own = f"{MOD}:item/{item}"
    own_file = model_file(own)
    # an override can't point at the model that carries the overrides: give that one a copy
    for i, (pred, m) in enumerate(overrides):
        if m == own:
            copy = f"{MOD}:item/{item}_case"
            shutil.copyfile(own_file, model_file(copy))
            overrides[i] = (pred, copy)
    if base == own:
        model = read(own_file)
    else:
        model = {"parent": base}
    overrides.sort(key=lambda o: list(o[0].values())[0])
    if overrides:
        model["overrides"] = [{"predicate": p, "model": m} for p, m in overrides]
    write(own_file, model)
shutil.rmtree(A / "items")
shutil.rmtree(A / "equipment", ignore_errors=True)
write(A / "item_tints.json", tints)

# spawn eggs: the vanilla template, tinted with the 1.7.10 egg colours by DeferredSpawnEggItem
for f in (A / "models/item").glob("*_spawn_egg.json"):
    write(f, {"parent": "minecraft:item/template_spawn_egg"})
    (A / "textures/item" / f"{f.stem}.png").unlink(missing_ok=True)

# the 1.21.1 pig and cow models still use the 1.7.10 64x32 texture layout
for n in ("pig", "cow"):
    shutil.copyfile(ref / "textures/entities" / f"failed_{n}.png", A / "textures/entity" / f"failed_{n}.png")


# --- recipes: ingredients as objects --------------------------------------------------------------------------
def ingredient(x):
    if isinstance(x, str):
        return {"tag": x[1:]} if x.startswith("#") else {"item": x}
    if isinstance(x, list):
        return [ingredient(e) for e in x]
    return x


for f in sorted((D / "recipe").glob("*.json")):
    r = read(f)
    if "key" in r:
        r["key"] = {k: ingredient(v) for k, v in r["key"].items()}
    if "ingredients" in r:
        r["ingredients"] = [ingredient(v) for v in r["ingredients"]]
    for field in ("ingredient", "main", "secondary", "template", "base", "addition"):
        if field in r:
            r[field] = ingredient(r[field])
    write(f, r)


# --- structure NBT: the 1.21.1 data version -------------------------------------------------------------------
def set_data_version(path):
    raw = gzip.decompress(path.read_bytes())
    key = b"\x03\x00\x0bDataVersion"
    i = raw.find(key)
    if i < 0:
        return
    j = i + len(key)
    raw = raw[:j] + struct.pack(">i", DATA_VERSION_1_21_1) + raw[j + 4:]
    buf = io.BytesIO()
    with gzip.GzipFile(fileobj=buf, mode="wb", mtime=0) as gz:
        gz.write(raw)
    path.write_bytes(buf.getvalue())


for f in (res / "data").rglob("*.nbt"):
    set_data_version(f)

# --- vanilla content newer than 1.21.1 -------------------------------------------------------------------------
def known(kind, ident):
    return not ident.startswith("minecraft:") or ident in VANILLA[kind]


dropped = 0
for f in (res / "data").rglob("data_maps/item/*.json"):
    m = read(f)
    values = {k: v for k, v in m["values"].items() if known("item", k)}
    dropped += len(m["values"]) - len(values)
    m["values"] = values
    write(f, m)
optional = 0
for kind in ("item", "block"):
    for f in (res / "data").rglob(f"tags/{kind}/**/*.json"):
        t = read(f)
        vals = []
        for v in t["values"]:
            if isinstance(v, str) and not v.startswith("#") and not known(kind, v):
                v = {"id": v, "required": False}
                optional += 1
            vals.append(v)
        t["values"] = vals
        write(f, t)

print(f"1.21.1: {dropped} newer vanilla items left the matter map, {optional} tag entries made optional")
print(f"1.21.1: {len(tints)} tinted items, recipes and structures converted")
