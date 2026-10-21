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
        boolean searchFocus=search!=null&&search.isFocused(),nameFocus=playlistName!=null&&playlistName.isFocused();
        player.resize(width, height);
        prepareTextures();
        PlayerLayout.Box sb=player.layout.search, nb=player.layout.name;
        search = new PlayerField(sb.x+6,sb.y+6,sb.w-12,14,"Search name, source or resource");
        search.setMaxLength(128); search.setValue(query); search.setBordered(false); search.setTextColor(player.theme.text);
        addRenderableWidget(search);
        playlistName = new PlayerField(nb.x+6,nb.y+6,nb.w-12,14,"New playlist name");
        playlistName.setMaxLength(48); playlistName.setValue(newName); playlistName.setBordered(false); playlistName.setTextColor(player.theme.text);
        addRenderableWidget(playlistName);
        search.setFocused(searchFocus&&!player.settingsOpen);playlistName.setFocused(nameFocus&&!player.settingsOpen);
        if(searchFocus&&!player.settingsOpen)setFocused(search);else if(nameFocus&&!player.settingsOpen)setFocused(playlistName);
        controls.clear();
        for (int id=0;id<25;id++) {
            PlayerLayout.Box b=player.layout.buttons[id];
            if(b!=null)add(id,b.x,b.y,b.w,player.label(id));
        }
    }
    private void add(int id, int x, int y, int w, String label) {
        Button button = new PlayerButton(id, x, y, w, label);
        button.visible=player.shown(id);button.active=player.available(id);controls.put(id, button); addRenderableWidget(button);
    }
    private void act(int id) {
        if(id==3) { onClose();return; }
        player.query=search.getValue();player.playlistName=playlistName.getValue();player.action(id);
        if(id>=20) { clearWidgets(); init();return; }
        if(id==18) { search.setValue(player.query);search.setFocused(true); }
    }
    @Override public void render(GuiGraphics graphics, int x, int y, float partial) {
        this.graphics = graphics;
        beginCanvas();
        try {
        player.query = search.getValue();
        player.draw(this, x, y);
        for (Map.Entry<Integer, Button> entry : controls.entrySet()) {
            int id = entry.getKey(); Button button = entry.getValue();
            String label=player.label(id); if(!button.getMessage().getString().equals(label)) button.setMessage(Component.literal(label));
            button.active = player.available(id); button.visible=player.shown(id);
        }
        search.visible=!player.settingsOpen; playlistName.visible=!player.settingsOpen;
        if(!player.settingsOpen) {
            player.theme.nine(this,player.layout.search.x,player.layout.search.y,player.layout.search.w,20,search.isFocused()?3:1);
            player.theme.nine(this,player.layout.name.x,player.layout.name.y,player.layout.name.w,20,playlistName.isFocused()?3:1);
        }
        super.render(graphics, x, y, partial);
        if (!player.settingsOpen) {
            if (search.getValue().length()==0&&!search.isFocused()) text(player.editingNickname?"Nickname (empty clears)":"Search tracks or source",player.layout.search.x+6,72,player.layout.search.w-12,player.theme.muted);
            if (playlistName.getValue().length()==0&&!playlistName.isFocused()) text("New name",player.layout.name.x+6,46,player.layout.name.w-12,player.theme.muted);
        }
        player.overlay(this,x,y);
        } finally { endCanvas();this.graphics = null; }
    }
    @Override public int textWidth(String value) { return font.width(value); }
    @Override public void rectangle(int left, int top, int right, int bottom, int color) { graphics.fill(left, top, right, bottom, color); }
    @Override public void text(String value, int x, int y, int availableWidth, int color) {
        String text = font.plainSubstrByWidth(value, Math.max(0, availableWidth));
        graphics.drawString(font, text, x, y, color, !player.theme.light);
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (super.mouseClicked(x, y, button)) return true;
        player.query = search.getValue();
        if (button == 0 && player.click(x, y, this)) {
            setFocused(null);search.setFocused(false);playlistName.setFocused(false);return true;
        }
        return false;
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy) { return player.drag(x,y,this)||super.mouseDragged(x,y,button,dx,dy); }
    @Override public boolean mouseReleased(double x,double y,int button) { player.release(this);return super.mouseReleased(x,y,button); }
    @Override public boolean keyPressed(int key,int scan,int modifiers) {
        if(player.settingsOpen&&key==256) { act(24);return true; }
        if(!search.isFocused()&&!playlistName.isFocused()&&!player.settingsOpen) {
            if(key==265||key==264) { player.moveSelection(key==265?-1:1);return true; }
            if(key==266||key==267) { player.scroll(key==266?-player.visible():player.visible());return true; }
            if(key==61||key==45) { musicVolume(Math.max(0,Math.min(1,musicVolume()+(key==61?.05f:-.05f))),true);return true; }
            if(key==257&&getFocused()==null) { if(player.available(6))act(6);return true; }
            if(key==32&&getFocused()==null) { if(player.available(7))act(7);return true; }
        }
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean mouseScrolled(double x, double y, double delta) { player.scroll(delta > 0 ? -3 : delta < 0 ? 3 : 0); return true; }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public void removed() { player.release(this); ClientAdapter.library.save(); }
    @Override public boolean isPauseScreen() { return false; }

    private boolean canvasBlend;
    private int canvasTexture;
    private final int[] canvasBlendFactors=new int[4];
    private final float[] canvasColor=new float[4];
    private net.minecraft.client.renderer.ShaderInstance canvasShader;
    private final java.util.function.Supplier<net.minecraft.client.renderer.ShaderInstance> restoreShader=()->canvasShader;
    private void beginCanvas() {
        canvasBlend=org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);
        canvasBlendFactors[0]=org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_SRC_RGB);
        canvasBlendFactors[1]=org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_DST_RGB);
        canvasBlendFactors[2]=org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_SRC_ALPHA);
        canvasBlendFactors[3]=org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_DST_ALPHA);
        canvasTexture=com.mojang.blaze3d.systems.RenderSystem.getShaderTexture(0);
        canvasShader=com.mojang.blaze3d.systems.RenderSystem.getShader();
        System.arraycopy(com.mojang.blaze3d.systems.RenderSystem.getShaderColor(),0,canvasColor,0,4);
    }
    private void endCanvas() {
        if(canvasBlend)com.mojang.blaze3d.systems.RenderSystem.enableBlend();else com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        com.mojang.blaze3d.systems.RenderSystem.blendFuncSeparate(canvasBlendFactors[0],canvasBlendFactors[1],canvasBlendFactors[2],canvasBlendFactors[3]);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(canvasColor[0],canvasColor[1],canvasColor[2],canvasColor[3]);
        com.mojang.blaze3d.systems.RenderSystem.setShaderTexture(0,canvasTexture);
        if(canvasShader!=null)com.mojang.blaze3d.systems.RenderSystem.setShader(restoreShader);
    }
    private final java.util.Map<String,net.minecraft.resources.ResourceLocation> guiTextures=new java.util.HashMap<String,net.minecraft.resources.ResourceLocation>();
    private final java.util.Map<String,String> discTextures=new java.util.HashMap<String,String>();
    private boolean texturesPrepared;
    private net.minecraft.resources.ResourceLocation resource(String id) {
        net.minecraft.resources.ResourceLocation value=guiTextures.get(id);
        if(value==null) { value=new net.minecraft.resources.ResourceLocation(id);guiTextures.put(id,value); }
        return value;
    }
    private void prepareTextures() {
        if(texturesPrepared)return;
        texturesPrepared=true;
        for(String name:new String[]{"13","cat","blocks","chirp","far","mall","mellohi","stal","strad","ward","11","wait","pigstep","otherside","5","relic","creator","creator_music_box","precipice","tears","lava_chicken"}) {
            String id="minecraft:textures/item/music_disc_"+name+".png";
            if(!minecraft.getResourceManager().getResource(resource(id)).isPresent())continue;
            discTextures.put(name,id);
        }
    }
    @Override public boolean disc(String name,int x,int y,int size) {
        String id=discTextures.get(name);if(id==null)return false;
        texture(id,x,y,size,size,0,0,16,16,16,16,0);return true;
    }
    @Override public void texture(String id,int x,int y,int w,int h,int u,int v,int rw,int rh,int tw,int th,float degrees) {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);
        graphics.pose().pushPose();
        try { graphics.pose().translate(x+w/2f,y+h/2f,0);graphics.pose().mulPose(com.mojang.math.Axis.ZP.rotationDegrees(degrees));graphics.pose().translate(-w/2f,-h/2f,0);
            graphics.blit(resource(id),0,0,w,h,(float)u,(float)v,rw,rh,tw,th);
        } finally { graphics.pose().popPose(); }
    }
    @Override public void clip(int x,int y,int w,int h) { graphics.enableScissor(x,y,x+w,y+h); }
    @Override public void unclip() { graphics.disableScissor(); }
    @Override public float musicVolume() { return minecraft.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MUSIC); }
    @Override public void musicVolume(float value,boolean save) { minecraft.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MUSIC).set((double)value);if(save)minecraft.options.save(); }

    private final class PlayerField extends EditBox {
        private int displayStart,highlight;
        private final int drawX,drawY,drawWidth;
        PlayerField(int x,int y,int w,int h,String label) { super(font,x,y,w,h,Component.literal(label));drawX=x;drawY=y;drawWidth=w; }
        @Override public void setHighlightPos(int position) {
            super.setHighlightPos(position);highlight=Math.max(0,Math.min(position,getValue().length()));
            displayStart=GuiTheme.inputScroll(SelectorScreen.this,getValue(),highlight,displayStart,getInnerWidth());
        }
        @Override public void renderWidget(GuiGraphics graphics,int mouseX,int mouseY,float partial) {
            if(!player.theme.light) { super.renderWidget(graphics,mouseX,mouseY,partial);return; }
            if(isVisible())player.theme.input(SelectorScreen.this,getValue(),getCursorPosition(),highlight,displayStart,drawX,drawY,drawWidth,isFocused());
        }
    }

    private final class PlayerButton extends Button {
        private final int id;
        PlayerButton(int id, int x, int y, int w, String label) {
            super(x, y, w, player.layout.buttons[id].h, Component.literal(label), button -> act(id), DEFAULT_NARRATION);
            this.id=id;
        }
        @Override public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
            player.button(SelectorScreen.this,id,player.layout.buttons[id].contains(mouseX,mouseY),isFocused());
        }
    }
}
