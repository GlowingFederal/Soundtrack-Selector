package com.glowingfederal.soundtrackselector;
import com.glowingfederal.soundtrackselector.domain.*;
import java.util.Map;
import java.util.LinkedHashMap;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.GuiGraphics;

public final class SelectorScreen extends Screen implements PlayerView.Canvas {
    private final Screen parent;
    private final PlayerView player = new PlayerView(ClientAdapter.library);
    private final Map<Integer, Button> controls = new LinkedHashMap<Integer, Button>();
    private EditBox search;
    private EditBox playlistName;
    private GuiGraphics graphics;
    public SelectorScreen(Screen parent) { super(Component.literal("Soundtrack Selector")); this.parent = parent; }
    @Override protected void init() {
        String newName = playlistName == null ? "" : playlistName.getValue();
        String query = search == null ? "" : search.getValue();
        player.width = width; player.height = height;
        search = new EditBox(font, player.left(), 64, width - player.left() - 16, 20, Component.literal("Search name, source or resource"));
        search.setMaxLength(128); search.setValue(query);
        addRenderableWidget(search);
        playlistName = new EditBox(font, player.left() + 94, 38, 76, 20, Component.literal("New playlist name"));
        playlistName.setMaxLength(48); playlistName.setValue(newName);
        addRenderableWidget(playlistName);
        controls.clear();
        int left = player.left();
        add(16, width - 164, 5, 60, player.label(16)); add(17, width - 100, 5, 84, player.label(17));
        add(4, left, 90, 90, player.label(4)); add(5, left + 94, 90, 76, player.label(5));
        add(1, left + 174, 90, 48, "All on"); add(2, left + 226, 90, 48, "All off");
        add(13, left, 38, 90, player.label(13)); add(14, left + 174, 38, 48, "New"); add(15, left + 226, 38, 48, "Delete");
        add(6, 16, height - 28, 56, "Play"); add(7, 76, height - 28, 52, "Pause");
        add(8, 132, height - 28, 44, "Stop"); add(9, 180, height - 28, 44, "Next");
        add(3, width - 72, height - 28, 56, "Done");
        if (width >= 460) {
            add(10, 10, 72, 108, "Music"); add(11, 10, 98, 108, "Soundtrack"); add(12, 10, 124, 108, "Long audio");
        }
    }
    private void add(int id, int x, int y, int w, String label) {
        Button button = new PlayerButton(id, x, y, w, label);
        controls.put(id, button); addRenderableWidget(button);
    }
    private void act(int id) { if (id == 3) onClose(); else { player.query = search.getValue(); player.playlistName = playlistName.getValue(); player.action(id); } }
    @Override public void render(GuiGraphics graphics, int x, int y, float partial) {
        this.graphics = graphics;
        player.query = search.getValue();
        player.draw(this, x, y);
        for (Map.Entry<Integer, Button> entry : controls.entrySet()) {
            int id = entry.getKey(); Button button = entry.getValue();
            if (id == 4 || id == 5 || id == 7 || id == 13 || id == 15 || id == 16 || id == 17) button.setMessage(Component.literal(player.label(id)));
            button.active = player.available(id);
        }
        super.render(graphics, x, y, partial);
        if (search.getValue().length() == 0 && !search.isFocused()) text("Search tracks or source", player.left()+4, 70, width-player.left()-24, 0xff8094a8);
        if (playlistName.getValue().length() == 0 && !playlistName.isFocused()) text("New name", player.left()+98, 44, 68, 0xff8094a8);
        this.graphics = null;
    }
    @Override public void rectangle(int left, int top, int right, int bottom, int color) { graphics.fill(left, top, right, bottom, color); }
    @Override public void text(String value, int x, int y, int availableWidth, int color) {
        String text = font.plainSubstrByWidth(value, Math.max(0, availableWidth));
        graphics.drawString(font, text, x, y, color);
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (super.mouseClicked(x, y, button)) return true;
        player.query = search.getValue();
        return button == 0 && player.click(x, y);
    }
    @Override public boolean mouseScrolled(double x, double y, double delta) { player.scroll(delta > 0 ? -3 : delta < 0 ? 3 : 0); return true; }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public void removed() { ClientAdapter.library.release(); ClientAdapter.library.save(); }
    @Override public boolean isPauseScreen() { return false; }

    private final class PlayerButton extends Button {
        private final int id, px, py, pw;
        PlayerButton(int id, int x, int y, int w, String label) {
            super(x, y, w, 20, Component.literal(label), button -> act(id), DEFAULT_NARRATION);
            this.id=id; px=x; py=y; pw=w;
        }
        @Override public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            boolean hover = isFocused() || (mouseX >= px && mouseX < px + pw && mouseY >= py && mouseY < py + 20);
            rectangle(px, py, px + pw, py + 20, active ? (hover ? 0xff304c60 : 0xff203447) : 0xff16232f);
            if (id == 6 && active) rectangle(px, py + 19, px + pw, py + 20, 0xff7ce6b5);
            String label = font.plainSubstrByWidth(getMessage().getString(), pw - 6);
            text(label, px + (pw - font.width(label)) / 2, py + 6, pw - 4, active ? 0xffeef4fa : 0xff586d80);
        }
    }
}
