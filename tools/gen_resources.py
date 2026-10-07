"""Generate the 1.21.10 resources for Matter Overdrive's simple content from the 1.7.10 reference assets.

    python3 -I tools/gen_resources.py ~/mo-reference/mo-1.7.10/src/main/resources/assets/mo src/main/resources

Writes models, client item definitions, blockstates, loot tables, tags, recipes, worldgen and lang
(en_us + ru_ru converted from the original .lang files), and copies the original textures.
Hand-written resources must not live at the paths this script owns: it overwrites them.
"""
import json
import shutil
import sys
from pathlib import Path

ref = Path(sys.argv[1])
out = Path(sys.argv[2])
MOD = "matteroverdrive"
A = out / "assets" / MOD
D = out / "data" / MOD

# name -> (texture file in textures/items, model parent)
ITEMS = {n: (n, "generated") for n in [
    "tritanium_ingot", "tritanium_nugget", "tritanium_dust", "tritanium_plate", "dilithium_crystal",
    "matter_dust", "matter_dust_refined", "machine_casing", "s_magnet", "h_compensator", "integration_matrix",
    "me_conversion_matrix", "forcefield_emitter", "weapon_handle", "weapon_receiver", "plasma_core",
    "isolinear_circuit_mk1", "isolinear_circuit_mk2", "isolinear_circuit_mk3", "isolinear_circuit_mk4",
    "tritanium_helmet", "tritanium_chestplate", "tritanium_leggings", "tritanium_boots"]}
ITEMS.update({n: (n, "handheld") for n in ["tritanium_sword", "tritanium_pickaxe", "tritanium_axe", "tritanium_hoe"]})
BLOCKS = ["tritanium_ore", "dilithium_ore", "tritanium_block"]

# lang keys that don't follow item.<name>.name / tile.<name>.name in the 1.7.10 files
LANG_KEYS = {f"isolinear_circuit_mk{i}": f"item.isolinear_circuit.mk{i}.name" for i in range(1, 5)}
# Strings the original translation never had.
EXTRA = {"ru_ru": {"weapon_handle": "Рукоять оружия", "weapon_receiver": "Ствольная коробка оружия",
                   "plasma_core": "Плазменное ядро"}}


def w(p, obj):
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(obj, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def cp(src, dst):
    if not src.exists():
        sys.exit(f"missing texture: {src}")
    dst.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy(src, dst)


def mid(n):
    return f"{MOD}:{n}"


# --- items and blocks -------------------------------------------------------------------------
for n, (tex, parent) in ITEMS.items():
    cp(ref / "textures/items" / f"{tex}.png", A / "textures/item" / f"{n}.png")
    w(A / "models/item" / f"{n}.json", {"parent": f"minecraft:item/{parent}", "textures": {"layer0": f"{MOD}:item/{n}"}})
    w(A / "items" / f"{n}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{n}"}})

for n in BLOCKS:
    cp(ref / "textures/blocks" / f"{n}.png", A / "textures/block" / f"{n}.png")
    w(A / "models/block" / f"{n}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MOD}:block/{n}"}})
    w(A / "blockstates" / f"{n}.json", {"variants": {"": {"model": f"{MOD}:block/{n}"}}})
    w(A / "items" / f"{n}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:block/{n}"}})

# Armor worn on the player. The 1.7.10 mod rendered a custom model with 64x64 textures
# (Tritanium_Armor2_layer_*); the vanilla-layout textures it also shipped are used until that model is ported.
w(A / "equipment/tritanium.json", {"layers": {
    "humanoid": [{"texture": mid("tritanium")}],
    "humanoid_leggings": [{"texture": mid("tritanium")}]}})
cp(ref / "textures/armor/tritanium_layer_1.png", A / "textures/entity/equipment/humanoid/tritanium.png")
cp(ref / "textures/armor/tritanium_layer_2.png", A / "textures/entity/equipment/humanoid_leggings/tritanium.png")

# --- loot tables -----------------------------------------------------------------------------
def self_drop(n):
    return {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
            "entries": [{"type": "minecraft:item", "name": mid(n)}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}]}


w(D / "loot_table/blocks/tritanium_ore.json", self_drop("tritanium_ore"))
w(D / "loot_table/blocks/tritanium_block.json", self_drop("tritanium_block"))
# Like vanilla diamond ore: silk touch keeps the ore, otherwise one crystal with fortune ore_drops.
w(D / "loot_table/blocks/dilithium_ore.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
  "entries": [{"type": "minecraft:alternatives", "children": [
      {"type": "minecraft:item", "name": mid("dilithium_ore"), "conditions": [{"condition": "minecraft:match_tool",
          "predicate": {"predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch",
                                                                   "levels": {"min": 1}}]}}}]},
      {"type": "minecraft:item", "name": mid("dilithium_crystal"), "functions": [
          {"function": "minecraft:apply_bonus", "enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops"},
          {"function": "minecraft:explosion_decay"}]}]}]}]})

# --- tags ------------------------------------------------------------------------------------
TAGS = out / "data"
w(TAGS / "minecraft/tags/block/mineable/pickaxe.json", {"values": [mid(n) for n in BLOCKS]})
w(TAGS / "minecraft/tags/block/needs_iron_tool.json", {"values": [mid("tritanium_ore"), mid("tritanium_block")]})
w(TAGS / "minecraft/tags/block/needs_diamond_tool.json", {"values": [mid("dilithium_ore")]})  # harvest level 3
w(D / "tags/item/tritanium_tool_materials.json", {"values": [mid("tritanium_ingot")]})
# Common (c:) tags so other mods' machines and recipes recognise the materials.
COMMON = {
    "ores/tritanium": ["tritanium_ore"], "ores/dilithium": ["dilithium_ore"],
    "storage_blocks/tritanium": ["tritanium_block"],
}
for tag, vals in COMMON.items():
    w(TAGS / f"c/tags/block/{tag}.json", {"values": [mid(v) for v in vals]})
    w(TAGS / f"c/tags/item/{tag}.json", {"values": [mid(v) for v in vals]})
for tag, vals in {"ingots/tritanium": ["tritanium_ingot"], "nuggets/tritanium": ["tritanium_nugget"],
                  "dusts/tritanium": ["tritanium_dust"], "plates/tritanium": ["tritanium_plate"],
                  "gems/dilithium": ["dilithium_crystal"]}.items():
    w(TAGS / f"c/tags/item/{tag}.json", {"values": [mid(v) for v in vals]})
for parent, children in {"ores": ["tritanium", "dilithium"], "storage_blocks": ["tritanium"]}.items():
    w(TAGS / f"c/tags/block/{parent}.json", {"values": [f"#c:{parent}/{c}" for c in children]})
    w(TAGS / f"c/tags/item/{parent}.json", {"values": [f"#c:{parent}/{c}" for c in children]})
for parent, children in {"ingots": ["tritanium"], "nuggets": ["tritanium"], "dusts": ["tritanium"],
                         "plates": ["tritanium"], "gems": ["dilithium"]}.items():
    w(TAGS / f"c/tags/item/{parent}.json", {"values": [f"#c:{parent}/{c}" for c in children]})
w(TAGS / "minecraft/tags/item/swords.json", {"values": [mid("tritanium_sword")]})
w(TAGS / "minecraft/tags/item/pickaxes.json", {"values": [mid("tritanium_pickaxe")]})
w(TAGS / "minecraft/tags/item/axes.json", {"values": [mid("tritanium_axe")]})
w(TAGS / "minecraft/tags/item/hoes.json", {"values": [mid("tritanium_hoe")]})
for slot in ["head", "chest", "leg", "foot"]:
    piece = {"head": "helmet", "chest": "chestplate", "leg": "leggings", "foot": "boots"}[slot]
    w(TAGS / f"minecraft/tags/item/{slot}_armor.json", {"values": [mid(f"tritanium_{piece}")]})

# --- recipes (1.7.10 MatterOverdriveRecipes; metadata items mapped to their 1.21 names) ----------
INGOT, PLATE, NUGGET = "#c:ingots/tritanium", "#c:plates/tritanium", "#c:nuggets/tritanium"
DILITHIUM = "#c:gems/dilithium"
MK = {i: mid(f"isolinear_circuit_mk{i}") for i in range(1, 5)}


def shaped(name, result, pattern, key, count=1, category="misc"):
    w(D / "recipe" / f"{name}.json", {"type": "minecraft:crafting_shaped", "category": category,
                                       "pattern": pattern, "key": key, "result": {"id": result, "count": count}})


def shapeless(name, result, ingredients, count=1):
    w(D / "recipe" / f"{name}.json", {"type": "minecraft:crafting_shapeless", "category": "misc",
                                       "ingredients": ingredients, "result": {"id": result, "count": count}})


def cook(name, ingredient, result, xp):
    for kind, time in [("smelting", 200), ("blasting", 100)]:
        w(D / "recipe" / f"{name}_from_{kind}.json", {"type": f"minecraft:{kind}", "category": "misc",
          "ingredient": ingredient, "result": {"id": result}, "experience": xp, "cookingtime": time})


cook("tritanium_ingot_from_ore", "#c:ores/tritanium", mid("tritanium_ingot"), 1.0)
cook("tritanium_ingot_from_dust", "#c:dusts/tritanium", mid("tritanium_ingot"), 0.5)
shaped("tritanium_block", mid("tritanium_block"), ["TTT", "TTT", "TTT"], {"T": INGOT}, category="building")
shapeless("tritanium_ingot_from_block", mid("tritanium_ingot"), ["#c:storage_blocks/tritanium"], 9)
shaped("tritanium_ingot_from_nuggets", mid("tritanium_ingot"), ["###", "###", "###"], {"#": NUGGET})
shapeless("tritanium_nugget", mid("tritanium_nugget"), [INGOT], 9)
shaped("tritanium_plate", mid("tritanium_plate"), ["TT"], {"T": INGOT})
shaped("isolinear_circuit_mk1", MK[1], ["I", "R", "G"],
       {"I": "minecraft:iron_ingot", "R": "minecraft:redstone", "G": "minecraft:glass"})
shaped("machine_casing", mid("machine_casing"), [" T ", "I I", "GRG"],
       {"T": PLATE, "I": INGOT, "G": "minecraft:gold_ingot", "R": "minecraft:redstone"})
shaped("s_magnet", mid("s_magnet"), ["RRR", "TET", "RRR"],
       {"R": "minecraft:redstone", "T": INGOT, "E": "minecraft:ender_pearl"}, count=4)
shaped("h_compensator", mid("h_compensator"), [" M ", "CPC", "DED"],
       {"M": mid("machine_casing"), "C": MK[1], "P": MK[2], "D": DILITHIUM, "E": "minecraft:ender_eye"})
shaped("integration_matrix", mid("integration_matrix"), [" M ", "GPG", "DED"],
       {"M": mid("machine_casing"), "G": "minecraft:glass", "P": MK[2], "D": DILITHIUM, "E": "minecraft:ender_pearl"})
shaped("me_conversion_matrix", mid("me_conversion_matrix"), ["EIE", "CDC", "EIE"],
       {"E": "minecraft:ender_pearl", "I": "minecraft:iron_ingot", "C": MK[2], "D": DILITHIUM})
shaped("forcefield_emitter", mid("forcefield_emitter"), ["CGC", "CDC", "P1P"],
       {"C": mid("s_magnet"), "G": "minecraft:glass", "D": DILITHIUM, "P": PLATE, "1": MK[2]})
shaped("weapon_handle", mid("weapon_handle"), ["TWT", "I I", "I I"],
       {"T": INGOT, "W": "minecraft:black_wool", "I": "minecraft:iron_ingot"}, category="equipment")
shaped("weapon_receiver", mid("weapon_receiver"), ["IRT", "IIT"],
       {"I": "minecraft:iron_ingot", "R": "minecraft:redstone", "T": INGOT}, category="equipment")
STICK = "minecraft:stick"
for tool, pattern in {"axe": ["XX", "X#", " #"], "pickaxe": ["XXX", " # ", " # "],
                      "sword": ["X", "X", "#"], "hoe": ["XX", " #", " #"]}.items():
    shaped(f"tritanium_{tool}", mid(f"tritanium_{tool}"), pattern, {"X": INGOT, "#": STICK}, category="equipment")
for piece, pattern in {"helmet": ["XCX", "X X"], "chestplate": ["X X", "XCX", "XXX"],
                       "leggings": ["XCX", "X X", "X X"], "boots": ["X X", "X X"]}.items():
    key = {"X": INGOT}
    if "C" in "".join(pattern):
        key["C"] = MK[2]
    shaped(f"tritanium_{piece}", mid(f"tritanium_{piece}"), pattern, key, category="equipment")

# --- world generation (1.7.10 MOWorldGen: veins per chunk, vein size, y = 4 + rand(n)) ----------
ORES = {"tritanium": (10, 6, 4, 63), "dilithium": (6, 5, 4, 31)}
for ore, (count, size, lo, hi) in ORES.items():
    w(D / f"worldgen/configured_feature/ore_{ore}.json", {"type": "minecraft:ore", "config": {
        "size": size, "discard_chance_on_air_exposure": 0.0, "targets": [{
            "target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"},
            "state": {"Name": mid(f"{ore}_ore")}}]}})
    w(D / f"worldgen/placed_feature/ore_{ore}.json", {"feature": mid(f"ore_{ore}"), "placement": [
        {"type": "minecraft:count", "count": count},
        {"type": "minecraft:in_square"},
        {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform",
            "min_inclusive": {"absolute": lo}, "max_inclusive": {"absolute": hi}}},
        {"type": "minecraft:biome"}]})
    w(D / f"neoforge/biome_modifier/ore_{ore}.json", {"type": "neoforge:add_features",
        "biomes": "#minecraft:is_overworld", "features": mid(f"ore_{ore}"), "step": "underground_ores"})


# --- lang ------------------------------------------------------------------------------------
def parse_lang(p):
    d = {}
    for line in p.read_text(encoding="utf-8", errors="replace").splitlines():
        if "=" in line and not line.startswith("#"):
            k, v = line.split("=", 1)
            d[k.strip()] = v.strip()
    return d


en = parse_lang(ref / "lang/en_US.lang")
for src_name, dst_name in [("en_US", "en_us"), ("ru_RU", "ru_ru")]:
    src = parse_lang(ref / "lang" / f"{src_name}.lang")
    lang = {"itemGroup.matteroverdrive": "Matter Overdrive"}
    fallback = []
    for n in ITEMS:
        key = LANG_KEYS.get(n, f"item.{n}.name")
        lang[f"item.{MOD}.{n}"] = src.get(key) or EXTRA.get(dst_name, {}).get(n) or en.get(key) or n
        if key not in src and n not in EXTRA.get(dst_name, {}):
            fallback.append(n)
    for n in BLOCKS:
        key = LANG_KEYS.get(n, f"tile.{n}.name")
        lang[f"block.{MOD}.{n}"] = src.get(key) or en.get(key) or n
        if key not in src:
            fallback.append(n)
    w(A / "lang" / f"{dst_name}.json", lang)
    print(f"{dst_name}: {len(lang)} keys, not in original {src_name}: {fallback or 'none'}")
