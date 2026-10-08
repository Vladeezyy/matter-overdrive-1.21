package matteroverdrive.item;

import matteroverdrive.init.MODataComponents;
import matteroverdrive.init.MOFluids;
import matteroverdrive.matter.MatterHelper;
import matteroverdrive.util.MOText;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.ItemAccessEnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 1.7.10 PortableDecomposer: 128000 FE (charges at 256 FE/t), holds up to 512 matter. Items put next to it in an anvil
 * (3 levels, one item) join its list; listed items picked up while it is in the hotbar turn into 10% of their matter
 * value for 1 FE per point of matter value. Using it on a block with a fluid tank pours the matter in as Matter Plasma.
 */
public class PortableDecomposerItem extends Item {
    public static final int CAPACITY = 128000;
    public static final int CHARGE_RATE = 256;
    public static final int MAX_MATTER = 512;
    public static final float MATTER_RATIO = 0.1f;

    public PortableDecomposerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public EnergyHandler createEnergyHandler(ItemAccess access) {
        return new ItemAccessEnergyHandler(access, MODataComponents.ENERGY.get(), CAPACITY, CHARGE_RATE, 0);
    }

    public static int getEnergy(ItemStack stack) {
        return stack.getOrDefault(MODataComponents.ENERGY.get(), 0);
    }

    public static float getMatter(ItemStack stack) {
        return stack.getOrDefault(MODataComponents.STORED_MATTER.get(), 0f);
    }

    public static List<Item> getList(ItemStack stack) {
        return stack.getOrDefault(MODataComponents.DECOMPOSE_LIST.get(), List.of());
    }

    /** 1.7.10 addStackToList (from the anvil): only items that have matter. */
    public static boolean addToList(ItemStack decomposer, ItemStack item) {
        if (!MatterHelper.hasMatter(item) || getList(decomposer).contains(item.getItem())) return false;
        List<Item> list = new ArrayList<>(getList(decomposer));
        list.add(item.getItem());
        decomposer.set(MODataComponents.DECOMPOSE_LIST.get(), List.copyOf(list));
        return true;
    }

    /** 1.7.10 decomposeItem: takes as many of the picked-up items as matter room and energy allow. */
    public static void decompose(MinecraftServer server, ItemStack decomposer, ItemStack item) {
        int value = MatterHelper.getMatter(server, item);
        if (value <= 0 || !getList(decomposer).contains(item.getItem())) return;
        float matterFromItem = value * MATTER_RATIO;
        int energyForItem = Mth.ceil(matterFromItem / MATTER_RATIO);
        float free = MAX_MATTER - getMatter(decomposer);
        int energy = getEnergy(decomposer);
        if (free <= 0 || energy <= energyForItem) return;
        int taken = Math.min(Math.min((int) (free / matterFromItem), item.getCount()), energy / energyForItem);
        if (taken <= 0) return;
        decomposer.set(MODataComponents.ENERGY.get(), energy - taken * energyForItem);
        decomposer.set(MODataComponents.STORED_MATTER.get(), getMatter(decomposer) + taken * matterFromItem);
        item.shrink(taken);
    }

    /** 1.7.10 onItemUse: fills the clicked block's fluid tank with the stored matter. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        var tank = context.getLevel().getCapability(Capabilities.Fluid.BLOCK, context.getClickedPos(), context.getClickedFace());
        if (tank == null) return InteractionResult.PASS;
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        ItemStack decomposer = context.getItemInHand();
        int amount = (int) getMatter(decomposer);
        int filled = 0;
        if (amount > 0) {
            try (var tx = Transaction.openRoot()) {
                filled = tank.insert(FluidResource.of(MOFluids.MATTER_PLASMA.get()), amount, tx);
                tx.commit();
            }
        }
        decomposer.set(MODataComponents.STORED_MATTER.get(), (float) Math.max(0, amount - filled));
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * getEnergy(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return Mth.hsvToRgb(Math.max(0f, (float) getEnergy(stack) / CAPACITY) / 3f, 1f, 1f);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.matteroverdrive.portable_decomposer.details").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.matteroverdrive.energy_stored", MOText.energy(getEnergy(stack)), MOText.energy(CAPACITY))
                .withStyle(ChatFormatting.YELLOW));
        tooltip.accept(Component.translatable("tooltip.matteroverdrive.matter", (int) getMatter(stack) + "/" + MAX_MATTER).withStyle(ChatFormatting.AQUA));
        for (Item item : getList(stack)) {
            tooltip.accept(Component.translatable(item.getDescriptionId()).withStyle(ChatFormatting.GRAY));
        }
    }
}
