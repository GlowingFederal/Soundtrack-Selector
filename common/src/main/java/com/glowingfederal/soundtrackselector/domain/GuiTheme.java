package com.glowingfederal.soundtrackselector.domain;

/** Reusable textured skin. Classic uses deep six-pixel frames; Modern uses thin two-pixel edges. */
public final class GuiTheme {
    public final boolean modern, light;
    public final int background, panel, text, muted, accent, line, selection, hover, disabled;
    public final String atlas, icons;
    public GuiTheme(Appearance appearance) {
        modern=appearance.modern; light=appearance.light;
        background=light?(modern?0xffe2e8ec:0xffb69261):(modern?0xff090f19:0xff20150f);
        panel=light?(modern?0xfff5f7f8:0xfff1e2be):(modern?0xff111d2b:0xff2d2118);
        text=light?0xff202d32:0xfff5efdd;
        muted=light?(modern?0xff465c69:0xff60523e):(modern?0xff9eafc0:0xffc0ae8b);
        accent=light?(modern?0xff087a8a:0xff356327):(modern?0xff63d9bb:0xffefbd61);
        line=light?(modern?0xffc6d3d9:0xffc4ab7c):(modern?0xff2a3c50:0xff60462d);
        selection=light?(modern?0xffc9e5e9:0xffd4dfac):(modern?0xff183e40:0xff3d4927);
        hover=light?(modern?0xffe0edf1:0xffe5d6af):(modern?0xff1d3042:0xff443322);
        disabled=light?0xff414d47:0xff8e8c83;
        atlas="soundtrackselector:textures/gui/"+(modern?"modern/":"classic/")+(light?"light":"dark")+".png";
        icons="soundtrackselector:textures/gui/icons/"+(light?"light":"dark")+".png";
    }
    public void panel(PlayerView.Canvas c, PlayerLayout.Box b, boolean inset) { nine(c,b.x,b.y,b.w,b.h,inset?1:0); }
    public void nine(PlayerView.Canvas c, int x,int y,int w,int h,int cell) {
        if(w<=0||h<=0)return;
        int edge=Math.min(modern?2:6,Math.min(w,h)/2), u=(cell%4)*32,v=(cell/4)*32;
        for(int iy=0;iy<3;iy++) for(int ix=0;ix<3;ix++) {
            int sx=ix==0?0:ix==1?edge:32-edge, sy=iy==0?0:iy==1?edge:32-edge;
            int sw=ix==1?32-2*edge:edge,sh=iy==1?32-2*edge:edge;
            int dx=x+(ix==0?0:ix==1?edge:w-edge),dy=y+(iy==0?0:iy==1?edge:h-edge);
            int dw=ix==1?w-2*edge:edge,dh=iy==1?h-2*edge:edge;
            if(dw>0&&dh>0)c.texture(atlas,dx,dy,dw,dh,u+sx,v+sy,sw,sh,128,64,0);
        }
    }
    public void icon(PlayerView.Canvas c,int icon,int x,int y,int size) {
        c.texture(icons,x,y,size,size,icon*16,0,16,16,256,16,0);
    }
    public void artwork(PlayerView.Canvas c,Track track,int x,int y,int size) {
        if(track!=null && track.disc!=null && c.disc(track.disc,x,y,size))return;
        c.texture("soundtrackselector:textures/gui/icons/artwork.png",x,y,size,size,(track==null?5:track.artwork)*16,0,16,16,128,16,0);
    }
    /** Match the native edit-box scroll algorithm while leaving all editing and clipboard behavior native. */
    public static int inputScroll(PlayerView.Canvas c,String value,int highlight,int start,int width) {
        start=Math.min(start,value.length());
        int end=start;
        while(end<value.length()&&c.textWidth(value.substring(start,end+1))<=width)end++;
        if(highlight==start) {
            int count=0;
            while(count<value.length()&&c.textWidth(value.substring(value.length()-count-1))<=width)count++;
            start-=count;
        }
        if(highlight>end)start+=highlight-end;
        else if(highlight<=start)start=highlight;
        return Math.max(0,Math.min(start,value.length()));
    }
    /** Shadow-free light input text, selection, and caret; native widgets still own input and focus. */
    public void input(PlayerView.Canvas c,String value,int cursor,int highlight,int start,int x,int y,int width,boolean focused) {
        start=Math.min(start,value.length());
        c.clip(x,y,width,12);
        try {
            if(cursor!=highlight) {
                int a=Math.max(start,Math.min(cursor,highlight)),b=Math.max(start,Math.max(cursor,highlight));
                c.rectangle(x+c.textWidth(value.substring(start,a)),y-1,x+c.textWidth(value.substring(start,b)),y+9,selection);
            }
            c.text(value.substring(start),x,y,width,text);
            if(focused&&cursor>=start&&(System.nanoTime()/500000000L)%2==0) {
                int cx=x+c.textWidth(value.substring(start,cursor));
                c.rectangle(cx,y-1,cx+1,y+9,accent);
            }
        } finally { c.unclip(); }
    }
    public void button(PlayerView.Canvas c, PlayerLayout.Box b, String label, int icon, boolean enabled, boolean hovered, boolean focused, boolean selected, float transition) {
        if(b==null)return;
        nine(c,b.x,b.y,b.w,b.h,!enabled?5:selected?4:hovered?3:2);
        if(enabled && transition>0) c.rectangle(b.x+2,b.y+b.h-2,b.x+2+(int)((b.w-4)*transition),b.y+b.h-1,accent);
        if(focused) {
            c.rectangle(b.x,b.y,b.x+b.w,b.y+1,accent); c.rectangle(b.x,b.y+b.h-1,b.x+b.w,b.y+b.h,accent);
            c.rectangle(b.x,b.y,b.x+1,b.y+b.h,accent); c.rectangle(b.x+b.w-1,b.y,b.x+b.w,b.y+b.h,accent);
        }
        if(icon>=0)icon(c,icon,b.x+(label.length()==0?(b.w-12)/2:6),b.y+(b.h-12)/2,12);
        if(label.length()!=0)c.text(label,b.x+(icon>=0?23:6),b.y+(b.h-8)/2,b.w-(icon>=0?27:12),enabled?text:disabled);
        if(selected && label.length()==0)c.text("+",b.x+b.w-8,b.y+2,7,text);
        if(!enabled)c.rectangle(b.x+3,b.y+b.h-3,b.x+b.w-3,b.y+b.h-2,line);
    }
}
