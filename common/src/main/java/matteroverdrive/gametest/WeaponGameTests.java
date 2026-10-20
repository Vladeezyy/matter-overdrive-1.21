package matteroverdrive.gametest;

import java.util.List;

import matteroverdrive.entity.PlasmaBolt;
import matteroverdrive.init.MOItems;
import matteroverdrive.item.weapon.EnergyPackItem;
import matteroverdrive.item.weapon.EnergyWeaponItem;
import matteroverdrive.recipe.EnergyPackRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.phys.AABB;

/** Phase 5 checks: energy weapons, plasma bolts, energy packs. */
final class WeaponGameTests {
    static void addAll() {
        MOGameTests.add("rifle_fires_bolt", 20, false, WeaponGameTests::rifleFires);
        MOGameTests.add("bolt_hurts_mob", 60, false, WeaponGameTests::boltHurtsMob);
        MOGameTests.add("rifle_overheats", 20, false, WeaponGameTests::rifleOverheats);
        MOGameTests.add("shotgun_spread", 20, false, WeaponGameTests::shotgunSpread);
        MOGameTests.add("shotgun_charged_shot", 20, false, WeaponGameTests::shotgunChargedShot);
        MOGameTests.add("weapon_cools_down", 20, false, WeaponGameTests::coolsDown);
        MOGameTests.add("energy_pack_reloads", 20, false, WeaponGameTests::energyPackReloads);
        MOGameTests.add("energy_pack_recipe", 20, false, WeaponGameTests::energyPackRecipe);
        MOGameTests.add("phaser_stuns", 20, false, h -> phaserHits(h, 2));
        MOGameTests.add("phaser_kills", 20, false, h -> phaserHits(h, 5));
        MOGameTests.add("phaser_levels_energy_heat", 20, false, WeaponGameTests::phaserLevels);
        MOGameTests.add("legendary_weapon", 20, false, WeaponGameTests::legendaryWeapon);
    }

    /** Legendary multipliers: damage and cooldown change; the range multiplier is read as an int like 1.7.10 (no change). */
    private static void legendaryWeapon(GameTestHelper helper) {
        var rifle = matteroverdrive.init.MOItems.PHASER_RIFLE.get();
        net.minecraft.world.item.ItemStack plain = new net.minecraft.world.item.ItemStack(rifle);
        net.minecraft.world.item.ItemStack legendary = plain.copy();
        legendary.set(matteroverdrive.init.MODataComponents.LEGENDARY_WEAPON.get(), new matteroverdrive.item.weapon.WeaponFactory.Legendary(1.3f, 0.7f, 0.85f, 1.45f));
        var zombie = helper.spawn(net.minecraft.world.entity.EntityType.ZOMBIE, new net.minecraft.core.BlockPos(1, 1, 1));
        float base = rifle.getDamage(plain, zombie), boosted = rifle.getDamage(legendary, zombie);
        float attack = (float) zombie.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        helper.assertTrue(Math.abs((boosted - attack) - (base - attack) * 1.3f) < 1e-4, "damage " + base + " -> " + boosted);
        helper.assertTrue(rifle.getShootCooldown(legendary) == (int) (rifle.getShootCooldown(plain) * 0.85f),
                "cooldown " + rifle.getShootCooldown(legendary));
        helper.assertTrue(rifle.getRange(legendary) == rifle.getRange(plain), "range " + rifle.getRange(legendary));
        var generated = matteroverdrive.item.weapon.WeaponFactory.randomDecorated(helper.getLevel().random, 3, true);
        helper.assertTrue(generated.has(matteroverdrive.init.MODataComponents.LEGENDARY_WEAPON.get()), "no legendary stats");
        zombie.discard();
        helper.succeed();
    }

    /** 1.7.10 PlasmaShotgun.onPlayerStoppedUsing: 15 of 20 charge ticks leave 2 bolts of render size (10 / 2) x 0.5. */
    private static void shotgunChargedShot(GameTestHelper helper) {
        ItemStack shotgun = charged(MOItems.PLASMA_SHOTGUN.get());
        ServerPlayer player = shooter(helper, shotgun);
        var item = MOItems.PLASMA_SHOTGUN.get();
        helper.assertTrue(item.releaseUsing(shotgun, helper.getLevel(), player, item.getUseDuration(shotgun, player) - 15),
                "charged shot not fired");
        List<PlasmaBolt> bolts = bolts(helper, player);
        helper.assertTrue(bolts.size() == 2 && bolts.stream().allMatch(b -> b.getRenderSize() == 2.5f),
                "charged bolts: " + bolts.size() + " " + bolts.stream().map(PlasmaBolt::getRenderSize).toList());
        helper.assertTrue(matteroverdrive.item.weapon.PlasmaShotgunItem.chargedShots(0) == 10
                && matteroverdrive.item.weapon.PlasmaShotgunItem.chargedShots(40) == 1, "charge curve");
        helper.assertFalse(item.releaseUsing(shotgun, helper.getLevel(), player, 0), "fired during cooldown");
        helper.succeed();
    }

    private static ServerPlayer shooter(GameTestHelper helper, ItemStack weapon) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        var pos = helper.absolutePos(new BlockPos(6, 1, 1)).getBottomCenter();
        player.moveTo(pos.x, pos.y, pos.z, 0, 0);      // yaw 0: looking south (+z)
        player.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        return player;
    }

    private static ItemStack charged(EnergyWeaponItem item) {
        ItemStack stack = new ItemStack(item);
        EnergyWeaponItem.setEnergy(stack, EnergyWeaponItem.CAPACITY);
        return stack;
    }

    /** Bolts fired by this shooter (neighbouring tests fire too). */
    private static List<PlasmaBolt> bolts(GameTestHelper helper, ServerPlayer shooter) {
        return helper.getLevel().getEntitiesOfClass(PlasmaBolt.class, new AABB(shooter.blockPosition()).inflate(40), b -> b.getOwner() == shooter);
    }

    private static void rifleFires(GameTestHelper helper) {
        ItemStack rifle = charged(MOItems.PHASER_RIFLE.get());
        ServerPlayer player = shooter(helper, rifle);
        MOItems.PHASER_RIFLE.get().tryFire(player, rifle, false);
        // 1.7.10: 1024 / 11 = 93 FE per tick of cooldown, x 11 ticks
        helper.assertTrue(EnergyWeaponItem.getEnergy(rifle) == 32000 - 93 * 11, "energy " + EnergyWeaponItem.getEnergy(rifle));
        helper.assertTrue(Math.abs(EnergyWeaponItem.getHeat(rifle) - 8.8f) < 1e-4, "heat " + EnergyWeaponItem.getHeat(rifle));
        helper.assertTrue(bolts(helper, player).size() == 1, "bolts: " + bolts(helper, player).size());
        MOItems.PHASER_RIFLE.get().tryFire(player, rifle, false);     // still on cooldown
        helper.assertTrue(bolts(helper, player).size() == 1, "fired during cooldown");
        helper.succeed();
    }

    private static void boltHurtsMob(GameTestHelper helper) {
        ItemStack rifle = charged(MOItems.PHASER_RIFLE.get());
        ServerPlayer player = shooter(helper, rifle);
        var pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(6, 1, 6));
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, pig.position().add(0, 0.4, 0));
        MOItems.PHASER_RIFLE.get().tryFire(player, rifle, true);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(pig.isDeadOrDying() || pig.getHealth() < pig.getMaxHealth(), "pig unhurt: " + pig.getHealth());
            helper.succeed();
        });
    }

    private static void rifleOverheats(GameTestHelper helper) {
        ItemStack rifle = charged(MOItems.PHASER_RIFLE.get());
        ServerPlayer player = shooter(helper, rifle);
        // heat: 8.8, 28.16, 70.8, 164.6 -> the fourth shot passes 80
        for (int i = 0; i < 4; i++) {
            player.getCooldowns().removeCooldown(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(rifle.getItem()));
            MOItems.PHASER_RIFLE.get().tryFire(player, rifle, false);
            helper.assertTrue(EnergyWeaponItem.isOverheated(rifle) == (i == 3), "overheated after shot " + (i + 1) + ": "
                    + EnergyWeaponItem.isOverheated(rifle) + ", heat " + EnergyWeaponItem.getHeat(rifle));
        }
        helper.assertFalse(MOItems.PHASER_RIFLE.get().canFire(rifle), "can fire while overheated");
        helper.succeed();
    }

    private static void shotgunSpread(GameTestHelper helper) {
        ItemStack shotgun = charged(MOItems.PLASMA_SHOTGUN.get());
        ServerPlayer player = shooter(helper, shotgun);
        MOItems.PLASMA_SHOTGUN.get().tryFire(player, shotgun, false);
        List<PlasmaBolt> bolts = bolts(helper, player);
        helper.assertTrue(bolts.size() == 10, "shotgun bolts: " + bolts.size());
        float expected = (16 + 1) / 10f;   // 16 split over 10 bolts, +1 player attack damage
        helper.assertTrue(bolts.stream().allMatch(b -> Math.abs(b.getDamage() - expected) < 1e-4), "bolt damage " + bolts.get(0).getDamage());
        helper.succeed();
    }

    private static void coolsDown(GameTestHelper helper) {
        ItemStack rifle = charged(MOItems.PHASER_RIFLE.get());
        EnergyWeaponItem.setHeat(rifle, 79);
        rifle.set(matteroverdrive.init.MODataComponents.OVERHEATED.get(), true);
        for (int i = 0; i < 200; i++) MOItems.PHASER_RIFLE.get().inventoryTick(rifle, helper.getLevel(), null, 0, false);
        helper.assertTrue(EnergyWeaponItem.getHeat(rifle) < 2 && !EnergyWeaponItem.isOverheated(rifle),
                "after cooling: heat " + EnergyWeaponItem.getHeat(rifle));
        helper.succeed();
    }

    private static void energyPackReloads(GameTestHelper helper) {
        ItemStack rifle = new ItemStack(MOItems.PHASER_RIFLE.get());
        ServerPlayer player = shooter(helper, rifle);
        player.getInventory().setItem(9, new ItemStack(MOItems.ENERGY_PACK.get(), 2));
        MOItems.PHASER_RIFLE.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(EnergyWeaponItem.getEnergy(rifle) == EnergyPackItem.ENERGY, "energy " + EnergyWeaponItem.getEnergy(rifle));
        helper.assertTrue(player.getInventory().getItem(9).getCount() == 1, "pack not used");
        helper.succeed();
    }

    private static void energyPackRecipe(GameTestHelper helper) {
        EnergyPackRecipe recipe = new EnergyPackRecipe(CraftingBookCategory.MISC);
        CraftingInput input = CraftingInput.of(3, 1, List.of(new ItemStack(MOItems.TRITANIUM_PLATE.get()),
                MOItems.BATTERY.get().charged(), new ItemStack(Items.GUNPOWDER)));
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "recipe doesn't match");
        ItemStack out = recipe.assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(out.is(MOItems.ENERGY_PACK.get()) && out.getCount() == 16, "result " + out);
        CraftingInput empty = CraftingInput.of(3, 1, List.of(new ItemStack(MOItems.TRITANIUM_PLATE.get()),
                new ItemStack(MOItems.BATTERY.get()), new ItemStack(Items.GUNPOWDER)));
        helper.assertFalse(recipe.matches(empty, helper.getLevel()), "empty battery matches");
        helper.succeed();
    }

    private static void phaserHits(GameTestHelper helper, int level) {
        ItemStack phaser = charged(MOItems.PHASER.get());
        phaser.set(matteroverdrive.init.MODataComponents.PHASER_LEVEL.get(), level);
        ServerPlayer player = shooter(helper, phaser);
        var pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(6, 1, 5));
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, pig.position().add(0, 0.4, 0));
        MOItems.PHASER.get().onUseTick(helper.getLevel(), player, phaser, 1000);
        if (level < 3) {
            // stun: slowness for (level + 1)^5 ticks; 1.7.10 still dealt the shooter's base attack damage (1)
            var slow = pig.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
            helper.assertTrue(slow != null && slow.getDuration() == (int) Math.pow(level + 1, 5), "slowness " + slow);
            helper.assertTrue(pig.getHealth() == pig.getMaxHealth() - 1, "stun damage: health " + pig.getHealth());
        } else {
            // kill mode: 2^(level - 2) + 1
            float expected = pig.getMaxHealth() - ((float) Math.pow(2, level - 2) + 1);
            helper.assertTrue(pig.isDeadOrDying() || pig.getHealth() == expected, "kill damage: health " + pig.getHealth());
            helper.assertTrue(pig.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN) == null, "kill mode stunned");
        }
        helper.succeed();
    }

    private static void phaserLevels(GameTestHelper helper) {
        ItemStack phaser = charged(MOItems.PHASER.get());
        ServerPlayer player = shooter(helper, phaser);
        var item = MOItems.PHASER.get();
        helper.assertTrue(item.getEnergyUse(phaser) == 2, "level 0 energy " + item.getEnergyUse(phaser));
        item.onUseTick(helper.getLevel(), player, phaser, 1000);
        helper.assertTrue(Math.abs(EnergyWeaponItem.getHeat(phaser) - 1.1f) < 1e-4, "level 0 heat " + EnergyWeaponItem.getHeat(phaser));
        player.setShiftKeyDown(true);
        for (int i = 0; i < 5; i++) item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(matteroverdrive.item.weapon.PhaserItem.getLevel(phaser) == 5, "level " + matteroverdrive.item.weapon.PhaserItem.getLevel(phaser));
        helper.assertTrue(item.getEnergyUse(phaser) == 85, "level 5 energy " + item.getEnergyUse(phaser));
        EnergyWeaponItem.setHeat(phaser, 0);
        player.setShiftKeyDown(false);
        item.onUseTick(helper.getLevel(), player, phaser, 1000);
        helper.assertTrue(Math.abs(EnergyWeaponItem.getHeat(phaser) - 1.15f) < 1e-4, "level 5 heat " + EnergyWeaponItem.getHeat(phaser));
        player.setShiftKeyDown(true);
        item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(matteroverdrive.item.weapon.PhaserItem.getLevel(phaser) == 0, "level didn't wrap");
        helper.succeed();
    }

    private WeaponGameTests() {}
}
