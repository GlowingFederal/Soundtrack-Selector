package com.glowingfederal.soundtrackselector;
import com.glowingfederal.soundtrackselector.domain.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import org.lwjgl.input.Mouse;
import org.lwjgl.input.Keyboard;

public final class SelectorScreen extends GuiScreen implements PlayerView.Canvas {
    private final GuiScreen parent;
    private final PlayerView player = new PlayerView(ClientAdapter.library);
    private GuiTextField search;
    private GuiTextField playlistName;
    public SelectorScreen(GuiScreen parent) { this.parent = parent; }
    @Override public void initGui() {
        String newName = playlistName == null ? "" : playlistName.getText();
        String query = search == null ? "" : search.getText();
        Keyboard.enableRepeatEvents(true);
        player.width = width; player.height = height;
        search = new GuiTextField(fontRenderer, player.left(), 64, width - player.left() - 16, 20);
        search.setMaxStringLength(128); search.setText(query);
        playlistName = new GuiTextField(fontRenderer, player.left() + 94, 38, 76, 20);
        playlistName.setMaxStringLength(48); playlistName.setText(newName);
        buttonList.clear();
        int left = player.left();
        add(16, width - 164, 5, 60, player.label(16)); add(17, width - 100, 5, 84, player.label(17));
        add(4, left, 90, 90, player.label(4)); add(5, left + 94, 90, 76, player.label(5));
        add(1, left + 174, 90, 48, "All on"); add(2, left + 226, 90, 48, "All off");
        add(13, left, 38, 90, player.label(13)); add(14, left + 174, 38, 48, "New"); add(15, left + 226, 38, 48, "Delete");
        add(6, 16, height - 28, 44, "Play"); add(7, 64, height - 28, 44, "Pause");
        add(8, 112, height - 28, 36, "Stop"); add(9, 152, height - 28, 36, "Next");
        add(18, 192, height - 28, 64, player.label(18));
        add(3, width - 60, height - 28, 44, "Done");
        if (width >= 460) {
            add(10, 10, 72, 108, "Music"); add(11, 10, 98, 108, "Soundtrack"); add(12, 10, 124, 108, "Long audio");
        }
    }
    private void add(int id, int x, int y, int w, String label) { buttonList.add(new PlayerButton(id, x, y, w, label)); }
    @Override public void drawScreen(int x, int y, float partial) {
        player.query = search.getText();
        player.draw(this, x, y);
        search.drawTextBox(); playlistName.drawTextBox();
        for (Object object : buttonList) {
            GuiButton button = (GuiButton) object;
            if (button.id == 4 || button.id == 5 || button.id == 7 || button.id == 9 || button.id == 13 || button.id == 15 || button.id == 16 || button.id == 17 || button.id == 18) button.displayString = player.label(button.id);
            button.enabled = player.available(button.id);
        }
        super.drawScreen(x, y, partial);
        if (search.getText().length() == 0 && !search.isFocused()) text(player.editingNickname ? "Nickname (empty clears)" : "Search tracks or source", player.left()+4, 70, width-player.left()-24, 0xff8094a8);
        if (playlistName.getText().length() == 0 && !playlistName.isFocused()) text("New name", player.left()+98, 44, 68, 0xff8094a8);
    }
    @Override public void rectangle(int left, int top, int right, int bottom, int color) { drawRect(left, top, right, bottom, color); }
    @Override public void text(String value, int x, int y, int availableWidth, int color) {
        fontRenderer.drawString(fontRenderer.trimStringToWidth(value, Math.max(0, availableWidth)), x, y, color);
    }
    @Override protected void mouseClicked(int x, int y, int button) {
        super.mouseClicked(x, y, button); search.mouseClicked(x, y, button); playlistName.mouseClicked(x, y, button);
        player.query = search.getText();
        if (button == 0) player.click(x, y);
    }
    @Override protected void actionPerformed(GuiButton button) {
        if (button.id == 3) mc.displayGuiScreen(parent); else { player.query = search.getText(); player.playlistName = playlistName.getText(); player.action(button.id); if (button.id == 18) { search.setText(player.query); search.setFocused(true); } }
    }
    @Override public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) player.scroll(wheel > 0 ? -3 : 3);
    }
    @Override protected void keyTyped(char character, int key) {
        if (key == Keyboard.KEY_ESCAPE) { mc.displayGuiScreen(parent); return; }
        if (key == Keyboard.KEY_PRIOR) player.scroll(-player.visible());
        else if (key == Keyboard.KEY_NEXT) player.scroll(player.visible());
        else { search.textboxKeyTyped(character, key); playlistName.textboxKeyTyped(character, key); }
    }
    @Override public void updateScreen() { search.updateCursorCounter(); playlistName.updateCursorCounter(); }
    @Override public void onGuiClosed() { Keyboard.enableRepeatEvents(false); ClientAdapter.library.release(); ClientAdapter.library.save(); }
    @Override public boolean doesGuiPauseGame() { return false; }

    private final class PlayerButton extends GuiButton {
        private final int px, py, pw;
        PlayerButton(int id, int x, int y, int w, String label) { super(id, x, y, w, 20, label); px=x; py=y; pw=w; }
        @Override public void drawButton(Minecraft client, int mouseX, int mouseY) {
            boolean hover = mouseX >= px && mouseX < px + pw && mouseY >= py && mouseY < py + 20;
            rectangle(px, py, px + pw, py + 20, enabled ? (hover ? 0xff304c60 : 0xff203447) : 0xff16232f);
            if (id == 6 && enabled) rectangle(px, py + 19, px + pw, py + 20, 0xff7ce6b5);
            String label = fontRenderer.trimStringToWidth(displayString, pw - 6);
            text(label, px + (pw - fontRenderer.getStringWidth(label)) / 2, py + 6, pw - 4, enabled ? 0xffeef4fa : 0xff586d80);
        }
    }
}
