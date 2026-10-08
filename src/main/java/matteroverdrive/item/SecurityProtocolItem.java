package matteroverdrive.item;

import java.util.UUID;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;

import matteroverdrive.init.MODataComponents;
import matteroverdrive.machine.MachineBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * 1.7.10 SecurityProtocol (stacks of 16): sneak-use an empty one to make it yours ([Claim]); its owner (or a creative
 * player) sneak-uses it to cycle claim / access / remove. Used on a machine, [Claim] claims an unowned machine for the
 * owner and [Remove] removes the owner's claim, using up one protocol. A player carrying the owner's [Access] protocol
 * may use the owner's machines.
 */
public class SecurityProtocolItem extends Item {
    public static final int EMPTY = 0, CLAIM = 1, ACCESS = 2, REMOVE = 3;
    private static final String[] TYPES = {"empty", "claim", "access", "remove"};

    public SecurityProtocolItem(Properties properties) {
        super(properties.stacksTo(16));
    }

    public static int getType(ItemStack stack) {
        return stack.getOrDefault(MODataComponents.SECURITY_TYPE.get(), EMPTY);
    }

    public static @Nullable UUID getOwner(ItemStack stack) {
        return stack.get(MODataComponents.SECURITY_OWNER.get());
    }

    /** Whether the stack is a [type] protocol of the given owner. */
    public static boolean is(ItemStack stack, int type, @Nullable UUID owner) {
        return stack.getItem() instanceof SecurityProtocolItem && getType(stack) == type && owner != null && owner.equals(getOwner(stack));
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(getDescriptionId() + "." + TYPES[getType(stack)]);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        UUID owner = getOwner(stack);
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        if (owner == null) {
            stack.set(MODataComponents.SECURITY_OWNER.get(), player.getUUID());
            stack.set(MODataComponents.SECURITY_TYPE.get(), CLAIM);
            return InteractionResult.SUCCESS;
        }
        if (owner.equals(player.getUUID()) || player.getAbilities().instabuild) {
            int type = getType(stack) + 1;
            stack.set(MODataComponents.SECURITY_TYPE.get(), type >= TYPES.length ? CLAIM : type);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /** 1.7.10 onItemUseFirst: before the machine opens its screen. */
    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof MachineBlockEntity machine)) return InteractionResult.PASS;
        int type = getType(stack);
        if (type != CLAIM && type != REMOVE) return InteractionResult.PASS;
        if (context.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (type == CLAIM ? machine.claim(stack) : machine.unclaim(stack)) {
            stack.shrink(1);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        UUID owner = getOwner(stack);
        if (owner == null) return;
        Player player = ContractItem.clientPlayer.get();
        Player ownerPlayer = player == null ? null : player.level().getPlayerByUUID(owner);
        if (ownerPlayer != null) {
            tooltip.accept(Component.translatable("item.matteroverdrive.security_protocol.owner", ownerPlayer.getName())
                    .withStyle(ChatFormatting.YELLOW));
        }
        tooltip.accept(Component.translatable(getDescriptionId() + "." + TYPES[getType(stack)] + ".details").withStyle(ChatFormatting.GRAY));
    }
}
