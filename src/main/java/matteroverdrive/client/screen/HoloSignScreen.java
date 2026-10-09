package matteroverdrive.client.screen;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.block.entity.HoloSignBlockEntity;
import matteroverdrive.network.HoloSignPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.CommonComponents;
import net.neoforged.neoforge.network.PacketDistributor;

/** 1.7.10 GuiHoloSign: a text box for the sign's lines. */
public class HoloSignScreen extends Screen {
    private final BlockPos pos;
    private final String initial;
    private MultiLineEditBox box;

    public HoloSignScreen(BlockPos pos, String text) {
        super(Component.translatable("block." + MatterOverdrive.MODID + ".holo_sign"));
        this.pos = pos;
        this.initial = text;
    }

    @Override
    protected void init() {
        int w = 220, h = 110;
        // 1.21.1: MultiLineEditBox has no text colour setting (the 1.21.10 build draws it in the holo colour)
        box = new MultiLineEditBox(font, width / 2 - w / 2, height / 2 - h / 2 - 10, w, h, net.minecraft.network.chat.Component.empty(), title);
        box.setCharacterLimit(HoloSignBlockEntity.MAX_LENGTH);
        box.setValue(initial);
        addRenderableWidget(box);
        setInitialFocus(box);
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
                .bounds(width / 2 - 50, height / 2 + h / 2, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, height / 2 - 85, 0xFFFFFFFF);
    }

    @Override
    public void removed() {
        PacketDistributor.sendToServer(new HoloSignPayload(pos, box.getValue()));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
