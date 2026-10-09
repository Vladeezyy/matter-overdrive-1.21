package matteroverdrive.world;

import matteroverdrive.MatterOverdrive;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

/**
 * 1.7.10 VillageCreatationMadScientist: the mad scientist's house joins the village houses (plains, and the sandstone
 * variant in deserts - 1.7.10 villages only grew in those). Jigsaw pools have no per-village limit like 1.7.10's
 * PieceWeight(20, 0-2 + size), so the weight is chosen for about one house per village.
 */
@EventBusSubscriber(modid = MatterOverdrive.MODID)
public final class VillageHouses {
    public static final int WEIGHT = 6;

    @SubscribeEvent
    static void onServerAboutToStart(ServerAboutToStartEvent event) {
        var registries = event.getServer().registryAccess();
        Registry<StructureTemplatePool> pools = registries.registryOrThrow(Registries.TEMPLATE_POOL);
        Registry<StructureProcessorList> processors = registries.registryOrThrow(Registries.PROCESSOR_LIST);
        add(pools, processors, "village/plains/houses", "village/mad_scientist_house", "mad_scientist_house");
        add(pools, processors, "village/desert/houses", "village/mad_scientist_house_desert", "mad_scientist_house_desert");
    }

    private static void add(Registry<StructureTemplatePool> pools, Registry<StructureProcessorList> processors, String pool, String template,
                            String processorList) {
        StructureTemplatePool target = pools.get(ResourceLocation.withDefaultNamespace(pool));
        Holder<StructureProcessorList> list = processors.getHolder(ResourceKey.create(Registries.PROCESSOR_LIST,
                ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, processorList))).orElse(null);
        if (target == null || list == null) return;
        StructurePoolElement element = StructurePoolElement.legacy(MatterOverdrive.MODID + ":" + template, list)
                .apply(StructureTemplatePool.Projection.RIGID);
        if (target.templates.contains(element)) return;
        for (int i = 0; i < WEIGHT; i++) target.templates.add(element);
    }

    private VillageHouses() {}
}
