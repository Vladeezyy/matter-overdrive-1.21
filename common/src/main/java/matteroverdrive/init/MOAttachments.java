package matteroverdrive.init;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.android.AndroidData;
import matteroverdrive.platform.PlayerData;
import net.minecraft.resources.ResourceLocation;

/** Data kept on players (NeoForge attachments, Fabric data attachments) */
public final class MOAttachments {
    /** 1.7.10 AndroidPlayer: kept through death (the 1.7.10 Clone handler copied it), synced to everyone tracking the player. */
    public static final PlayerData<AndroidData> ANDROID = new PlayerData<>(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "android"),
            AndroidData::new, AndroidData.SYNC);

    /** 1.7.10 MOExtendedProperties quest data: kept through death (1.7.10 copied it on clone), synced to its player. */
    public static final PlayerData<matteroverdrive.quest.PlayerQuests> QUESTS = new PlayerData<>(ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "quests"),
            matteroverdrive.quest.PlayerQuests::new, matteroverdrive.quest.PlayerQuests.SYNC);

    public static final java.util.List<PlayerData<?>> ALL = java.util.List.of(ANDROID, QUESTS);

    private MOAttachments() {}
}
