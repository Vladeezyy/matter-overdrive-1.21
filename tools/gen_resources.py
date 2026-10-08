"""Generate the 1.21.10 resources for Matter Overdrive's simple content from the 1.7.10 reference assets.

    python3 -I tools/gen_resources.py ~/mo-reference/mo-1.7.10/src/main/resources/assets/mo src/main/resources

Writes models, client item definitions, blockstates, loot tables, tags, recipes, worldgen and lang
(en_us + ru_ru converted from the original .lang files), and copies the original textures.
Hand-written resources must not live at the paths this script owns: it overwrites them.
"""
import gzip
import json
import shutil
import struct
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
ITEMS.update({n: (n, "handheld") for n in ["tritanium_sword", "tritanium_pickaxe", "tritanium_axe", "tritanium_hoe",
                                            "tritanium_wrench"]})
UPGRADES = ["base", "speed", "power", "failsafe", "range", "power_storage", "hyper_speed", "matter_storage"]
ITEMS.update({f"upgrade_{u}": (f"upgrade_{u}", "generated") for u in UPGRADES})
# Batteries: one base texture + an overlay tinted per battery (1.7.10 Battery colours: COLOR_MATTER,
# COLOR_YELLOW_STRIPES, COLOR_HOLO_RED).
BATTERIES = {"battery": (191, 228, 230), "hc_battery": (254, 203, 4), "creative_battery": (230, 80, 20)}
BLOCKS = ["tritanium_ore", "dilithium_ore", "tritanium_block"]
MACHINES = ["solar_panel", "inscriber"]
MACHINES_P3 = ["decomposer", "matter_recycler", "matter_pipe", "heavy_matter_pipe", "matter_analyzer", "pattern_storage",
               "replicator", "pattern_monitor", "network_router", "network_switch", "network_pipe",
               "gravitational_anomaly", "gravitational_stabilizer", "machine_hull"]

# lang keys that don't follow item.<name>.name / tile.<name>.name in the 1.7.10 files
LANG_KEYS = {f"isolinear_circuit_mk{i}": f"item.isolinear_circuit.mk{i}.name" for i in range(1, 5)}
LANG_KEYS.update({f"upgrade_{u}": f"item.upgrade.{u}.name" for u in UPGRADES})
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


# --- phase 2: energy items, machines, GUI textures ----------------------------------------------
for n, rgb in BATTERIES.items():
    cp(ref / "textures/items/battery.png", A / "textures/item/battery.png")
    cp(ref / "textures/items/battery_overlay.png", A / "textures/item/battery_overlay.png")
    w(A / "models/item" / f"{n}.json", {"parent": "minecraft:item/generated", "textures": {
        "layer0": f"{MOD}:item/battery", "layer1": f"{MOD}:item/battery_overlay"}})
    w(A / "items" / f"{n}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{n}", "tints": [
        {"type": "minecraft:constant", "value": -1},
        {"type": "minecraft:constant", "value": (0xFF << 24 | rgb[0] << 16 | rgb[1] << 8 | rgb[2]) - (1 << 32)}]}})

FACING_Y = {"north": 0, "east": 90, "south": 180, "west": 270}


def facing_blockstate(n, model):
    """Every machine block has facing and active; this one looks the same while working."""
    w(A / "blockstates" / f"{n}.json", {"variants": {
        f"active={a},facing={f}": ({"model": model, "y": y} if y else {"model": model})
        for f, y in FACING_Y.items() for a in ("false", "true")}})
    w(A / "items" / f"{n}.json", {"model": {"type": "minecraft:model", "model": model}})


cp(ref / "textures/blocks/base.png", A / "textures/block/base.png")
cp(ref / "textures/blocks/solar_panel.png", A / "textures/block/solar_panel.png")
w(A / "models/block/solar_panel.json", {"parent": "minecraft:block/slab", "textures": {
    "bottom": f"{MOD}:block/base", "top": f"{MOD}:block/solar_panel", "side": f"{MOD}:block/base"}})
facing_blockstate("solar_panel", f"{MOD}:block/solar_panel")

# Inscriber: the original Wavefront model. NeoForge's OBJ loader needs a material, and expects block-corner
# coordinates where the 1.7.10 model is centred on x/z, so shift it by half a block.
cp(ref / "textures/blocks/inscriber.png", A / "textures/block/inscriber.png")
obj_lines = ["mtllib inscriber.mtl", "usemtl inscriber"]
for line in (ref / "models/block/inscriber.obj").read_text().splitlines():
    if line.startswith("v "):
        _, x, y, z = line.split()
        line = f"v {float(x) + 0.5:.4f} {float(y):.4f} {float(z) + 0.5:.4f}"
    obj_lines.append(line)
(A / "models/block").mkdir(parents=True, exist_ok=True)
(A / "models/block/inscriber.obj").write_text("\n".join(obj_lines) + "\n")
(A / "models/block/inscriber.mtl").write_text("newmtl inscriber\nmap_Kd #texture\n")
w(A / "models/block/inscriber.json", {"loader": "neoforge:obj", "model": f"{MOD}:models/block/inscriber.obj",
    "flip_v": True, "textures": {"texture": f"{MOD}:block/inscriber", "particle": f"{MOD}:block/base"}})
facing_blockstate("inscriber", f"{MOD}:block/inscriber")

for n in MACHINES:
    w(D / f"loot_table/blocks/{n}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
        "entries": [{"type": "minecraft:item", "name": mid(n), "functions": [{"function": "minecraft:copy_components",
            "source": "block_entity", "include": [mid("energy")]}]}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})

# GUI: the 1.7.10 element textures, lower-cased; the machine background as a nine-slice sprite
# (1.7.10 ScaleTexture offsets left 57, right 34, top 42, bottom 34).
GUI = A / "textures/gui"
for src, dst in {"elements/Energy.png": "energy", "elements/Progress_Arrow_Right.png": "progress_arrow_right",
                 "elements/slot_big.png": "slot_big", "elements/slot_small.png": "slot_small",
                 "elements/indicator.png": "indicator", "elements/close_button.png": "close_button",
                 "elements/page_button.png": "page_button", "items/page_icon_home.png": "page_icon_home",
                 "items/page_icon_upgrades.png": "page_icon_upgrades", "items/page_icon_config.png": "page_icon_config"}.items():
    cp(ref / "textures/gui" / src, GUI / "elements" / f"{dst}.png")
cp(ref / "textures/gui/elements/base_gui_hotbar.png", GUI / "sprites/machine_background.png")
w(GUI / "sprites/machine_background.png.mcmeta", {"gui": {"scaling": {"type": "nine_slice", "width": 92, "height": 77,
    "border": {"left": 57, "top": 42, "right": 34, "bottom": 34}, "stretch_inner": True}}})

for tag in ["mineable/pickaxe", "needs_iron_tool"]:
    p = TAGS / f"minecraft/tags/block/{tag}.json"
    values = json.loads(p.read_text())["values"]
    w(p, {"values": values + [mid(n) for n in MACHINES]})

# Recipes. Upgrades, batteries and the wrench are from ItemUpgrade.register / registerItemRecipes;
# the solar panel and inscriber from registerBlockRecipes.
shaped("battery", mid("battery"), [" R ", "TGT", "TDT"],
       {"R": "minecraft:redstone", "T": INGOT, "G": "minecraft:gold_ingot", "D": DILITHIUM}, category="equipment")
shaped("hc_battery", mid("hc_battery"), [" P ", "DBD", " P "],
       {"P": PLATE, "D": DILITHIUM, "B": mid("battery")}, category="equipment")
shaped("tritanium_wrench", mid("tritanium_wrench"), ["T T", " Y ", " T "],
       {"T": INGOT, "Y": "minecraft:yellow_wool"}, category="equipment")
shaped("solar_panel", mid("solar_panel"), ["CGC", "GQG", "KMK"],
       {"C": "minecraft:coal", "G": "minecraft:glass", "Q": "minecraft:quartz", "K": MK[2], "M": mid("machine_casing")})
shaped("inscriber", mid("inscriber"), ["IDI", "TPT", "RMR"],
       {"I": "minecraft:iron_ingot", "D": DILITHIUM, "T": PLATE, "P": "minecraft:piston", "R": "minecraft:redstone",
        "M": mid("machine_casing")})
U = mid("upgrade_base")
shaped("upgrade_base", U, ["R", "C", "T"], {"R": "minecraft:redstone", "C": MK[1], "T": PLATE})
shaped("upgrade_speed", mid("upgrade_speed"), [" R ", "GUG", " E "],
       {"R": "minecraft:redstone", "G": "minecraft:glowstone_dust", "U": U, "E": "minecraft:emerald"})
shaped("upgrade_power", mid("upgrade_power"), [" B ", "RUR", " C "],
       {"B": mid("battery"), "R": "minecraft:redstone", "U": U, "C": "minecraft:quartz"})
shaped("upgrade_failsafe", mid("upgrade_failsafe"), [" D ", "RUR", " G "],
       {"D": "minecraft:diamond", "R": "minecraft:redstone", "U": U, "G": "minecraft:gold_ingot"})
shaped("upgrade_range", mid("upgrade_range"), [" E ", "RUR", " G "],
       {"E": "minecraft:ender_pearl", "R": "minecraft:redstone", "U": U, "G": "minecraft:gold_ingot"})
shaped("upgrade_power_storage", mid("upgrade_power_storage"), ["RUR", " B "],
       {"R": "minecraft:redstone", "U": U, "B": mid("hc_battery")})
shapeless("upgrade_hyper_speed", mid("upgrade_hyper_speed"), [DILITHIUM, "minecraft:nether_star", mid("upgrade_speed")])
shaped("upgrade_matter_storage", mid("upgrade_matter_storage"), [" R ", "MUM", " R "],
       {"R": "minecraft:redstone", "M": mid("s_magnet"), "U": U})
# 1.7.10 registerInscriberRecipes: circuit + material -> next circuit, energy (FE) over time (ticks).
for i, (material, energy, time) in {2: ("minecraft:gold_ingot", 64000, 300), 3: ("minecraft:diamond", 88000, 600),
                                    4: ("minecraft:emerald", 114000, 1200)}.items():
    w(D / "recipe" / f"isolinear_circuit_mk{i}.json", {"type": mid("inscriber"), "main": MK[i - 1],
        "secondary": material, "result": {"id": MK[i]}, "energy": energy, "time": time})


# --- phase 3: matter machines, pipes, matter fluid --------------------------------------------------
def machine_blockstate(n, model, active_model=None):
    """facing x active variants (active_model defaults to the idle model)."""
    variants = {}
    for f, y in FACING_Y.items():
        for active in (False, True):
            m = active_model if active and active_model else model
            variants[f"active={str(active).lower()},facing={f}"] = {"model": m, "y": y} if y else {"model": m}
    w(A / "blockstates" / f"{n}.json", {"variants": variants})
    w(A / "items" / f"{n}.json", {"model": {"type": "minecraft:model", "model": model}})


for tex in ["base_stripes", "decomposer_top", "tank_empty", "tank_full", "recycler_side", "matter_pipe", "heavy_matter_pipe",
            "network_pipe"]:
    cp(ref / "textures/blocks" / f"{tex}.png", A / "textures/block" / f"{tex}.png")
cp(ref / "textures/blocks/recycler_side_anim.png", A / "textures/block/recycler_side_anim.png")
cp(ref / "textures/blocks/recycler_side_anim.png.mcmeta", A / "textures/block/recycler_side_anim.png.mcmeta")
for t in ["matter_plasma_still", "matter_plasma_flowing"]:
    cp(ref / "textures/blocks" / f"{t}.png", A / "textures/block" / f"{t}.png")
    cp(ref / "textures/blocks" / f"{t}.png.mcmeta", A / "textures/block" / f"{t}.png.mcmeta")
cp(ref / "textures/gui/elements/Matter.png", A / "textures/gui/elements/matter.png")


def orientable(top, front, side, bottom=None):
    return {"parent": "minecraft:block/orientable_with_bottom", "textures": {
        "top": f"{MOD}:block/{top}", "front": f"{MOD}:block/{front}", "side": f"{MOD}:block/{side}",
        "bottom": f"{MOD}:block/{bottom or 'base'}"}}


# 1.7.10 BlockDecomposer: front shows the matter tank, top decomposer_top, other sides yellow stripes.
w(A / "models/block/decomposer.json", orientable("decomposer_top", "tank_empty", "base_stripes"))
w(A / "models/block/decomposer_active.json", orientable("decomposer_top", "tank_full", "base_stripes"))
machine_blockstate("decomposer", f"{MOD}:block/decomposer", f"{MOD}:block/decomposer_active")
# 1.7.10 BlockMatterRecycler: recycler_side all round (animated while working), decomposer_top on top.
w(A / "models/block/matter_recycler.json", orientable("decomposer_top", "recycler_side", "recycler_side"))
w(A / "models/block/matter_recycler_active.json", orientable("decomposer_top", "recycler_side_anim", "recycler_side_anim"))
machine_blockstate("matter_recycler", f"{MOD}:block/matter_recycler", f"{MOD}:block/matter_recycler_active")

# Pipes: multipart core + arms, 1/3-block cubes. UV quadrant (0,0)-(6,6) is the core, (6,0)-(12,6) the arm.
P0, P1 = 16 / 3, 32 / 3


def cube(frm, to, uv, rot=0):
    faces = {}
    for face in ["north", "south", "east", "west", "up", "down"]:
        faces[face] = {"uv": uv, "texture": "#pipe"} | ({"rotation": rot} if rot else {})
    return {"from": frm, "to": to, "faces": faces}


ARM_BOX = {"north": ([P0, P0, 0], [P1, P1, P0]), "south": ([P0, P0, P1], [P1, P1, 16]),
           "west": ([0, P0, P0], [P0, P1, P1]), "east": ([P1, P0, P0], [16, P1, P1]),
           "down": ([P0, 0, P0], [P1, P0, P1]), "up": ([P0, P1, P0], [P1, 16, P1])}
for pipe in ["matter_pipe", "heavy_matter_pipe", "network_pipe"]:
    tex = {"pipe": f"{MOD}:block/{pipe}", "particle": f"{MOD}:block/{pipe}"}
    w(A / f"models/block/{pipe}_core.json", {"textures": tex, "elements": [cube([P0, P0, P0], [P1, P1, P1], [0, 0, 6, 6])]})
    for d, (frm, to) in ARM_BOX.items():
        w(A / f"models/block/{pipe}_{d}.json", {"textures": tex, "elements": [
            cube(frm, to, [6, 0, 12, 6], 90 if d in ("up", "down") else 0)]})
    w(A / "blockstates" / f"{pipe}.json", {"multipart": [{"apply": {"model": f"{MOD}:block/{pipe}_core"}}] + [
        {"when": {d: "true"}, "apply": {"model": f"{MOD}:block/{pipe}_{d}"}} for d in ARM_BOX]})
    # item: a straight pipe along x, like 1.7.10's inventory render
    w(A / f"models/item/{pipe}.json", {"parent": "minecraft:block/block", "textures": tex, "elements": [
        cube([0, P0, P0], [16, P1, P1], [6, 0, 12, 6])],
        "display": {"gui": {"rotation": [30, 225, 0], "scale": [0.9, 0.9, 0.9]}}})
    w(A / "items" / f"{pipe}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{pipe}"}})

PHASE3_BLOCKS = ["decomposer", "matter_recycler", "matter_pipe", "heavy_matter_pipe"]
for n in PHASE3_BLOCKS:
    w(D / f"loot_table/blocks/{n}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
        "entries": [{"type": "minecraft:item", "name": mid(n)} | ({"functions": [{"function": "minecraft:copy_components",
            "source": "block_entity", "include": [mid("energy")]}]} if n in ("decomposer", "matter_recycler") else {})],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
for tag in ["mineable/pickaxe", "needs_iron_tool"]:
    p = TAGS / f"minecraft/tags/block/{tag}.json"
    values = json.loads(p.read_text())["values"]
    w(p, {"values": values + [mid(n) for n in PHASE3_BLOCKS if not (tag == "needs_iron_tool" and "pipe" in n)]})

shaped("decomposer", mid("decomposer"), ["TCT", "S S", "NTM"],
       {"T": PLATE, "C": MK[3], "S": "minecraft:sticky_piston", "N": mid("integration_matrix"), "M": mid("me_conversion_matrix")})
shaped("matter_recycler", mid("matter_recycler"), ["T T", "1P2", "NTM"],
       {"T": PLATE, "1": MK[1], "2": MK[2], "P": "minecraft:piston", "N": mid("integration_matrix"), "M": mid("me_conversion_matrix")})
shaped("matter_pipe", mid("matter_pipe"), [" G ", "IMI", " G "],
       {"G": "minecraft:glass", "I": "minecraft:iron_ingot", "M": mid("s_magnet")}, count=8)
shaped("heavy_matter_pipe", mid("heavy_matter_pipe"), ["RMR", "TMT", "RMR"],
       {"R": "minecraft:redstone", "M": mid("s_magnet"), "T": PLATE}, count=8)


# --- phase 3 step 3: patterns and the matter analyzer ------------------------------------------------
for tex in ["analyzer_front", "analyzer_top", "network_port", "vent2"]:
    cp(ref / "textures/blocks" / f"{tex}.png", A / "textures/block" / f"{tex}.png")
cp(ref / "textures/blocks/analyzer_front_anim.png", A / "textures/block/analyzer_front_anim.png")
cp(ref / "textures/blocks/analyzer_front_anim.png.mcmeta", A / "textures/block/analyzer_front_anim.png.mcmeta")
cp(ref / "textures/gui/elements/screen.png", A / "textures/gui/elements/screen.png")


def six_sided(front, back, sides, top, bottom="base"):
    t = lambda n: f"{MOD}:block/{n}"
    return {"parent": "minecraft:block/cube", "textures": {"north": t(front), "south": t(back), "east": t(sides),
            "west": t(sides), "up": t(top), "down": t(bottom), "particle": t(sides)}}


# 1.7.10 BlockMatterAnalyzer: front analyzer_front (animated while working), back network port, sides vent2.
w(A / "models/block/matter_analyzer.json", six_sided("analyzer_front", "network_port", "vent2", "analyzer_top"))
w(A / "models/block/matter_analyzer_active.json", six_sided("analyzer_front_anim", "network_port", "vent2", "analyzer_top"))
machine_blockstate("matter_analyzer", f"{MOD}:block/matter_analyzer", f"{MOD}:block/matter_analyzer_active")
w(D / "loot_table/blocks/matter_analyzer.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
    "entries": [{"type": "minecraft:item", "name": mid("matter_analyzer"), "functions": [{"function": "minecraft:copy_components",
        "source": "block_entity", "include": [mid("energy")]}]}],
    "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
for tag in ["mineable/pickaxe", "needs_iron_tool"]:
    p = TAGS / f"minecraft/tags/block/{tag}.json"
    w(p, {"values": json.loads(p.read_text())["values"] + [mid("matter_analyzer")]})

# Pattern drive: empty / partially full / full, chosen by custom model data set from the stored patterns.
for state in ["pattern_drive", "pattern_drive_partially_full", "pattern_drive_full"]:
    cp(ref / "textures/items" / f"{state}.png", A / "textures/item" / f"{state}.png")
    w(A / "models/item" / f"{state}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{state}"}})
w(A / "items/pattern_drive.json", {"model": {"type": "minecraft:range_dispatch", "property": "minecraft:custom_model_data",
    "entries": [{"threshold": 1, "model": {"type": "minecraft:model", "model": f"{MOD}:item/pattern_drive_partially_full"}},
                {"threshold": 2, "model": {"type": "minecraft:model", "model": f"{MOD}:item/pattern_drive_full"}}],
    "fallback": {"type": "minecraft:model", "model": f"{MOD}:item/pattern_drive"}}})
# Network flash drive: flash drive + overlay tinted COLOR_YELLOW_STRIPES (1.7.10 NetworkFlashDrive colour).
cp(ref / "textures/items/flash_drive.png", A / "textures/item/flash_drive.png")
cp(ref / "textures/items/flash_drive_overlay.png", A / "textures/item/flash_drive_overlay.png")
w(A / "models/item/network_flash_drive.json", {"parent": "minecraft:item/generated", "textures": {
    "layer0": f"{MOD}:item/flash_drive", "layer1": f"{MOD}:item/flash_drive_overlay"}})
w(A / "items/network_flash_drive.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/network_flash_drive",
    "tints": [{"type": "minecraft:constant", "value": -1},
              {"type": "minecraft:constant", "value": (0xFF << 24 | 254 << 16 | 203 << 8 | 4) - (1 << 32)}]}})

shaped("pattern_drive", mid("pattern_drive"), [" E ", "RMR", " C "],
       {"E": "minecraft:ender_pearl", "R": "minecraft:redstone", "M": mid("machine_casing"), "C": MK[2]})
shaped("network_flash_drive", mid("network_flash_drive"), ["RCR"], {"R": "minecraft:redstone", "C": MK[1]})
shaped("matter_analyzer", mid("matter_analyzer"), [" C ", "PMF", "ONO"],
       {"C": MK[3], "P": mid("pattern_drive"), "M": mid("me_conversion_matrix"), "F": mid("network_flash_drive"),
        "O": "minecraft:iron_block", "N": mid("integration_matrix")})


# --- phase 3 step 4: the matter network -----------------------------------------------------------
def obj_model(name, materials, shift_y=False, hidden=(), particle="base"):
    """Copy a 1.7.10 Wavefront model for NeoForge's OBJ loader: one material per group (as the 1.7.10 renderer
    picked an icon per group), block-corner coordinates (shift the centred models by half a block)."""
    lines = [f"mtllib {name}.mtl"]
    for line in (ref / "models/block" / f"{name}.obj").read_text().splitlines():
        if line.startswith("v "):
            _, x, y, z = line.split()
            line = f"v {float(x) + 0.5:.4f} {float(y) + (0.5 if shift_y else 0):.4f} {float(z) + 0.5:.4f}"
        if line.startswith("g "):
            lines.append(line)
            lines.append(f"usemtl {materials.get(line[2:].strip(), next(iter(materials.values())))}")
            continue
        lines.append(line)
    (A / "models/block").mkdir(parents=True, exist_ok=True)
    (A / "models/block" / f"{name}.obj").write_text("\n".join(lines) + "\n")
    mats = sorted(set(materials.values()))
    (A / "models/block" / f"{name}.mtl").write_text("".join(f"newmtl {m}\nmap_Kd #{m}\n" for m in mats))
    model = {"loader": "neoforge:obj", "model": f"{MOD}:models/block/{name}.obj", "flip_v": True,
             "textures": {m: f"{MOD}:block/{m}" for m in mats} | {"particle": f"{MOD}:block/{particle}"}}
    if hidden:
        model["visibility"] = {h: False for h in hidden}
    w(A / "models/block" / f"{name}.json", model)


for tex in ["pattern_storage", "replicator", "vent", "network_router", "network_switch", "holo_monitor", "pattern_monitor_holo"]:
    cp(ref / "textures/blocks" / f"{tex}.png", A / "textures/block" / f"{tex}.png")
# 1.7.10 RendererBlockPatternStorage: pattern_storage + vents; the drive group was drawn by the tile renderer.
obj_model("pattern_storage", {"pattern_storage": "pattern_storage", "Vents": "vent", "drive": "pattern_storage"},
          hidden=("drive",), particle="pattern_storage")
facing_blockstate("pattern_storage", f"{MOD}:block/pattern_storage")
# 1.7.10 RendererBlockReplicator: front + inside replicator, shell base, vents, back network port.
obj_model("replicator", {"Front": "replicator", "Inside": "replicator", "Shell": "base", "Vents": "vent", "Back": "network_port"},
          shift_y=True, particle="replicator")
facing_blockstate("replicator", f"{MOD}:block/replicator")
for n in ["network_router", "network_switch"]:
    w(A / f"models/block/{n}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MOD}:block/{n}"}})
    w(A / f"blockstates/{n}.json", {"variants": {"": {"model": f"{MOD}:block/{n}"}}})
    w(A / f"items/{n}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:block/{n}"}})

# Pattern monitor: a 5 px panel on the back of the block. Its screen is built from the four corners of the
# 1.7.10 connected-texture atlas holo_monitor (64x64; 8 px corners = 2 UV units), with the holo icon on top.
def quad(frm, to, uv, tex, cull=None):
    face = {"uv": uv, "texture": tex}
    return {"from": frm, "to": to, "faces": {"north": face}}


screen = [quad([0, 8, 10.99], [8, 16, 10.99], [0, 0, 2, 2], "#screen"), quad([8, 8, 10.99], [16, 16, 10.99], [14, 0, 16, 2], "#screen"),
          quad([0, 0, 10.99], [8, 8, 10.99], [0, 14, 2, 16], "#screen"), quad([8, 0, 10.99], [16, 8, 10.99], [14, 14, 16, 16], "#screen"),
          quad([4, 4, 10.9], [12, 12, 10.9], [0, 0, 16, 16], "#holo")]
body = {"from": [0, 0, 11], "to": [16, 16, 16], "faces": {d: {"texture": "#base"} for d in ["south", "east", "west", "up", "down"]}
        | {"south": {"texture": "#port"}}}
w(A / "models/block/pattern_monitor.json", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": {
    "base": f"{MOD}:block/base", "port": f"{MOD}:block/network_port", "screen": f"{MOD}:block/holo_monitor",
    "holo": f"{MOD}:block/pattern_monitor_holo", "particle": f"{MOD}:block/base"}, "elements": [body] + screen})
facing_blockstate("pattern_monitor", f"{MOD}:block/pattern_monitor")

for tex in ["refresh", "request"]:
    cp(ref / "textures/gui/items" / f"{tex}.png", A / "textures/gui/elements" / f"{tex}.png")
cp(ref / "textures/gui/elements/slot_big_main.png", A / "textures/gui/elements/slot_big_main.png")
cp(ref / "textures/gui/elements/search_field.png", A / "textures/gui/elements/search_field.png")

NET = ["pattern_storage", "replicator", "pattern_monitor", "network_router", "network_switch", "network_pipe"]
for n in NET:
    keep_energy = n in ("pattern_storage", "replicator")
    w(D / f"loot_table/blocks/{n}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
        "entries": [{"type": "minecraft:item", "name": mid(n)} | ({"functions": [{"function": "minecraft:copy_components",
            "source": "block_entity", "include": [mid("energy")]}]} if keep_energy else {})],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
for tag in ["mineable/pickaxe", "needs_iron_tool"]:
    p = TAGS / f"minecraft/tags/block/{tag}.json"
    w(p, {"values": json.loads(p.read_text())["values"] + [mid(n) for n in NET if not (tag == "needs_iron_tool" and n == "network_pipe")]})

shaped("replicator", mid("replicator"), ["PCF", "IHI", "NTM"],
       {"P": mid("pattern_drive"), "C": MK[3], "F": mid("network_flash_drive"), "I": "minecraft:iron_ingot",
        "H": mid("h_compensator"), "N": mid("integration_matrix"), "T": PLATE, "M": mid("me_conversion_matrix")})
# 1.7.10 used an undefined 'O' in the router and switch patterns, which crafted as an empty cell.
shaped("network_router", mid("network_router"), ["IGI", "DFC", " M "],
       {"I": "minecraft:iron_ingot", "G": "minecraft:glass", "D": MK[2], "F": mid("network_flash_drive"), "C": MK[1],
        "M": mid("machine_casing")})
shaped("network_switch", mid("network_switch"), [" G ", "CFC", " M "],
       {"G": "minecraft:glass", "C": MK[1], "F": mid("network_flash_drive"), "M": mid("machine_casing")})
shaped("network_pipe", mid("network_pipe"), ["IGI", "BCB", "IGI"],
       {"I": "minecraft:iron_ingot", "G": "minecraft:glass", "B": "minecraft:gold_ingot", "C": MK[1]}, count=16)
shaped("pattern_storage", mid("pattern_storage"), ["B3B", "TCT", "2M1"],
       {"B": "minecraft:black_wool", "3": MK[3], "T": INGOT, "C": "minecraft:chest", "2": MK[2], "M": mid("machine_casing"), "1": MK[1]})
# The holo sign (phase 7) isn't ported yet; a glass pane stands in for it until then.
shaped("pattern_monitor", mid("pattern_monitor"), [" H ", "1N1", " F "],
       {"H": "minecraft:glass_pane", "1": MK[2], "N": mid("network_switch"), "F": mid("network_flash_drive")})


# --- phase 4: gravitational anomaly, stabilizer, machine hull ------------------------------------------
for tex in ["gravitational_anomaly_core", "base_coil"]:
    cp(ref / "textures/blocks" / f"{tex}.png", A / "textures/block" / f"{tex}.png")
cp(ref / "textures/items/spacetime_equalizer.png", A / "textures/item/spacetime_equalizer.png")
w(A / "models/item/spacetime_equalizer.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/spacetime_equalizer"}})
w(A / "items/spacetime_equalizer.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/spacetime_equalizer"}})
# The anomaly: the 1.7.10 sphere.obj (centred, radius 0.5) with the black core texture.
obj_model("sphere", {"GeoSphere001": "gravitational_anomaly_core"}, shift_y=True, particle="gravitational_anomaly_core")
anomaly_model = json.loads((A / "models/block/sphere.json").read_text()) | {"render_type": "minecraft:cutout"}
(A / "models/block/sphere.json").unlink()
w(A / "models/block/gravitational_anomaly.json", anomaly_model)
w(A / "blockstates/gravitational_anomaly.json", {"variants": {"": {"model": f"{MOD}:block/gravitational_anomaly"}}})
w(A / "items/gravitational_anomaly.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:block/gravitational_anomaly"}})
# 1.7.10 BlockGravitationalStabilizer: front network port (the emitter), back monitor, sides vent2, top/bottom coils.
w(A / "models/block/gravitational_stabilizer.json", six_sided("network_port", "base", "vent2", "base_coil", "base_coil"))
facing_blockstate("gravitational_stabilizer", f"{MOD}:block/gravitational_stabilizer")
w(A / "models/block/machine_hull.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MOD}:block/base"}})
w(A / "blockstates/machine_hull.json", {"variants": {"": {"model": f"{MOD}:block/machine_hull"}}})
w(A / "items/machine_hull.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:block/machine_hull"}})
for n in ["gravitational_stabilizer", "machine_hull"]:
    w(D / f"loot_table/blocks/{n}.json", self_drop(n))
for tag in ["mineable/pickaxe", "needs_iron_tool"]:
    p = TAGS / f"minecraft/tags/block/{tag}.json"
    w(p, {"values": json.loads(p.read_text())["values"] + [mid("gravitational_stabilizer"), mid("machine_hull")]})
# Damage when an anomaly swallows something alive (1.7.10 DamageSource "blackHole").
w(D / "damage_type/black_hole.json", {"message_id": "matteroverdrive.black_hole", "exhaustion": 0.0, "scaling": "never"})
w(TAGS / "minecraft/tags/damage_type/bypasses_armor.json", {"values": [mid("black_hole")]})
# World gen (1.7.10 MOWorldGen: 0.5% of chunks, y = 4 + rand(60)).
w(D / "worldgen/configured_feature/gravitational_anomaly.json", {"type": mid("gravitational_anomaly"), "config": {}})
w(D / "worldgen/placed_feature/gravitational_anomaly.json", {"feature": mid("gravitational_anomaly"), "placement": [
    {"type": "minecraft:rarity_filter", "chance": 200}, {"type": "minecraft:in_square"},
    {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform", "min_inclusive": {"absolute": 4},
                                                  "max_inclusive": {"absolute": 63}}},
    {"type": "minecraft:biome"}]})
w(D / "neoforge/biome_modifier/gravitational_anomaly.json", {"type": "neoforge:add_features",
    "biomes": "#minecraft:is_overworld", "features": mid("gravitational_anomaly"), "step": "underground_decoration"})

shaped("spacetime_equalizer", mid("spacetime_equalizer"), [" M ", "EHE", " M "],
       {"M": mid("s_magnet"), "E": "minecraft:ender_pearl", "H": mid("h_compensator")}, category="equipment")
shaped("machine_hull", mid("machine_hull"), [" T ", "T T", " T "], {"T": PLATE}, category="building")
# The holo sign (phase 7) isn't ported yet; a glass pane stands in for it until then.
shaped("gravitational_stabilizer", mid("gravitational_stabilizer"), [" H ", "TST", "CMC"],
       {"H": "minecraft:glass_pane", "T": PLATE, "S": mid("spacetime_equalizer"), "C": mid("s_magnet"), "M": mid("machine_casing")})


# --- matter values (1.7.10 MatterOverdriveMatter.registerBasic*) -----------------------------------
# Base values of the matteroverdrive:matter data map; everything else is calculated from recipes at runtime.
# Ore dictionary names are mapped to today's tags. Tags come first so that single items can override them.
MATTER_TAGS = {
    "#minecraft:wool": 2, "#c:glass_blocks": 3, "#c:glass_panes": 1, "#c:cobblestones": 1, "#minecraft:logs": 16,
    "#c:sands": 2, "#c:sandstone/blocks": 4, "#minecraft:planks": 4, "#minecraft:leaves": 1, "#minecraft:saplings": 2,
    "#c:flowers/small": 1, "#c:flowers/tall": 1, "#minecraft:terracotta": 3, "#c:music_discs": 4, "#minecraft:skulls": 16,
    "#c:crops/wheat": 1, "#c:crops/carrot": 1, "#c:crops/potato": 1, "#c:nuggets/gold": 4, "#c:rods/wooden": 1,
    "#c:dusts/redstone": 4, "#c:dusts/glowstone": 2, "#c:slime_balls": 2, "#c:gems/diamond": 256, "#c:gems/quartz": 3,
    "#c:gems/lapis": 4, "#c:gems/emerald": 256, "#c:ingots/iron": 32, "#c:ingots/gold": 42, "#c:ingots/copper": 28,
    "#c:bricks/normal": 2, "#c:bricks/nether": 1,
    # regOre: ore = 2 x its product (4 x for redstone and lapis)
    "#c:ores/diamond": 512, "#c:ores/emerald": 512, "#c:ores/coal": 16, "#c:ores/redstone": 16, "#c:ores/lapis": 16,
    "#c:ores/iron": 64, "#c:ores/gold": 84, "#c:ores/quartz": 6, "#c:ores/copper": 56,
    # Not in 1.7.10: raw ores smelt into one ingot, so they hold one ingot of matter.
    "#c:raw_materials/iron": 32, "#c:raw_materials/gold": 42, "#c:raw_materials/copper": 28,
}
MATTER_ITEMS = {
    "dirt": 1, "coarse_dirt": 1, "podzol": 1, "grass_block": 1, "gravel": 2, "clay": 4, "cactus": 4, "end_stone": 6,
    "stone": 1, "soul_sand": 4, "snow_block": 2, "pumpkin": 2, "obsidian": 16, "mycelium": 5, "ice": 3, "packed_ice": 4,
    "bedrock": 1024, "sponge": 8, "vine": 1, "short_grass": 1, "fern": 1, "mossy_cobblestone": 2, "netherrack": 1,
    "stone_bricks": 2, "mossy_stone_bricks": 2, "cracked_stone_bricks": 2, "chiseled_stone_bricks": 2,
    "cobblestone_wall": 1, "cobweb": 1, "brown_mushroom": 1, "red_mushroom": 1, "brown_mushroom_block": 1,
    "red_mushroom_block": 1, "dead_bush": 1, "lily_pad": 1,
    "apple": 1, "arrow": 1, "baked_potato": 1, "beef": 2, "blaze_rod": 4, "bone": 2, "clay_ball": 1, "coal": 8,
    "charcoal": 5, "egg": 1, "cocoa_beans": 1, "ink_sac": 1, "green_dye": 1, "ender_pearl": 8, "feather": 1,
    "fermented_spider_eye": 1, "flint": 1, "cod": 1, "salmon": 1, "tropical_fish": 1, "pufferfish": 1, "ghast_tear": 8,
    "gunpowder": 2, "melon_slice": 1, "wheat_seeds": 1, "sugar": 1, "string": 1, "spider_eye": 1, "saddle": 18,
    "sugar_cane": 1, "leather": 3, "pumpkin_seeds": 1, "porkchop": 2, "cooked_porkchop": 4, "paper": 1,
    "lava_bucket": 24 + 96, "water_bucket": 12 + 96, "milk_bucket": 12 + 96, "nether_wart": 3, "nether_star": 1024,
    "iron_horse_armor": 32 * 5, "golden_horse_armor": 42 * 5, "diamond_horse_armor": 256 * 5, "experience_bottle": 32,
    "chicken": 2, "cooked_chicken": 3, "rotten_flesh": 1, "name_tag": 32, "glass_bottle": 3,
}
w(D / "data_maps/item/matter.json", {"values": {**MATTER_TAGS, **{f"minecraft:{k}": v for k, v in MATTER_ITEMS.items()}}})


# --- game test area ---------------------------------------------------------------------------------
# GameTests are laid out (size + 5) blocks apart; vanilla's 1x1x1 "minecraft:empty" structure lets tests that
# build bigger scenes overwrite their neighbours. This is that same empty structure with size 12x12x12.
EMPTY_STRUCTURE = bytes.fromhex(
    "0a0000090004" "73697a65" "0300000003" "00000001" "00000001" "00000001"
    "090008" "656e746974696573" "0000000000"
    "090006" "626c6f636b73" "0a00000001" "090003" "706f73" "0300000003" "000000000000000000000000"
    "030005" "7374617465" "00000000" "00"
    "090007" "70616c65747465" "0a00000001" "080004" "4e616d65" "000d" "6d696e6563726166743a616972" "00"
    "03000b" "4461746156657273696f6e" "000011cc" "00")
size_at = EMPTY_STRUCTURE.index(bytes.fromhex("0300000003")) + 5
area = EMPTY_STRUCTURE[:size_at] + struct.pack(">3i", 12, 12, 12) + EMPTY_STRUCTURE[size_at + 12:]
(D / "structure").mkdir(parents=True, exist_ok=True)
with gzip.open(D / "structure/gametest_area.nbt", "wb") as f:
    f.write(area)


# --- lang ------------------------------------------------------------------------------------
def parse_lang(p):
    d = {}
    for line in p.read_text(encoding="utf-8", errors="replace").splitlines():
        if "=" in line and not line.startswith("#"):
            k, v = line.split("=", 1)
            d[k.strip()] = v.strip()
    return d

# Our key -> 1.7.10 key, or literal strings where the original had none.
GUI_KEYS = {
    "gui.matteroverdrive.page.home": "gui.tooltip.page.home",
    "gui.matteroverdrive.page.upgrades": "gui.tooltip.page.upgrades",
    "gui.matteroverdrive.page.config": "gui.tooltip.page.configurations",
    "gui.matteroverdrive.redstone_mode.low": "gui.redstone_mode.low",
    "gui.matteroverdrive.redstone_mode.high": "gui.redstone_mode.high",
    "gui.matteroverdrive.redstone_mode.disabled": "gui.redstone_mode.disabled",
    "gui.matteroverdrive.config.redstone": {"en_us": "Redstone Mode", "ru_ru": "Режим редстоуна"},
    "gui.matteroverdrive.generating": {"en_us": "+%s FE/t", "ru_ru": "+%s FE/т"},
    "tooltip.matteroverdrive.energy_stored": {"en_us": "Energy: %s / %s", "ru_ru": "Энергия: %s / %s"},
    "tooltip.matteroverdrive.matter": {"en_us": "Matter: %s kM", "ru_ru": "Материя: %s kM"},
    "tooltip.matteroverdrive.matter_stored": {"en_us": "Matter: %s / %s kM", "ru_ru": "Материя: %s / %s kM"},
    "item.matteroverdrive.matter_dust.details": "item.matter_dust.details",
    "item.matteroverdrive.pattern_drive.details": "item.pattern_drive.details",
    "gui.matteroverdrive.refresh": "gui.tooltip.button.refresh",
    "death.attack.matteroverdrive.black_hole": "death.attack.blackHole",
    "death.attack.matteroverdrive.black_hole.player": "death.attack.blackHole",
    "gui.matteroverdrive.request": "gui.tooltip.button.request",
    "gui.matteroverdrive.search": {"en_us": "Search", "ru_ru": "Поиск"},
    "gui.matteroverdrive.pattern": {"en_us": "%s (pattern %s%%)", "ru_ru": "%s (шаблон %s%%)"},
    "gui.matteroverdrive.queue": {"en_us": "Queue: %s requests, %s items", "ru_ru": "Очередь: %s заказов, %s предметов"},
    "gui.matteroverdrive.replicating": {"en_us": "%s x%s (pattern %s%%)", "ru_ru": "%s x%s (шаблон %s%%)"},
    "fluid.matteroverdrive.matter_plasma": {"en_us": "Matter Plasma", "ru_ru": "Плазменная материя"},
    "tooltip.matteroverdrive.energy_io": {"en_us": "Input/Output: %s/%s FE/t", "ru_ru": "Вход/выход: %s/%s FE/т"},
    "upgrade_type.matteroverdrive.speed": "upgradetype.Speed.name",
    "upgrade_type.matteroverdrive.power_usage": "upgradetype.PowerUsage.name",
    "upgrade_type.matteroverdrive.output": "upgradetype.Output.name",
    "upgrade_type.matteroverdrive.second_output": "upgradetype.SecondOutput.name",
    "upgrade_type.matteroverdrive.fail": "upgradetype.Fail.name",
    "upgrade_type.matteroverdrive.range": "upgradetype.Range.name",
    "upgrade_type.matteroverdrive.power_storage": "upgradetype.PowerStorage.name",
    "upgrade_type.matteroverdrive.power_transfer": {"en_us": "Power Transfer", "ru_ru": "Передача мощности"},
    "upgrade_type.matteroverdrive.matter_storage": "upgradetype.MatterStorage.name",
    "upgrade_type.matteroverdrive.matter_transfer": {"en_us": "Matter Transfer", "ru_ru": "Передача материи"},
}


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
    for n in list(BATTERIES) + ["pattern_drive", "network_flash_drive", "spacetime_equalizer"]:
        lang[f"item.{MOD}.{n}"] = src.get(f"item.{n}.name") or en.get(f"item.{n}.name")
    for n in BLOCKS + MACHINES + MACHINES_P3:
        key = LANG_KEYS.get(n, f"tile.{n}.name")
        lang[f"block.{MOD}.{n}"] = src.get(key) or en.get(key) or n
        if key not in src:
            fallback.append(n)
    for ours, theirs in GUI_KEYS.items():
        if isinstance(theirs, dict):
            lang[ours] = theirs[dst_name]
        else:
            lang[ours] = src.get(theirs) or en[theirs]
            if theirs not in src:
                fallback.append(ours)
    w(A / "lang" / f"{dst_name}.json", lang)
    print(f"{dst_name}: {len(lang)} keys, not in original {src_name}: {fallback or 'none'}")
