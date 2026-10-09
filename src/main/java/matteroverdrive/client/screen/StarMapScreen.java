package matteroverdrive.client.screen;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import matteroverdrive.MatterOverdrive;
import matteroverdrive.block.entity.StarMapBlockEntity;
import matteroverdrive.client.starmap.HoloSink;
import matteroverdrive.client.starmap.StarMapPipRenderer;
import matteroverdrive.client.starmap.StarMapRenderer;
import matteroverdrive.init.MOSounds;
import matteroverdrive.menu.StarMapMenu;
import matteroverdrive.starmap.GalacticPosition;
import matteroverdrive.starmap.Galaxy;
import matteroverdrive.starmap.GalaxyClient;
import matteroverdrive.starmap.Planet;
import matteroverdrive.starmap.Quadrant;
import matteroverdrive.starmap.ShipItem;
import matteroverdrive.starmap.SpaceBody;
import matteroverdrive.starmap.StarMapPayloads;
import matteroverdrive.starmap.Star;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import matteroverdrive.compat.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 1.7.10 GuiStarMap: a full-screen holo screen over the star map's hologram. Pages (tabs on the right): galaxy (the
 * quadrants), quadrant (its stars), star (its planets), planet (the 2 building + 2 ship construction slots of an own
 * planet) and planet stats (the ships of the current planet that can be sent to the selected one). A list entry selects
 * the body (the map's destination); a selected entry has "Enter" (that body's page) and, for planets, "Travel To"
 * (makes it the map's position). The selected body's info is drawn at the bottom, the inventory bottom left.
 */
public class StarMapScreen extends AbstractContainerScreen<StarMapMenu> {
    private static final int ENTRY_W = 192, ENTRY_H = 32, PADDING = 6;
    private static final int COLOR_MATTER = 0xBFE4E6;
    private static final String[] PAGE_ICONS = {"page_icon_galaxy", "page_icon_quadrant", "page_icon_star", "page_icon_planet", "icon_stats"};
    private static final String[] PAGE_NAMES = {"galaxy", "quadrant", "star", "planet", "planet_stats"};
    private static final net.minecraft.resources.ResourceLocation ALT = net.minecraft.client.Minecraft.ALT_FONT;
    private static final int[] SCROLL = new int[5];
    private final StarMapBlockEntity starMap;
    private int page;
    private float smoothScroll;
    private int selectedShip = -1;

    public StarMapScreen(StarMapMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.starMap = menu.getStarMap();
        this.page = starMap.getZoomLevel();
    }

    private static ResourceLocation sprite(String name) {
        return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, name);
    }

    private static ResourceLocation holo(String name) {
        return ResourceLocation.fromNamespaceAndPath(MatterOverdrive.MODID, "textures/gui/holo/" + name + ".png");
    }

    @Override
    protected void init() {
        imageWidth = width;
        imageHeight = height;
        super.init();
        leftPos = 0;
        topPos = 0;
        smoothScroll = SCROLL[page];
        layoutSlots();
    }

    /** 1.7.10 AddMainPlayerSlots(45, ySize - 104) / AddHotbarPlayerSlots(45, ySize - 25); the planet slots on an arc. */
    private void layoutSlots() {
        List<Slot> slots = menu.slots;
        for (int i = 0; i < 27; i++) {
            Slot slot = slots.get(StarMapMenu.SLOTS + i);
            slot.x = 45 + (i % 9) * 18 + 1;
            slot.y = height - 104 + (i / 9) * 18 + 1;
        }
        for (int i = 0; i < 9; i++) {
            Slot slot = slots.get(StarMapMenu.SLOTS + 27 + i);
            slot.x = 45 + i * 18 + 1;
            slot.y = height - 25 + 1;
        }
        boolean visible = page == 3 && planetSlotsVisible();
        for (int i = 0; i < StarMapMenu.SLOTS; i++) {
            double angle = -Math.PI / 1.8 + Math.PI / 15 * i;
            Slot slot = slots.get(i);
            slot.x = visible ? width / 2 - 10 + (int) (Math.sin(angle) * 140) + 3 : -10000;
            slot.y = visible ? height / 2 - 48 + (int) (Math.cos(angle) * 140) + 3 : -10000;
        }
    }

    private boolean planetSlotsVisible() {
        Planet planet = starMap.getPlanet();
        return planet != null && minecraft.player != null && planet.isOwner(minecraft.player);
    }

    private @Nullable Galaxy galaxy() {
        return GalaxyClient.getGalaxy();
    }

    // --- pages (1.7.10 updateElementInformation: the tabs that make sense for the destination) ---------------

    private boolean pageVisible(int index) {
        Galaxy galaxy = galaxy();
        if (galaxy == null) return index == 0;
        return switch (index) {
            case 1 -> galaxy.getQuadrant(starMap.getDestination()) != null;
            case 2 -> galaxy.getStar(starMap.getDestination()) != null;
            case 3, 4 -> galaxy.getPlanet(starMap.getDestination()) != null;
            default -> true;
        };
    }

    public void setPage(int newPage) {
        SCROLL[page] = (int) smoothScroll;
        page = newPage;
        smoothScroll = SCROLL[page];
        selectedShip = -1;
        if (newPage != starMap.getZoomLevel()) {
            starMap.setZoomLevel(newPage);
            send(newPage, starMap.getGalaxyPosition(), starMap.getDestination());
        }
        layoutSlots();
    }

    private void send(int zoom, GalacticPosition position, GalacticPosition destination) {
        starMap.setGalacticPosition(position);
        starMap.setDestination(destination);
        PacketDistributor.sendToServer(new StarMapPayloads.Command(starMap.getBlockPos(), zoom, position, destination));
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (starMap.getZoomLevel() != page) {
            page = starMap.getZoomLevel();
            smoothScroll = SCROLL[page];
        }
        layoutSlots();
    }

    // --- list entries (1.7.10 ElementAbstractStarMapEntry + Quadrant / Star / Planet / Ship entries) -------------

    private record Entry(SpaceBody body, @Nullable ItemStack ship, int shipId) {}

    private List<Entry> entries() {
        List<Entry> list = new ArrayList<>();
        Galaxy galaxy = galaxy();
        if (galaxy == null) return list;
        switch (page) {
            case 0 -> galaxy.getQuadrants().forEach(q -> list.add(new Entry(q, null, -1)));
            case 1 -> {
                Quadrant quadrant = galaxy.getQuadrant(starMap.getDestination());
                if (quadrant != null) quadrant.getStars().forEach(s -> list.add(new Entry(s, null, -1)));
            }
            case 2 -> {
                Star star = galaxy.getStar(starMap.getDestination());
                if (star != null) {
                    GalaxyClient.requestPlanets(star);
                    star.getPlanets().forEach(p -> list.add(new Entry(p, null, -1)));
                }
            }
            case 4 -> {
                // 1.7.10 PagePlanetStats.loadShips: the fleet of the current planet, when another one is selected
                Planet from = galaxy.getPlanet(starMap.getGalaxyPosition());
                if (from != null && !starMap.getGalaxyPosition().equals(starMap.getDestination())) {
                    for (int i = 0; i < from.getFleet().size(); i++) list.add(new Entry(from, from.getFleet().get(i), i));
                }
            }
            default -> {}
        }
        return list;
    }

    private boolean isSelected(Entry entry) {
        if (entry.ship() != null) return entry.shipId() == selectedShip;
        SpaceBody body = entry.body();
        GalacticPosition destination = starMap.getDestination();
        if (body instanceof Quadrant q) return destination.is(q);
        if (body instanceof Star s) return destination.is(s);
        return body instanceof Planet p && destination.is(p);
    }

    private boolean isCurrent(SpaceBody body) {
        GalacticPosition position = starMap.getGalaxyPosition();
        if (body instanceof Quadrant q) return position.is(q);
        if (body instanceof Star s) return position.is(s);
        return body instanceof Planet p && position.is(p);
    }

    private float multiply(Entry entry) {
        if (isSelected(entry)) return 1;
        return entry.ship() == null && isCurrent(entry.body()) ? 0.5f : 0.1f;
    }

    private int color(Entry entry) {
        Player player = minecraft.player;
        if (entry.ship() != null) return canView(entry) ? Galaxy.COLOR_HOLO : Galaxy.COLOR_HOLO_RED;
        if (entry.body() instanceof Star star) return star.getColor() & 0xFFFFFF;
        if (entry.body() instanceof Planet planet) return planet.getGuiColor(player);
        return Galaxy.COLOR_HOLO;
    }

    private boolean canTravelTo(Entry entry) {
        return entry.ship() == null && entry.body() instanceof Planet planet && !starMap.getGalaxyPosition().is(planet);
    }

    /** "Enter" (planets: not someone else's); for ships "Attack": the player's ship, and the selected planet takes it. */
    private boolean canView(Entry entry) {
        Player player = minecraft.player;
        if (entry.ship() != null) {
            Galaxy galaxy = galaxy();
            Planet to = galaxy == null ? null : galaxy.getPlanet(starMap.getDestination());
            return player != null && entry.ship().getItem() instanceof ShipItem ship && ship.isOwner(entry.ship(), player)
                    && to != null && to != entry.body() && to.canAddShip(entry.ship(), player);
        }
        if (entry.body() instanceof Planet planet) return !planet.hasOwner() || (player != null && planet.isOwner(player));
        return true;
    }

    private void select(Entry entry) {
        SpaceBody body = entry.body();
        GalacticPosition pos = body instanceof Quadrant q ? GalacticPosition.of(q) : body instanceof Star s ? GalacticPosition.of(s)
                : GalacticPosition.of((Planet) body);
        send(starMap.getZoomLevel(), starMap.getGalaxyPosition(), pos);
    }

    private void view(Entry entry) {
        if (entry.ship() != null) {
            PacketDistributor.sendToServer(new StarMapPayloads.Attack(starMap.getGalaxyPosition(), starMap.getDestination(), entry.shipId()));
            return;
        }
        setPage(entry.body() instanceof Quadrant ? 1 : entry.body() instanceof Star ? 2 : 3);
    }

    private void travel(Entry entry) {
        send(starMap.getZoomLevel(), GalacticPosition.of((Planet) entry.body()), starMap.getDestination());
    }

    /** 1.7.10 getIcons: home (own system / planet), factories and own ships, with counts. */
    private Map<String, Integer> icons(Entry entry) {
        Map<String, Integer> icons = new LinkedHashMap<>();
        Player player = minecraft.player;
        if (player == null || entry.ship() != null) return icons;
        if (entry.body() instanceof Quadrant quadrant) {
            for (Star star : quadrant.getStars()) if (star.isClaimed(player) >= 2) icons.put("home_icon", -1);
        } else if (entry.body() instanceof Star star) {
            icons.put("icon_shuttle", 0);
            icons.put("factory", 0);
            for (Planet planet : star.getPlanets()) {
                if (planet.isOwner(player)) {
                    if (planet.isHomeworld()) icons.put("home_icon", -1);
                    if (!planet.getBuildings().isEmpty()) icons.merge("factory", 1, Integer::sum);
                }
                for (ItemStack ship : planet.getFleet()) if (ship.getItem() instanceof ShipItem) icons.merge("icon_shuttle", 1, Integer::sum);
            }
        } else if (entry.body() instanceof Planet planet) {
            icons.put("icon_shuttle", 0);
            if (planet.isOwner(player)) {
                if (planet.isHomeworld()) icons.put("home_icon", -1);
                if (!planet.getBuildings().isEmpty()) icons.put("factory", planet.getBuildings().size());
            }
            for (ItemStack ship : planet.getFleet()) {
                if (ship.getItem() instanceof ShipItem) {
                    icons.merge("icon_shuttle", 1, Integer::sum);
                    break;
                }
            }
        }
        return icons;
    }

    private static int iconSize(String name) {
        return switch (name) {
            case "home_icon" -> 14;
            case "travel_icon" -> 18;
            case "page_icon_galaxy" -> 11;
            case "page_icon_quadrant" -> 9;
            case "page_icon_star" -> 12;
            default -> 16;
        };
    }

    private int listHeight() {
        return page == 4 ? 256 : height - 100 - 32;
    }

    private int listContentHeight(List<Entry> list) {
        return list.size() * (ENTRY_H + PADDING);
    }

    // --- rendering -------------------------------------------------------------------------------------

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // 1.7.10 drawWorldBackground: black, then the hologram in perspective
        g.fill(0, 0, width, height, 0xFF000000);
        g.flush();
        StarMapPipRenderer.render(starMap, partialTick, width, height);
        renderBg(g, partialTick, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        matteroverdrive.compat.Gui.blitSprite(g, sprite("star_map"), 0, 0, width, height);
        // the bottom info panel of the selected body (1.7.10 renderGUIInfo at xSize / 1.9, ySize - 16, opacity 0.8)
        Galaxy galaxy = galaxy();
        if (galaxy != null) {
            g.pose().pushPose();
            g.pose().translate((float) (width / 1.9), height - 16, 0);
            var ctx = new StarMapRenderer.Ctx(StarMapRenderer.stateOf(starMap, partialTick, minecraft.player), new com.mojang.blaze3d.vertex.PoseStack(),
                    HoloSink.of(g, font), font, 0, 0, new org.joml.Quaternionf(), minecraft.player, galaxy);
            StarMapRenderer.renderGuiInfo(ctx, 0.8f);
            g.pose().popPose();
        }
        renderPageTabs(g, mouseX, mouseY);
        renderList(g, mouseX, mouseY);
        // holo slots: inventory at 1/3 and 1/2 of the holo colour, planet slots at 1/2 with their icons
        for (int i = 0; i < menu.slots.size(); i++) {
            Slot slot = menu.slots.get(i);
            if (slot.x < -1000) continue;
            boolean planetSlot = i < StarMapMenu.SLOTS, hotbar = i >= StarMapMenu.SLOTS + 27;
            int color = planetSlot || hotbar ? Galaxy.COLOR_HOLO >> 1 & 0x7F7F7F : div(Galaxy.COLOR_HOLO, 3);
            int size = planetSlot ? 22 : 18, off = planetSlot ? 3 : 1;
            matteroverdrive.compat.Gui.blitSprite(g, sprite("slot_holo_with_bg"), slot.x - off, slot.y - off, size, size, 0xFF000000 | color);
            if (planetSlot && !slot.hasItem()) {
                String icon = i < StarMapMenu.SLOTS / 2 ? "factory" : "icon_shuttle";
                matteroverdrive.compat.Gui.blit(g, holo(icon), slot.x, slot.y, 0, 0, 16, 16, 16, 16, 0xFF000000 | color);
            }
        }
        if (page == 3) {
            renderPlanetPage(g);
            renderSlotInfo(g);
        }
    }

    /** 1.7.10 ElementSlotBuilding / ElementSlotShip.drawForeground: time left, or why it can't be built, left of the slot. */
    private void renderSlotInfo(GuiGraphics g) {
        Planet planet = starMap.getPlanet();
        if (planet == null || minecraft.level == null) return;
        for (int i = 0; i < StarMapMenu.SLOTS; i++) {
            Slot slot = menu.slots.get(i);
            ItemStack stack = slot.getItem();
            if (slot.x < -1000 || !(stack.getItem() instanceof matteroverdrive.starmap.Buildable buildable)) continue;
            List<Component> info = new ArrayList<>();
            if (planet.canBuild(buildable, stack, info)) {
                long remaining = buildable.getRemainingBuildTimeTicks(stack, planet, minecraft.level) / 20;
                if (remaining >= 0) {
                    String time = AndroidSpawnerScreen.formatRemainingTime(remaining);
                    g.drawString(font, time, slot.x - 3 - font.width(time) - 4, slot.y - 3 + 6, 0xFF000000 | Galaxy.COLOR_HOLO, false);
                }
            } else {
                String text = String.join(". ", info.stream().map(Component::getString).toList());
                g.drawString(font, text, slot.x - 3 - font.width(text) - 4, slot.y - 3 + 7, 0xFF000000 | Galaxy.COLOR_HOLO_RED, false);
            }
        }
    }

    private static int div(int rgb, int d) {
        return (rgb >> 16 & 255) / d << 16 | (rgb >> 8 & 255) / d << 8 | (rgb & 255) / d;
    }

    private void renderPageTabs(GuiGraphics g, int mouseX, int mouseY) {
        int x = width - 42;
        matteroverdrive.compat.Gui.blitSprite(g, sprite("right_side_bar_panel_bg_holo"), x, 16, 42, 5 * 26 + 8);
        for (int i = 0; i < 5; i++) {
            if (!pageVisible(i)) continue;
            int bx = x + 9, by = 20 + i * 26;
            boolean over = mouseX >= bx && mouseX < bx + 24 && mouseY >= by && mouseY < by + 24;
            float m = i == page ? 1 : over ? 0.8f : 0.4f;
            int color = 0xFF000000 | mul(COLOR_MATTER, m);
            int s = iconSize(PAGE_ICONS[i]);
            matteroverdrive.compat.Gui.blit(g, holo(PAGE_ICONS[i]), bx + 12 - s / 2, by + 12 - s / 2, 0, 0, s, s, s, s, color);
            if (over) matteroverdrive.compat.Gui.setTooltipForNextFrame(g, font, Component.translatable("gui.matteroverdrive.page." + PAGE_NAMES[i]), mouseX, mouseY);
        }
    }

    private static int mul(int rgb, float m) {
        return Mth.clamp((int) ((rgb >> 16 & 255) * m), 0, 255) << 16 | Mth.clamp((int) ((rgb >> 8 & 255) * m), 0, 255) << 8
                | Mth.clamp((int) ((rgb & 255) * m), 0, 255);
    }

    private void renderList(GuiGraphics g, int mouseX, int mouseY) {
        List<Entry> list = entries();
        if (list.isEmpty()) return;
        int x0 = 16, y0 = 16, h = listHeight();
        SCROLL[page] = Mth.clamp(SCROLL[page], Math.min(0, -(listContentHeight(list) - h + 32)), 0);
        smoothScroll = Mth.lerp(0.1f, smoothScroll, SCROLL[page]);
        g.enableScissor(x0, y0, width, y0 + h);
        for (int i = 0; i < list.size(); i++) {
            int y = y0 + (int) smoothScroll + i * (ENTRY_H + PADDING);
            if (y + ENTRY_H < y0 || y > y0 + h) continue;
            renderEntry(g, list.get(i), x0, y, mouseX, mouseY);
        }
        g.disableScissor();
    }

    private void renderEntry(GuiGraphics g, Entry entry, int x, int y, int mouseX, int mouseY) {
        int color = color(entry);
        float m = multiply(entry);
        boolean selected = isSelected(entry);
        String bg = entry.body() instanceof Planet p && entry.ship() == null && starMap.getGalaxyPosition().is(p) ? "holo_list_entry_middle_down"
                : "holo_list_entry";
        int bgColor = 0xFF000000 | mul(color, m);
        matteroverdrive.compat.Gui.blitSprite(g, sprite(bg), x, y, ENTRY_W - 64, ENTRY_H, bgColor);
        int iconsX = 0;
        if (selected) {
            if (canView(entry) || entry.ship() != null) {
                matteroverdrive.compat.Gui.blitSprite(g, sprite("holo_list_entry_middle_normal"), x + ENTRY_W - 64, y, 32, ENTRY_H, bgColor);
            }
            if (canTravelTo(entry)) {
                matteroverdrive.compat.Gui.blitSprite(g, sprite("holo_list_entry_flipped"), x + ENTRY_W - 32, y, 32, ENTRY_H, bgColor);
            }
            drawName(g, entry, x, y, color, 1);
            int rel = mouseX - x;
            boolean overEntry = mouseY >= y && mouseY < y + ENTRY_H;
            if (canTravelTo(entry)) {
                float im = overEntry && rel > ENTRY_W - 32 && rel < ENTRY_W ? 1 : 0.5f;
                matteroverdrive.compat.Gui.blit(g, holo("travel_icon"), x + ENTRY_W - 32 + 6, y + 5, 0, 0, 18, 18, 18, 18, 0xFF000000 | mul(color, im));
                if (im == 1) matteroverdrive.compat.Gui.setTooltipForNextFrame(g, font, Component.literal("Travel To"), mouseX, mouseY);
                iconsX += 32;
            }
            if (canView(entry) || entry.ship() != null) {
                float im = overEntry && rel > ENTRY_W - 64 && rel < ENTRY_W - 32 ? 1 : 0.5f;
                String icon = entry.ship() != null ? "icon_attack" : "icon_search";
                matteroverdrive.compat.Gui.blit(g, holo(icon), x + ENTRY_W - 64 + 8, y + 8, 0, 0, 16, 16, 16, 16, 0xFF000000 | mul(color, im));
                if (im == 1 && entry.ship() == null) matteroverdrive.compat.Gui.setTooltipForNextFrame(g, font, Component.literal("Enter"), mouseX, mouseY);
                iconsX += 32;
            }
            drawIcons(g, entry, x + 128 + iconsX, y, color, 0.8f, Galaxy.COLOR_HOLO, 1);
        } else {
            drawName(g, entry, x, y, color, 0.3f);
            drawIcons(g, entry, x + 128, y, color, 0.3f, color, 0.6f);
        }
    }

    private void drawIcons(GuiGraphics g, Entry entry, int x, int y, int color, float m, int countColor, float countM) {
        for (var icon : icons(entry).entrySet()) {
            if (icon.getValue() == 0) continue;
            matteroverdrive.compat.Gui.blitSprite(g, sprite("holo_list_entry_circle"), x, y, 32, 32, 0xFF000000 | mul(color, m));
            int s = iconSize(icon.getKey());
            matteroverdrive.compat.Gui.blit(g, holo(icon.getKey()), x + 16 - s / 2, y + 16 - s / 2, 0, 0, s, s, s, s, 0xFF000000 | mul(color, m));
            if (icon.getValue() > 0) g.drawString(font, String.valueOf(icon.getValue()), x + 16 + 3, y + 16 + 3, 0xFF000000 | mul(countColor, countM), false);
            x += 32;
        }
    }

    private void drawName(GuiGraphics g, Entry entry, int x, int y, int color, float m) {
        Player player = minecraft.player;
        if (entry.ship() != null) {
            g.renderItem(entry.ship(), x + 10, y + ENTRY_H / 2 - 8);
            g.drawString(font, entry.ship().getHoverName(), x + 31, y + 12, 0xFF000000 | mul(color, m), false);
            return;
        }
        SpaceBody body = entry.body();
        String name = body.getName();
        boolean known = true;
        if (body instanceof Star star) known = player != null && (player.getAbilities().instabuild || GalaxyClient.canSeeStarInfo(star, player));
        if (body instanceof Planet planet) known = player != null && (GalaxyClient.canSeePlanetInfo(planet, player) || player.getAbilities().instabuild);
        Component text = Component.literal(name);
        if (!(body instanceof Quadrant) && isCurrent(body)) text = Component.literal("@ ").append(Component.literal(name).withStyle(s -> s.withItalic(true)));
        if (!known) text = text.copy().withStyle(s -> s.withFont(ALT));
        g.drawString(font, text, x + 16, y + 10, 0xFF000000 | mul(color, m), false);
    }

    /** 1.7.10 PagePlanetMenu.drawForeground: the planet's name (at x = height / 2, a 1.7.10 quirk). */
    private void renderPlanetPage(GuiGraphics g) {
        Planet planet = starMap.getPlanet();
        if (planet == null || minecraft.player == null) return;
        int w = font.width(planet.getName());
        boolean known = GalaxyClient.canSeePlanetInfo(planet, minecraft.player) || minecraft.player.getAbilities().instabuild;
        Component name = Component.literal(planet.getName());
        if (!known) name = name.copy().withStyle(s -> s.withFont(ALT));
        g.drawString(font, name, height / 2 + w / 2 + 12 - w / 2, 16 + 4, 0xFF000000 | planet.getGuiColor(minecraft.player), false);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {}

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    // --- input -------------------------------------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        MouseButtonEvent event = new MouseButtonEvent(mouseX, mouseY, button);
        boolean doubleClick = false;
        int x = width - 42 + 9;
        for (int i = 0; i < 5; i++) {
            int by = 20 + i * 26;
            if (pageVisible(i) && mouseX >= x && mouseX < x + 24 && mouseY >= by && mouseY < by + 24) {
                click();
                setPage(i);
                return true;
            }
        }
        List<Entry> list = entries();
        int h = listHeight();
        if (mouseX >= 16 && mouseX < 16 + ENTRY_W && mouseY >= 16 && mouseY < 16 + h) {
            for (int i = 0; i < list.size(); i++) {
                int y = 16 + (int) smoothScroll + i * (ENTRY_H + PADDING);
                if (mouseY < y || mouseY >= y + ENTRY_H) continue;
                Entry entry = list.get(i);
                double rel = mouseX - 16;
                if (isSelected(entry)) {
                    if (rel > ENTRY_W - 32 && canTravelTo(entry)) {
                        click();
                        travel(entry);
                    } else if (rel > ENTRY_W - 64 && rel <= ENTRY_W - 32 && canView(entry)) {
                        click();
                        view(entry);
                    }
                } else if (rel < ENTRY_W - 64) {
                    click();
                    if (entry.ship() != null) selectedShip = entry.shipId();
                    else select(entry);
                }
                return true;
            }
        }
        return super.mouseClicked(event.x(), event.y(), event.button());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // 1.7.10 ElementGroupList.onMouseWheel: scroll += movement * 0.2 (LWJGL wheel steps were 120)
        SCROLL[page] += (int) (scrollY * 120 * 0.2);
        return true;
    }

    private void click() {
        minecraft.getSoundManager().play(SimpleSoundInstance.forUI(MOSounds.BUTTON_SOFT.get(), 0.9f + minecraft.level.random.nextFloat() * 0.2f, 0.5f));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
