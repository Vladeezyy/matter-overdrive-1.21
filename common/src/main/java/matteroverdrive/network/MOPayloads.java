package matteroverdrive.network;

import matteroverdrive.platform.PayloadRegistrar;

/** Every payload of the mod; each loader registers them with its own network API */
public final class MOPayloads {
    public static void register(PayloadRegistrar registrar) {
        AndroidPayloads.register(registrar);
        ScannerPayloads.register(registrar);
        matteroverdrive.dialog.DialogPayloads.register(registrar);
        matteroverdrive.quest.QuestPayloads.register(registrar);
        GuideRecipePayload.register(registrar);
        matteroverdrive.starmap.StarMapPayloads.register(registrar);
        registrar
                .playToClient(MatterValuesPayload.TYPE, MatterValuesPayload.STREAM_CODEC, MatterValuesPayload::handle)
                .playToClient(PatternListPayload.TYPE, PatternListPayload.STREAM_CODEC, PatternListPayload::handle)
                .playToClient(BoltHitPayload.TYPE, BoltHitPayload.STREAM_CODEC, BoltHitPayload::handle)
                .playToServer(PatternRequestPayload.TYPE, PatternRequestPayload.STREAM_CODEC, PatternRequestPayload::handle)
                .playToServer(FireWeaponPayload.TYPE, FireWeaponPayload.STREAM_CODEC, FireWeaponPayload::handle)
                .playToServer(TransporterPayload.TYPE, TransporterPayload.STREAM_CODEC, TransporterPayload::handle)
                .playToServer(AndroidSpawnerPayload.TYPE, AndroidSpawnerPayload.STREAM_CODEC, AndroidSpawnerPayload::handle)
                .playToServer(HoloSignPayload.TYPE, HoloSignPayload.STREAM_CODEC, HoloSignPayload::handle)
                .playToServer(UnlockStatPayload.TYPE, UnlockStatPayload.STREAM_CODEC, UnlockStatPayload::handle);
    }

    private MOPayloads() {}
}
