package matteroverdrive.world;

import java.util.List;

import com.mojang.serialization.MapCodec;

import matteroverdrive.block.TritaniumCrateBlock;
import matteroverdrive.init.MODataComponents;
import matteroverdrive.init.MOItems;
import matteroverdrive.init.MOStructures;
import matteroverdrive.item.ContractItem;
import matteroverdrive.item.DataPadItem;
import matteroverdrive.quest.Quests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * 1.7.10 MadScientistHouse crate: a GMO contract and the "Mad Scientist's Data Pad" (scans carrots, potatoes and wheat,
 * destroys what it scans, no screen).
 */
public class MadScientistCrateProcessor extends StructureProcessor {
    public static final MadScientistCrateProcessor INSTANCE = new MadScientistCrateProcessor();
    public static final MapCodec<MadScientistCrateProcessor> CODEC = MapCodec.unit(() -> INSTANCE);

    public static List<ItemStack> contents(RandomSource random) {
        ItemStack contract = ContractItem.of(Quests.GMO.generate(random));
        ItemStack pad = new ItemStack(MOItems.DATA_PAD.get());
        pad.set(DataComponents.CUSTOM_NAME, Component.literal("Mad Scientist's Data Pad"));
        pad.set(MODataComponents.DATA_PAD_SCAN.get(), new DataPadItem.Scan(List.of(Blocks.CARROTS, Blocks.POTATOES, Blocks.WHEAT), true, true));
        return List.of(contract, pad);
    }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pos,
                                                            StructureTemplate.StructureBlockInfo blockInfo,
                                                            StructureTemplate.StructureBlockInfo relative, StructurePlaceSettings settings) {
        if (!(relative.state().getBlock() instanceof TritaniumCrateBlock)) return relative;
        var ops = RegistryOps.create(NbtOps.INSTANCE, level.registryAccess());
        ListTag items = new ListTag();
        List<ItemStack> contents = contents(settings.getRandom(relative.pos()));
        for (int i = 0; i < contents.size(); i++) {
            CompoundTag entry = (CompoundTag) ItemStack.CODEC.encodeStart(ops, contents.get(i)).getOrThrow();
            entry.putByte("Slot", (byte) i);
            items.add(entry);
        }
        CompoundTag nbt = relative.nbt() == null ? new CompoundTag() : relative.nbt().copy();
        nbt.put("Items", items);
        return new StructureTemplate.StructureBlockInfo(relative.pos(), relative.state(), nbt);
    }

    @Override
    protected StructureProcessorType<?> getType() {
        return MOStructures.MAD_SCIENTIST_CRATE.get();
    }
}
