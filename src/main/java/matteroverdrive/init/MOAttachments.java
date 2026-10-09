package matteroverdrive.init;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.android.AndroidData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public final class MOAttachments {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MatterOverdrive.MODID);

    /** 1.7.10 AndroidPlayer: kept through death (the 1.7.10 Clone handler copied it), synced to everyone tracking the player. */
    public static final Supplier<AttachmentType<AndroidData>> ANDROID = ATTACHMENTS.register("android",
            () -> AttachmentType.serializable(AndroidData::new).copyOnDeath().build());

    /** 1.7.10 MOExtendedProperties quest data: kept through death (1.7.10 copied it on clone), synced to its player. */
    public static final Supplier<AttachmentType<matteroverdrive.quest.PlayerQuests>> QUESTS = ATTACHMENTS.register("quests",
            () -> AttachmentType.serializable(matteroverdrive.quest.PlayerQuests::new).copyOnDeath().build());

    static {
        // NeoForge 21.5 has no attachment sync: compat.AttachmentSync sends them
        matteroverdrive.compat.AttachmentSync.register("android", ANDROID, AndroidData.SYNC);
        matteroverdrive.compat.AttachmentSync.register("quests", QUESTS, matteroverdrive.quest.PlayerQuests.SYNC);
    }

    private MOAttachments() {}
}
