package matteroverdrive.block.entity;

import java.util.Optional;
import java.util.Set;

import matteroverdrive.init.MOBlockEntities;
import matteroverdrive.init.MORecipes;
import matteroverdrive.machine.MachineBlockEntity;
import matteroverdrive.machine.MachineInventory;
import matteroverdrive.machine.UpgradeType;
import matteroverdrive.menu.InscriberMenu;
import matteroverdrive.recipe.InscriberRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import matteroverdrive.compat.ValueInput;
import matteroverdrive.compat.ValueOutput;

/**
 * 1.7.10 TileEntityInscriber: combines a main and a secondary item using an inscriber recipe.
 * Each tick drains energy / time FE; SPEED and POWER_USAGE upgrades scale time and energy.
 */
public class InscriberBlockEntity extends MachineBlockEntity {
    public static final int MAIN = 0;
    public static final int SECONDARY = 1;
    public static final int OUTPUT = 2;

    private int inscribeTime;
    private Optional<RecipeHolder<InscriberRecipe>> recipe = Optional.empty();
    private boolean recipeDirty = true;

    public InscriberBlockEntity(BlockPos pos, BlockState state) {
        super(MOBlockEntities.INSCRIBER.get(), pos, state, slots(), true, 4, 512000, 256, 256,
                Set.of(UpgradeType.POWER_USAGE, UpgradeType.SPEED, UpgradeType.POWER_STORAGE, UpgradeType.POWER_TRANSFER));
    }

    private static MachineInventory.Builder slots() {
        MachineInventory.Builder b = MachineInventory.builder();
        b.add(MachineInventory.Role.INPUT, r -> true);
        b.add(MachineInventory.Role.INPUT, r -> true);
        b.add(MachineInventory.Role.OUTPUT, r -> true);
        return b;
    }

    @Override
    protected void onInventoryChanged() {
        super.onInventoryChanged();
        recipeDirty = true;
    }

    @Override
    protected boolean tickMachine(boolean redstoneAllows) {
        if (recipeDirty) {
            recipe = findRecipe();
            recipeDirty = false;
        }
        if (recipe.isEmpty() || !canOutput(recipe.get().value())) {
            inscribeTime = 0;
            return false;
        }
        int drain = getEnergyDrainPerTick();
        if (!redstoneAllows || energy.getEnergy() < drain) {
            return false;
        }
        energy.add(-drain);
        inscribeTime++;
        if (inscribeTime >= getSpeed()) {
            inscribeTime = 0;
            inscribe(recipe.get().value());
        }
        setChanged();
        return true;
    }

    private Optional<RecipeHolder<InscriberRecipe>> findRecipe() {
        ItemStack main = inventory.getStack(MAIN);
        ItemStack secondary = inventory.getStack(SECONDARY);
        if (main.isEmpty() || secondary.isEmpty() || !(getLevel() instanceof ServerLevel level)) {
            return Optional.empty();
        }
        return level.recipeAccess().getRecipeFor(MORecipes.INSCRIBER_TYPE.get(), new InscriberRecipe.Input(main, secondary), level);
    }

    /** 1.7.10 required an empty output slot; here the result may also stack onto a matching output. */
    private boolean canOutput(InscriberRecipe recipe) {
        ItemStack out = inventory.getStack(OUTPUT);
        ItemStack result = recipe.result();
        return out.isEmpty() || (ItemStack.isSameItemSameComponents(out, result) && out.getCount() + result.getCount() <= out.getMaxStackSize());
    }

    private void inscribe(InscriberRecipe recipe) {
        ItemStack result = recipe.assemble(new InscriberRecipe.Input(inventory.getStack(MAIN), inventory.getStack(SECONDARY)), getLevel().registryAccess());
        ItemStack out = inventory.getStack(OUTPUT);
        if (out.isEmpty()) {
            inventory.setStack(OUTPUT, result);
        } else {
            inventory.setStack(OUTPUT, out.copyWithCount(out.getCount() + result.getCount()));
        }
        inventory.shrink(MAIN, 1);
        inventory.shrink(SECONDARY, 1);
    }

    /** Total ticks for the current recipe after SPEED upgrades. */
    public int getSpeed() {
        return recipe.map(r -> (int) (r.value().time() * getUpgradeMultiplier(UpgradeType.SPEED))).orElse(0);
    }

    public int getEnergyDrainPerTick() {
        int speed = getSpeed();
        if (speed <= 0) return 0;
        return recipe.map(r -> (int) (r.value().energy() * getUpgradeMultiplier(UpgradeType.POWER_USAGE)) / speed).orElse(0);
    }

    @Override
    public float getProgress() {
        int speed = getSpeed();
        return speed > 0 ? (float) inscribeTime / speed : 0;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("inscribe_time", inscribeTime);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        inscribeTime = input.getIntOr("inscribe_time", 0);
        recipeDirty = true;
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new InscriberMenu(id, inventory, this, dataAccess);
    }

    @Override
    public net.minecraft.sounds.SoundEvent getLoopSound() {
        return matteroverdrive.init.MOSounds.MACHINE.get();
    }
}
