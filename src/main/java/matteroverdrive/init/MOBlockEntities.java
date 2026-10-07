package matteroverdrive.init;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.block.entity.InscriberBlockEntity;
import matteroverdrive.block.entity.SolarPanelBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class MOBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MatterOverdrive.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL =
            BLOCK_ENTITIES.register("solar_panel", () -> new BlockEntityType<>(SolarPanelBlockEntity::new, MOBlocks.SOLAR_PANEL.get()));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<InscriberBlockEntity>> INSCRIBER =
            BLOCK_ENTITIES.register("inscriber", () -> new BlockEntityType<>(InscriberBlockEntity::new, MOBlocks.INSCRIBER.get()));

    private MOBlockEntities() {}
}
