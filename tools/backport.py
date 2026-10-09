#!/usr/bin/env python3
"""Turns the 1.21.10 resources written by gen_resources.py into the formats of an older Minecraft 1.21.x (the
version branches of this repo). The same file on every branch; the steps follow the target version.

    python3 -I tools/backport.py <1.7.10 assets/mo> src/main/resources <minecraft client resources jar>

The jar is the target's build/moddev/artifacts/neoforge-*-client-extra-aka-minecraft-resources.jar (after a build);
its version.json gives the Minecraft version and data version. Steps:
- always: structure NBT files get the target's data version; vanilla items / blocks newer than the target leave the
  matter data map and become optional tag entries;
- before 1.21.4: client item definitions (assets/*/items/*.json) become item models with overrides; their constant
  layer tints go to assets/matteroverdrive/item_tints.json (read by client/BarrelProperty.java);
- 1.21.4: the component select property of the security protocol becomes matteroverdrive:security_type;
- before 1.21.2: recipe ingredients are objects ({"item": ...} / {"tag": ...}); no equipment assets;
- 1.21.2 - 1.21.3: equipment assets live in models/equipment;
- before 1.21.5: spawn eggs use the vanilla two-colour template with the 1.7.10 egg colours (1.21.4: constant tints
  of the client item definition; before 1.21.4 DeferredSpawnEggItem gives them); the failed pig / cow textures keep
  the 1.7.10 64x32 layout (the 1.21.5+ models are 64x64).
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
# select property matteroverdrive:barrel -> (WeaponBarrelItem.Type ordinal + 1) / 10
BARREL = {"damage": 0.1, "fire": 0.2, "explosion": 0.3, "heal": 0.4}
# 1.7.10 spawn egg colours (background, spots)
EGGS = {
    "rogue_android": (0x0FFFFF, 0), "ranged_rogue_android": (0x0FFFFF, 0), "mad_scientist": (0xFFFFFF, 0),
    "mutant_scientist": (0xFFFFFF, 0x00FF00), "failed_pig": (15771042, 0x33CC33), "failed_cow": (4470310, 0x33CC33),
    "failed_chicken": (10592673, 0x33CC33), "failed_sheep": (15198183, 0x33CC33),
}

ref = Path(sys.argv[1])
res = Path(sys.argv[2])
A = res / "assets" / MOD
D = res / "data" / MOD
with zipfile.ZipFile(sys.argv[3]) as jar:
    names = jar.namelist()
    version = json.loads(jar.read("version.json"))
MC = tuple(int(x) for x in version["id"].split("."))
DATA_VERSION = version["world_version"]
# every vanilla item has an item model (1.21.4+: a client item definition), every block a blockstate
ITEM_DIR = "assets/minecraft/items/" if MC >= (1, 21, 4) else "assets/minecraft/models/item/"
VANILLA = {
    "item": {"minecraft:" + n[len(ITEM_DIR):-5] for n in names if n.startswith(ITEM_DIR) and n.endswith(".json")},
    "block": {"minecraft:" + n[len("assets/minecraft/blockstates/"):-5] for n in names if n.startswith("assets/minecraft/blockstates/")},
}


def read(p):
    return json.loads(p.read_text(encoding="utf-8"))


def write(p, obj):
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def model_file(model_id):
    ns, path = model_id.split(":", 1) if ":" in model_id else ("minecraft", model_id)
    return res / "assets" / ns / "models" / f"{path}.json"


# --- before 1.21.4: client item definitions -> item models with overrides ------------------------------------
def leaf(node):
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


tints = {}
if MC < (1, 21, 4):
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
        model = read(own_file) if base == own else {"parent": base}
        overrides.sort(key=lambda o: list(o[0].values())[0])
        if overrides:
            model["overrides"] = [{"predicate": p, "model": m} for p, m in overrides]
        write(own_file, model)
    shutil.rmtree(A / "items")
    write(A / "item_tints.json", tints)

# --- 1.21.4: no minecraft:component select property (client/SecurityTypeProperty.java) -------------------------
if MC == (1, 21, 4):
    for f in sorted((A / "items").glob("*.json")):
        d = read(f)
        m = d["model"]
        if m["type"] == "minecraft:select" and m["property"] == "minecraft:component":
            assert m.pop("component") == f"{MOD}:security_type", f
            m["property"] = f"{MOD}:security_type"
            write(f, d)

# --- equipment assets ------------------------------------------------------------------------------------------
if MC < (1, 21, 2):
    shutil.rmtree(A / "equipment", ignore_errors=True)
elif MC < (1, 21, 4):
    for f in list((A / "equipment").glob("*.json")):
        (A / "models/equipment").mkdir(parents=True, exist_ok=True)
        shutil.move(str(f), A / "models/equipment" / f.name)
    shutil.rmtree(A / "equipment", ignore_errors=True)

# --- before 1.21.5: spawn eggs and the 64x32 animal textures -----------------------------------------------------
if MC < (1, 21, 5):
    for f in (A / "models/item").glob("*_spawn_egg.json"):
        write(f, {"parent": "minecraft:item/template_spawn_egg"})
        (A / "textures/item" / f"{f.stem}.png").unlink(missing_ok=True)
        if MC >= (1, 21, 4):
            colours = EGGS[f.stem[:-len("_spawn_egg")]]
            write(A / "items" / f.name, {"model": {"type": "minecraft:model", "model": "minecraft:item/template_spawn_egg", "tints": [
                {"type": "minecraft:constant", "value": (c | 0xFF000000) - (1 << 32)} for c in colours]}})
    for n in ("pig", "cow"):
        shutil.copyfile(ref / "textures/entities" / f"failed_{n}.png", A / "textures/entity" / f"failed_{n}.png")


# --- before 1.21.2: recipe ingredients as objects -----------------------------------------------------------------
def ingredient(x):
    if isinstance(x, str):
        return {"tag": x[1:]} if x.startswith("#") else {"item": x}
    if isinstance(x, list):
        return [ingredient(e) for e in x]
    return x


if MC < (1, 21, 2):
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


# --- structure NBT: the target's data version ---------------------------------------------------------------------
def set_data_version(path):
    raw = gzip.decompress(path.read_bytes())
    key = b"\x03\x00\x0bDataVersion"
    i = raw.find(key)
    if i < 0:
        return
    j = i + len(key)
    raw = raw[:j] + struct.pack(">i", DATA_VERSION) + raw[j + 4:]
    buf = io.BytesIO()
    with gzip.GzipFile(fileobj=buf, mode="wb", mtime=0) as gz:
        gz.write(raw)
    path.write_bytes(buf.getvalue())


for f in (res / "data").rglob("*.nbt"):
    set_data_version(f)


# --- vanilla content newer than the target -------------------------------------------------------------------------
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

print(f"{version['id']}: {dropped} newer vanilla items left the matter map, {optional} tag entries made optional, "
      f"{len(tints)} tinted items, data version {DATA_VERSION}")
