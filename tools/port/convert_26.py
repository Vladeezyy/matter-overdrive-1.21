"""The 1.21.1 resources in the 26.x formats (run from the repo root; running it again changes nothing):

    python3 tools/port/convert_26.py

- repair tags of the tool and armor materials (data/rats/tags/item/repairs/*.json: the original's repair ingredients);
- the empty slot icons of the rat's inventory as GUI sprites (textures/gui/sprites/container/slot);
- the Ratlantis armor as an equipment asset (assets/rats/equipment/ratlantis.json) with its layer textures moved to
  textures/entity/equipment/humanoid(_leggings)/ratlantis.png;
- recipes: ingredients as strings ("item" -> id, "tag" -> "#tag"; a list with tags becomes an item tag of its own),
  the stonecutting count in the result;
- client item definitions (assets/rats/items): the 1.21.1 overrides as select / range_dispatch / condition models on
  the mod's properties (rat_count, special, soul) and vanilla's (trim_material, using_item + use_duration), the item
  colors as tints, the builtin/entity items as special models (rats:block_entity), spawn eggs with textures made from
  the 1.21.1 egg template and the original's two colors;
- the Ratlantis worldgen (built-in datapack): the dimension type with environment attributes, the noise router's
  preliminary surface, the biome's attributes and carver list, the flower patch as placement modifiers.
"""
import json
import pathlib
import shutil

GEN = pathlib.Path('common/src/generated/resources')
MAIN = pathlib.Path('common/src/main/resources')


def load(path):
    return json.loads(path.read_text(encoding='utf-8'))


def dump(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(obj, indent=2) + '\n'
    if not path.exists() or path.read_text(encoding='utf-8') != text:
        path.write_text(text, encoding='utf-8')


REPAIRS = {
    # tools (RatsToolMaterialRegistry)
    'cutlass': 'minecraft:iron_ingot', 'ghost_cutlass': 'rats:ghost_pirat_ectoplasm', 'baghnakhs': 'rats:feral_rat_claw',
    'plague_scythe': 'rats:plague_essence', 'ratlantis': 'rats:oratchalcum_ingot',
    # armor (RatsArmorMaterialRegistry)
    'piper_hat': 'rats:pied_wool', 'chef_toque': 'minecraft:white_wool', 'plague_mask': 'rats:plague_essence',
    'farmer_hat': 'minecraft:hay_block', 'top_hat': 'minecraft:black_wool', 'fez': 'minecraft:red_wool',
    'santa_hat': 'minecraft:red_wool', 'halo': 'minecraft:gold_nugget', 'crown': 'minecraft:gold_ingot',
    'ghost_hat': 'rats:ghost_pirat_ectoplasm', 'generic_hat': 'minecraft:leather',
}
for name, item in REPAIRS.items():
    dump(GEN / 'data/rats/tags/item/repairs' / (name + '.json'), {'values': [item]})

# Ratlantis armor: 1.21.1 drew textures/model/armor/ratlantis_armor_0 (outer) and _1 (leggings) through the item
textures = MAIN / 'assets/rats/textures'
for old, new in [('model/armor/ratlantis_armor_0.png', 'entity/equipment/humanoid/ratlantis.png'),
                 ('model/armor/ratlantis_armor_1.png', 'entity/equipment/humanoid_leggings/ratlantis.png')]:
    if (textures / old).exists() and not (textures / new).exists():
        (textures / new).parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(textures / old, textures / new)
dump(MAIN / 'assets/rats/equipment/ratlantis.json',
     {'layers': {'humanoid': [{'texture': 'rats:ratlantis'}], 'humanoid_leggings': [{'texture': 'rats:ratlantis'}]}})
# empty slot icons: GUI sprites in 26.x (1.21.1: block atlas sprites textures/item/empty_rat_*_slot)
for name in ['empty_rat_banner_slot', 'empty_rat_helmet_slot', 'empty_rat_item_slot', 'empty_rat_upgrade_slot']:
    old, new = textures / 'item' / (name + '.png'), textures / 'gui/sprites/container/slot' / (name + '.png')
    if old.exists() and not new.exists():
        new.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(old, new)

# --- recipes (26.1: an ingredient is an id, a #tag or a list of ids) ---
def ingredient(value):
    if isinstance(value, list):
        values = [ingredient(v) for v in value]
        return values[0] if len(values) == 1 else values
    if isinstance(value, dict):
        if 'item' in value:
            return value['item']
        if 'tag' in value:
            return '#' + value['tag']
    return value


def merged(data_root, recipe_name, index, value):
    """A list of choices holds items only in 26.1; choices with tags become an item tag of their own."""
    if isinstance(value, list) and any(v.startswith('#') for v in value):
        tag = 'recipe/%s_%s' % (recipe_name.replace('/', '_'), index)
        dump(data_root / 'rats/tags/item' / (tag + '.json'), {'values': [{'id': v, 'required': False} for v in value]})
        return '#rats:' + tag
    return value


for data_root in (GEN / 'data', GEN / 'data/minecraft/datapacks/ratlantis/data', MAIN / 'data'):
    for recipe in sorted(data_root.glob('*/recipe/**/*.json')):
        if 'datapacks' in recipe.relative_to(data_root).parts:
            continue
        obj = load(recipe)
        name = recipe.relative_to(data_root / recipe.relative_to(data_root).parts[0] / 'recipe').with_suffix('').as_posix()
        if 'key' in obj:
            obj['key'] = {k: merged(data_root, name, k, ingredient(v)) for k, v in obj['key'].items()}
        if 'ingredients' in obj:
            obj['ingredients'] = [merged(data_root, name, i, ingredient(v)) for i, v in enumerate(obj['ingredients'])]
        for key in ('ingredient', 'base', 'addition', 'template'):
            if key in obj:
                obj[key] = merged(data_root, name, key, ingredient(obj[key]))
        if obj.get('type') == 'minecraft:stonecutting' and 'count' in obj:
            count = obj.pop('count')
            if count != 1:
                obj['result']['count'] = count
        dump(recipe, obj)

# --- client item definitions (26.1: assets/<ns>/items/<id>.json) ---
import io
import re
import zipfile
from PIL import Image

ASSET_DIRS = [GEN / 'assets/rats', MAIN / 'assets/rats']
JARS = pathlib.Path.home() / '.gradle/caches/neoformruntime/artifacts'
VANILLA_26 = zipfile.ZipFile(JARS / 'minecraft_26.1.2_client.jar')
VANILLA_1211 = zipfile.ZipFile(JARS / 'minecraft_1.21.1_client.jar')


def vanilla_image(jar, path):
    return Image.open(io.BytesIO(jar.read('assets/minecraft/' + path))).convert('RGBA')


def argb(rgb):
    value = 0xFF000000 | (rgb & 0xFFFFFF)
    return value - (1 << 32)  # a signed int, as vanilla's item definitions write it


def model(ref):
    return {'type': 'minecraft:model', 'model': ref}


# DyeColor.getFireworkColor (the 1.21.1 item colors of tubes, igloos and hammocks), read from the 26.1.2 DyeColor
FIREWORK = {'white': 15790320, 'orange': 15435844, 'magenta': 12801229, 'light_blue': 6719955, 'yellow': 14602026,
            'lime': 4312372, 'pink': 14188952, 'gray': 4408131, 'light_gray': 11250603, 'cyan': 2651799,
            'purple': 8073150, 'blue': 2437522, 'brown': 5320730, 'green': 3887386, 'red': 11743532, 'black': 1973019}
# FoliageColor.get(0.5, 1.0): the marbled cheese grass item (1.21.1 item color)
foliage = vanilla_image(VANILLA_26, 'textures/colormap/foliage.png')
r, g, b, _ = foliage.getpixel((int((1.0 - 0.5) * 255.0), int((1.0 - 0.5 * 1.0) * 255.0)))
FOLIAGE = r << 16 | g << 8 | b
VANILLA_TRIMS = ['quartz', 'iron', 'netherite', 'redstone', 'copper', 'gold', 'emerald', 'diamond', 'lapis', 'amethyst']
SPECIAL = ['auto_curdler', 'trash_can', 'rat_trap', 'marbled_cheese_rat_head', 'ratlantis_portal']

# spawn eggs: the original's colors on the 1.21.1 egg template (26.1 eggs have their own textures)
eggs = {}
for registry in ('RatsEntityRegistry', 'RatlantisEntityRegistry'):
    source = (pathlib.Path('common/src/main/java/com/github/alexthe666/rats/registry') / (registry + '.java')).read_text(encoding='utf-8')
    for name, base, spots in re.findall(r'registerEntity\("([a-z_]+)",.*?, (0[xX][0-9A-Fa-f]+), (0[xX][0-9A-Fa-f]+)\)', source):
        eggs[name + '_spawn_egg'] = (int(base, 16), int(spots, 16))
egg_base = vanilla_image(VANILLA_1211, 'textures/item/spawn_egg.png')
egg_overlay = vanilla_image(VANILLA_1211, 'textures/item/spawn_egg_overlay.png')


def tinted(image, rgb):
    out = Image.new('RGBA', image.size)
    r0, g0, b0 = rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255
    for x in range(image.width):
        for y in range(image.height):
            r, g, b, a = image.getpixel((x, y))
            out.putpixel((x, y), (r * r0 // 255, g * g0 // 255, b * b0 // 255, a))
    return out


for egg, (base, spots) in sorted(eggs.items()):
    texture = MAIN / 'assets/rats/textures/item' / (egg + '.png')
    if not texture.exists():
        Image.alpha_composite(tinted(egg_base, base), tinted(egg_overlay, spots)).save(texture)
    for assets in ASSET_DIRS:
        old = assets / 'models/item' / (egg + '.json')
        if old.exists() and 'spawn_egg' in load(old).get('parent', ''):
            dump(old, {'parent': 'minecraft:item/generated', 'textures': {'layer0': 'rats:item/' + egg}})

item_models = {}
for assets in ASSET_DIRS:
    for file in sorted((assets / 'models/item').glob('*.json')):
        item_models[file.stem] = file
# the 1.21.1 overrides, kept beside this script: the models lose them on the first run
OVERRIDES_FILE = pathlib.Path(__file__).with_name('item_overrides_1.21.1.json')
overrides_by_item = load(OVERRIDES_FILE) if OVERRIDES_FILE.exists() else {}
for name, file in item_models.items():
    if 'overrides' in load(file):
        overrides_by_item[name] = load(file)['overrides']
dump(OVERRIDES_FILE, dict(sorted(overrides_by_item.items())))
submodels = {o['model'].split('/')[-1] for overrides in overrides_by_item.values() for o in overrides}
# models only other item models inherit from (cutlass, bagh_nakhs): not items, no definition
parents = {load(file).get('parent', '').split('/')[-1] for file in item_models.values() if load(file).get('parent', '').startswith('rats:item/')}
templates = {name for name in parents if name in item_models and 'textures' not in load(item_models[name])}
# the particle of the block entity items (1.21.1 builtin/entity models had none; 26.1 wants one): their block's texture
SPECIAL_PARTICLES = {'auto_curdler': 'minecraft:block/cauldron_side', 'trash_can': 'rats:block/trash_can_particle',
                     'rat_trap': 'minecraft:block/oak_planks', 'marbled_cheese_rat_head': 'rats:block/marbled_cheese',
                     'ratlantis_portal': 'minecraft:block/black_concrete'}
for template in templates:
    (GEN / 'assets/rats/items' / (template + '.json')).unlink(missing_ok=True)

for name, file in sorted(item_models.items()):
    obj = load(file)
    if name in submodels or name in templates:
        # NeoForge 1.21.1's emissive item layers: no such loader in 26.1
        if 'loader' in obj or 'neoforge_data' in obj:
            obj.pop('loader', None)
            obj.pop('neoforge_data', None)
            dump(file, obj)
        continue
    changed = obj.pop('overrides', None) is not None
    overrides = overrides_by_item.get(name, [])
    for key in ('loader', 'neoforge_data'):  # NeoForge 1.21.1's emissive item layers: no such loader in 26.1
        if key in obj:
            obj.pop(key)
            changed = True
    base = model('rats:item/' + name)
    if obj.get('parent') == 'builtin/entity':
        obj.pop('parent')  # 26.1 has no builtin/entity: the model only gives the transforms and the particle
        changed = True
    if name in SPECIAL_PARTICLES and obj.get('textures', {}).get('particle') != SPECIAL_PARTICLES[name]:
        obj['textures'] = {'particle': SPECIAL_PARTICLES[name]}
        changed = True
    if changed:
        dump(file, obj)
    predicates = {key for o in overrides for key in o['predicate']}
    if name in SPECIAL:
        definition = {'type': 'minecraft:special', 'base': 'rats:item/' + name, 'model': {'type': 'rats:block_entity', 'block': name}}
    elif 'minecraft:trim_type' in predicates:
        cases = []
        for o in overrides:
            material = o['model'].split('/')[-1][len(name) + 1:-len('_trim')]
            cases.append({'model': model(o['model']), 'when': ('minecraft:' if material in VANILLA_TRIMS else 'rats:') + material})
        definition = {'type': 'minecraft:select', 'property': 'minecraft:trim_material', 'cases': cases, 'fallback': base}
    elif 'minecraft:pulling' in predicates:
        pulls = sorted(overrides, key=lambda o: o['predicate'].get('minecraft:pull', 0.0))
        definition = {'type': 'minecraft:condition', 'property': 'minecraft:using_item', 'on_false': base,
                      'on_true': {'type': 'minecraft:range_dispatch', 'property': 'minecraft:use_duration', 'scale': 0.05,
                                  'entries': [{'model': model(o['model']), 'threshold': o['predicate']['minecraft:pull']} for o in pulls[1:]],
                                  'fallback': model(pulls[0]['model'])}}
    elif predicates & {'minecraft:rat_count', 'rats:special'}:
        prop = 'rats:rat_count' if 'minecraft:rat_count' in predicates else 'rats:special'
        definition = {'type': 'minecraft:range_dispatch', 'property': prop, 'fallback': base,
                      'entries': [{'model': model(o['model']), 'threshold': next(iter(o['predicate'].values()))} for o in overrides]}
    elif 'rats:soul' in predicates:
        definition = {'type': 'minecraft:condition', 'property': 'rats:soul', 'on_true': model(overrides[0]['model']), 'on_false': base}
    else:
        definition = base
        color = None
        for kind in ('rat_tube_', 'rat_igloo_', 'rat_hammock_'):
            if name.startswith(kind) and name[len(kind):] in FIREWORK:
                color = FIREWORK[name[len(kind):]]
        if color is not None:
            definition['tints'] = [{'type': 'minecraft:constant', 'value': argb(color)}] * 2
        elif name == 'marbled_cheese_grass':
            definition['tints'] = [{'type': 'minecraft:constant', 'value': argb(FOLIAGE)}]
        elif name == 'rat_nugget_ore':
            definition['tints'] = [{'type': 'minecraft:constant', 'value': -1}, {'type': 'rats:nugget_ore'}]
        elif name == 'party_hat':
            definition['tints'] = [{'type': 'minecraft:dye', 'default': argb(0x25C9E7)}, {'type': 'rats:party_hat'}]
    dump(GEN / 'assets/rats/items' / (name + '.json'), {'model': definition})

# block models that only give a particle had the builtin/entity parent in 1.21.1 (gone in 26.1)
for assets in ASSET_DIRS:
    for file in sorted((assets / 'models/block').glob('*.json')):
        obj = load(file)
        if obj.get('parent') in ('builtin/entity', 'minecraft:builtin/entity'):
            obj.pop('parent')
            dump(file, obj)

# trim materials: the ingredient and the model index are gone (the items carry provides_trim_material, see
# RatlantisItemRegistry; the item models choose by material id)
# The materials live in the mod's data, not in the Ratlantis pack: the items' provides_trim_material component is
# bound with the registries and a missing material stops the world from loading when the pack is off
pack_materials = GEN / 'data/minecraft/datapacks/ratlantis/data/rats/trim_material'
for material in sorted(pack_materials.glob('*.json')) if pack_materials.exists() else []:
    target = GEN / 'data/rats/trim_material' / material.name
    target.parent.mkdir(parents=True, exist_ok=True)
    material.replace(target)
for material in sorted((GEN / 'data/rats/trim_material').glob('*.json')):
    obj = load(material)
    if 'ingredient' in obj or 'item_model_index' in obj:
        obj.pop('ingredient', None)
        obj.pop('item_model_index', None)
        dump(material, obj)

# trim palettes of the Ratlantis materials: item trims are in the items atlas in 26.1 (1.21.1: blocks), armor trims
# are trims/entity/humanoid(_leggings)/<pattern> (1.21.1: trims/models/armor/<pattern>(_leggings))
atlases = MAIN / 'assets/minecraft/atlases'
PERMUTATIONS = {'gem_of_ratlantis': 'rats:trims/color_palettes/gem_of_ratlantis', 'oratchalcum': 'rats:trims/color_palettes/oratchalcum'}
vanilla_trims = json.loads(VANILLA_26.read('assets/minecraft/atlases/armor_trims.json'))['sources'][0]
if (atlases / 'blocks.json').exists():
    (atlases / 'blocks.json').unlink()
dump(atlases / 'items.json', {'sources': [{'type': 'minecraft:paletted_permutations',
                                           'textures': ['minecraft:trims/items/%s_trim' % piece for piece in ('helmet', 'chestplate', 'leggings', 'boots')],
                                           'palette_key': 'minecraft:trims/color_palettes/trim_palette', 'permutations': PERMUTATIONS}]})
dump(atlases / 'armor_trims.json', {'sources': [{'type': 'minecraft:paletted_permutations', 'textures': vanilla_trims['textures'],
                                                 'palette_key': vanilla_trims['palette_key'], 'permutations': PERMUTATIONS}]})

# game rule names: 26.1 keys are gamerule.<namespace>.<path> (1.21.1: gamerule.<camelCase name>); text-level, so the
# translations keep their formatting and characters
GAME_RULES = {'doRatSpawning': 'rats.do_rat_spawning', 'doPiperSpawning': 'rats.do_piper_spawning',
              'doPlagueDoctorSpawning': 'rats.do_plague_doctor_spawning'}
for lang in sorted([*(GEN / 'assets/rats/lang').glob('*.json'), *(MAIN / 'assets/rats/lang').glob('*.json')]):
    text = lang.read_text(encoding='utf-8')
    new = text
    for old_name, new_name in GAME_RULES.items():
        new = new.replace('"gamerule.%s' % old_name, '"gamerule.%s' % new_name)
    if new != text:
        lang.write_text(new, encoding='utf-8')

# --- Ratlantis worldgen (26.1 formats) ---
RAT = GEN / 'data/minecraft/datapacks/ratlantis/data/rats'


def hex_color(value):
    return value if isinstance(value, str) else '#%06x' % (value & 0xFFFFFF)


# dimension type: 1.21.1 flags -> 26.1 attributes (as the overworld: Ratlantis had the overworld effects, beds and
# clocks; no raids, no respawn anchors, natural -> portals spawn piglins)
dim_type = RAT / 'dimension_type/ratlantis_type.json'
old = load(dim_type)
if 'bed_works' in old:
    dump(dim_type, {
        'ambient_light': old['ambient_light'],
        'attributes': {
            'minecraft:audio/ambient_sounds': {'mood': {'block_search_extent': 8, 'offset': 2.0, 'sound': 'minecraft:ambient.cave', 'tick_delay': 6000}},
            'minecraft:audio/background_music': {
                'creative': {'max_delay': 24000, 'min_delay': 12000, 'sound': 'minecraft:music.creative'},
                'default': {'max_delay': 24000, 'min_delay': 12000, 'sound': 'minecraft:music.game'}},
            'minecraft:gameplay/bed_rule': {'can_set_spawn': 'always', 'can_sleep': 'when_dark', 'error_message': {'translate': 'block.minecraft.bed.no_sleep'}},
            'minecraft:gameplay/can_start_raid': old['has_raids'],
            'minecraft:gameplay/nether_portal_spawns_piglin': old['natural'],
            'minecraft:gameplay/respawn_anchor_works': old['respawn_anchor_works'],
            'minecraft:visual/ambient_light_color': '#0a0a0a',
            'minecraft:visual/cloud_color': '#ccffffff',
            'minecraft:visual/cloud_height': 192.33,
            'minecraft:visual/fog_color': '#c0d8ff',
            'minecraft:visual/sky_color': '#78a7ff',
        },
        'coordinate_scale': old['coordinate_scale'],
        'default_clock': 'minecraft:overworld',
        'has_ceiling': old['has_ceiling'],
        'has_ender_dragon_fight': False,
        'has_skylight': old['has_skylight'],
        'height': old['height'],
        'infiniburn': old['infiniburn'],
        'logical_height': old['logical_height'],
        'min_y': old['min_y'],
        'monster_spawn_block_light_limit': old['monster_spawn_block_light_limit'],
        'monster_spawn_light_level': old['monster_spawn_light_level'],
        'timelines': '#minecraft:in_overworld',
    })

# noise router: 1.21.1 scanned initial_density_without_jaggedness down from the top in cell steps for the first value
# above 0.390625; 26.1 has that as a density function
settings_file = RAT / 'worldgen/noise_settings/ratlantis_noise_gen.json'
settings = load(settings_file)
router = settings['noise_router']
if 'initial_density_without_jaggedness' in router:
    noise = settings['noise']
    router['preliminary_surface_level'] = {
        'type': 'minecraft:find_top_surface',
        'cell_height': noise['size_vertical'] * 4,
        'density': {'type': 'minecraft:add', 'argument1': -0.390625, 'argument2': router.pop('initial_density_without_jaggedness')},
        'lower_bound': noise['min_y'],
        'upper_bound': float(noise['min_y'] + noise['height']),
    }
    settings['noise_router'] = dict(sorted(router.items()))
    dump(settings_file, settings)

# biome: sky, fog, water fog and mood sound became attributes, colors are hex strings, carvers a list
biome_file = RAT / 'worldgen/biome/ratlantis.json'
biome = load(biome_file)
if isinstance(biome.get('carvers'), dict):
    effects = biome['effects']
    attributes = biome.setdefault('attributes', {})
    if 'mood_sound' in effects:
        attributes['minecraft:audio/ambient_sounds'] = {'mood': effects.pop('mood_sound')}
    for old_key, new_key in (('fog_color', 'minecraft:visual/fog_color'), ('sky_color', 'minecraft:visual/sky_color'),
                             ('water_fog_color', 'minecraft:visual/water_fog_color')):
        if old_key in effects:
            attributes[new_key] = hex_color(effects.pop(old_key))
    for key in list(effects):
        if key.endswith('_color'):
            effects[key] = hex_color(effects[key])
    biome['attributes'] = dict(sorted(attributes.items()))
    carvers = biome['carvers']
    biome['carvers'] = [c for step in ('air', 'liquid') for c in ([carvers[step]] if isinstance(carvers.get(step), str) else carvers.get(step, []))]
    order = ['attributes', 'carvers', 'creature_spawn_probability', 'downfall', 'effects', 'features', 'has_precipitation', 'spawn_costs', 'spawners', 'temperature', 'temperature_modifier']
    dump(biome_file, {k: biome[k] for k in sorted(biome, key=lambda k: order.index(k) if k in order else 99)})

# the flower patch: Feature.FLOWER (RandomPatchConfiguration) is gone; the tries are a count with a triangular offset
flowers_cf = RAT / 'worldgen/configured_feature/ratglove_flowers.json'
flowers = load(flowers_cf)
if flowers['type'] == 'minecraft:flower':
    patch = flowers['config']
    flowers_pf = RAT / 'worldgen/placed_feature/ratglove_flowers.json'
    placed = load(flowers_pf)
    placed['placement'] += [
        {'type': 'minecraft:count', 'count': patch['tries']},
        {'type': 'minecraft:random_offset',
         'xz_spread': {'type': 'minecraft:trapezoid', 'max': patch['xz_spread'], 'min': -patch['xz_spread'], 'plateau': 0},
         'y_spread': {'type': 'minecraft:trapezoid', 'max': patch['y_spread'], 'min': -patch['y_spread'], 'plateau': 0}},
    ] + patch['feature']['placement']
    dump(flowers_pf, placed)
    dump(flowers_cf, patch['feature']['feature'])

print('converted')
