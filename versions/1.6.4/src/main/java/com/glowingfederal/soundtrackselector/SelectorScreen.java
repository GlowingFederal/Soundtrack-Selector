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
        boolean searchFocus=search!=null&&search.isFocused(),nameFocus=playlistName!=null&&playlistName.isFocused();
        player.resize(width, height);
        prepareTextures();
        PlayerLayout.Box sb=player.layout.search, nb=player.layout.name;
        search = new PlayerField(sb.x+6,sb.y+6,sb.w-12,14);
        search.setMaxStringLength(128); search.setText(query); search.setEnableBackgroundDrawing(false); search.setTextColor(player.theme.text);
        playlistName = new PlayerField(nb.x+6,nb.y+6,nb.w-12,14);
        playlistName.setMaxStringLength(48); playlistName.setText(newName); playlistName.setEnableBackgroundDrawing(false); playlistName.setTextColor(player.theme.text);
        search.setFocused(searchFocus&&!player.settingsOpen);playlistName.setFocused(nameFocus&&!player.settingsOpen);
        buttonList.clear();
        for (int id=0;id<25;id++) {
            PlayerLayout.Box b=player.layout.buttons[id];
            if(b!=null)add(id,b.x,b.y,b.w,player.label(id));
        }
    }
    private void add(int id, int x, int y, int w, String label) { PlayerButton button=new PlayerButton(id,x,y,w,label);button.drawButton=player.shown(id);button.enabled=player.available(id);buttonList.add(button); }
    @Override public void drawScreen(int x, int y, float partial) {
        player.query = search.getText();
        player.draw(this, x, y);
        if (!player.settingsOpen) {
            player.theme.nine(this,player.layout.search.x,player.layout.search.y,player.layout.search.w,20,search.isFocused()?3:1);
            player.theme.nine(this,player.layout.name.x,player.layout.name.y,player.layout.name.w,20,playlistName.isFocused()?3:1);
            search.drawTextBox(); playlistName.drawTextBox();
        }
        for (Object object : buttonList) {
            GuiButton button = (GuiButton) object;
            button.displayString = player.label(button.id);
            button.enabled = player.available(button.id); button.drawButton=player.shown(button.id);
        }
        super.drawScreen(x, y, partial);
        if (!player.settingsOpen) {
            if (search.getText().length()==0&&!search.isFocused()) text(player.editingNickname?"Nickname (empty clears)":"Search tracks or source",player.layout.search.x+6,72,player.layout.search.w-12,player.theme.muted);
            if (playlistName.getText().length()==0&&!playlistName.isFocused()) text("New name",player.layout.name.x+6,46,player.layout.name.w-12,player.theme.muted);
        }
        player.overlay(this,x,y);

    }
    @Override public int textWidth(String value) { return fontRenderer.getStringWidth(value); }
    @Override public void rectangle(int left, int top, int right, int bottom, int color) { drawRect(left, top, right, bottom, color); }
    @Override public void text(String value, int x, int y, int availableWidth, int color) {
        fontRenderer.drawString(fontRenderer.trimStringToWidth(value, Math.max(0, availableWidth)), x, y, color);
    }
    @Override protected void mouseClicked(int x, int y, int button) {
        boolean control=false;
        for(int id=0;id<25;id++)if(player.shown(id)&&player.layout.buttons[id].contains(x,y)) { control=true;break; }
        super.mouseClicked(x, y, button);
        if(control)return;
        if(!player.settingsOpen) { search.mouseClicked(x,y,button);playlistName.mouseClicked(x,y,button); }
        player.focusControl=-1;
        player.query = search.getText();
        if (button == 0) player.click(x, y, this);
    }
    private void act(int id) {
        if (id==3) { mc.displayGuiScreen(parent); return; }
        player.query=search.getText();player.playlistName=playlistName.getText();player.action(id);
        if(id>=20) { initGui(); return; }
        if(id==18) { search.setText(player.query);search.setFocused(true); }
    }
    @Override protected void actionPerformed(GuiButton button) { act(button.id); }
    @Override protected void mouseClickMove(int x,int y,int button,long time) { if(button==0)player.drag(x,y,this); }
    @Override protected void mouseMovedOrUp(int x,int y,int button) { super.mouseMovedOrUp(x,y,button);player.release(this); }
    @Override public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) player.scroll(wheel > 0 ? -3 : 3);
    }
    @Override protected void keyTyped(char character, int key) {
        if(key==Keyboard.KEY_ESCAPE) { if(player.settingsOpen)act(24);else mc.displayGuiScreen(parent);return; }
        if(key==Keyboard.KEY_TAB) {
            if(search.isFocused()) { search.setFocused(false);playlistName.setFocused(true);player.focusControl=-1; }
            else if(playlistName.isFocused()) { playlistName.setFocused(false);player.nextFocus(); }
            else if(player.focusControl==24||player.focusControl==20) { player.focusControl=-1;search.setFocused(!player.settingsOpen);if(player.settingsOpen)player.nextFocus(); }
            else player.nextFocus();
            return;
        }
        if(!search.isFocused()&&!playlistName.isFocused()) {
            if(key==Keyboard.KEY_UP) { player.moveSelection(-1);return; }
            if(key==Keyboard.KEY_DOWN) { player.moveSelection(1);return; }
            if(key==Keyboard.KEY_RETURN) { int id=player.focusControl>=0?player.focusControl:6;if(player.available(id))act(id);return; }
            if(key==Keyboard.KEY_SPACE) { if(player.focusControl>=0&&player.available(player.focusControl))act(player.focusControl);else if(player.available(7))act(7);return; }
            if(character=='+'||character=='-') { musicVolume(Math.max(0,Math.min(1,musicVolume()+(character=='+'?.05f:-.05f))),true);return; }
        }
        if(key==Keyboard.KEY_PRIOR)player.scroll(-player.visible());
        else if(key==Keyboard.KEY_NEXT)player.scroll(player.visible());
        else if(!player.settingsOpen) { search.textboxKeyTyped(character,key);playlistName.textboxKeyTyped(character,key); }

    }
    @Override public void updateScreen() { search.updateCursorCounter(); playlistName.updateCursorCounter(); }
    @Override public void onGuiClosed() { Keyboard.enableRepeatEvents(false); player.release(this); ClientAdapter.library.save(); }
    @Override public boolean doesGuiPauseGame() { return false; }

    private final java.util.Map<String,net.minecraft.util.ResourceLocation> guiTextures=new java.util.HashMap<String,net.minecraft.util.ResourceLocation>();
    private final java.util.Map<String,String> discTextures=new java.util.HashMap<String,String>();
    private boolean texturesPrepared;
    private net.minecraft.util.ResourceLocation resource(String id) {
        net.minecraft.util.ResourceLocation value=guiTextures.get(id);
        if(value==null) { value=new net.minecraft.util.ResourceLocation(id);guiTextures.put(id,value); }
        return value;
    }
    private void prepareTextures() {
        if(texturesPrepared)return;
        texturesPrepared=true;
        for(String name:new String[]{"13","cat","blocks","chirp","far","mall","mellohi","stal","strad","ward","11","wait","pigstep","otherside","5","relic","creator","creator_music_box","precipice","tears","lava_chicken"}) {
            String id="minecraft:textures/items/record_"+name+".png";
            try {
                java.io.InputStream in=mc.getResourceManager().getResource(resource(id)).getInputStream();in.close();
                discTextures.put(name,id);
            } catch(java.io.IOException absent) { /* Category artwork is the fallback. */ }
        }
    }
    @Override public boolean disc(String name,int x,int y,int size) {
        String id=discTextures.get(name);if(id==null)return false;
        texture(id,x,y,size,size,0,0,16,16,16,16,0);return true;
    }
    @Override public void texture(String id,int x,int y,int w,int h,int u,int v,int rw,int rh,int tw,int th,float degrees) {
        org.lwjgl.opengl.GL11.glPushAttrib(org.lwjgl.opengl.GL11.GL_ENABLE_BIT|org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT|org.lwjgl.opengl.GL11.GL_CURRENT_BIT);
        org.lwjgl.opengl.GL11.glPushMatrix();
        try {
            mc.getTextureManager().bindTexture(resource(id));
            org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_TEXTURE_2D);
            org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_BLEND);
            org.lwjgl.opengl.GL11.glBlendFunc(org.lwjgl.opengl.GL11.GL_SRC_ALPHA,org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA);
            org.lwjgl.opengl.GL11.glColor4f(1,1,1,1);
            org.lwjgl.opengl.GL11.glTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D,org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER,org.lwjgl.opengl.GL11.GL_NEAREST);
            org.lwjgl.opengl.GL11.glTexParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D,org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER,org.lwjgl.opengl.GL11.GL_NEAREST);
            org.lwjgl.opengl.GL11.glTranslatef(x+w/2f,y+h/2f,0);org.lwjgl.opengl.GL11.glRotatef(degrees,0,0,1);
            float a=u/(float)tw,b=v/(float)th,cc=(u+rw)/(float)tw,d=(v+rh)/(float)th;
            org.lwjgl.opengl.GL11.glBegin(org.lwjgl.opengl.GL11.GL_QUADS);
            org.lwjgl.opengl.GL11.glTexCoord2f(a,b);org.lwjgl.opengl.GL11.glVertex2f(-w/2f,-h/2f);
            org.lwjgl.opengl.GL11.glTexCoord2f(a,d);org.lwjgl.opengl.GL11.glVertex2f(-w/2f,h/2f);
            org.lwjgl.opengl.GL11.glTexCoord2f(cc,d);org.lwjgl.opengl.GL11.glVertex2f(w/2f,h/2f);
            org.lwjgl.opengl.GL11.glTexCoord2f(cc,b);org.lwjgl.opengl.GL11.glVertex2f(w/2f,-h/2f);
            org.lwjgl.opengl.GL11.glEnd();
        } finally { org.lwjgl.opengl.GL11.glPopMatrix();org.lwjgl.opengl.GL11.glPopAttrib(); }
    }
    @Override public void clip(int x,int y,int w,int h) {
        org.lwjgl.opengl.GL11.glPushAttrib(org.lwjgl.opengl.GL11.GL_SCISSOR_BIT);
        double sx=mc.displayWidth/(double)width,sy=mc.displayHeight/(double)height;
        org.lwjgl.opengl.GL11.glEnable(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
        org.lwjgl.opengl.GL11.glScissor((int)Math.floor(x*sx),(int)Math.floor(mc.displayHeight-(y+h)*sy),(int)Math.ceil(w*sx),(int)Math.ceil(h*sy));
    }
    @Override public void unclip() { org.lwjgl.opengl.GL11.glPopAttrib(); }
    @Override public float musicVolume() { return mc.gameSettings.musicVolume; }
    @Override public void musicVolume(float value,boolean save) { mc.gameSettings.musicVolume=value;if(save)mc.gameSettings.saveOptions(); }

    private final class PlayerField extends GuiTextField {
        private int displayStart;
        private final int drawX,drawY,drawWidth;
        PlayerField(int x,int y,int w,int h) { super(fontRenderer, x,y,w,h);drawX=x;drawY=y;drawWidth=w; }
        @Override public void setSelectionPos(int position) {
            super.setSelectionPos(position);
            displayStart=GuiTheme.inputScroll(SelectorScreen.this,getText(),getSelectionEnd(),displayStart,getWidth());
        }
        @Override public void drawTextBox() {
            if(!player.theme.light) { super.drawTextBox();return; }
            player.theme.input(SelectorScreen.this,getText(),getCursorPosition(),getSelectionEnd(),displayStart,drawX,drawY,drawWidth,isFocused());
        }
    }

    private final class PlayerButton extends GuiButton {

        PlayerButton(int id, int x, int y, int w, String label) { super(id, x, y, w, player.layout.buttons[id].h, label); }
        @Override public void drawButton(Minecraft client, int mouseX, int mouseY) {
            if(!drawButton)return;
            player.button(SelectorScreen.this,id,player.layout.buttons[id].contains(mouseX,mouseY),player.focusControl==id);
        }
    }
}
