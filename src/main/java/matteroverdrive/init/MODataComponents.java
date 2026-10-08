package matteroverdrive.init;

import java.util.List;

import com.mojang.serialization.Codec;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.matter.ItemPattern;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MODataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MatterOverdrive.MODID);

    /** Stored energy (FE) of batteries and of machines carried as items. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> ENERGY =
            COMPONENTS.registerComponentType("energy", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Matter carried by matter dust (1.7.10 kept it in the item damage). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> MATTER =
            COMPONENTS.registerComponentType("matter", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Item patterns on a pattern drive. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<ItemPattern>>> PATTERNS =
            COMPONENTS.registerComponentType("patterns", b -> b.persistent(ItemPattern.CODEC.listOf())
                    .networkSynchronized(ItemPattern.STREAM_CODEC.apply(ByteBufCodecs.list())));

    /** Energy weapon heat (1.7.10 "heat" tag) and whether it overheated. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> HEAT =
            COMPONENTS.registerComponentType("heat", b -> b.persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> OVERHEATED =
            COMPONENTS.registerComponentType("overheated", b -> b.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
    /** Modules installed in an energy weapon (1.7.10 kept them as an inventory in the weapon's NBT). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemContainerContents>> WEAPON_MODULES =
            COMPONENTS.registerComponentType("weapon_modules", b -> b.persistent(ItemContainerContents.CODEC)
                    .networkSynchronized(ItemContainerContents.STREAM_CODEC));

    /** Phaser power level 0-5 (1.7.10 "power" tag): 0-2 stun, 3-5 kill. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> PHASER_LEVEL =
            COMPONENTS.registerComponentType("phaser_level", b -> b.persistent(Codec.intRange(0, 5)).networkSynchronized(ByteBufCodecs.VAR_INT));

    /** Portable decomposer: matter it holds (1.7.10 float "Matter") and the items it decomposes ("Items"). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Float>> STORED_MATTER =
            COMPONENTS.registerComponentType("stored_matter", b -> b.persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<net.minecraft.world.item.Item>>> DECOMPOSE_LIST =
            COMPONENTS.registerComponentType("decompose_list", b -> b.persistent(net.minecraft.core.registries.BuiltInRegistries.ITEM.byNameCodec().listOf())
                    .networkSynchronized(ByteBufCodecs.registry(Registries.ITEM).apply(ByteBufCodecs.list())));

    /** Bionic part bonuses replacing the item's defaults (1.7.10 CustomAttributes). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<matteroverdrive.item.android.BionicPartItem.Stats>> BIONIC_STATS =
            COMPONENTS.registerComponentType("bionic_stats", b -> b.persistent(matteroverdrive.item.android.BionicPartItem.Stats.CODEC)
                    .networkSynchronized(matteroverdrive.item.android.BionicPartItem.Stats.STREAM_CODEC));
    /** A contract's quest (1.7.10 Contract NBT = the QuestStack). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<matteroverdrive.quest.QuestStack>> CONTRACT_QUEST =
            COMPONENTS.registerComponentType("contract_quest", b -> b.persistent(matteroverdrive.quest.QuestStack.CODEC)
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.fromCodecWithRegistries(matteroverdrive.quest.QuestStack.CODEC)));

    /** Data Pad: screen state (page, selected quest, scroll) and the mad scientist's scan settings. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<matteroverdrive.item.DataPadItem.State>> DATA_PAD =
            COMPONENTS.registerComponentType("data_pad", b -> b.persistent(matteroverdrive.item.DataPadItem.State.CODEC)
                    .networkSynchronized(matteroverdrive.item.DataPadItem.State.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<matteroverdrive.item.DataPadItem.Scan>> DATA_PAD_SCAN =
            COMPONENTS.registerComponentType("data_pad_scan", b -> b.persistent(matteroverdrive.item.DataPadItem.Scan.CODEC)
                    .networkSynchronized(matteroverdrive.item.DataPadItem.Scan.STREAM_CODEC));

    /** Matter scanner: the pattern storage it is linked to and the pattern it last selected (1.7.10 link_x/y/z, lastSelected). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.core.GlobalPos>> SCANNER_LINK =
            COMPONENTS.registerComponentType("scanner_link", b -> b.persistent(net.minecraft.core.GlobalPos.CODEC)
                    .networkSynchronized(net.minecraft.core.GlobalPos.STREAM_CODEC));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ItemPattern>> SCANNER_SELECTED =
            COMPONENTS.registerComponentType("scanner_selected", b -> b.persistent(ItemPattern.CODEC).networkSynchronized(ItemPattern.STREAM_CODEC));

    /** Transporter destinations carried by the block item (1.7.10 transportLocations / selectedTransport). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<matteroverdrive.transport.TransportLocation>>> TRANSPORT_LOCATIONS =
            COMPONENTS.registerComponentType("transport_locations", b -> b.persistent(matteroverdrive.transport.TransportLocation.CODEC.listOf())
                    .networkSynchronized(matteroverdrive.transport.TransportLocation.STREAM_CODEC.apply(ByteBufCodecs.list())));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> TRANSPORT_SELECTED =
            COMPONENTS.registerComponentType("transport_selected", b -> b.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    /** Transport flash drive: the block it marked (1.7.10 TargetX/Y/Z). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.core.BlockPos>> TRANSPORT_TARGET =
            COMPONENTS.registerComponentType("transport_target", b -> b.persistent(net.minecraft.core.BlockPos.CODEC)
                    .networkSynchronized(net.minecraft.core.BlockPos.STREAM_CODEC));

    /** Security protocol (1.7.10 damage 0-3 + "Owner"): its type, and the owner it carries - also a claimed machine's owner on its item. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> SECURITY_TYPE =
            COMPONENTS.registerComponentType("security_type", b -> b.persistent(Codec.intRange(0, 3)).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<java.util.UUID>> SECURITY_OWNER =
            COMPONENTS.registerComponentType("security_owner", b -> b.persistent(net.minecraft.core.UUIDUtil.CODEC)
                    .networkSynchronized(net.minecraft.core.UUIDUtil.STREAM_CODEC));

    /** Star map buildings / ships: when their construction started (1.7.10 "BuildStart"); the owner is security_owner. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> BUILD_START =
            COMPONENTS.registerComponentType("build_start", b -> b.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));

    /** 1.7.10 AndroidPartsFactory.addLegendaryAttributesToPart: a legendary part's "CustomAttributes". */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<matteroverdrive.item.android.BionicPartItem.Legendary>> LEGENDARY_PART =
            COMPONENTS.registerComponentType("legendary_part", b -> b.persistent(matteroverdrive.item.android.BionicPartItem.Legendary.CODEC)
                    .networkSynchronized(ByteBufCodecs.fromCodec(matteroverdrive.item.android.BionicPartItem.Legendary.CODEC)));
    /** 1.7.10 WeaponFactory.modifyToLegendary: the CUSTOM_*_MULTIPLY stats. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<matteroverdrive.item.weapon.WeaponFactory.Legendary>> LEGENDARY_WEAPON =
            COMPONENTS.registerComponentType("legendary_weapon", b -> b.persistent(matteroverdrive.item.weapon.WeaponFactory.Legendary.CODEC)
                    .networkSynchronized(ByteBufCodecs.fromCodec(matteroverdrive.item.weapon.WeaponFactory.Legendary.CODEC)));

    /** Network flash drive: the matter network blocks it lists (1.7.10 "CONNECTIONS"). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<List<net.minecraft.core.BlockPos>>> NETWORK_FILTER =
            COMPONENTS.registerComponentType("network_filter", b -> b.persistent(net.minecraft.core.BlockPos.CODEC.listOf())
                    .networkSynchronized(net.minecraft.core.BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list())));

    private MODataComponents() {}
}
