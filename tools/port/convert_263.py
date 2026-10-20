#!/usr/bin/env python3
"""Converts the 26.2 data JSON to the 26.3 formats, in place; safe to run again (26.3 input stays as it is).

    python3 -I tools/port/convert_263.py                         # the mod's resources (common/src/*/resources)
    python3 -I tools/port/convert_263.py --check OLD NEW         # checks the converters on vanilla: OLD and NEW are the
                                                                 # data/minecraft folders of the 26.2 and 26.3 client jars

What changes in 26.3 (only what this mod's data uses):
- block states are "mod:block" or {"id", "properties"} (was {"Name", "Properties"}); a simple state provider is the
  state itself, the other providers lost the "_state_provider"/"_provider" suffix;
- configured features are worldgen/feature with the config inline, configured carvers are worldgen/carver (the cave
  carver got new fields, liquids and debug states are gone);
- density functions: "argument" -> "input", "argument1/2" -> "left/right", y_clamped_gradient -> gradient,
  shifted_noise -> noise, cache_2d/flat_cache -> cache, end_islands -> the minecraft:end/islands function, interpolated
  takes its cell size (was the noise settings' size_horizontal/vertical);
- noise settings: surface_rule -> a material_rule (worldgen/material_rule file), aquifers are an object of their own
  (with an exclusion function, was hardcoded), the router has chunk_surface_level and no vein or aquifer entries, the
  beardifier is part of final_density;
- biome mob spawns are the natural_mob_spawns attribute, spawner counts are int providers ("count");
- loot: typed conditions and functions ("type"), one "condition", "modifier" (one or a list), match_block, whole
  set_count numbers; advancement entity conditions are one condition, recipe_unlocked takes "recipes".
"""
import json
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent.parent


def whole(value):
    if isinstance(value, float) and value.is_integer():
        return int(value)
    if isinstance(value, dict):
        return {k: whole(v) for k, v in value.items()}
    return value


# ---------------------------------------------------------------- block states and providers

def state(s):
    """{"Name", "Properties"} -> "id" / {"id", "properties"}"""
    if isinstance(s, dict) and "Name" in s:
        return {"id": s["Name"], "properties": s["Properties"]} if s.get("Properties") else s["Name"]
    return s


def state_object(s):
    """A block state where a provider is expected: a string would be a provider reference, so always an object"""
    s = state(s)
    return {"id": s} if isinstance(s, str) else s


PROVIDERS = {"simple_state_provider": "simple", "weighted_state_provider": "weighted", "noise_provider": "noise",
             "dual_noise_provider": "dual_noise", "noise_threshold_provider": "noise_threshold",
             "randomized_int_state_provider": "randomized_int", "rotated_block_provider": "rotated",
             "rule_based_state_provider": "rule_based"}


def provider(p):
    if not isinstance(p, dict) or "type" not in p:
        return p
    kind = p["type"].removeprefix("minecraft:")
    if kind not in PROVIDERS:
        return p
    p = dict(p)
    if kind == "simple_state_provider":
        return state_object(p["state"])
    p["type"] = "minecraft:" + PROVIDERS[kind]
    if "entries" in p:
        p["entries"] = [{**e, "data": state(e["data"])} for e in p["entries"]]
    for key in ("states",):
        if key in p:
            p[key] = [state(s) for s in p[key]]
    if "state" in p:
        p["state"] = state(p["state"])
    if "source" in p:
        p["source"] = provider(p["source"])
    if "fallback" in p:
        p["fallback"] = provider(p["fallback"])
    if "rules" in p:
        p["rules"] = [{**r, "then": provider(r["then"])} for r in p["rules"]]
    return p


# ---------------------------------------------------------------- features

def states(node):
    """Every {"Name", "Properties"} block state in a worldgen object (after the providers are converted)"""
    if isinstance(node, list):
        return [states(n) for n in node]
    if not isinstance(node, dict):
        return node
    if "Name" in node and set(node) <= {"Name", "Properties"}:
        return state(node)
    return {k: states(v) for k, v in node.items()}


def worldgen(node):
    """Providers, offsets and block states anywhere in a feature or placed feature (they nest)"""
    if isinstance(node, list):
        return [worldgen(n) for n in node]
    if not isinstance(node, dict):
        return node
    if node.get("type", "").removeprefix("minecraft:") in PROVIDERS:
        return states(provider(node))
    if node.get("type") == "minecraft:random_offset":
        return {"type": "minecraft:offset", "x": node["xz_spread"], "y": node["y_spread"], "z": node["xz_spread"]}
    if "Name" in node and set(node) <= {"Name", "Properties"}:
        return state(node)
    if isinstance(node.get("type"), str) and isinstance(node.get("config"), dict) and len(node) == 2:
        node = {"type": node["type"], **node["config"]}  # an inline configured feature
    return {k: worldgen(v) for k, v in node.items()}


def feature(obj):
    return worldgen(obj)


def placed_feature(obj):
    return worldgen(obj)


# the 26.2 cave carver's fixed values (vanilla minecraft:cave in 26.3)
CAVE_DEFAULTS = {"count": {"type": "minecraft:very_biased_to_bottom", "max_inclusive": 14, "min_inclusive": 0},
                 "thickness": {"type": "minecraft:trapezoid", "max": 3.0, "min": 0.0, "plateau": 1.0},
                 "weird_thickness_bias": True}


def carver(obj):
    if "config" not in obj:
        return obj
    c = obj["config"]
    # rats:ratlantis_caves was the cave carver without the water check (WorldCarver.liquids), 26.3 has aquifers only
    if obj["type"] not in ("minecraft:cave", "minecraft:cave_extra_underground", "rats:ratlantis_caves"):
        return obj  # only caves here
    out = {"type": "minecraft:cave", "probability": c["probability"], "y": c["y"]}
    out.update(CAVE_DEFAULTS)
    out["room_vertical_radius_multiplier"] = c["yScale"]
    for key in ("horizontal_radius_multiplier", "vertical_radius_multiplier", "floor_level"):
        out[key] = c[key]
    return dict(sorted(out.items()))


# ---------------------------------------------------------------- density functions and noise settings

UNARY = {"abs", "square", "cube", "half_negative", "quarter_negative", "squeeze", "blend_density", "invert"}
BINARY = {"add", "mul", "min", "max"}


def density(f, cells):
    if isinstance(f, list):
        return [density(x, cells) for x in f]
    if not isinstance(f, dict):
        return f
    kind = f.get("type", "").removeprefix("minecraft:")
    if kind in BINARY:
        return {"type": f["type"], "left": density(f["argument1"], cells), "right": density(f["argument2"], cells)}
    if kind in UNARY:
        return {"type": "minecraft:reciprocal" if kind == "invert" else f["type"], "input": density(f["argument"], cells)}
    if kind in ("cache_2d", "flat_cache"):
        inner = density(f["argument"], cells)
        if isinstance(inner, str) or isinstance(inner, dict) and inner.get("type") == "minecraft:cache":
            return inner
        return {"type": "minecraft:cache", "input": inner}
    if kind in ("cache_once", "cache_all_in_cell"):
        return density(f["argument"], cells)
    if kind == "interpolated":
        return {"type": f["type"], "cell_size_xz": cells[0], "cell_size_y": cells[1], "input": density(f["argument"], cells)}
    if kind == "y_clamped_gradient":
        return {"type": "minecraft:gradient", "axis": "y", "from_coordinate": f["from_y"], "from_value": f["from_value"],
                "to_coordinate": f["to_y"], "to_value": f["to_value"]}
    if kind == "shifted_noise":
        out = {"type": "minecraft:noise", "noise": f["noise"]}
        for key in ("shift_x", "shift_y", "shift_z"):
            if f.get(key) not in (0, 0.0, None):
                out[key] = density(f[key], cells)
        out["xz_scale"], out["y_scale"] = f["xz_scale"], f["y_scale"]
        return out
    if kind in ("shift_a", "shift_b", "shift"):
        return {"type": f["type"], "noise": f["argument"]}
    if kind == "end_islands":
        return "minecraft:end/islands"
    return {k: (density(v, cells) if isinstance(v, (dict, list)) else v) for k, v in f.items()}


def material_rule(rule):
    if isinstance(rule, list):
        return [material_rule(r) for r in rule]
    if not isinstance(rule, dict):
        return rule
    out = {k: material_rule(v) for k, v in rule.items()}
    if "result_state" in out:
        out["result_state"] = state(rule["result_state"])
    return out


def noise_settings(obj, name, extra):
    """extra: callback(folder, name, json) for the material rule file"""
    if "material_rule" in obj:
        return obj
    noise = obj["noise"]
    cells = (noise["size_horizontal"] * 4, noise["size_vertical"] * 4)
    router = obj["noise_router"]
    prelim = density(router["preliminary_surface_level"], cells)
    final = density(router["final_density"], cells)
    out = {
        "default_block": state(obj["default_block"]),
        "default_fluid": state({**obj["default_fluid"], "Properties": {k: v for k, v in obj["default_fluid"].get("Properties", {}).items()
                                                                      if (k, v) != ("level", "0")}}),
        "disable_mob_generation": obj["disable_mob_generation"],
        "legacy_random_source": obj["legacy_random_source"],
        "material_rule": name,
        "noise": {"height": noise["height"], "min_y": noise["min_y"]},
        "noise_router": {
            "chunk_surface_level": {"type": "minecraft:interpolated", "cell_size_xz": 16, "cell_size_y": 1, "input": prelim}
            if prelim not in (0, 0.0) else 0.0,
            "continents": density(router["continents"], cells),
            "depth": density(router["depth"], cells),
            "erosion": density(router["erosion"], cells),
            "final_density": {"type": "minecraft:add", "left": final, "right": {"type": "minecraft:beardifier"}},
            "ridges": density(router["ridges"], cells),
            "temperature": density(router["temperature"], cells),
            "vegetation": density(router["vegetation"], cells),
        },
        "sea_level": obj["sea_level"],
        "spawn_target": obj["spawn_target"],
    }
    if obj.get("aquifers_enabled"):
        # the 26.2 aquifers skipped the deep dark (erosion < -0.225 and depth > 0.9)
        erosion, depth = out["noise_router"]["erosion"], out["noise_router"]["depth"]
        exclusion = 0.0 if depth in (0, 0.0) else {
            "type": "minecraft:min", "left": {"type": "minecraft:sub", "left": -0.225, "right": erosion},
            "right": {"type": "minecraft:max", "left": {"type": "minecraft:sub", "left": depth, "right": 0.9}, "right": 0.0}}
        out["aquifers"] = {"barrier": density(router["barrier"], cells), "exclusion": exclusion,
                           "fluid_level_floodedness": density(router["fluid_level_floodedness"], cells),
                           "fluid_level_spread": density(router["fluid_level_spread"], cells),
                           "lava": density(router["lava"], cells), "surface_level": prelim}
    extra("worldgen/material_rule", name, material_rule(obj["surface_rule"]))
    return dict(sorted(out.items()))


# ---------------------------------------------------------------- spawns

def spawner(s):
    if "minCount" not in s:
        return s
    low, high = s["minCount"], s["maxCount"]
    return {"type": s["type"], "count": low if low == high else {"type": "minecraft:uniform", "max_inclusive": high, "min_inclusive": low},
            "weight": s["weight"]}


def biome(obj):
    if "spawners" not in obj:
        return obj
    obj = dict(obj)
    spawners = obj.pop("spawners")
    costs = obj.pop("spawn_costs", {})
    if "creature_spawn_probability" in obj:
        obj.setdefault("attributes", {})["minecraft:gameplay/creature_world_gen_spawn_probability"] = obj.pop("creature_spawn_probability")
    by_category = {k: [spawner(s) for s in v] for k, v in spawners.items() if v}
    attributes = dict(obj.get("attributes", {}))
    if by_category or costs:
        attributes["minecraft:gameplay/natural_mob_spawns"] = {"argument": {"spawn_costs": costs, "spawns_by_category": by_category},
                                                               "modifier": "overlay"}
    obj["attributes"] = dict(sorted(attributes.items()))
    return obj


def structure(obj):
    overrides = obj.get("spawn_overrides")
    if overrides:
        obj = {**obj, "spawn_overrides": {k: {**v, "spawns": [spawner(s) for s in v["spawns"]]} for k, v in overrides.items()}}
    return obj


def processor_list(obj):
    return states(obj)


def biome_modifier(obj):
    if obj.get("type") != "neoforge:add_spawns":
        return obj
    spawners = obj["spawners"]
    return {**obj, "spawners": [spawner(s) for s in spawners] if isinstance(spawners, list) else spawner(spawners)}


# ---------------------------------------------------------------- loot and advancements

SILK = {"predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}
TOOL_NAMES = {json.dumps({"items": "minecraft:shears"}, sort_keys=True): "minecraft:tool/can_shear",
              json.dumps(SILK, sort_keys=True): "minecraft:tool/can_silk_touch"}


def condition(c):
    if isinstance(c, str):
        return c
    c = dict(c)
    if "condition" not in c:
        return c
    kind = c.pop("condition")
    if kind == "minecraft:match_tool" and json.dumps(c.get("predicate"), sort_keys=True) in TOOL_NAMES:
        return TOOL_NAMES[json.dumps(c["predicate"], sort_keys=True)]
    if kind == "minecraft:block_state_property":
        out = {"type": "minecraft:match_block", "blocks": c["block"]}
        if c.get("properties"):
            out["state"] = c["properties"]
        return out
    if kind in ("minecraft:any_of", "minecraft:all_of"):
        c["terms"] = [condition(t) for t in c["terms"]]
    if kind == "minecraft:inverted":
        c["term"] = condition(c["term"])
    return {"type": kind, **c}


def conditions(node):
    if "conditions" in node:
        terms = [condition(c) for c in node.pop("conditions")]
        node["condition"] = terms[0] if len(terms) == 1 else {"type": "minecraft:all_of", "terms": terms}


def loot(node):
    if isinstance(node, list):
        return [loot(n) for n in node]
    if not isinstance(node, dict):
        return node
    node = {k: (v if k in ("predicate", "properties", "state") else loot(v)) for k, v in node.items()}
    conditions(node)
    if "function" in node:
        node = {"type": node.pop("function"), **node}
        if node["type"] == "minecraft:set_count":
            node["count"] = whole(node["count"])
            if node.get("add") is False:
                node.pop("add")
    if "functions" in node:
        functions = node.pop("functions")
        node["modifier"] = functions[0] if len(functions) == 1 else functions
    if isinstance(node.get("rolls"), float) and node["rolls"].is_integer():
        node["rolls"] = int(node["rolls"])
    if node.get("bonus_rolls") in (0, 0.0):
        node.pop("bonus_rolls")
    return node


RENAMED = {"block": "blocks", "recipe_id": "recipes", "loot_table": "loot_tables"}


def condition_list(value):
    """A 26.2 list of loot conditions -> one condition (lists of such lists stay lists)"""
    if isinstance(value, list) and value and all(isinstance(v, dict) and "condition" in v for v in value):
        terms = [condition(t) for t in value]
        return terms[0] if len(terms) == 1 else {"type": "minecraft:all_of", "terms": terms}
    if isinstance(value, list) and value and all(isinstance(v, list) for v in value):
        return [condition_list(v) for v in value]
    return value


def damage_tags(node):
    if isinstance(node, list):
        return [damage_tags(n) for n in node]
    if not isinstance(node, dict):
        return node
    out = {k: damage_tags(v) for k, v in node.items()}
    if isinstance(out.get("tags"), list):
        out["tags"] = [{**t, "id": t["id"] if t["id"].startswith("#") else "#" + t["id"]} if isinstance(t, dict) and "id" in t else t
                       for t in out["tags"]]
    return out


def item_predicates(node):
    """1.20 item predicates had "tag" (1.21 ignores it, so the check matched any item): it is "items": "#tag" now"""
    if isinstance(node, list):
        return [item_predicates(n) for n in node]
    if not isinstance(node, dict):
        return node
    out = {k: item_predicates(v) for k, v in node.items()}
    if "tag" in out and "items" not in out and isinstance(out["tag"], str) and set(out) <= {"tag", "count", "components", "predicates"}:
        out["items"] = "#" + out.pop("tag")
    return out


def advancement(obj):
    for criterion in obj.get("criteria", {}).values():
        c = criterion.get("conditions")
        if not c:
            continue
        if criterion.get("trigger") == "minecraft:recipe_unlocked" and "recipe" in c:
            c["recipes"] = c.pop("recipe")
        for old, new in RENAMED.items():
            if old in c:
                c[new] = c.pop(old)
        for key, value in list(c.items()):
            c[key] = item_predicates(condition_list(value))
        if "damage" in c or "killing_blow" in c:
            for key in ("damage", "killing_blow"):
                if key in c:
                    c[key] = damage_tags(c[key])
    return obj


# ---------------------------------------------------------------- recipes, trims, dimensions, loot modifiers

# 26.3: cookingtime is required; blasting and smoking take as long as smelting (the 2x speed of the blast furnace and
# the smoker comes from the fuel, CookingFuel.speedMultiplier), vanilla's 100 tick recipes became 200
COOKING = {"minecraft:smelting": 200, "minecraft:blasting": 200, "minecraft:smoking": 200, "minecraft:campfire_cooking": 100}


def recipe(obj):
    kind = obj.get("type")
    if kind in COOKING:
        obj = dict(obj)
        time = obj.get("cookingtime", 100 if kind in ("minecraft:blasting", "minecraft:smoking") else COOKING[kind])
        if kind in ("minecraft:blasting", "minecraft:smoking") and time == 100:
            time = 200
        obj["cookingtime"] = time
    return obj


def trim_material(obj):
    if "asset_name" not in obj:
        return obj
    obj = dict(obj)
    obj["palette_id"] = TRIM_NAMESPACE[0] + ":trim/" + obj.pop("asset_name")
    return obj


TRIM_NAMESPACE = ["minecraft"]


def dimension_type(obj):
    attributes = obj.get("attributes", {})
    bed = attributes.get("minecraft:gameplay/bed_rule")
    if bed is not None:
        attributes = dict(attributes)
        if bed.pop("explodes", False):
            bed["destroy_on_use"] = True
        if "minecraft:gameplay/straw_bed_rule" not in attributes:
            # as vanilla: a straw bed never sets the spawn and goes away when left (or on use where beds blow up)
            straw = {"can_set_spawn": "never", "can_sleep": bed.get("can_sleep", "when_dark")}
            if bed.get("destroy_on_use"):
                straw["destroy_on_use"] = True
            else:
                straw["destroy_on_leave"] = True
            if "error_message" in bed:
                straw["error_message"] = bed["error_message"]
            attributes["minecraft:gameplay/straw_bed_rule"] = straw
        obj = {**obj, "attributes": dict(sorted(attributes.items()))}
    return obj


def loot_modifier(obj):
    """NeoForge global loot modifiers: one typed "condition" (was a "conditions" list)"""
    obj = dict(obj)
    conditions(obj)
    return obj


# ---------------------------------------------------------------- driver

def mo_building(obj):
    """Matter Overdrive's image buildings (data/<ns>/mo_buildings): the palette's block states are BlockState.CODEC"""
    for entry in obj.get("palette", []):
        entry["states"] = [state(s) for s in entry.get("states", [])]
    return obj


CONVERTERS = {
    "mo_buildings": mo_building,
    "loot_table": loot,
    "advancement": advancement,
    "worldgen/placed_feature": placed_feature,
    "worldgen/biome": biome,
    "worldgen/structure": structure,
    "worldgen/processor_list": processor_list,
    "neoforge/biome_modifier": biome_modifier,
    "recipe": recipe,
    "trim_material": trim_material,
    "dimension_type": dimension_type,
    "loot_modifiers": loot_modifier,
}
MOVED = {"worldgen/configured_feature": ("worldgen/feature", feature), "worldgen/configured_carver": ("worldgen/carver", carver)}


def folder_of(rel):
    parts = rel.parts
    for depth in (2, 1):
        key = "/".join(parts[:depth])
        if key in CONVERTERS or key in MOVED or key == "worldgen/noise_settings":
            return key, pathlib.Path(*parts[depth:])
    return None, None


def dump(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n", encoding="utf-8")


def convert_namespace(ns_dir):
    """ns_dir: data/<namespace>; a carver moved earlier (worldgen/carver, still with "config") is converted again"""
    for path in sorted((ns_dir / "worldgen/carver").glob("*.json")) if (ns_dir / "worldgen/carver").exists() else []:
        obj = json.loads(path.read_text(encoding="utf-8"))
        if "config" in obj:
            dump(path, carver(obj))
    namespace = ns_dir.name
    for path in sorted(ns_dir.rglob("*.json")):
        rel = path.relative_to(ns_dir)
        key, rest = folder_of(rel)
        if key is None:
            continue
        obj = json.loads(path.read_text(encoding="utf-8"))
        TRIM_NAMESPACE[0] = namespace
        if key in MOVED:
            target, fn = MOVED[key]
            dump(ns_dir / target / rest, fn(obj))
            path.unlink()
        elif key == "worldgen/noise_settings":
            name = namespace + ":" + rest.with_suffix("").as_posix()

            def extra(folder, ident, value):
                ns, p = ident.split(":")
                dump(ns_dir.parent / ns / folder / (p + ".json"), value)
            dump(path, noise_settings(obj, name, extra))
        else:
            new = CONVERTERS[key](obj)
            if new != json.loads(path.read_text(encoding="utf-8")):
                dump(path, new)
    for key in MOVED:
        d = ns_dir / key
        if d.exists() and not any(d.rglob("*")):
            d.rmdir()


def assets():
    """Trim palettes: textures/trims/color_palettes/x.png -> textures/palettes/trim/x.png (palette_id mod:trim/x), the
    armor_trims atlas is gone (armor trims use the palettes directly), the items atlas takes minecraft:trim_base"""
    res = ROOT / "common/src/main/resources/assets"
    for ns in res.iterdir():
        old = ns / "textures/trims/color_palettes"
        if old.exists():
            new = ns / "textures/palettes/trim"
            new.mkdir(parents=True, exist_ok=True)
            for png in old.glob("*.png"):
                png.rename(new / png.name)
            for d in (old, old.parent):
                if not any(d.iterdir()):
                    d.rmdir()
    atlases = res / "minecraft/atlases"
    if (atlases / "armor_trims.json").exists():
        (atlases / "armor_trims.json").unlink()
    items = atlases / "items.json"
    if items.exists():
        obj = json.loads(items.read_text(encoding="utf-8"))
        for source in obj["sources"]:
            if source.get("type") == "minecraft:paletted_permutations":
                source["palette_key"] = "minecraft:trim_base"
                source["permutations"] = {k: v.replace("trims/color_palettes/", "trim/") for k, v in source["permutations"].items()}
        dump(items, obj)


def data_roots():
    for resources in (ROOT / "common/src/main/resources", ROOT / "common/src/generated/resources"):
        data = resources / "data"
        if not data.exists():
            continue
        for ns in data.iterdir():
            yield ns
        packs = data / "minecraft" / "datapacks"
        if packs.exists():
            for pack in packs.iterdir():
                if (pack / "data").exists():
                    yield from (pack / "data").iterdir()


# ---------------------------------------------------------------- check against vanilla

def same(a, b):
    # vanilla leaves out default properties: {"id": x, "properties": ...} counts as "x" here
    if isinstance(a, dict) and isinstance(b, str) and set(a) == {"id", "properties"}:
        return a["id"] == b
    if isinstance(a, bool) or isinstance(b, bool):
        return a == b
    if isinstance(a, (int, float)) and isinstance(b, (int, float)):
        return abs(a - b) < 1e-6
    if isinstance(a, dict) and isinstance(b, dict):
        return a.keys() == b.keys() and all(same(a[k], b[k]) for k in a)
    if isinstance(a, list) and isinstance(b, list):
        return len(a) == len(b) and all(same(x, y) for x, y in zip(a, b))
    return a == b


def first_diff(a, b, path=""):
    if isinstance(a, dict) and isinstance(b, dict):
        for k in sorted(set(a) | set(b)):
            if k not in a or k not in b:
                return f"{path}/{k}: {json.dumps(a.get(k))[:200]} != {json.dumps(b.get(k))[:200]}"
            d = first_diff(a[k], b[k], f"{path}/{k}")
            if d:
                return d
        return None
    if isinstance(a, list) and isinstance(b, list) and len(a) == len(b):
        for i, (x, y) in enumerate(zip(a, b)):
            d = first_diff(x, y, f"{path}[{i}]")
            if d:
                return d
        return None
    return None if same(a, b) else f"{path}: {json.dumps(a)[:200]} != {json.dumps(b)[:200]}"


def check(old, new, only=None):
    old, new = pathlib.Path(old), pathlib.Path(new)
    stats = {}
    shown = {}
    for path in sorted(old.rglob("*.json")):
        rel = path.relative_to(old)
        key, rest = folder_of(rel)
        if key is None or (only and only not in key):
            continue
        obj = json.loads(path.read_text(encoding="utf-8"))
        extras = {}
        if key in MOVED:
            target, fn = MOVED[key]
            got, want_path = fn(obj), new / target / rest
        elif key == "worldgen/noise_settings":
            got = noise_settings(obj, "minecraft:" + rest.with_suffix("").as_posix(), lambda f, i, v: extras.update({f: v}))
            want_path = new / rel
        else:
            got, want_path = CONVERTERS[key](obj), new / rel
        if not want_path.exists():
            continue
        want = json.loads(want_path.read_text(encoding="utf-8"))
        if isinstance(want, dict):
            want.pop("debug_functions", None)
        ok = same(got, want)
        s = stats.setdefault(key, [0, 0])
        s[0 if ok else 1] += 1
        if not ok and shown.get(key, 0) < 4:
            shown[key] = shown.get(key, 0) + 1
            print(f"  {rel}: {first_diff(got, want)}")
    for key, (ok, bad) in sorted(stats.items()):
        print(f"{key}: {ok} same, {bad} different")


if __name__ == "__main__":
    if len(sys.argv) > 1 and sys.argv[1] == "--check":
        check(sys.argv[2], sys.argv[3], sys.argv[4] if len(sys.argv) > 4 else None)
    else:
        for ns in data_roots():
            convert_namespace(ns)
        assets()
