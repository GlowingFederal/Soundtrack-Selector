package com.glowingfederal.soundtrackselector;
import com.glowingfederal.soundtrackselector.domain.*;
import java.util.Map;
import java.util.LinkedHashMap;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.StringTextComponent;
import com.mojang.blaze3d.matrix.MatrixStack;

public final class SelectorScreen extends Screen implements PlayerView.Canvas {
    private final Screen parent;
    private final PlayerView player = new PlayerView(ClientAdapter.library);
    private final Map<Integer, Button> controls = new LinkedHashMap<Integer, Button>();
    private TextFieldWidget search;
    private TextFieldWidget playlistName;
    private MatrixStack graphics;
    public SelectorScreen(Screen parent) { super(new StringTextComponent("Soundtrack Selector")); this.parent = parent; }
    @Override protected void init() {
        String newName = playlistName == null ? "" : playlistName.getValue();
        String query = search == null ? "" : search.getValue();
        boolean searchFocus=search!=null&&search.isFocused(),nameFocus=playlistName!=null&&playlistName.isFocused();
        player.resize(width, height);
        prepareTextures();
        PlayerLayout.Box sb=player.layout.search, nb=player.layout.name;
        search = new PlayerField(sb.x+6,sb.y+6,sb.w-12,14,"Search name, source or resource");
        search.setMaxLength(128); search.setValue(query); search.setBordered(false); search.setTextColor(player.theme.text);
        addButton(search);
        playlistName = new PlayerField(nb.x+6,nb.y+6,nb.w-12,14,"New playlist name");
        playlistName.setMaxLength(48); playlistName.setValue(newName); playlistName.setBordered(false); playlistName.setTextColor(player.theme.text);
        addButton(playlistName);
        search.setFocus(searchFocus&&!player.settingsOpen);playlistName.setFocus(nameFocus&&!player.settingsOpen);
        if(searchFocus&&!player.settingsOpen)setFocused(search);else if(nameFocus&&!player.settingsOpen)setFocused(playlistName);
        controls.clear();
        for (int id=0;id<25;id++) {
            PlayerLayout.Box b=player.layout.buttons[id];
            if(b!=null)add(id,b.x,b.y,b.w,player.label(id));
        }
    }
    private void add(int id, int x, int y, int w, String label) {
        Button button = new PlayerButton(id, x, y, w, label);
        button.visible=player.shown(id);button.active=player.available(id);controls.put(id, button); addButton(button);
    }
    private void act(int id) {
        if(id==3) { onClose();return; }
        player.query=search.getValue();player.playlistName=playlistName.getValue();player.action(id);
        if(id>=20) { buttons.clear(); children.clear(); init();return; }
        if(id==18) { search.setValue(player.query);search.setFocus(true); }
    }
    @Override public void render(MatrixStack graphics, int x, int y, float partial) {
        this.graphics = graphics;
        beginCanvas();
        try {
        player.query = search.getValue();
        player.draw(this, x, y);
        for (Map.Entry<Integer, Button> entry : controls.entrySet()) {
            int id = entry.getKey(); Button button = entry.getValue();
            String label=player.label(id); if(!button.getMessage().getString().equals(label)) button.setMessage(new StringTextComponent(label));
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
    @Override public void rectangle(int left, int top, int right, int bottom, int color) { fill(graphics, left, top, right, bottom, color); }
    @Override public void text(String value, int x, int y, int availableWidth, int color) {
        String text = font.plainSubstrByWidth(value, Math.max(0, availableWidth));
        if(player.theme.light)font.draw(graphics,text,x,y,color);else font.drawShadow(graphics,text,x,y,color);
    }
    @Override public boolean mouseClicked(double x, double y, int button) {
        if (super.mouseClicked(x, y, button)) return true;
        player.query = search.getValue();
        if (button == 0 && player.click(x, y, this)) {
            setFocused(null);search.setFocus(false);playlistName.setFocus(false);return true;
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
    private void beginCanvas() {
        canvasBlend=org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_BLEND);
        canvasBlendFactors[0]=org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_SRC_RGB);
        canvasBlendFactors[1]=org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_DST_RGB);
        canvasBlendFactors[2]=org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_SRC_ALPHA);
        canvasBlendFactors[3]=org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL14.GL_BLEND_DST_ALPHA);
        canvasTexture=org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_TEXTURE_BINDING_2D);
        org.lwjgl.opengl.GL11.glGetFloatv(org.lwjgl.opengl.GL11.GL_CURRENT_COLOR,canvasColor);
    }
    private void endCanvas() {
        if(canvasBlend)com.mojang.blaze3d.systems.RenderSystem.enableBlend();else com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        com.mojang.blaze3d.systems.RenderSystem.blendFuncSeparate(canvasBlendFactors[0],canvasBlendFactors[1],canvasBlendFactors[2],canvasBlendFactors[3]);
        com.mojang.blaze3d.systems.RenderSystem.color4f(canvasColor[0],canvasColor[1],canvasColor[2],canvasColor[3]);
        com.mojang.blaze3d.systems.RenderSystem.bindTexture(canvasTexture);
    }
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
            String id="minecraft:textures/item/music_disc_"+name+".png";
            try {
                java.io.InputStream in=minecraft.getResourceManager().getResource(resource(id)).getInputStream();in.close();
                discTextures.put(name,id);
            } catch(java.io.IOException absent) { /* Category artwork is the fallback. */ }
        }
    }
    @Override public boolean disc(String name,int x,int y,int size) {
        String id=discTextures.get(name);if(id==null)return false;
        texture(id,x,y,size,size,0,0,16,16,16,16,0);return true;
    }
    @Override public void texture(String id,int x,int y,int w,int h,int u,int v,int rw,int rh,int tw,int th,float degrees) {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.color4f(1,1,1,1);
        graphics.pushPose();
        try {
            minecraft.getTextureManager().bind(resource(id));
            graphics.translate(x+w/2f,y+h/2f,0);graphics.mulPose(net.minecraft.util.math.vector.Vector3f.ZP.rotationDegrees(degrees));
            graphics.scale(w/(float)rw,h/(float)rh,1);
            blit(graphics,-rw/2,-rh/2,(float)u,(float)v,rw,rh,tw,th);
        } finally { graphics.popPose(); }
    }
    private final int[] oldScissor=new int[4];
    private boolean hadScissor;
    @Override public void clip(int x,int y,int w,int h) {
        hadScissor=org.lwjgl.opengl.GL11.glIsEnabled(org.lwjgl.opengl.GL11.GL_SCISSOR_TEST);
        org.lwjgl.opengl.GL11.glGetIntegerv(org.lwjgl.opengl.GL11.GL_SCISSOR_BOX,oldScissor);
        double scale=minecraft.getWindow().getGuiScale();
        com.mojang.blaze3d.systems.RenderSystem.enableScissor((int)(x*scale),(int)(minecraft.getWindow().getHeight()-(y+h)*scale),(int)Math.ceil(w*scale),(int)Math.ceil(h*scale));
    }
    @Override public void unclip() {
        if(hadScissor)com.mojang.blaze3d.systems.RenderSystem.enableScissor(oldScissor[0],oldScissor[1],oldScissor[2],oldScissor[3]);
        else com.mojang.blaze3d.systems.RenderSystem.disableScissor();
    }
    @Override public float musicVolume() { return minecraft.options.getSoundSourceVolume(net.minecraft.util.SoundCategory.MUSIC); }
    @Override public void musicVolume(float value,boolean save) { minecraft.options.setSoundCategoryVolume(net.minecraft.util.SoundCategory.MUSIC,value);if(save)minecraft.options.save(); }

    private final class PlayerField extends TextFieldWidget {
        private int displayStart,highlight;
        private final int drawX,drawY,drawWidth;
        PlayerField(int x,int y,int w,int h,String label) { super(font,x,y,w,h,new StringTextComponent(label));drawX=x;drawY=y;drawWidth=w; }
        @Override public void setHighlightPos(int position) {
            super.setHighlightPos(position);highlight=Math.max(0,Math.min(position,getValue().length()));
            displayStart=GuiTheme.inputScroll(SelectorScreen.this,getValue(),highlight,displayStart,getInnerWidth());
        }
        @Override public void renderButton(MatrixStack graphics,int mouseX,int mouseY,float partial) {
            if(!player.theme.light) { super.renderButton(graphics,mouseX,mouseY,partial);return; }
            if(isVisible())player.theme.input(SelectorScreen.this,getValue(),getCursorPosition(),highlight,displayStart,drawX,drawY,drawWidth,isFocused());
        }
    }

    private final class PlayerButton extends Button {
        private final int id;
        PlayerButton(int id, int x, int y, int w, String label) {
            super(x, y, w, player.layout.buttons[id].h, new StringTextComponent(label), button -> act(id));
            this.id=id;
        }
        @Override public void renderButton(MatrixStack graphics, int mouseX, int mouseY, float partial) {
            player.button(SelectorScreen.this,id,player.layout.buttons[id].contains(mouseX,mouseY),isFocused());
        }
    }
}
