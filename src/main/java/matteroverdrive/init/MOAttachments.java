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
            () -> AttachmentType.serializable(AndroidData::new).copyOnDeath().sync(AndroidData.SYNC).build());

    private MOAttachments() {}
}
