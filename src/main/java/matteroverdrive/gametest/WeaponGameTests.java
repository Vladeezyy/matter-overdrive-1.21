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
        MOGameTests.add("weapon_cools_down", 20, false, WeaponGameTests::coolsDown);
        MOGameTests.add("energy_pack_reloads", 20, false, WeaponGameTests::energyPackReloads);
        MOGameTests.add("energy_pack_recipe", 20, false, WeaponGameTests::energyPackRecipe);
    }

    private static ServerPlayer shooter(GameTestHelper helper, ItemStack weapon) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        var pos = helper.absolutePos(new BlockPos(6, 1, 1)).getBottomCenter();
        player.snapTo(pos.x, pos.y, pos.z, 0, 0);      // yaw 0: looking south (+z)
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
        helper.assertTrue(EnergyWeaponItem.getEnergy(rifle) == 32000 - 93 * 11, Component.literal("energy " + EnergyWeaponItem.getEnergy(rifle)));
        helper.assertTrue(Math.abs(EnergyWeaponItem.getHeat(rifle) - 8.8f) < 1e-4, Component.literal("heat " + EnergyWeaponItem.getHeat(rifle)));
        helper.assertTrue(bolts(helper, player).size() == 1, Component.literal("bolts: " + bolts(helper, player).size()));
        MOItems.PHASER_RIFLE.get().tryFire(player, rifle, false);     // still on cooldown
        helper.assertTrue(bolts(helper, player).size() == 1, Component.literal("fired during cooldown"));
        helper.succeed();
    }

    private static void boltHurtsMob(GameTestHelper helper) {
        ItemStack rifle = charged(MOItems.PHASER_RIFLE.get());
        ServerPlayer player = shooter(helper, rifle);
        var pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(6, 1, 6));
        player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, pig.position().add(0, 0.4, 0));
        MOItems.PHASER_RIFLE.get().tryFire(player, rifle, true);
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(pig.isDeadOrDying() || pig.getHealth() < pig.getMaxHealth(), Component.literal("pig unhurt: " + pig.getHealth()));
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
            helper.assertTrue(EnergyWeaponItem.isOverheated(rifle) == (i == 3), Component.literal("overheated after shot " + (i + 1) + ": "
                    + EnergyWeaponItem.isOverheated(rifle) + ", heat " + EnergyWeaponItem.getHeat(rifle)));
        }
        helper.assertFalse(MOItems.PHASER_RIFLE.get().canFire(rifle), Component.literal("can fire while overheated"));
        helper.succeed();
    }

    private static void shotgunSpread(GameTestHelper helper) {
        ItemStack shotgun = charged(MOItems.PLASMA_SHOTGUN.get());
        ServerPlayer player = shooter(helper, shotgun);
        MOItems.PLASMA_SHOTGUN.get().tryFire(player, shotgun, false);
        List<PlasmaBolt> bolts = bolts(helper, player);
        helper.assertTrue(bolts.size() == 10, Component.literal("shotgun bolts: " + bolts.size()));
        float expected = (16 + 1) / 10f;   // 16 split over 10 bolts, +1 player attack damage
        helper.assertTrue(bolts.stream().allMatch(b -> Math.abs(b.getDamage() - expected) < 1e-4), Component.literal("bolt damage " + bolts.get(0).getDamage()));
        helper.succeed();
    }

    private static void coolsDown(GameTestHelper helper) {
        ItemStack rifle = charged(MOItems.PHASER_RIFLE.get());
        EnergyWeaponItem.setHeat(rifle, 79);
        rifle.set(matteroverdrive.init.MODataComponents.OVERHEATED.get(), true);
        for (int i = 0; i < 200; i++) MOItems.PHASER_RIFLE.get().inventoryTick(rifle, helper.getLevel(), null, null);
        helper.assertTrue(EnergyWeaponItem.getHeat(rifle) < 2 && !EnergyWeaponItem.isOverheated(rifle),
                Component.literal("after cooling: heat " + EnergyWeaponItem.getHeat(rifle)));
        helper.succeed();
    }

    private static void energyPackReloads(GameTestHelper helper) {
        ItemStack rifle = new ItemStack(MOItems.PHASER_RIFLE.get());
        ServerPlayer player = shooter(helper, rifle);
        player.getInventory().setItem(9, new ItemStack(MOItems.ENERGY_PACK.get(), 2));
        MOItems.PHASER_RIFLE.get().use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(EnergyWeaponItem.getEnergy(rifle) == EnergyPackItem.ENERGY, Component.literal("energy " + EnergyWeaponItem.getEnergy(rifle)));
        helper.assertTrue(player.getInventory().getItem(9).getCount() == 1, Component.literal("pack not used"));
        helper.succeed();
    }

    private static void energyPackRecipe(GameTestHelper helper) {
        EnergyPackRecipe recipe = new EnergyPackRecipe(CraftingBookCategory.MISC);
        CraftingInput input = CraftingInput.of(3, 1, List.of(new ItemStack(MOItems.TRITANIUM_PLATE.get()),
                MOItems.BATTERY.get().charged(), new ItemStack(Items.GUNPOWDER)));
        helper.assertTrue(recipe.matches(input, helper.getLevel()), Component.literal("recipe doesn't match"));
        ItemStack out = recipe.assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(out.is(MOItems.ENERGY_PACK.get()) && out.getCount() == 16, Component.literal("result " + out));
        CraftingInput empty = CraftingInput.of(3, 1, List.of(new ItemStack(MOItems.TRITANIUM_PLATE.get()),
                new ItemStack(MOItems.BATTERY.get()), new ItemStack(Items.GUNPOWDER)));
        helper.assertFalse(recipe.matches(empty, helper.getLevel()), Component.literal("empty battery matches"));
        helper.succeed();
    }

    private WeaponGameTests() {}
}
