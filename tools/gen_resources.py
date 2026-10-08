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
# 1.7.10 WeaponModuleBarrel subtypes became one item each; the sniper scope (phase 5c).
BARRELS = ["damage", "fire", "explosion", "heal"]
ITEMS.update({f"weapon_module_barrel_{b}": (f"barrel_{b}", "generated") for b in BARRELS})
ITEMS["sniper_scope"] = ("sniper_scope", "generated")
# Batteries: one base texture + an overlay tinted per battery (1.7.10 Battery colours: COLOR_MATTER,
# COLOR_YELLOW_STRIPES, COLOR_HOLO_RED).
BATTERIES = {"battery": (191, 228, 230), "hc_battery": (254, 203, 4), "creative_battery": (230, 80, 20)}
BLOCKS = ["tritanium_ore", "dilithium_ore", "tritanium_block"]
MACHINES = ["solar_panel", "inscriber"]
MACHINES_P3 = ["decomposer", "matter_recycler", "matter_pipe", "heavy_matter_pipe", "matter_analyzer", "pattern_storage",
               "replicator", "pattern_monitor", "network_router", "network_switch", "network_pipe",
               "gravitational_anomaly", "gravitational_stabilizer", "machine_hull", "fusion_reactor_coil", "fusion_reactor_io",
               "fusion_reactor_controller"]

# lang keys that don't follow item.<name>.name / tile.<name>.name in the 1.7.10 files
LANG_KEYS = {f"isolinear_circuit_mk{i}": f"item.isolinear_circuit.mk{i}.name" for i in range(1, 5)}
LANG_KEYS.update({f"upgrade_{u}": f"item.upgrade.{u}.name" for u in UPGRADES})
LANG_KEYS.update({f"weapon_module_barrel_{b}": f"item.weapon_module_barrel.{b}.name" for b in BARRELS})
# Strings the original translation never had.
EXTRA = {"ru_ru": {"weapon_handle": "Рукоять оружия", "weapon_receiver": "Ствольная коробка оружия",
                   "plasma_core": "Плазменное ядро", "entity.mutant_scientist.name": "Учёный-мутант"}}


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
def obj_model(name, materials, shift_y=False, hidden=(), particle="base", tints=None):
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
    (A / "models/block" / f"{name}.mtl").write_text("".join(
        f"newmtl {m}\nmap_Kd #{m}\n" + (f"neoforge_TintIndex {tints[m]}\n" if tints and m in tints else "") for m in mats))
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
       {"H": mid("holo_sign"), "1": MK[2], "N": mid("network_switch"), "F": mid("network_flash_drive")})


# --- phase 4: gravitational anomaly, stabilizer, machine hull ------------------------------------------
for tex in ["gravitational_anomaly_core", "base_coil"]:
    cp(ref / "textures/blocks" / f"{tex}.png", A / "textures/block" / f"{tex}.png")
cp(ref / "textures/items/spacetime_equalizer.png", A / "textures/item/spacetime_equalizer.png")
w(A / "models/item/spacetime_equalizer.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/spacetime_equalizer"}})
w(A / "items/spacetime_equalizer.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/spacetime_equalizer"}})
# The anomaly: the 1.7.10 sphere.obj (centred, radius 0.5) with the black core texture.
# In the world the anomaly is drawn by AnomalyRenderer (1.7.10 TileEntityRendererGravitationalAnomaly: black sphere +
# core/glow billboard); the block model only carries the particle. The item shows the sphere.obj in black.
for tex in ["black", "gravitational_anomaly_glow"]:
    cp(ref / "textures/blocks" / f"{tex}.png", A / "textures/block" / f"{tex}.png")
obj_model("sphere", {"GeoSphere001": "black"}, shift_y=True, particle="gravitational_anomaly_core")
w(A / "models/block/gravitational_anomaly.json", {"textures": {"particle": f"{MOD}:block/gravitational_anomaly_core"}})
w(A / "blockstates/gravitational_anomaly.json", {"variants": {"": {"model": f"{MOD}:block/gravitational_anomaly"}}})
w(A / "items/gravitational_anomaly.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:block/sphere"}})
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
       {"H": mid("holo_sign"), "T": PLATE, "S": mid("spacetime_equalizer"), "C": mid("s_magnet"), "M": mid("machine_casing")})


# --- phase 4: fusion reactor ---------------------------------------------------------------------------
for n, tex in {"fusion_reactor_coil": "base_stripes", "fusion_reactor_io": "network_port"}.items():
    w(A / f"models/block/{n}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MOD}:block/{tex}"}})
    w(A / f"blockstates/{n}.json", {"variants": {"": {"model": f"{MOD}:block/{n}"}}})
    w(A / f"items/{n}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:block/{n}"}})
# 1.7.10 BlockFusionReactorController: a monitor in front (its holo_monitor frame corners), decomposer tops left and
# right, yellow stripes elsewhere.
front = [quad([8, 8, -0.01], [16, 16, -0.01], [0, 0, 2, 2], "#screen"), quad([0, 8, -0.01], [8, 16, -0.01], [14, 0, 16, 2], "#screen"),
         quad([8, 0, -0.01], [16, 8, -0.01], [0, 14, 2, 16], "#screen"), quad([0, 0, -0.01], [8, 8, -0.01], [14, 14, 16, 16], "#screen")]
w(A / "models/block/fusion_reactor_controller.json", {"parent": "minecraft:block/block", "textures": {
    "stripes": f"{MOD}:block/base_stripes", "side": f"{MOD}:block/decomposer_top", "screen": f"{MOD}:block/holo_monitor",
    "particle": f"{MOD}:block/base_stripes"}, "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
        "north": {"texture": "#stripes", "cullface": "north"}, "south": {"texture": "#stripes", "cullface": "south"},
        "up": {"texture": "#stripes", "cullface": "up"}, "down": {"texture": "#stripes", "cullface": "down"},
        "east": {"texture": "#side", "cullface": "east"}, "west": {"texture": "#side", "cullface": "west"}}}] + front})
facing_blockstate("fusion_reactor_controller", f"{MOD}:block/fusion_reactor_controller")
FUSION = ["fusion_reactor_coil", "fusion_reactor_io", "fusion_reactor_controller"]
for n in FUSION:
    w(D / f"loot_table/blocks/{n}.json", self_drop(n))
for tag in ["mineable/pickaxe", "needs_iron_tool"]:
    p = TAGS / f"minecraft/tags/block/{tag}.json"
    w(p, {"values": json.loads(p.read_text())["values"] + [mid(n) for n in FUSION]})
shaped("fusion_reactor_coil", mid("fusion_reactor_coil"), ["TMT", "M M", "CMC"], {"T": PLATE, "M": mid("s_magnet"), "C": MK[1]})
shaped("fusion_reactor_io", mid("fusion_reactor_io"), ["TGT", "C C", "TGT"], {"T": PLATE, "G": "minecraft:gold_ingot", "C": MK[1]})
# The holo sign (phase 7) isn't ported yet; a glass pane stands in for it until then.
shaped("fusion_reactor_controller", mid("fusion_reactor_controller"), ["CHC", "2M3", "CTC"],
       {"C": mid("fusion_reactor_coil"), "H": mid("holo_sign"), "2": MK[2], "3": MK[3], "M": mid("machine_casing"), "T": PLATE})


# --- phase 5a: energy weapons ---------------------------------------------------------------------------
import subprocess


def weapon_obj(name, src_name, texture, scale, flip=False, hidden=(), display=None):
    """A 1.7.10 weapon .obj as an item model. Keeps the model's origin (the grip) at the block centre and the
    1.7.10 renderer's relative scale (SCALE / 0.06 rifle units; the rifle is ~1 block long); flip turns a model
    that 1.7.10 rendered facing the other way (the shotgun) by 180 degrees so one display transform fits all."""
    lines = (ref / "models/item" / f"{src_name}.obj").read_text().splitlines()
    k = scale / (0.06 * 103.6)
    out = [f"mtllib {name}.mtl", "usemtl weapon"]
    for l in lines:
        if l.startswith("v "):
            x, y, z = map(float, l.split()[1:4])
            if flip:
                x, z = -x, -z
            l = f"v {x * k + 0.5:.5f} {y * k + 0.5:.5f} {z * k + 0.5:.5f}"
        elif l.startswith("vn ") and flip:
            x, y, z = map(float, l.split()[1:4])
            l = f"vn {-x} {y} {-z}"
        elif l.startswith("usemtl") or l.startswith("mtllib"):
            continue
        out.append(l)
    (A / "models/item").mkdir(parents=True, exist_ok=True)
    (A / "models/item" / f"{name}.obj").write_text("\n".join(out) + "\n")
    (A / "models/item" / f"{name}.mtl").write_text("newmtl weapon\nmap_Kd #weapon\n")
    cp(ref / "textures/items" / f"{texture}.png", A / "textures/item" / f"{texture}.png")
    w(A / "models/item" / f"{name}.json", {"loader": "neoforge:obj", "model": f"{MOD}:models/item/{name}.obj", "flip_v": True,
        "textures": {"weapon": f"{MOD}:item/{texture}", "particle": f"{MOD}:item/{texture}"},
        "display": WEAPON_DISPLAY | (display or {})} | ({"visibility": {h: False for h in hidden}} if hidden else {}))
    w(A / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{name}"}})


WEAPON_DISPLAY = {
    "firstperson_righthand": {"rotation": [0, 175, 0], "translation": [-3, 5, 2], "scale": [1.4, 1.4, 1.4]},
    "firstperson_lefthand": {"rotation": [0, 185, 0], "translation": [-3, 5, 2], "scale": [1.4, 1.4, 1.4]},
    "thirdperson_righthand": {"rotation": [0, 180, 0], "translation": [0, 2, 2], "scale": [1, 1, 1]},
    "thirdperson_lefthand": {"rotation": [0, 180, 0], "translation": [0, 2, 2], "scale": [1, 1, 1]},
    "gui": {"rotation": [0, 90, -40], "translation": [0, 0, 0], "scale": [1.45, 1.45, 1.45]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.6, 0.6, 0.6]},
    "fixed": {"rotation": [0, 90, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
}
weapon_obj("phaser_rifle", "phaser_rifle", "phaser_rifle", 0.06)
weapon_obj("plasma_shotgun", "plasma_shotgun", "plasma_shotgun", 0.85, flip=True)
weapon_obj("ion_sniper", "ion_sniper", "ion_sniper", 0.06)
# 1.7.10 ItemRendererPhaser: phaser2.obj at scale 6; it drew the barrel part matching the barrel module (none for now).
weapon_obj("phaser", "phaser2", "phaser2", 6.0, hidden=("weapon_module_barrel_damage", "weapon_module_barrel_explosion",
                                                        "weapon_module_barrel_fire", "weapon_module_barrel_heal"),
           display={"firstperson_righthand": {"rotation": [0, 175, 0], "translation": [-1, 4, 1], "scale": [0.75, 0.75, 0.75]},
                    "firstperson_lefthand": {"rotation": [0, 185, 0], "translation": [-1, 4, 1], "scale": [0.75, 0.75, 0.75]}})
# The barrel part shown follows the barrel module (1.7.10 WeaponItemRenderer.renderBarrel): one model per barrel, picked by
# the matteroverdrive:barrel select property.
PHASER_BARRELS = ["damage", "explosion", "fire", "heal"]
phaser_display = json.loads((A / "models/item/phaser.json").read_text())
for b in PHASER_BARRELS:
    w(A / "models/item" / f"phaser_{b}.json", phaser_display | {"visibility": {
        f"weapon_module_barrel_{o}": False for o in ["none"] + [x for x in PHASER_BARRELS if x != b]}})
w(A / "items/phaser.json", {"model": {"type": "minecraft:select", "property": mid("barrel"),
    "cases": [{"when": b, "model": {"type": "minecraft:model", "model": f"{MOD}:item/phaser_{b}"}} for b in PHASER_BARRELS],
    "fallback": {"type": "minecraft:model", "model": f"{MOD}:item/phaser"}}})
cp(ref / "textures/fx/plasmabeam.png", A / "textures/fx/plasmabeam.png")
# 1.7.10 ItemRendererOmniTool: wielder.obj at scale 7; renderGun drew the arms, grip, barrel, hull, rails and indicator
# (not the level/kill indicators, Group7749 or the dig_effect used by the beam renderer).
OMNI_HIDDEN = ("Group7749", "level_bg", "level_slider", "kill_indicator", "dig_effect")
OMNI_BARRELS = ["damage", "fire"]
weapon_obj("omni_tool", "wielder", "wielder", 7.0, hidden=OMNI_HIDDEN + tuple(f"weapon_module_barrel_{b}" for b in OMNI_BARRELS),
           display={"firstperson_righthand": {"rotation": [0, 175, 0], "translation": [-1, 4, 1], "scale": [0.75, 0.75, 0.75]},
                    "firstperson_lefthand": {"rotation": [0, 185, 0], "translation": [-1, 4, 1], "scale": [0.75, 0.75, 0.75]}})
omni_display = json.loads((A / "models/item/omni_tool.json").read_text())
for b in OMNI_BARRELS:
    w(A / "models/item" / f"omni_tool_{b}.json", omni_display | {"visibility": {h: False for h in OMNI_HIDDEN} | {
        f"weapon_module_barrel_{o}": False for o in ["none"] + [x for x in OMNI_BARRELS if x != b]}})
w(A / "items/omni_tool.json", {"model": {"type": "minecraft:select", "property": mid("barrel"),
    "cases": [{"when": b, "model": {"type": "minecraft:model", "model": f"{MOD}:item/omni_tool_{b}"}} for b in OMNI_BARRELS],
    "fallback": {"type": "minecraft:model", "model": f"{MOD}:item/omni_tool"}}})

def tinted_item(name, base, overlay, rgb):
    w(A / "models/item" / f"{name}.json", {"parent": "minecraft:item/generated", "textures": {
        "layer0": f"{MOD}:item/{base}", "layer1": f"{MOD}:item/{overlay}"}})
    w(A / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{name}", "tints": [
        {"type": "minecraft:constant", "value": -1},
        {"type": "minecraft:constant", "value": (0xFF << 24 | rgb >> 16 << 16 | (rgb >> 8 & 255) << 8 | (rgb & 255)) - (1 << 32)}]}})


for t in ["container_2", "container_2_overlay", "weapon_module_color", "weapon_module_color_overlay", "container"]:
    cp(ref / "textures/items" / f"{t}.png", A / "textures/item" / f"{t}.png")
tinted_item("energy_pack", "container_2", "container_2_overlay", 0xE65014)          # COLOR_HOLO_RED
COLOR_NAMES = ["red", "green", "blue", "brown", "pink", "sky_blue", "gold", "lime_green", "black", "grey"]
COLOR_VALUES = [0xCC0000, 0x009933, 0x0066FF, 0x663333, 0xFF99FF, 0x99CCFF, 0xD4AF37, 0x66FF66, 0x1E1E1E, 0x808080]
for n, c in zip(COLOR_NAMES, COLOR_VALUES):
    tinted_item(f"weapon_module_color_{n}", "weapon_module_color", "weapon_module_color_overlay", c)
w(A / "models/item/matter_container.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/container"}})
w(A / "items/matter_container.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/matter_container"}})
# 1.7.10 MatterContainer full: 3 render passes, the centre fill in COLOR_MATTER and the bottom fill in COLOR_YELLOW_STRIPES
for t in ["container_center_fill", "container_bottom_fill"]:
    cp(ref / "textures/items" / f"{t}.png", A / "textures/item" / f"{t}.png")
w(A / "models/item/matter_container_full.json", {"parent": "minecraft:item/generated", "textures": {
    "layer0": f"{MOD}:item/container", "layer1": f"{MOD}:item/container_center_fill", "layer2": f"{MOD}:item/container_bottom_fill"}})
w(A / "items/matter_container_full.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/matter_container_full",
    "tints": [{"type": "minecraft:constant", "value": -1}, {"type": "minecraft:constant", "value": (0xFFBFE4E6) - (1 << 32)},
              {"type": "minecraft:constant", "value": (0xFFFECB04) - (1 << 32)}]}})
# The placed Matter Plasma (rendered by the fluid renderer; the model only gives the break particles)
w(A / "models/block/matter_plasma.json", {"textures": {"particle": f"{MOD}:block/matter_plasma_still"}})
w(A / "blockstates/matter_plasma.json", {"variants": {"": {"model": f"{MOD}:block/matter_plasma"}}})
cp(ref / "textures/entities/PlasmaFire.png", A / "textures/entity/plasma_fire.png")

# Sounds: Minecraft only positions and attenuates mono sounds, so stereo originals are downmixed (needs ffmpeg + oggenc).
SOUNDS = {"phaser_rifle_shot": ["weapon/phaser_rifle_shot"], "plasma_shotgun_shot": ["weapon/plasma_shotgun_shot"],
          "sniper_rifle_fire": ["weapon/sniper_rifle_fire"], "reload": ["weapon/reload"], "overheat": ["weapon/overheat_med"],
          "overheat_alarm": ["weapon/overheat_alarm"], "phaser_beam": ["phaser/phaser_beam_0", "phaser/phaser_beam_1"],
          "phaser_switch_mode": ["phaser/phaser_switch_mode"],
          "laser_fire": ["weapon/laser_fire_0"], "omni_tool_hum": ["weapon/omni_tool_hum"],
          # androids (phase 6)
          "glitch": [f"gui/glitch_{i}" for i in range(11)], "transformation_music": ["music/transformation_music"],
          "biotic_stat_unlock": ["gui/biotic_stat_unlock"], "android_teleport": ["entities/android_teleport"],
          "shield_loop": ["shield_loop"], "shield_hit": ["shield_hit_0", "shield_hit_1"], "shield_power_up": ["shield_power_up"],
          "shield_power_down": ["shield_power_up"], "cloak_on": ["entities/cloak_on"], "cloak_off": ["entities/cloak_off"],
          "night_vision": ["night_vision"], "power_down": ["power_down"], "shockwave": ["shockwave"],
          "rogue_android_say": [f"entities/rogue_android_say_{i}" for i in range(3)],
          "rogue_android_death": [f"entities/rogue_android_death_{i}" for i in range(2)],
          # failed animals (phase 7c)
          **{f"failed_animal_idle_{a}": [f"entities/failed_animal_idle_{a}"] for a in ["pig", "cow", "chicken", "sheep"]},
          "failed_animal_die": [f"entities/failed_animal_die_{i}" for i in range(2)],
          "crate_open": ["blocks/crate_open"], "crate_close": ["blocks/crate_close"],
          "scanner_scanning": ["matter_scanner/scanner_scanning"], "scanner_success": ["matter_scanner/scanner_success_2"],
          "scanner_fail": ["matter_scanner/scanner_fail"], "scanner_beep": ["matter_scanner/scanner_beep"]}
SOUND_CATEGORY = {k: "neutral" for k in SOUNDS if k.startswith("failed_animal")} | {k: "hostile" for k in SOUNDS if k.startswith("rogue_android")} | \
                 {k: "block" for k in SOUNDS if k.startswith("crate_")}
for files in SOUNDS.values():
    for f in files:
        dst = A / "sounds" / f"{f}.ogg"
        dst.parent.mkdir(parents=True, exist_ok=True)
        # ffmpeg downmixes to a mono WAV, oggenc (vorbis-tools) encodes it: ffmpeg's own Vorbis encoder is stereo-only
        wav = dst.with_suffix(".wav")
        subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", str(ref / "sounds" / f"{f}.ogg"), "-ac", "1", "-map_metadata", "-1",
                        "-fflags", "+bitexact", "-flags:a", "+bitexact", str(wav)], check=True)
        subprocess.run(["oggenc", "-Q", "-q", "5", "-s", "1", "-o", str(dst), str(wav)], check=True)
        wav.unlink()
w(A / "sounds.json", {k: {"category": SOUND_CATEGORY.get(k, "player"), "sounds": [f"{MOD}:{f}" for f in v]} for k, v in SOUNDS.items()})

w(D / "damage_type/plasma.json", {"message_id": "matteroverdrive.plasma", "exhaustion": 0.1, "scaling": "when_caused_by_living_non_player"})
w(TAGS / "minecraft/tags/damage_type/is_projectile.json", {"values": [mid("plasma")]})

shaped("phaser", mid("phaser"), ["IGI", "IPH", "WCW"],
       {"I": "minecraft:iron_ingot", "G": "minecraft:glass", "P": mid("plasma_core"), "H": mid("weapon_handle"), "W": "#minecraft:wool",
        "C": MK[3]}, category="equipment")
shaped("phaser_rifle", mid("phaser_rifle"), ["III", "SPC", "WHB"],
       {"I": "minecraft:iron_ingot", "S": mid("weapon_receiver"), "P": mid("plasma_core"), "C": MK[3], "W": "#minecraft:wool",
        "H": mid("weapon_handle"), "B": mid("battery")}, category="equipment")
shaped("omni_tool", mid("omni_tool"), ["IFC", "SPI", " BH"],
       {"I": "minecraft:iron_ingot", "F": mid("forcefield_emitter"), "C": MK[3], "S": mid("weapon_receiver"), "P": mid("plasma_core"),
        "B": mid("battery"), "H": mid("weapon_handle")}, category="equipment")
shaped("plasma_shotgun", mid("plasma_shotgun"), ["SP ", "ICH", "SPB"],
       {"S": mid("weapon_receiver"), "P": mid("plasma_core"), "I": "minecraft:iron_ingot", "C": MK[3], "H": mid("weapon_handle"),
        "B": mid("battery")}, category="equipment")
shaped("ion_sniper", mid("ion_sniper"), ["ICI", "SPP", " HB"],
       {"I": "minecraft:iron_ingot", "C": MK[4], "S": mid("weapon_receiver"), "P": mid("plasma_core"), "H": mid("weapon_handle"),
        "B": mid("battery")}, category="equipment")
shaped("plasma_core", mid("plasma_core"), ["GI ", "MCM", " IG"],
       {"G": "minecraft:glass", "I": "minecraft:iron_ingot", "M": mid("s_magnet"), "C": mid("matter_container")})
shaped("matter_container", mid("matter_container"), ["TMT", " T "], {"T": INGOT, "M": mid("s_magnet")}, count=4)
# 1.7.10 EnergyPackRecipe: tritanium plate + a charged battery + gunpowder = one pack per 32000 FE in the battery.
w(D / "recipe/energy_pack.json", {"type": mid("energy_pack"), "category": "misc"})


# --- phase 5c: weapon modules and the weapon station ---------------------------------------------------
# 1.7.10 WeaponModuleBarrel.register and MatterOverdriveRecipes (sniper scope, weapon station).
shaped("weapon_module_barrel_damage", mid("weapon_module_barrel_damage"), [" G ", "RDR", " T "],
       {"G": "minecraft:glass", "R": "minecraft:redstone", "D": DILITHIUM, "T": PLATE}, category="equipment")
shaped("weapon_module_barrel_fire", mid("weapon_module_barrel_fire"), [" G ", "BFB", " T "],
       {"G": "minecraft:glass", "B": "minecraft:blaze_rod", "F": "minecraft:fire_charge", "T": PLATE}, category="equipment")
shaped("weapon_module_barrel_explosion", mid("weapon_module_barrel_explosion"), [" B ", "BRB", "DTD"],
       {"B": "minecraft:tnt", "R": "minecraft:blaze_rod", "D": "minecraft:diamond", "T": PLATE}, category="equipment")
shaped("weapon_module_barrel_heal", mid("weapon_module_barrel_heal"), [" S ", "SAS", "ETE"],
       {"S": "minecraft:sugar", "A": "minecraft:golden_apple", "E": "minecraft:emerald", "T": PLATE}, category="equipment")
shaped("sniper_scope", mid("sniper_scope"), ["IIC", "GFG", "III"],
       {"I": "minecraft:iron_ingot", "C": MK[2], "G": "minecraft:lime_stained_glass_pane", "F": mid("forcefield_emitter")},
       category="equipment")
shaped("weapon_station", mid("weapon_station"), ["GFR", "CMB"],
       {"G": "minecraft:glowstone_dust", "F": mid("forcefield_emitter"), "R": "minecraft:redstone", "C": MK[3],
        "M": mid("machine_casing"), "B": mid("battery")})

# Weapon station: a 9/16 high table (1.7.10 block bounds), top / side / bottom textures, light 10.
for t in ["top", "side", "bottom"]:
    cp(ref / "textures/blocks" / f"weapon_station_{t}.png", A / "textures/block" / f"weapon_station_{t}.png")
w(A / "models/block/weapon_station.json", {"parent": "minecraft:block/block", "textures": {
    "top": f"{MOD}:block/weapon_station_top", "side": f"{MOD}:block/weapon_station_side",
    "bottom": f"{MOD}:block/weapon_station_bottom", "particle": f"{MOD}:block/weapon_station_side"},
    "elements": [{"from": [0, 0, 0], "to": [16, 9, 16], "faces": {
        "up": {"texture": "#top"}, "down": {"texture": "#bottom", "cullface": "down"},
        **{f: {"uv": [0, 7, 16, 16], "texture": "#side", "cullface": f} for f in ["north", "south", "east", "west"]}}}]})
facing_blockstate("weapon_station", f"{MOD}:block/weapon_station")
w(D / "loot_table/blocks/weapon_station.json", self_drop("weapon_station"))
for tag in ["mineable/pickaxe", "needs_iron_tool"]:
    p = TAGS / f"minecraft/tags/block/{tag}.json"
    w(p, {"values": json.loads(p.read_text())["values"] + [mid("weapon_station")]})
# Weapon station GUI: the module slot holo icons.
for n in ["battery", "color", "barrel", "sights", "module"]:
    cp(ref / "textures/gui/items" / f"{n}.png", GUI / "elements" / f"holo_{n}.png")

# 1.7.10 WeaponModuleColor.addToDunguns: every colour module in dungeon, desert temple, mineshaft, stronghold corridor and
# blacksmith chests at weight 1. Loot tables can't be appended to directly any more, so a global loot modifier adds a
# roll of this table (one random colour module, 20% of chests) to the same chests.
CHESTS = ["simple_dungeon", "desert_pyramid", "abandoned_mineshaft", "stronghold_corridor", "village/village_weaponsmith"]
w(D / "loot_table/chests/weapon_module_colors.json", {"type": "minecraft:chest", "pools": [{"rolls": 1, "bonus_rolls": 0,
    "conditions": [{"condition": "minecraft:random_chance", "chance": 0.2}],
    "entries": [{"type": "minecraft:item", "name": mid(f"weapon_module_color_{n}")} for n in COLOR_NAMES]}]})
w(D / "loot_modifiers/weapon_module_colors.json", {"type": "neoforge:add_table", "table": mid("chests/weapon_module_colors"),
    "conditions": [{"condition": "minecraft:any_of", "terms": [
        {"condition": "neoforge:loot_table_id", "loot_table_id": f"minecraft:chests/{c}"} for c in CHESTS]}]})


# --- phase 6: androids -------------------------------------------------------------------------------
for t in ["pill_bottom", "pill_top"]:
    cp(ref / "textures/items" / f"{t}.png", A / "textures/item" / f"{t}.png")
for n, c in {"red": 0xD00000, "blue": 0x019FEA, "yellow": 0xFFE400}.items():     # 1.7.10 AndroidPill colours
    tinted_item(f"android_pill_{n}", "pill_bottom", "pill_top", c)
# 1.7.10 manageTurning: an absolute, armour-piercing hit.
w(D / "damage_type/android_transformation.json", {"message_id": "matteroverdrive.android_transformation", "exhaustion": 0.0,
                                                  "scaling": "never"})
for tag in ["bypasses_armor", "bypasses_effects", "bypasses_enchantments", "bypasses_resistance"]:
    p = TAGS / f"minecraft/tags/damage_type/{tag}.json"
    values = json.loads(p.read_text())["values"] if p.exists() else []
    w(p, {"values": values + [mid("android_transformation")]})
# 1.7.10 BioticStatShockwave.ShockwaveDamage: an armour-piercing explosion.
w(D / "damage_type/android_shockwave.json", {"message_id": "matteroverdrive.android_shockwave", "exhaustion": 0.1,
                                             "scaling": "when_caused_by_living_non_player"})
for tag in ["bypasses_armor", "is_explosion"]:
    p = TAGS / f"minecraft/tags/damage_type/{tag}.json"
    values = json.loads(p.read_text())["values"] if p.exists() else []
    w(p, {"values": values + [mid("android_shockwave")]})
# 1.7.10 AndroidPill.addToDunguns: the red pill in stronghold corridors (weight 1 there; 10% of chests here).
w(D / "loot_table/chests/android_pill.json", {"type": "minecraft:chest", "pools": [{"rolls": 1, "bonus_rolls": 0,
    "conditions": [{"condition": "minecraft:random_chance", "chance": 0.1}],
    "entries": [{"type": "minecraft:item", "name": mid("android_pill_red")}]}]})
w(D / "loot_modifiers/android_pill.json", {"type": "neoforge:add_table", "table": mid("chests/android_pill"),
    "conditions": [{"condition": "neoforge:loot_table_id", "loot_table_id": "minecraft:chests/stronghold_corridor"}]})
w(out / "data/neoforge/loot_modifiers/global_loot_modifiers.json", {"replace": False,
    "entries": [mid("weapon_module_colors"), mid("android_pill")]})

# Android station (6b): the weapon station's table with its own sides; rogue android parts as bionic parts.
cp(ref / "textures/blocks/android_station_side.png", A / "textures/block/android_station_side.png")
w(A / "models/block/android_station.json", {"parent": f"{MOD}:block/weapon_station", "textures": {
    "side": f"{MOD}:block/android_station_side", "particle": f"{MOD}:block/android_station_side"}})
facing_blockstate("android_station", f"{MOD}:block/android_station")
w(D / "loot_table/blocks/android_station.json", self_drop("android_station"))
for tag in ["mineable/pickaxe", "needs_iron_tool"]:
    p = TAGS / f"minecraft/tags/block/{tag}.json"
    w(p, {"values": json.loads(p.read_text())["values"] + [mid("android_station")]})
PARTS = ["head", "arms", "legs", "chest"]
for part in PARTS:
    cp(ref / "textures/items" / f"rouge_android_{part}.png", A / "textures/item" / f"rouge_android_{part}.png")
    w(A / "models/item" / f"rogue_android_part_{part}.json", {"parent": "minecraft:item/generated",
        "textures": {"layer0": f"{MOD}:item/rouge_android_{part}"}})
    w(A / "items" / f"rogue_android_part_{part}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/rogue_android_part_{part}"}})
# 1.7.10 recipe: T torso, H head, A arm, 2/3 isolinear mk2/mk3, F force field emitter, G glowstone, M casing, R redstone.
shaped("android_station", mid("android_station"), ["THA", "2F3", "GMR"],
       {"T": mid("rogue_android_part_chest"), "H": mid("rogue_android_part_head"), "A": mid("rogue_android_part_arms"),
        "2": MK[2], "3": MK[3], "F": mid("forcefield_emitter"), "G": "minecraft:glowstone_dust", "M": mid("machine_casing"),
        "R": "minecraft:redstone"})
for n in ["slot_holo"]:
    cp(ref / "textures/gui/elements" / f"{n}.png", GUI / "elements" / f"{n}.png")
for n in ["up_arrow", "black_circle"] + [f"android_slot_{p}" for p in PARTS + ["other"]] + \
         [f"biotic_stat_{s}" for s in ["teleport", "nanobots", "nano_armor", "floatation", "speed", "high_jump", "equalizer",
                                       "shield", "attack", "cloak", "nightvision", "minimap", "shockwave"]]:
    cp(ref / "textures/gui/items" / f"{n}.png", GUI / "elements" / f"{n}.png")
# Charging station (6e): the 1.7.10 OBJ (base + rod, centred on x/z, 2.3 blocks high) on the bottom block only.
cp(ref / "textures/blocks/charging_station.png", A / "textures/block/charging_station.png")
obj_lines = ["mtllib charging_station.mtl"]
for line in (ref / "models/block/charging_station.obj").read_text().splitlines():
    if line.startswith("v "):
        _, x, y, z = line.split()
        line = f"v {float(x) + 0.5:.4f} {float(y):.4f} {float(z) + 0.5:.4f}"
    elif line.startswith("g "):
        line = line + "\nusemtl station"
    elif line.startswith("usemtl") or line.startswith("mtllib"):
        continue
    obj_lines.append(line)
(A / "models/block/charging_station.obj").write_text("\n".join(obj_lines) + "\n")
(A / "models/block/charging_station.mtl").write_text("newmtl station\nmap_Kd #texture\n")
w(A / "models/block/charging_station.json", {"loader": "neoforge:obj", "model": f"{MOD}:models/block/charging_station.obj",
    "flip_v": True, "textures": {"texture": f"{MOD}:block/charging_station", "particle": f"{MOD}:block/base"}})
w(A / "models/block/charging_station_part.json", {"textures": {"particle": f"{MOD}:block/base"}})
w(A / "blockstates/charging_station.json", {"variants": {
    f"active={a},facing={f},part={p}": ({"model": f"{MOD}:block/charging_station", "y": y} if p == 0 else {"model": f"{MOD}:block/charging_station_part"})
    for f, y in FACING_Y.items() for a in ("false", "true") for p in (0, 1, 2)}})
w(A / "items/charging_station.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:block/charging_station"}})
w(D / "loot_table/blocks/charging_station.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
    "entries": [{"type": "minecraft:item", "name": mid("charging_station"), "functions": [{"function": "minecraft:copy_components",
        "source": "block_entity", "include": [mid("energy")]}]}],
    "conditions": [{"condition": "minecraft:survives_explosion"}, {"condition": "minecraft:block_state_property",
        "block": mid("charging_station"), "properties": {"part": "0"}}]}]})
for tag in ["mineable/pickaxe", "needs_iron_tool"]:
    p = TAGS / f"minecraft/tags/block/{tag}.json"
    w(p, {"values": json.loads(p.read_text())["values"] + [mid("charging_station")]})
shaped("charging_station", mid("charging_station"), [" F ", "EDR", "BMB"],
       {"F": mid("forcefield_emitter"), "E": "minecraft:ender_eye", "D": DILITHIUM, "R": "minecraft:repeater",
        "B": mid("hc_battery"), "M": mid("machine_casing")})

# Android HUD (6d)
for n in ["android_bg_element", "cloak_overlay", "spinner"]:
    cp(ref / "textures/gui/elements" / f"{n}.png", GUI / "elements" / f"{n}.png")
for n in ["android_feature_icon_bg", "android_feature_icon_bg_active", "health", "battery", "person", "ammo", "temperature"]:
    cp(ref / "textures/gui/items" / f"{n}.png", GUI / "elements" / f"{n}.png")
cp(ref / "textures/gui/glitch.png", GUI / "glitch.png")
# 1.7.10 BioticStatFlashCooling used the "temperature" holo icon
cp(ref / "textures/gui/items/temperature.png", GUI / "elements/biotic_stat_flash_cooling.png")


# --- phase 7a: decorative blocks, tritanium glass, holo sign, food, tritanium spine --------------------------------
# 1.7.10 BlockDecorative: (id, textures [bottom, top, north, south, west, east] or one name, kind, harvest level, original key)
DECOR = [
    ("stripes", "base_stripes", "cube", 1), ("coils", "base_coil", "cube", 1), ("clean", "transporter_side", "cube", 1),
    ("vent_dark", "vent", "cube", 1), ("vent_bright", "vent2", "cube", 1), ("holo_matrix", "weapon_station_top", "cube", 1),
    ("tritanium_plate", "tritanium_plate", "cube", 1), ("carbon_fiber_plate", "carbon_fiber_plate", "cube", 1),
    ("matter_tube", "matter_tube", "pillar", 1), ("beams", "beams", "pillar", 1), ("floor_tiles", "floor_tiles", "cube", 0),
    ("floor_tiles_green", "floor_tiles_green", "cube", 0), ("floor_noise", "floor_noise", "cube", 0),
    ("tritanium_plate_stripe", ["tritanium_plate", "tritanium_plate", "tritanium_plate_yellow_stripe",
                                "tritanium_plate_yellow_stripe", "tritanium_plate_yellow_stripe", "tritanium_plate_yellow_stripe"], "faces", 1),
    ("floor_tile_white", "floor_tile_white", "cube", 0), ("white_plate", "white_plate", "cube", 1),
    ("separator", "separator", "pillar", 1),
    ("tritanium_lamp", ["tritanium_lamp_bottom", "tritanium_lamp_top", "tritanium_plate_yellow_stripe",
                        "tritanium_plate_yellow_stripe", "tritanium_lamp_sides", "tritanium_lamp_sides"], "faces", 1),
    ("engine_exhaust_plasma", "engine_exhaust_plasma", "cube", 1),
]
DECOR_KEYS = {"vent_dark": "vent.dark", "vent_bright": "vent.bright"}
DYES = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue",
        "brown", "green", "red", "black"]
DECOR_IDS = []

def decor_block(n, model):
    w(A / "blockstates" / f"{n}.json", {"variants": {"": {"model": model}}})
    w(A / "items" / f"{n}.json", {"model": {"type": "minecraft:model", "model": model}})
    w(D / f"loot_table/blocks/{n}.json", self_drop(n))
    DECOR_IDS.append(n)

for name, tex, kind, _lvl in DECOR:
    n = f"decorative_{name}"
    faces = tex if isinstance(tex, list) else [tex] * 6
    for t in set(faces):
        cp(ref / "textures/blocks" / f"{t}.png", A / "textures/block" / f"{t}.png")
    extra = {"render_type": "minecraft:cutout"} if name == "matter_tube" else {}
    if kind == "pillar":
        w(A / "models/block" / f"{n}.json", {"parent": "minecraft:block/cube_column", "textures": {
            "end": f"{MOD}:block/{tex}", "side": f"{MOD}:block/{tex}"}} | extra)
        w(A / "blockstates" / f"{n}.json", {"variants": {"axis=y": {"model": f"{MOD}:block/{n}"},
            "axis=z": {"model": f"{MOD}:block/{n}", "x": 90}, "axis=x": {"model": f"{MOD}:block/{n}", "x": 90, "y": 90}}})
        w(A / "items" / f"{n}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:block/{n}"}})
        w(D / f"loot_table/blocks/{n}.json", self_drop(n))
        DECOR_IDS.append(n)
        continue
    if kind == "faces":
        w(A / "models/block" / f"{n}.json", {"parent": "minecraft:block/cube", "textures": {
            "down": f"{MOD}:block/{faces[0]}", "up": f"{MOD}:block/{faces[1]}", "north": f"{MOD}:block/{faces[2]}",
            "south": f"{MOD}:block/{faces[3]}", "west": f"{MOD}:block/{faces[4]}", "east": f"{MOD}:block/{faces[5]}",
            "particle": f"{MOD}:block/{faces[2]}"}})
    else:
        w(A / "models/block" / f"{n}.json", {"parent": "minecraft:block/cube_all", "textures": {"all": f"{MOD}:block/{tex}"}} | extra)
    decor_block(n, f"{MOD}:block/{n}")
# 1.7.10 decorative.tritanium_plate_colored: one block per dye colour, the colorless plate tinted (block colour handler).
cp(ref / "textures/blocks/tritanium_plate_colorless.png", A / "textures/block/tritanium_plate_colorless.png")
w(A / "models/block/decorative_tritanium_plate_colored.json", {"parent": "minecraft:block/block", "textures": {
    "all": f"{MOD}:block/tritanium_plate_colorless", "particle": f"{MOD}:block/tritanium_plate_colorless"},
    "elements": [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": {f: {"texture": "#all", "cullface": f, "tintindex": 0}
                                                                    for f in ["down", "up", "north", "south", "west", "east"]}}]})
DYE_RGB = {"white": 0xF9FFFE, "orange": 0xF9801D, "magenta": 0xC74EBD, "light_blue": 0x3AB3DA, "yellow": 0xFED83D, "lime": 0x80C71F,
           "pink": 0xF38BAA, "gray": 0x474F52, "light_gray": 0x9D9D97, "cyan": 0x169C9C, "purple": 0x8932B8, "blue": 0x3C44AA,
           "brown": 0x835432, "green": 0x5E7C16, "red": 0xB02E26, "black": 0x1D1D21}
for dye in DYES:
    n = f"decorative_tritanium_plate_{dye}"
    w(A / "blockstates" / f"{n}.json", {"variants": {"": {"model": f"{MOD}:block/decorative_tritanium_plate_colored"}}})
    w(A / "items" / f"{n}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:block/decorative_tritanium_plate_colored",
        "tints": [{"type": "minecraft:constant", "value": (0xFF << 24 | DYE_RGB[dye]) - (1 << 32)}]}})
    w(D / f"loot_table/blocks/{n}.json", self_drop(n))
    DECOR_IDS.append(n)
    shaped(n, mid(n), ["###", "#D#", "###"], {"#": mid("decorative_tritanium_plate"), "D": f"minecraft:{dye}_dye"}, count=8,
           category="building")
# Tritanium glass (1.7.10 ForceGlass, connected textures in 1.7.10).
cp(ref / "textures/blocks/force_glass.png", A / "textures/block/force_glass.png")
w(A / "models/block/force_glass.json", {"parent": "minecraft:block/cube_all", "render_type": "minecraft:translucent",
                                        "textures": {"all": f"{MOD}:block/force_glass"}})
decor_block("force_glass", f"{MOD}:block/force_glass")
for tag in ["mineable/pickaxe", "needs_stone_tool"]:
    p = TAGS / f"minecraft/tags/block/{tag}.json"
    values = json.loads(p.read_text())["values"] if p.exists() else []
    stone_free = [f"decorative_{n}" for n, _, _, lvl in DECOR if lvl == 0]
    w(p, {"values": values + [mid(n) for n in DECOR_IDS if not (tag == "needs_stone_tool" and n in stone_free)]})
# 1.7.10 MatterOverdriveRecipes decorative recipes
PLATE_BLOCK = mid("decorative_tritanium_plate")
shaped("decorative_tritanium_plate", PLATE_BLOCK, ["##", "##"], {"#": PLATE}, count=12, category="building")
shaped("decorative_beams", mid("decorative_beams"), ["#", "T", "#"], {"#": PLATE, "T": INGOT}, count=6, category="building")
shaped("decorative_tritanium_plate_stripe", mid("decorative_tritanium_plate_stripe"), ["###", "#Y#", "###"],
       {"#": PLATE_BLOCK, "Y": "minecraft:yellow_dye"}, count=8, category="building")
shaped("decorative_holo_matrix", mid("decorative_holo_matrix"), ["###", "#I#", "###"], {"#": PLATE_BLOCK, "I": MK[1]}, count=8, category="building")
shaped("decorative_carbon_fiber_plate", mid("decorative_carbon_fiber_plate"), ["###", "#C#", "###"], {"#": PLATE_BLOCK, "C": "minecraft:coal"},
       count=8, category="building")
shaped("decorative_vent_bright", mid("decorative_vent_bright"), [" # ", "T T", " # "], {"#": PLATE, "T": INGOT}, count=6, category="building")
shaped("decorative_vent_dark", mid("decorative_vent_dark"), ["###", "#B#", "###"], {"#": mid("decorative_vent_bright"), "B": "minecraft:black_dye"},
       count=8, category="building")
shaped("decorative_clean", mid("decorative_clean"), ["TT", "TT"], {"T": INGOT}, count=8, category="building")
shaped("decorative_floor_tiles", mid("decorative_floor_tiles"), ["###", "#Q#", "###"], {"#": "minecraft:clay", "Q": "minecraft:quartz"},
       count=12, category="building")
shaped("decorative_floor_tiles_green", mid("decorative_floor_tiles_green"), ["#G#", "#Q#", "#G#"],
       {"#": "minecraft:clay", "Q": "minecraft:quartz", "G": "minecraft:green_dye"}, count=12, category="building")
shaped("decorative_floor_tile_white", mid("decorative_floor_tile_white"), ["#W#", "#Q#", "#W#"],
       {"#": "minecraft:clay", "Q": "minecraft:quartz", "W": "minecraft:white_dye"}, count=12, category="building")
shaped("decorative_separator", mid("decorative_separator"), ["#N#", "#N#", "#N#"], {"#": PLATE_BLOCK, "N": NUGGET},
       count=8, category="building")
shaped("decorative_floor_noise", mid("decorative_floor_noise"), ["#G#", "#Q#", "#G#"],
       {"#": "minecraft:clay", "Q": "minecraft:quartz", "G": "minecraft:gravel"}, count=12, category="building")
shaped("decorative_white_plate", mid("decorative_white_plate"), ["#W#", "###", "#W#"], {"#": PLATE_BLOCK, "W": "#minecraft:wool"},
       count=8, category="building")
shaped("decorative_coils", mid("decorative_coils"), ["###", "#C#", "###"], {"#": PLATE_BLOCK, "C": mid("s_magnet")}, count=9, category="building")
shaped("decorative_stripes", mid("decorative_stripes"), ["#B#", "###", "#Y#"],
       {"#": PLATE_BLOCK, "B": "minecraft:black_dye", "Y": "minecraft:yellow_dye"}, count=8, category="building")
shaped("force_glass", mid("force_glass"), [" G ", "GTG", " G "], {"G": "minecraft:glass", "T": PLATE}, count=4, category="building")
shaped("holo_sign", mid("holo_sign"), ["GGG", "g0g", " T "],
       {"G": "#c:glass_blocks", "g": "minecraft:glowstone_dust", "0": MK[1], "T": PLATE})
# Holo sign: a 2 px panel at the back of the block, the holo monitor face to the front (facing north by default).
w(A / "models/block/holo_sign.json", {"parent": "minecraft:block/block", "render_type": "minecraft:translucent", "textures": {
    "front": f"{MOD}:block/holo_monitor", "side": f"{MOD}:block/base", "particle": f"{MOD}:block/base"},
    "elements": [{"from": [0, 0, 14], "to": [16, 16, 16], "faces": {
        "north": {"texture": "#front"}, "south": {"texture": "#side", "cullface": "south"},
        "east": {"uv": [0, 0, 2, 16], "texture": "#side"}, "west": {"uv": [14, 0, 16, 16], "texture": "#side"},
        "up": {"uv": [0, 14, 16, 16], "texture": "#side"}, "down": {"uv": [0, 0, 16, 2], "texture": "#side"}}}]})
w(A / "blockstates/holo_sign.json", {"variants": {f"facing={f}": ({"model": f"{MOD}:block/holo_sign", "y": y} if y else
                                                                {"model": f"{MOD}:block/holo_sign"}) for f, y in FACING_Y.items()}})
w(A / "items/holo_sign.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:block/holo_sign"}})
w(D / "loot_table/blocks/holo_sign.json", self_drop("holo_sign"))
for tag in ["mineable/pickaxe"]:
    p = TAGS / f"minecraft/tags/block/{tag}.json"
    w(p, {"values": json.loads(p.read_text())["values"] + [mid("holo_sign")]})
# Food and the tritanium spine (textures only; 1.7.10 had no recipes: replicator, trades and rogue androids).
for item_id, tex in {"emergency_ration": "emergency_ration", "earl_gray_tea": "earl_gray_tea", "romulan_ale": "romulan_ale",
                     "tritanium_spine": "tritainum_spine"}.items():
    cp(ref / "textures/items" / f"{tex}.png", A / "textures/item" / f"{tex}.png")
    w(A / "models/item" / f"{item_id}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{tex}"}})
    w(A / "items" / f"{item_id}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{item_id}"}})


# --- phase 7e: tritanium crate ------------------------------------------------------------------------------
# 1.7.10 RendererBlockTritaniumCrate: OBJ base + overlay tinted with the crate's dye colour (block colour handler / item tint).
for t in ["tritanium_crate_base", "tritanium_crate_overlay"]:   # 16-bit originals → 8-bit RGBA
    dst_png = A / "textures/block" / f"{t}.png"
    dst_png.parent.mkdir(parents=True, exist_ok=True)
    subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", str(ref / "textures/blocks" / f"{t}.png"), "-pix_fmt", "rgba",
                    "-fflags", "+bitexact", "-flags", "+bitexact", str(dst_png)], check=True)
obj_model("tritanium_crate", {"base": "tritanium_crate_base", "overlay": "tritanium_crate_overlay"},
          particle="tritanium_crate_base", tints={"tritanium_crate_overlay": 0})
CRATES = [f"tritanium_crate_{dye}" for dye in DYES]
for dye, n in zip(DYES, CRATES):
    w(A / "blockstates" / f"{n}.json", {"variants": {f"facing={f}": ({"model": f"{MOD}:block/tritanium_crate", "y": y} if y else
                                                                    {"model": f"{MOD}:block/tritanium_crate"}) for f, y in FACING_Y.items()}})
    w(A / "items" / f"{n}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:block/tritanium_crate",
        "tints": [{"type": "minecraft:constant", "value": (0xFF << 24 | DYE_RGB[dye]) - (1 << 32)}]}})
    w(D / f"loot_table/blocks/{n}.json", {"type": "minecraft:block", "pools": [{"rolls": 1, "bonus_rolls": 0,
        "entries": [{"type": "minecraft:item", "name": mid(n), "functions": [{"function": "minecraft:copy_components",
            "source": "block_entity", "include": ["minecraft:container", "minecraft:custom_name"]}]}],
        "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    shaped(n, mid(n), [" D ", "TCT", " T "], {"D": f"minecraft:{dye}_dye", "T": mid("tritanium_plate"), "C": "minecraft:chest"})
for tag in ["mineable/pickaxe", "needs_iron_tool"]:
    p = TAGS / f"minecraft/tags/block/{tag}.json"
    w(p, {"values": json.loads(p.read_text())["values"] + [mid(n) for n in CRATES]})
w(D / "tags/item/tritanium_crates.json", {"values": [mid(n) for n in CRATES]})


# --- phase 7i: portable decomposer, microwave --------------------------------------------------------------
cp(ref / "textures/items/portable_decomposer.png", A / "textures/item/portable_decomposer.png")
w(A / "models/item/portable_decomposer.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/portable_decomposer"}})
w(A / "items/portable_decomposer.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/portable_decomposer"}})
shaped("portable_decomposer", mid("portable_decomposer"), [" T ", "IPM", " T "],
       {"T": mid("tritanium_plate"), "I": mid("integration_matrix"), "M": mid("me_conversion_matrix"), "P": "minecraft:sticky_piston"},
       category="equipment")
# 1.7.10 BlockMicrowave via MOBlockRenderer: the block bounds drawn with the front icon on the facing side, the back icon
# opposite and "microwave" elsewhere (UVs follow the bounds, like the element defaults)
for t in ["microwave", "microwave_front", "microwave_back"]:
    cp(ref / "textures/blocks" / f"{t}.png", A / "textures/block" / f"{t}.png")
w(A / "models/block/microwave.json", {"parent": "minecraft:block/block", "textures": {
    "particle": f"{MOD}:block/microwave", "side": f"{MOD}:block/microwave", "front": f"{MOD}:block/microwave_front",
    "back": f"{MOD}:block/microwave_back"}, "elements": [{"from": [1, 0, 3], "to": [15, 10, 13], "faces": {
        "north": {"texture": "#front"}, "south": {"texture": "#back"}, "east": {"texture": "#side"}, "west": {"texture": "#side"},
        "up": {"texture": "#side"}, "down": {"texture": "#side", "cullface": "down"}}}]})
w(A / "blockstates/microwave.json", {"variants": {f"facing={f}": ({"model": f"{MOD}:block/microwave", "y": y} if y else
                                                                 {"model": f"{MOD}:block/microwave"}) for f, y in FACING_Y.items()}})
w(A / "items/microwave.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:block/microwave"}})
w(D / "loot_table/blocks/microwave.json", self_drop("microwave"))
for tag in ["mineable/pickaxe", "needs_iron_tool"]:
    p = TAGS / f"minecraft/tags/block/{tag}.json"
    w(p, {"values": json.loads(p.read_text())["values"] + [mid("microwave")]})


# --- phase 7j: matter scanner -------------------------------------------------------------------------------
# 1.7.10 MatterScanner.getIconIndex: the "offline" icon until it is linked to a pattern storage
for t in ["matter_scanner", "matter_scanner_offline"]:
    cp(ref / "textures/items" / f"{t}.png", A / "textures/item" / f"{t}.png")
    w(A / "models/item" / f"{t}.json", {"parent": "minecraft:item/handheld", "textures": {"layer0": f"{MOD}:item/{t}"}})
w(A / "items/matter_scanner.json", {"model": {"type": "minecraft:condition", "property": "minecraft:has_component",
    "component": mid("scanner_link"), "on_true": {"type": "minecraft:model", "model": f"{MOD}:item/matter_scanner"},
    "on_false": {"type": "minecraft:model", "model": f"{MOD}:item/matter_scanner_offline"}}})
shaped("matter_scanner", mid("matter_scanner"), ["III", "GDG", "IRI"],
       {"I": "minecraft:iron_ingot", "D": MK[3], "R": "minecraft:redstone", "G": "minecraft:gold_ingot"}, category="equipment")


# --- phase 7b: mobs -----------------------------------------------------------------------------------------
import zlib

def write_png(path, rows):
    """rows: list of lists of (r, g, b, a)"""
    h, w_ = len(rows), len(rows[0])
    raw = b"".join(b"\x00" + bytes(c for px in row for c in px) for row in rows)
    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w_, h, 8, 6, 0, 0, 0)) + \
          chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)

# Spawn eggs have one texture each since 1.21.5; drawn here in the 1.7.10 egg colours (o outline, b base, s spot, h highlight).
EGG = ["................", "......oooo......", ".....obbbbo.....", "....obbhbbbo....", "...obbhbbsbbo...", "...obhbbbsbbo...",
       "..obbbbbbbbbbo..", "..obbsbbbbbbbo..", "..obsssbbbsbbo..", ".obbbsbbbbssbbo.", ".obbbbbbbbbbbbo.", ".obbbbbbsbbbbbo.",
       ".obbsbbbsssbbbo.", "..obbbbbbsbbbo..", "...obbbbbbbbo...", "....oooooooo...."]

def egg(name, base, spot):
    def rgb(c, f=1.0):
        return tuple(min(255, int(((c >> sh) & 255) * f)) for sh in (16, 8, 0)) + (255,)
    colors = {"o": rgb(base, 0.45), "b": rgb(base), "s": rgb(spot), "h": rgb(base, 1.3), ".": (0, 0, 0, 0)}
    write_png(A / "textures/item" / f"{name}.png", [[colors[c] for c in row] for row in EGG])
    w(A / "models/item" / f"{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{name}"}})
    w(A / "items" / f"{name}.json", {"model": {"type": "minecraft:model", "model": f"{MOD}:item/{name}"}})

egg("rogue_android_spawn_egg", 0x0FFFFF, 0x000000)          # 1.7.10 addEntity(..., 0xFFFFF, 0)
egg("ranged_rogue_android_spawn_egg", 0x0FFFFF, 0x000000)
for t in ["android", "android_ranged"]:
    cp(ref / "textures/entities" / f"{t}.png", A / "textures/entity" / f"{t}.png")
# 7c failed animals: 1.7.10 egg colours; the pig and cow textures are 64x32, today's models use the same UVs on 64x64
for n, base in {"pig": 15771042, "cow": 4470310, "chicken": 10592673, "sheep": 15198183}.items():
    egg(f"failed_{n}_spawn_egg", base, 0x33CC33)
    src_png, dst_png = ref / "textures/entities" / f"failed_{n}.png", A / "textures/entity" / f"failed_{n}.png"
    if n in ("pig", "cow"):
        dst_png.parent.mkdir(parents=True, exist_ok=True)
        subprocess.run(["ffmpeg", "-v", "error", "-y", "-i", str(src_png), "-vf", "pad=64:64:0:0:color=0x00000000", "-pix_fmt", "rgba",
                        "-fflags", "+bitexact", "-flags", "+bitexact", str(dst_png)], check=True)
    else:
        cp(src_png, dst_png)
# 7d mutant scientist (1.7.10 file name has a typo)
egg("mutant_scientist_spawn_egg", 0xFFFFFF, 0x00FF00)
cp(ref / "textures/entities/hulking_scinetist.png", A / "textures/entity/hulking_scientist.png")
# 1.7.10 EntityRogueAndroid.addAsBiomeGen: weight 15, groups of 1-2, all overworld biomes but the mushroom fields
for n in ["rogue_android", "ranged_rogue_android"]:
    w(D / f"neoforge/biome_modifier/{n}.json", {"type": "neoforge:add_spawns",
        "biomes": {"type": "neoforge:and", "values": ["#minecraft:is_overworld",
                   {"type": "neoforge:not", "value": "minecraft:mushroom_fields"}]},
        "spawners": {"type": mid(n), "weight": 15, "minCount": 1, "maxCount": 2}})


# --- phase 7f: image-generated buildings ----------------------------------------------------------------------
# 1.7.10 MOImageGen: a building is a PNG cut into layerWidth x layerHeight tiles (left to right, top to bottom = y from
# the bottom up); pixel colour = block mapping, 255-alpha = metadata, black = leave the world as it is. The images are
# turned into templates here: a palette of finished block states (metadata already resolved) and one byte per pixel.
# matteroverdrive.world.ImageStructure places them chunk by chunk.
import base64

def png_rgba(path):
    size = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "stream=width,height", "-of", "csv=p=0", str(path)],
                          capture_output=True, text=True, check=True).stdout.strip()
    tw, th = map(int, size.split(","))
    raw = subprocess.run(["ffmpeg", "-v", "error", "-i", str(path), "-f", "rawvideo", "-pix_fmt", "rgba", "-"],
                         capture_output=True, check=True).stdout
    return tw, th, raw

WOOL = DYES                                                       # 1.7.10 wool/stained glass/carpet metadata order
ITEM_DYE = list(reversed(DYES))                                   # 1.7.10 ItemDye order (crates, colored plates)
FORGE_DIR = {2: "north", 3: "south", 4: "west", 5: "east"}

def k_plain(block):
    return lambda meta, ctx: (block, {})
def k_facing(block):
    return lambda meta, ctx: (block, {"facing": FORGE_DIR[meta]} if meta in FORGE_DIR else {})
def k_fixed(block, **props):
    return lambda meta, ctx: (block, props)
def k_axis(block):                                                # 1.7.10 rotated decoratives turned their texture by 90°
    return lambda meta, ctx: (block, {"axis": "y" if meta == 0 else "x"})
def k_stairs(block):
    return lambda meta, ctx: (block, {"facing": ["east", "west", "south", "north"][meta & 3], "half": "top" if meta & 4 else "bottom"})
def k_bed(block):
    return lambda meta, ctx: (block, {"facing": ["south", "west", "north", "east"][meta & 3], "part": "head" if meta & 8 else "foot"})
def k_door(block):
    def lower(meta):
        return {"facing": ["east", "south", "west", "north"][meta & 3], "open": "true" if meta & 4 else "false"}
    def f(meta, ctx):
        if meta & 8:                                             # upper half: facing and open come from the lower half
            below = ctx("below")
            props = lower(below) if below is not None else {}
            return block, props | {"half": "upper", "hinge": "right" if meta & 1 else "left"}
        above = ctx("above")
        return block, lower(meta) | {"half": "lower", "hinge": "right" if above is not None and above & 1 else "left"}
    return f
def k_button(block):
    return lambda meta, ctx: (block, {"face": "wall", "facing": {1: "east", 2: "west", 3: "south", 4: "north"}[meta]}
                              if meta in (1, 2, 3, 4) else {"face": "floor"})
def k_by_meta(blocks, **props):                                   # block id chosen by metadata (colours, plant types)
    return lambda meta, ctx: (blocks[meta] if meta < len(blocks) else blocks[0], props)
def k_charging_part():                                            # 1.7.10 boundingBox above a charging station
    return lambda meta, ctx: (mid("charging_station"), {"part": str(ctx("part"))})

DECOR_COLORS = {0xd4b108: k_plain(mid("decorative_stripes")), 0xb6621e: k_plain(mid("decorative_coils")),
                0x3b484b: k_plain(mid("decorative_clean")), 0x32393c: k_plain(mid("decorative_vent_dark")),
                0x3f4b4e: k_plain(mid("decorative_vent_bright")), 0x323b3a: k_plain(mid("decorative_holo_matrix")),
                0x475459: k_plain(mid("decorative_tritanium_plate")), 0x1c1f20: k_plain(mid("decorative_carbon_fiber_plate")),
                0x5088a5: k_axis(mid("decorative_matter_tube")), 0x1e2220: k_axis(mid("decorative_beams")),
                0x958d7c: k_plain(mid("decorative_floor_tiles")), 0x53593f: k_plain(mid("decorative_floor_tiles_green")),
                0x7f7e7b: k_plain(mid("decorative_floor_noise")), 0x576468: k_plain(mid("decorative_tritanium_plate_stripe")),
                0xa3a49c: k_plain(mid("decorative_floor_tile_white")), 0xe3e3e3: k_plain(mid("decorative_white_plate")),
                0x303837: k_axis(mid("decorative_separator")), 0xd4f8f5: k_plain(mid("decorative_tritanium_lamp")),
                0x505050: k_by_meta([mid(f"decorative_tritanium_plate_{d}") for d in ITEM_DYE]),
                0x387c9e: k_plain(mid("decorative_engine_exhaust_plasma"))}
CRATES_ALL = [k_facing(mid(f"tritanium_crate_{d}")) for d in ITEM_DYE]
def crate(dye):
    return k_facing(mid(f"tritanium_crate_{dye}"))
# Not ported yet: the star map and the transporter get stand-ins (swap them here when they exist).
STAR_MAP = k_plain(mid("decorative_holo_matrix"))
TRANSPORTER = k_plain(mid("machine_hull"))
CONNECT = {mid("network_pipe"), mid("heavy_matter_pipe"), mid("matter_pipe"), "minecraft:oak_fence"}

def m(kinds, noise=False, specials=()):
    return {"kinds": kinds if isinstance(kinds, list) else [kinds], "noise": noise, "specials": list(specials)}

BUILDINGS = {
    # 1.7.10 MOAndroidHouseBuilding: metadata = (255-alpha)/255*10
    "android_house": ("android_house", 21, 21, "android", {
        0x00fffc: m([k_axis(mid("decorative_beams")), k_plain(mid("decorative_carbon_fiber_plate")), k_plain(mid("decorative_white_plate"))]),
        0x623200: m(k_plain("minecraft:dirt")), 0xffa200: m(k_plain(mid("decorative_floor_tiles"))),
        0xfff600: m(k_plain(mid("decorative_holo_matrix"))), 0x80b956: m(k_plain("minecraft:grass_block")),
        0x539ac3: m(k_plain(mid("decorative_tritanium_plate"))),
        0xb1c8d5: m([k_plain(mid("decorative_floor_noise")), k_plain(mid("decorative_floor_tiles_green")), k_plain(mid("decorative_floor_tile_white"))]),
        0x5f6569: m(k_plain(mid("decorative_vent_dark"))), 0xf1f1f1: m(k_plain("minecraft:air")), 0xe400ff: m(STAR_MAP),
        0x1850ad: m(k_plain(mid("decorative_clean"))), 0x9553c3: m(k_plain(mid("force_glass"))),
        0x35d6e0: m(k_facing(mid("replicator"))), 0x35e091: m(k_facing(mid("network_switch"))),
        0xc8d43d: m(CRATES_ALL, specials=["crate_loot"]),
        0x2a4071: m([k_facing(mid("android_station")), k_facing(mid("weapon_station"))]),
        0xa13e5f: m(k_plain(mid("network_pipe"))), 0xa16a3e: m(k_facing(mid("charging_station"))),
        0x416173: m(k_plain(mid("decorative_tritanium_plate_stripe"))), 0x187716: m(k_facing(mid("pattern_monitor"))),
        0xac7c1e: m(k_plain(mid("decorative_vent_bright"))), 0x007eff: m(k_plain(mid("decorative_stripes")))}),
    # 1.7.10 MOSandPit: no metadata
    "sand_pit": ("sand_pit", 24, 24, None, {
        0xe1db35: m(k_plain("minecraft:sandstone")), 0xf1f1f1: m(k_plain("minecraft:air")), 0xffff00: m(k_plain("minecraft:sand")),
        0xc735e1: m(k_plain("minecraft:glowstone")), 0x35a2e1: m(k_plain("minecraft:water")),
        0x359ae1: m(k_plain(mid("decorative_tritanium_plate"))), 0xff8400: m(k_plain(mid("decorative_coils"))),
        0x6b4400: m(k_plain("minecraft:oak_fence"))}),
    # 1.7.10 MOWorldGenCrashedSpaceShip: no metadata; the holo signs face east/west by colour
    "crashed_ship": ("crashed_space_ship", 11, 35, None, {
        0x38c8df: m(k_plain(mid("decorative_clean"))), 0x187b8b: m(k_plain(mid("decorative_vent_bright"))),
        0xaa38df: m(k_plain(mid("force_glass"))), 0x00ff78: m(k_plain("minecraft:grass_block")),
        0xd8ff00: m(k_fixed(mid("holo_sign"), facing="east"), specials=["holo_text"]),
        0xaccb00: m(k_fixed(mid("holo_sign"), facing="west"), specials=["holo_text"]),
        0x3896df: m(k_plain(mid("decorative_tritanium_plate"))), 0xdfd938: m(k_plain(mid("decorative_tritanium_plate_stripe"))),
        0x5d89ab: m(k_plain(mid("decorative_holo_matrix"))), 0x77147d: m(k_plain(mid("weapon_station")), specials=["weapon"]),
        0xb04a90: m(CRATES_ALL, specials=["crate_loot"]), 0x94deea: m(k_axis(mid("decorative_separator"))),
        0xff9c00: m(k_plain(mid("decorative_coils"))), 0xaca847: m(k_axis(mid("decorative_matter_tube"))),
        0x0c3b60: m(k_plain(mid("decorative_carbon_fiber_plate"))), 0xc5ced0: m(k_plain("minecraft:air"))}),
    # 1.7.10 MOWorldGenUnderwaterBase: metadata = 255-alpha
    "underwater_base": ("underwater_base", 43, 43, "alpha", {**{c: m(k) for c, k in DECOR_COLORS.items()},
        0xdc979c: m(k_by_meta(["minecraft:short_grass", "minecraft:short_grass", "minecraft:fern"])),
        0x77d1b6: m(k_by_meta(["minecraft:poppy", "minecraft:blue_orchid", "minecraft:allium", "minecraft:azure_bluet", "minecraft:red_tulip",
                               "minecraft:orange_tulip", "minecraft:white_tulip", "minecraft:pink_tulip", "minecraft:oxeye_daisy"])),
        0xd2fb50: m(k_plain(mid("force_glass"))), 0x0c1e4e: m(k_plain("minecraft:farmland")),
        0xa7ac65: m(crate("orange")), 0xd6a714: m(k_by_meta([f"minecraft:{d}_stained_glass" for d in WOOL])),
        0x2c5ae9: m(k_facing(mid("weapon_station"))), 0x0acd8c: m(k_facing(mid("android_station"))),
        0x7018f9: m(crate("light_blue")), 0x4657cc: m(crate("lime")), 0x1f2312: m(crate("white")),
        0xd3371d: m(k_plain(mid("machine_hull"))), 0x3640f9: m(k_button("minecraft:stone_button")),
        0xeff73d: m(k_facing(mid("network_switch"))), 0x5a6388: m(k_charging_part()), 0xbf19a9: m(k_plain("minecraft:grass_block")),
        0xc05e5e: m(k_by_meta(["minecraft:flower_pot", "minecraft:potted_poppy", "minecraft:potted_dandelion"])),
        0x4d8dd3: m(k_facing(mid("pattern_monitor"))), 0xdb9c3a: m(k_facing(mid("holo_sign"))),
        0x68b68c: m(k_facing(mid("matter_analyzer"))), 0x2cb0c7: m(STAR_MAP, specials=["mutant"]),
        0x1b2ff7: m(k_plain(mid("network_pipe"))), 0x05eaab: m(crate("yellow")), 0x11003e: m(k_facing(mid("charging_station"))),
        0xb31e83: m(k_fixed("minecraft:carrots", age="7")), 0xc78e77: m(k_facing(mid("replicator"))),
        0x338a42: m(k_fixed("minecraft:potatoes", age="7")), 0xbdea8f: m(k_facing("minecraft:ladder")),
        0x4d12f4: m(k_facing(mid("pattern_storage"))),
        0xf7d20b: m(k_by_meta(["minecraft:oak_sapling", "minecraft:spruce_sapling", "minecraft:birch_sapling", "minecraft:jungle_sapling"])),
        0x854b38: m(k_door("minecraft:iron_door")), 0xff00ff: m(k_plain("minecraft:air"))}),
    # 1.7.10 MOWorldGenCargoShip: metadata = 255-alpha; the ores are picked per block
    "cargo_ship": ("cargo_ship", 58, 23, "alpha", {**{c: m(k) for c, k in DECOR_COLORS.items()},
        0xdb9c3a: m(k_facing(mid("holo_sign"))), 0x5fffbe: m(TRANSPORTER), 0xd2fb50: m(k_plain(mid("force_glass"))),
        0xdc01d8: m(k_plain("minecraft:oak_pressure_plate")),
        0xfc6b34: m([k_plain("minecraft:gold_ore"), k_plain("minecraft:iron_ore"), k_plain("minecraft:coal_ore"),
                     k_plain(mid("tritanium_ore"))], noise=True),
        0x0d1626: m(k_facing(mid("fusion_reactor_io"))), 0x1b2ff7: m(k_plain(mid("network_pipe"))), 0x1f2312: m(crate("white")),
        0xab4824: m(k_plain("minecraft:oak_fence")), 0x68d738: m(k_by_meta([f"minecraft:{d}_carpet" for d in WOOL])),
        0xbdea8f: m(k_facing("minecraft:ladder")), 0xeff73d: m(k_facing(mid("network_switch"))),
        0xa8ed1c: m(k_plain(mid("heavy_matter_pipe"))), 0x4b285d: m(k_stairs("minecraft:oak_stairs")),
        0xcfd752: m(k_facing(mid("network_router"))), 0x4d8dd3: m(k_facing(mid("pattern_monitor"))),
        0x6b3534: m(k_bed("minecraft:red_bed")), 0xff00ff: m(k_plain("minecraft:air"))}),
}
# 1.7.10 MOWorldGenUnderwaterBase/CargoShip mapped every BlockDecorative by its colour, then their own colours on top.

for bid, (img, lw, lh, meta_mode, mapping) in BUILDINGS.items():
    tw, th, raw = png_rgba(ref / "textures/world" / f"{img}.png")
    cols = tw // lw
    layers = cols * (th // lh)
    def pixel(layer, x, z):
        if not 0 <= layer < layers:
            return None
        px, py = (layer % cols) * lw + x, (layer // cols) * lh + z
        i = (py * tw + px) * 4
        return (raw[i] << 16 | raw[i + 1] << 8 | raw[i + 2]), raw[i + 3]
    def meta_of(alpha):
        if meta_mode == "alpha":
            return 255 - alpha
        if meta_mode == "android":
            return int((255 - alpha) / 255 * 10)
        return 0
    palette, index, data, unmapped = [], {}, [], set()
    for layer in range(layers):
        row = bytearray(lw * lh)
        for z in range(lh):
            for x in range(lw):
                color, alpha = pixel(layer, x, z)
                if color == 0:
                    continue
                mp = mapping.get(color)
                if mp is None:
                    unmapped.add(color)
                    continue
                meta = meta_of(alpha)
                def ctx(what, layer=layer, x=x, z=z):
                    if what in ("below", "above"):
                        p = pixel(layer + (-1 if what == "below" else 1), x, z)
                        return meta_of(p[1]) if p and p[0] == color else None
                    if what == "part":                            # 1 + the part of the charging station below
                        part, l = 0, layer - 1
                        while l >= 0 and pixel(l, x, z)[0] in (0x5a6388,):
                            part, l = part + 1, l - 1
                        return part + 1
                states = []
                for kind in mp["kinds"]:
                    block, props = kind(meta, ctx)
                    states.append({"Name": block} | ({"Properties": props} if props else {}))
                specials = mp["specials"] + (["connect"] if any(s["Name"] in CONNECT for s in states) else [])
                entry = {"states": states, "noise": mp["noise"], "group": color, "specials": specials}
                key = json.dumps(entry, sort_keys=True)
                if key not in index:
                    palette.append(entry)
                    index[key] = len(palette)                     # 0 = leave the world as it is
                row[z * lw + x] = index[key]
        data.append(base64.b64encode(bytes(row)).decode())
    assert len(palette) < 256, bid
    if unmapped:
        print(f"{bid}: unmapped colours {sorted('%06x' % c for c in unmapped)}")
    w(D / "mo_buildings" / f"{bid}.json", {"width": lw, "depth": lh, "height": layers, "palette": palette, "layers": data})

# 1.7.10 ChestGenHooks "android_house": 10-19 rolls (generateChestContents(random.nextInt(10) + 10))
def loot_item(item, lo, hi, weight):
    e = {"type": "minecraft:item", "name": item, "weight": weight}
    if (lo, hi) != (1, 1):
        e["functions"] = [{"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}}]
    return e
w(D / "loot_table/chests/android_house.json", {"type": "minecraft:chest", "pools": [{"rolls": {"type": "minecraft:uniform", "min": 10, "max": 19},
    "entries": [loot_item(mid("emergency_ration"), 8, 20, 100), loot_item(mid("earl_gray_tea"), 4, 10, 50),
                loot_item(mid("romulan_ale"), 4, 10, 50),
                loot_item(mid("isolinear_circuit_mk1"), 1, 5, 50), loot_item(mid("isolinear_circuit_mk2"), 1, 4, 40),
                loot_item(mid("isolinear_circuit_mk3"), 1, 3, 30), loot_item(mid("isolinear_circuit_mk4"), 1, 2, 20),
                loot_item(mid("android_pill_blue"), 1, 2, 10), loot_item(mid("android_pill_red"), 1, 1, 5),
                loot_item(mid("weapon_module_barrel_damage"), 1, 1, 10), loot_item(mid("weapon_module_barrel_fire"), 1, 1, 8),
                loot_item(mid("weapon_module_barrel_heal"), 1, 1, 10), loot_item(mid("weapon_module_barrel_explosion"), 1, 1, 5),
                loot_item(mid("tritanium_spine"), 1, 1, 10),
                *[loot_item(mid(f"rogue_android_part_{p}"), 1, 2, 15) for p in ["head", "arms", "legs", "chest"]],
                loot_item(mid("hc_battery"), 1, 1, 10), loot_item(mid("h_compensator"), 1, 2, 10),
                loot_item(mid("me_conversion_matrix"), 1, 2, 10),
                loot_item(mid("matter_container_full"), 4, 8, 20),
                loot_item(mid("phaser"), 1, 1, 10)]}]})

# Placement. 1.7.10 tried a building in 1% of chunks (weights android house 20, sand pit 100, crashed ship 60,
# underwater base 20, cargo ship 5) with minimum distances 256 (ship), 2048 (base), 4096 (cargo ship): random spreads
# of about the same frequency.
STRUCTURES = {"android_house": ("#minecraft:is_overworld", 24, 8), "sand_pit": ("minecraft:desert", 16, 6),
              "crashed_ship": ("#minecraft:is_overworld", 20, 16), "underwater_base": ("#minecraft:is_deep_ocean", 128, 96),
              "cargo_ship": (["#minecraft:is_overworld", "#minecraft:is_end"], 256, 200)}
for i, (bid, (biomes, spacing, separation)) in enumerate(STRUCTURES.items()):
    w(D / f"tags/worldgen/biome/has_structure/{bid}.json", {"values": biomes if isinstance(biomes, list) else [biomes]})
    w(D / f"worldgen/structure/{bid}.json", {"type": mid("image"), "building": bid, "biomes": f"#{MOD}:has_structure/{bid}",
        # 1.7.10 built after the chunk was populated, over its trees: place after the vegetation too
        "step": "top_layer_modification", "spawn_overrides": {}, "terrain_adaptation": "none"})
    w(D / f"worldgen/structure_set/{bid}.json", {"structures": [{"structure": mid(bid), "weight": 1}],
        "placement": {"type": "minecraft:random_spread", "spacing": spacing, "separation": separation, "salt": 1870231 + i}})


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
w(D / "data_maps/item/matter.json", {"values": {**MATTER_TAGS, **{f"minecraft:{k}": v for k, v in MATTER_ITEMS.items()},
    # 1.7.10 MatterOverdriveMatter: blue and yellow android pills can be replicated, the red one can't
    mid("android_pill_blue"): 64, mid("android_pill_yellow"): 32,
    mid("emergency_ration"): 3, mid("earl_gray_tea"): 2, mid("romulan_ale"): 2}})


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
with gzip.GzipFile(D / "structure/gametest_area.nbt", "wb", mtime=0) as f:
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
    "item.matteroverdrive.energy_pack.details": {"en_us": "Used to reload energy weapons", "ru_ru": "Перезаряжает энергетическое оружие"},
    "death.attack.matteroverdrive.plasma": "death.attack.plasmaBolt",
    "death.attack.matteroverdrive.plasma.player": "death.attack.plasmaBolt",
    "death.attack.matteroverdrive.plasma.item": "death.attack.plasmaBolt.item",
    "tooltip.matteroverdrive.phaser.stun": {"en_us": "Stun %s/%s", "ru_ru": "Оглушение %s/%s"},
    "tooltip.matteroverdrive.phaser.kill": {"en_us": "Kill %s/%s", "ru_ru": "Поражение %s/%s"},
    "tooltip.matteroverdrive.phaser.stun_time": {"en_us": "Stun: %ss", "ru_ru": "Оглушение: %s с"},
    "tooltip.matteroverdrive.weapon.power_use": {"en_us": "Power Use: %s/s", "ru_ru": "Расход энергии: %s/с"},
    "tooltip.matteroverdrive.weapon.damage": {"en_us": "Damage: %s", "ru_ru": "Урон: %s"},
    "tooltip.matteroverdrive.weapon.dps": {"en_us": "DPS: %s", "ru_ru": "Урон/с: %s"},
    "tooltip.matteroverdrive.weapon.speed": {"en_us": "Speed: %s s/m", "ru_ru": "Скорострельность: %s выстр./мин"},
    "tooltip.matteroverdrive.weapon.range": {"en_us": "Range: %s b", "ru_ru": "Дальность: %s бл."},
    "tooltip.matteroverdrive.weapon.heat": {"en_us": "Heat: %s", "ru_ru": "Нагрев: %s"},
    "gui.matteroverdrive.efficiency": {"en_us": "Efficiency %s%%", "ru_ru": "Эффективность %s%%"},
    "death.attack.matteroverdrive.black_hole": "death.attack.blackHole",
    "death.attack.matteroverdrive.black_hole.player": "death.attack.blackHole",
    "gui.matteroverdrive.request": "gui.tooltip.button.request",
    "gui.matteroverdrive.search": {"en_us": "Search", "ru_ru": "Поиск"},
    "gui.matteroverdrive.pattern": {"en_us": "%s (pattern %s%%)", "ru_ru": "%s (шаблон %s%%)"},
    "item.matteroverdrive.matter_scanner": {"en_us": "Matter Scanner", "ru_ru": "Сканер материи"},
    "key.matteroverdrive.matter_scanner": {"en_us": "Matter Scanner GUI", "ru_ru": "Экран сканера материи"},
    "tooltip.matteroverdrive.scanner.online": {"en_us": "Online (pattern storage at %s)", "ru_ru": "В сети (хранилище шаблонов в %s)"},
    "tooltip.matteroverdrive.scanner.offline": {"en_us": "Offline", "ru_ru": "Не в сети"},
    "tooltip.matteroverdrive.scanner.selected": {"en_us": "Selected: %s", "ru_ru": "Выбрано: %s"},
    "tooltip.matteroverdrive.scanner.progress": {"en_us": "Progress: %s / 100 %%", "ru_ru": "Прогресс: %s / 100 %%"},
    "tooltip.matteroverdrive.scanner.open": {"en_us": "Press '%s' to open GUI", "ru_ru": "Нажмите '%s', чтобы открыть экран"},
    "gui.matteroverdrive.scanner.online": {"en_us": "Online", "ru_ru": "В сети"},
    "gui.matteroverdrive.scanner.storage_offline": {"en_us": "Pattern storage unreachable", "ru_ru": "Хранилище шаблонов недоступно"},
    "gui.matteroverdrive.scanner.nothing_selected": {"en_us": "Nothing selected. Point the scanner at a block and hold use.",
                                                     "ru_ru": "Ничего не выбрано. Наведите сканер на блок и удерживайте ПКМ."},
    "chat.matteroverdrive.scanner.cannot_analyze": {"en_us": "%s cannot be analyzed!", "ru_ru": "%s нельзя проанализировать!"},
    "chat.matteroverdrive.scanner.fully_analyzed": {"en_us": "%s is fully analyzed!", "ru_ru": "%s полностью проанализирован!"},
    "chat.matteroverdrive.scanner.added": {"en_us": "%s added to Pattern Storage. Progress is now at %s%%",
                                           "ru_ru": "%s добавлен в хранилище шаблонов. Прогресс: %s%%"},
    "chat.matteroverdrive.scanner.no_space": {"en_us": "No space available for '%s' !", "ru_ru": "Нет места для '%s'!"},
    "gui.matteroverdrive.queue": {"en_us": "Queue: %s requests, %s items", "ru_ru": "Очередь: %s заказов, %s предметов"},
    "gui.matteroverdrive.replicating": {"en_us": "%s x%s (pattern %s%%)", "ru_ru": "%s x%s (шаблон %s%%)"},
    "fluid.matteroverdrive.matter_plasma": {"en_us": "Matter Plasma", "ru_ru": "Плазменная материя"},
    "block.matteroverdrive.microwave": {"en_us": "Microwave", "ru_ru": "Микроволновка"},
    "item.matteroverdrive.portable_decomposer": {"en_us": "Portable Decomposer", "ru_ru": "Портативный разборщик"},
    "item.matteroverdrive.portable_decomposer.details": {"en_us": "Decomposes picked up items into Matter Plasma",
                                                         "ru_ru": "Разбирает подобранные предметы в плазменную материю"},
    "block.matteroverdrive.matter_plasma": {"en_us": "Matter Plasma", "ru_ru": "Плазменная материя"},
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
    for n in list(BATTERIES) + ["pattern_drive", "network_flash_drive", "spacetime_equalizer", "phaser", "phaser_rifle", "plasma_shotgun", "omni_tool",
                                "ion_sniper", "energy_pack", "matter_container", "matter_container_full"]:
        lang[f"item.{MOD}.{n}"] = src.get(f"item.{n}.name") or en.get(f"item.{n}.name")
    for n in BLOCKS + MACHINES + MACHINES_P3:
        key = LANG_KEYS.get(n, f"tile.{n}.name")
        lang[f"block.{MOD}.{n}"] = src.get(key) or en.get(key) or n
        if key not in src:
            fallback.append(n)
    COLOR_WORDS = {"en_us": ["Red", "Green", "Blue", "Brown", "Pink", "Sky Blue", "Gold", "Lime Green", "Black", "Grey"],
                   "ru_ru": ["Красный", "Зелёный", "Синий", "Коричневый", "Розовый", "Голубой", "Золотой", "Лаймовый", "Чёрный", "Серый"]}
    base = src.get("item.weapon_module_color.name") or en["item.weapon_module_color.name"]
    for n, word in zip(COLOR_NAMES, COLOR_WORDS[dst_name]):
        lang[f"item.{MOD}.weapon_module_color_{n}"] = f"{base} ({word})"
    lang[f"block.{MOD}.weapon_station"] = src.get("tile.weapon_station.name") or en["tile.weapon_station.name"]
    for i, stat in enumerate(["damage", "ammo", "effect", "range", "fire_damage", "block_damage", "explosion_damage", "fire_rate", "heal"]):
        lang[f"weapon_stat.{MOD}.{stat}"] = src.get(f"weaponstat.{i}.name") or en[f"weaponstat.{i}.name"]
    for m in ["battery", "color", "barrel", "sights", "other"]:
        lang[f"gui.{MOD}.module.{m}"] = src.get(f"module.{m}.name") or en[f"module.{m}.name"]
    for c in ["red", "blue", "yellow"]:
        lang[f"item.{MOD}.android_pill_{c}"] = src.get(f"item.android_pill_{c}.name") or en[f"item.android_pill_{c}.name"]
        lang[f"item.{MOD}.android_pill_{c}.details"] = src.get(f"item.android_pill_{c}.details") or en[f"item.android_pill_{c}.details"]
    for k in [k for k in en if k.startswith("biotic_stat.") or k.startswith("gui.android_hud.transforming")]:
        lang[k.replace("biotic_stat.", f"biotic_stat.{MOD}.").replace("gui.android_hud.", f"gui.{MOD}.android_hud.")] = src.get(k) or en[k]
    lang[f"death.attack.{MOD}.android_shockwave"] = {"en_us": "%1$s was blown away by %2$s's shockwave",
                                                     "ru_ru": "%1$s отброшен ударной волной %2$s"}[dst_name]
    lang[f"death.attack.{MOD}.android_shockwave.player"] = lang[f"death.attack.{MOD}.android_shockwave"]
    lang[f"key.category.{MOD}.android"] = "Matter Overdrive"
    lang[f"key.{MOD}.ability_use"] = {"en_us": "Android Ability key", "ru_ru": "Способность андроида"}[dst_name]
    lang[f"key.{MOD}.ability_switch"] = {"en_us": "Android Switch Ability key", "ru_ru": "Выбор способности андроида"}[dst_name]
    for name, _, _, _ in DECOR:
        key = f"tile.decorative.{DECOR_KEYS.get(name, name)}.name"
        lang[f"block.{MOD}.decorative_{name}"] = src.get(key) or en[key]
    DYE_WORDS = {"en_us": ["White", "Orange", "Magenta", "Light Blue", "Yellow", "Lime", "Pink", "Gray", "Light Gray", "Cyan", "Purple",
                           "Blue", "Brown", "Green", "Red", "Black"],
                 "ru_ru": ["белая", "оранжевая", "сиреневая", "голубая", "жёлтая", "лаймовая", "розовая", "серая", "светло-серая",
                           "бирюзовая", "фиолетовая", "синяя", "коричневая", "зелёная", "красная", "чёрная"]}
    colored = src.get("tile.decorative.tritanium_plate_colored.name") or en["tile.decorative.tritanium_plate_colored.name"]
    for dye, word in zip(DYES, DYE_WORDS[dst_name]):
        lang[f"block.{MOD}.decorative_tritanium_plate_{dye}"] = f"{colored} ({word})"
    lang[f"block.{MOD}.force_glass"] = src.get("tile.force_glass.name") or en["tile.force_glass.name"]
    lang[f"block.{MOD}.holo_sign"] = src.get("tile.holo_sign.name") or en["tile.holo_sign.name"]
    for item_id, key in {"emergency_ration": "emergency_ration", "earl_gray_tea": "earl_gray_tea", "romulan_ale": "romulan_ale",
                         "tritanium_spine": "tritainum_spine"}.items():
        lang[f"item.{MOD}.{item_id}"] = src.get(f"item.{key}.name") or en[f"item.{key}.name"]
    lang[f"entity.{MOD}.rogue_android"] = src.get("entity.rogue_android.name") or en["entity.rogue_android.name"]
    lang[f"entity.{MOD}.ranged_rogue_android"] = src.get("entity.ranged_rogue_android.name") or en["entity.ranged_rogue_android.name"]
    for n in ["rogue_android", "ranged_rogue_android"]:
        egg_word = {"en_us": "Spawn Egg", "ru_ru": "Яйцо призыва"}[dst_name]
        lang[f"item.{MOD}.{n}_spawn_egg"] = f"{egg_word}: {lang[f'entity.{MOD}.{n}']}" if dst_name == "ru_ru" else f"{lang[f'entity.{MOD}.{n}']} {egg_word}"
    lang[f"entity.{MOD}.mutant_scientist"] = (src.get("entity.mutant_scientist.name")
        or EXTRA.get(dst_name, {}).get("entity.mutant_scientist.name") or en["entity.mutant_scientist.name"])
    lang[f"item.{MOD}.mutant_scientist_spawn_egg"] = {"en_us": f"{lang[f'entity.{MOD}.mutant_scientist']} Spawn Egg",
                                                     "ru_ru": f"Яйцо призыва: {lang[f'entity.{MOD}.mutant_scientist']}"}[dst_name]
    for n in ["pig", "cow", "chicken", "sheep"]:
        lang[f"entity.{MOD}.failed_{n}"] = src.get(f"entity.failed_{n}.name") or en[f"entity.failed_{n}.name"]
        egg_word = {"en_us": "Spawn Egg", "ru_ru": "Яйцо призыва"}[dst_name]
        lang[f"item.{MOD}.failed_{n}_spawn_egg"] = f"{egg_word}: {lang[f'entity.{MOD}.failed_{n}']}" if dst_name == "ru_ru" else f"{lang[f'entity.{MOD}.failed_{n}']} {egg_word}"
    old_dye = {"light_gray": "silver", "light_blue": "lightBlue"}
    for dye in DYES:
        key = f"tile.tritanium_crate.{old_dye.get(dye, dye)}"
        lang[f"block.{MOD}.tritanium_crate_{dye}"] = src.get(key + ".name") or src.get(key) or en.get(key + ".name") or en[key]
    lang[f"container.{MOD}.tritanium_crate"] = src.get("container.tritanium_crate") or en["container.tritanium_crate"]
    lang[f"rarity.{MOD}.legendary"] = src.get("rarity.legendary") or en.get("rarity.legendary") or "Legendary"
    lang[f"block.{MOD}.charging_station"] = src.get("tile.charging_station.name") or en["tile.charging_station.name"]
    lang[f"block.{MOD}.android_station"] = src.get("tile.android_station.name") or en["tile.android_station.name"]
    for part in ["head", "arms", "legs", "chest"]:
        lang[f"item.{MOD}.rogue_android_part_{part}"] = src.get(f"item.rouge_android_part.{part}.name") or en[f"item.rouge_android_part.{part}.name"]
    for k in ["melee", "range"]:
        lang[f"item.{MOD}.rogue_android_part.{k}"] = src.get(f"item.rouge_android_part.{k}") or en[f"item.rouge_android_part.{k}"]
    for part in ["head", "arms", "legs", "chest", "other", "battery"]:
        lang[f"gui.{MOD}.biopart.{part}"] = src.get(f"biopart.{part}.name") or en[f"biopart.{part}.name"]
    lang[f"gui.{MOD}.requires"] = src.get("gui.tooltip.requires") or en["gui.tooltip.requires"]
    lang[f"gui.{MOD}.locks"] = src.get("gui.tooltip.locks") or {"en_us": "Locks", "ru_ru": "Блокирует"}[dst_name]
    lang[f"alert.{MOD}.not_android"] = src.get("alert.not_android") or en["alert.not_android"]
    lang[f"death.attack.{MOD}.android_transformation"] = src.get("death.attack.android_transformation") or en["death.attack.android_transformation"]
    lang[f"attribute.name.{MOD}.android_glitch_time"] = {"en_us": "Glitch Time", "ru_ru": "Длительность сбоев"}[dst_name]
    lang[f"attribute.name.{MOD}.android_battery_use"] = {"en_us": "Battery Use", "ru_ru": "Расход батареи"}[dst_name]
    for ours, theirs in GUI_KEYS.items():
        if isinstance(theirs, dict):
            lang[ours] = theirs[dst_name]
        else:
            lang[ours] = src.get(theirs) or en[theirs]
            if theirs not in src:
                fallback.append(ours)
    w(A / "lang" / f"{dst_name}.json", lang)
    print(f"{dst_name}: {len(lang)} keys, not in original {src_name}: {fallback or 'none'}")
