#!/usr/bin/env python3
"""Turns the 1.21.1 resources of the branch into the Minecraft 1.20.1 formats (branch mc/1.20.1). Idempotent.

    python3 -I tools/backport_resources_1_20_1.py common/src/main/resources <neoforge-21.1.x-universal.jar>

(the jar: ~/.gradle/caches/modules-2/files-2.1/net.neoforged/neoforge/21.1.*/*/neoforge-21.1.*-universal.jar, for the
contents of the 1.21 c: tags the data uses)

What changes for 1.20.1:
- data folders are plural (recipes, loot_tables, tags/items, tags/blocks, structures);
- recipe results are {"item": ...} (cooking recipes: just the id);
- loot tables: copy_components from the block entity becomes copy_nbt of the crate's Items into BlockEntityTag and
  copy_name, or matteroverdrive:copy_components for what a machine carries as an item;
- the 1.21 c: tags the data refers to are shipped with the mod (from NeoForge 21.1, every entry optional): neither 1.20.1
  loader has them (Forge has forge:, Fabric the old c: names, which the NeoForge files already list as optional);
- the mod's materials also go into the 1.20.1 forge: and c: tags so other mods find them;
- NeoForge biome modifiers become Forge ones (data/<mod>/forge/biome_modifier, forge: types); OBJ models use forge:obj;
- structure NBT files get the 1.20.1 data version; pack.mcmeta with pack format 15.
"""
import gzip
import io
import json
import re
import shutil
import struct
import sys
import zipfile
from pathlib import Path

MOD = "matteroverdrive"
DATA_VERSION_1_20_1 = 3465
PLURAL = {"recipe": "recipes", "loot_table": "loot_tables", "structure": "structures", "advancement": "advancements",
          "predicate": "predicates", "item_modifier": "item_modifiers", "function": "functions"}
TAG_PLURAL = {"item": "items", "block": "blocks", "entity_type": "entity_types", "fluid": "fluids", "game_event": "game_events"}

root = Path(sys.argv[1])
neo = zipfile.ZipFile(sys.argv[2])
data = root / "data"


def load(path):
    return json.loads(path.read_text(encoding="utf-8"))


def save(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def move_dir(src, dst):
    if not src.exists():
        return
    for f in sorted(src.rglob("*")):
        if f.is_file():
            target = dst / f.relative_to(src)
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.move(str(f), str(target))
    shutil.rmtree(src)


# 1. folders
for ns in data.iterdir():
    if not ns.is_dir():
        continue
    for old, new in PLURAL.items():
        move_dir(ns / old, ns / new)
    tags = ns / "tags"
    if tags.exists():
        for old, new in TAG_PLURAL.items():
            move_dir(tags / old, tags / new)

# 2. recipes
for f in data.glob("*/recipes/**/*.json"):
    r = load(f)
    res = r.get("result")
    if isinstance(res, dict) and "id" in res:
        res["item"] = res.pop("id")
    if r.get("type") in ("minecraft:smelting", "minecraft:blasting", "minecraft:smoking", "minecraft:campfire_cooking") and isinstance(res, dict):
        r["result"] = res["item"]
    save(f, r)

# 3. loot tables
def fix_functions(funcs):
    out = []
    for fn in funcs:
        if fn.get("function") == "minecraft:copy_components" and fn.get("source") == "block_entity":
            include = fn.get("include", [])
            if "minecraft:container" in include:
                out.append({"function": "minecraft:copy_nbt", "source": "block_entity",
                            "ops": [{"source": "Items", "target": "BlockEntityTag.Items", "op": "replace"}]})
            if "minecraft:custom_name" in include:
                out.append({"function": "minecraft:copy_name", "source": "block_entity"})
            if any(i.startswith(MOD + ":") for i in include):
                out.append({"function": MOD + ":copy_components"})
        else:
            out.append(fn)
    return out


def fix_item_predicate(pred):
    """1.21 item predicate (items as one id, sub-predicates) -> 1.20.1 (items as a list, enchantments list)"""
    if isinstance(pred.get("items"), str):
        pred["items"] = [pred["items"]]
    subs = pred.pop("predicates", None)
    if subs:
        for e in subs.get("minecraft:enchantments", []):
            ench = e.pop("enchantments")
            pred.setdefault("enchantments", []).append({"enchantment": ench if isinstance(ench, str) else ench[0], **e})


def walk_loot(node):
    if isinstance(node, dict):
        if node.get("condition") == "minecraft:match_tool" and "predicate" in node:
            fix_item_predicate(node["predicate"])
        if "functions" in node:
            node["functions"] = fix_functions(node["functions"])
        for v in node.values():
            walk_loot(v)
    elif isinstance(node, list):
        for v in node:
            walk_loot(v)


for f in data.glob("*/loot_tables/**/*.json"):
    t = load(f)
    walk_loot(t)
    save(f, t)

# 4. the 1.21 c: tags the data uses, from NeoForge 21.1 (all entries optional)
def referenced_c_tags():
    found = set()
    for f in data.rglob("*.json"):
        if "/tags/" in f.as_posix() and f.as_posix().split("/data/")[-1].startswith("c/"):
            continue
        for m in re.finditer(r'"#c:([a-z0-9_/]+)"', f.read_text(encoding="utf-8")):
            found.add(m.group(1))
    return found


def optional(value):
    if isinstance(value, dict):
        return {"id": value["id"], "required": False}
    return {"id": value, "required": False}


# referenced but not in NeoForge 21.1
VANILLA_ONLY = {"flowers/small": "#minecraft:small_flowers", "flowers/tall": "#minecraft:tall_flowers"}


def copy_c_tag(kind, path, done):
    if (kind, path) in done:
        return
    done.add((kind, path))
    target = data / "c" / "tags" / TAG_PLURAL[kind] / (path + ".json")
    if target.exists() and (path.endswith("tritanium") or path.endswith("dilithium")):
        return  # the mod's own
    name = f"data/c/tags/{kind}/{path}.json"
    if path in VANILLA_ONLY:
        save(target, {"replace": False, "values": [VANILLA_ONLY[path]]})
        return
    if name not in neo.namelist():
        print("no NeoForge tag", name)
        return
    tag = json.loads(neo.read(name))
    values = []
    for v in tag["values"]:
        vid = v["id"] if isinstance(v, dict) else v
        if vid.startswith("#c:"):
            copy_c_tag(kind, vid[3:], done)
            values.append(vid)  # ours now: required
        else:
            values.append(optional(v))
    old = load(target)["values"] if target.exists() else []
    merged = old + [v for v in values if v not in old]
    save(target, {"replace": False, "values": merged})


done = set()
for path in sorted(referenced_c_tags()):
    copy_c_tag("item", path, done)

# the mod's materials in the 1.20.1 forge: tags (same paths) and Fabric's old c: names
for kind in ("items", "blocks"):
    for f in sorted((data / "c" / "tags" / kind).rglob("*.json")):
        rel = f.relative_to(data / "c" / "tags" / kind).with_suffix("").as_posix()
        if "/" not in rel or not (rel.endswith("tritanium") or rel.endswith("dilithium")):
            continue
        group, material = rel.split("/", 1)
        save(data / "forge" / "tags" / kind / f"{group}/{material}.json", {"replace": False, "values": [f"#c:{rel}"]})
        save(data / "forge" / "tags" / kind / f"{group}.json", {"replace": False, "values": [f"#forge:{group}/{material}"]}) \
            if not (data / "forge" / "tags" / kind / f"{group}.json").exists() else None
        group_name = {"storage_blocks": "blocks"}.get(group, group)
        save(data / "c" / "tags" / kind / f"{material}_{group_name}.json", {"replace": False, "values": [f"#c:{rel}"]})

# the mod's tags: vanilla entries optional (some name 1.20.5+ blocks, and one missing entry drops the whole tag)
for f in list(data.glob(MOD + "/tags/**/*.json")) + list(data.glob("minecraft/tags/**/*.json")):
    tag = load(f)
    tag["values"] = [optional(v) if (v["id"] if isinstance(v, dict) else v).lstrip("#").startswith("minecraft:") else v for v in tag["values"]]
    save(f, tag)

# 5. biome modifiers
src = data / MOD / "neoforge" / "biome_modifier"
dst = data / MOD / "forge" / "biome_modifier"
if src.exists():
    for f in sorted(src.glob("*.json")):
        save(dst / f.name, json.loads(f.read_text(encoding="utf-8").replace('"neoforge:', '"forge:')))
    shutil.rmtree(data / MOD / "neoforge")

# 6. structure templates: the 1.20.1 data version (the blocks are 1.7.10 ones, all in 1.20.1)
def set_data_version(path):
    raw = gzip.decompress(path.read_bytes())
    # TAG_Int "DataVersion" at the root: patch the 4 bytes after the name
    key = b"\x03\x00\x0bDataVersion"
    i = raw.find(key)
    if i < 0:
        return
    j = i + len(key)
    if struct.unpack(">i", raw[j:j + 4])[0] != DATA_VERSION_1_20_1:
        raw = raw[:j] + struct.pack(">i", DATA_VERSION_1_20_1) + raw[j + 4:]
        path.write_bytes(gzip.compress(raw))


for f in data.glob("*/structures/**/*.nbt"):
    set_data_version(f)

# OBJ models: the Forge 1.20.1 loader name (Fabric's FabricObjModels reads the same files)
for f in (root / "assets").rglob("models/**/*.json"):
    text = f.read_text(encoding="utf-8")
    if '"neoforge:obj"' in text:
        f.write_text(text.replace('"neoforge:obj"', '"forge:obj"'), encoding="utf-8")

# 7. pack.mcmeta
save(root / "pack.mcmeta", {"pack": {"description": "Matter Overdrive resources", "pack_format": 15}})
print("done")
