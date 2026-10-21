package com.glowingfederal.soundtrackselector.domain;

import java.util.List;

/** Native-canvas presentation; no resource discovery, persistence, or playback mutation. */
final class PlayerRenderer {
    private final float[] hover = new float[25];
    private long lastFrame;
    private float frameStep;
    private static final String DISC="soundtrackselector:textures/gui/common/disc.png";
    private static final String TONEARM="soundtrackselector:textures/gui/common/tonearm.png";
    private static final int[] CONTROL_ICONS={0,-1,-1,-1,-1,-1,1,2,3,4,6,7,8,-1,-1,-1,9,10,-1,-1,-1,-1,-1,-1,-1};
    public void draw(PlayerView p,PlayerView.Canvas c,int mx,int my) {
        long now=System.nanoTime();
        frameStep=p.library.appearance.reducedMotion?1:Math.min(1,(now-lastFrame)/120000000f);lastFrame=now;
        GuiTheme t=p.theme; PlayerLayout l=p.layout;
        c.rectangle(0,0,p.width,p.height,t.background);
        if(!t.modern) {
            t.nine(c,4,4,p.width-8,p.height-8,0);
            // Small brass corner studs and leafy brackets, rather than a full-screen decoration.
            t.icon(c,14,7,7,16);t.icon(c,14,p.width-23,7,16);
        }
        disc(p,c,14,8,22);
        c.text("Soundtrack Selector",42,10,p.width-154,t.text);
        if(p.width>=520)c.text(p.library.discovering()?"Discovering audio in background...":p.library.status,42,23,p.width-154,t.muted);
        t.panel(c,l.libraryPanel,true);
        if(l.sidebar.w>0) {
            t.panel(c,l.sidebar,false);
            c.text(t.modern?"YOUR LIBRARY":"LIBRARY VIEWS",l.sidebar.x+9,45,l.sidebar.w-18,t.muted);
            if(l.sidebar.h>=180) {
                t.artwork(c,null,l.sidebar.x+31,l.sidebar.y+133,40);
                c.text(p.library.catalog.enabledCount()+" enabled",l.sidebar.x+9,l.sidebar.y+l.sidebar.h-31,l.sidebar.w-18,t.accent);
                c.text("Music volume",l.sidebar.x+9,l.sidebar.y+l.sidebar.h-17,l.sidebar.w-18,t.muted);
            }
        }
        c.text("TRACK / SOURCE",l.library.x+27,117,l.library.w-82,t.muted);
        c.text("TIME",l.library.x+l.library.w-46,117,36,t.muted);
        c.rectangle(l.library.x,126,l.library.x+l.library.w,127,t.line);
        List<Track> rows=p.rows();
        c.clip(l.library.x,l.library.y,l.library.w,l.library.h);
        try {
            Track playing=p.playing();
            for(int i=0;i<p.visible()&&p.list.offset+i<rows.size();i++) {
                Track track=rows.get(p.list.offset+i);
                int x=l.library.x,y=l.library.y+i*l.rowHeight,w=l.library.w-10;
                boolean selected=p.list.selected!=null&&p.list.selected.id.equals(track.id);
                boolean active=playing!=null&&playing.id.equals(track.id);
                boolean over=mx>=x&&mx<x+w&&my>=y&&my<y+l.rowHeight;
                boolean enabled=p.library.catalog.preferences.enabled(track);
                c.rectangle(x,y,x+w,y+l.rowHeight-1,selected?t.selection:over?t.hover:t.panel);
                if(selected) { c.rectangle(x,y,x+2,y+l.rowHeight-1,t.accent); }
                if(!t.modern)c.rectangle(x+3,y+l.rowHeight-1,x+w,y+l.rowHeight,t.line);
                int checkboxY=y+(l.rowHeight-12)/2;
                c.rectangle(x+6,checkboxY,x+18,checkboxY+12,t.line);
                c.rectangle(x+7,checkboxY+1,x+17,checkboxY+11,enabled?t.selection:t.panel);
                c.text(enabled?"+":"-",x+9,y+(l.rowHeight-8)/2,8,t.text);
                t.artwork(c,track,x+26,y+5,18);
                c.text(p.library.catalog.preferences.displayName(track),x+50,y+4,w-98,t.text);
                c.text(track.event,x+50,y+16,w-98,t.muted);
                c.text(track.durationText(),x+w-38,y+10,36,t.muted);
                if(active)t.icon(c,p.paused()?2:1,x+w-55,y+9,10);
            }
            if(rows.isEmpty())c.text(p.library.discovering()?"Discovering audio...":"No tracks. Try Long audio or another search.",l.library.x+8,l.library.y+12,l.library.w-16,t.muted);
        } finally { c.unclip(); }
        if(rows.size()>p.visible()) {
            int x=l.library.x+l.library.w-7;
            c.rectangle(x,l.library.y,x+5,l.library.y+l.library.h,t.line);
            t.nine(c,x,p.thumbTop(),5,p.thumbHeight(),3);
        }
        if(l.nowPlaying.w>0)nowPlaying(p,c);
        footer(p,c);
        if(p.settingsOpen) {
            c.rectangle(0,0,p.width,p.height,0x99000000);
            t.panel(c,l.settings,false);
            c.text("Appearance",l.settings.x+14,l.settings.y+12,240,t.text);
            c.text("Changes apply immediately",l.settings.x+14,l.settings.y+131,166,t.muted);
        }
    }
    private void disc(PlayerView p,PlayerView.Canvas c,int x,int y,int size) {
        float degrees=p.playing()==null||p.library.appearance.reducedMotion?0:(float)((p.library.elapsed()*36)%360);
        c.texture(DISC,x,y,size,size,0,0,32,32,32,32,degrees);
        // The pickup is stationary and must be submitted after the rotating record.
        c.texture(TONEARM,x,y,size,size,0,0,32,32,32,32,0);
    }
    private void nowPlaying(PlayerView p,PlayerView.Canvas c) {
        PlayerLayout.Box b=p.layout.nowPlaying;GuiTheme t=p.theme;
        t.panel(c,b,false);c.text("NOW PLAYING",b.x+12,b.y+12,b.w-24,t.muted);
        int size=Math.min(b.w-40,Math.max(48,b.h-154));
        int x=b.x+(b.w-size)/2,y=b.y+34;
        t.nine(c,x-5,y-5,size+10,size+10,1);
        disc(p,c,x,y,size);
        Track track=p.playing();
        t.artwork(c,track,x+size-26,y+size-26,26);
        int titleY=y+size+17;
        c.text(track==null?"Vanilla music":p.library.catalog.preferences.displayName(track),b.x+12,titleY,b.w-24,t.text);
        c.text(track==null?"Minecraft is in control":track.namespace,b.x+12,titleY+15,b.w-24,t.muted);
        c.text(track==null?"No custom track":p.paused()?"Paused":"Playing",b.x+12,titleY+32,b.w-24,t.accent);
        if(titleY+66<b.y+b.h) {
            c.text("PLAYLIST",b.x+12,b.y+b.h-39,b.w-24,t.muted);
            c.text(p.library.catalog.preferences.activePlaylist(),b.x+12,b.y+b.h-23,b.w-24,t.text);
        }
    }
    private void footer(PlayerView p,PlayerView.Canvas c) {
        GuiTheme t=p.theme;PlayerLayout l=p.layout;PlayerLayout.Box b=l.footer;
        t.panel(c,b,false);
        Track track=p.playing();
        t.artwork(c,track,b.x+8,b.y+8,24);
        c.text(track==null?"Vanilla - Minecraft controls music":p.library.catalog.preferences.displayName(track),b.x+40,b.y+7,b.w-144,t.text);
        c.text(track==null?(p.list.selected==null?"Select a track, then Play":"Selected: "+p.library.catalog.preferences.displayName(p.list.selected)):(p.paused()?"PAUSED  ":"PLAYING  ")+track.event,b.x+40,b.y+20,b.w-144,t.muted);
        PlayerLayout.Box v=l.volume;
        t.icon(c,13,v.x-14,v.y,10);
        c.text((int)(c.musicVolume()*100)+"%",v.x+v.w-24,v.y-9,28,t.muted);
        c.rectangle(v.x,v.y+4,v.x+v.w,v.y+7,t.line);
        int vx=v.x+(int)((v.w-1)*c.musicVolume());
        c.rectangle(v.x,v.y+4,vx,v.y+7,t.accent);t.nine(c,vx-2,v.y,5,12,3);
        int progressX=b.x+8,progressY=b.y+35,progressW=b.w-98;
        c.rectangle(progressX,progressY,progressX+progressW,progressY+3,t.line);
        if(track!=null) {
            c.rectangle(progressX,progressY,progressX+(int)(progressW*Math.min(1,p.library.elapsed()/track.seconds)),progressY+3,t.accent);
            int seconds=(int)Math.min(track.seconds,p.library.elapsed());
            c.text(seconds/60+":"+(seconds%60<10?"0":"")+seconds%60+" / "+track.durationText(),progressX+progressW+6,progressY-2,82,t.muted);
        } else c.text("Stopped",progressX+progressW+6,progressY-2,82,t.muted);
    }
    public void button(PlayerView p,PlayerView.Canvas c,int id,boolean over,boolean focus) {
        hover[id]+=(over?1-hover[id]:-hover[id])*frameStep;
        int icon=CONTROL_ICONS[id];
        String label=p.label(id);
        if(id==18&&p.layout.buttons[id].w<64)label=p.editingNickname?"Save":"Name";
        if(id==0||id==6||id==7||id==8||id==9||id==16||id==17) {
            label="";
            if(id==7)icon=p.paused()?1:2;
        }
        p.theme.button(c,p.layout.buttons[id],label,icon,p.available(id),over,focus,p.selected(id),hover[id]);
    }
    public void overlay(PlayerView p,PlayerView.Canvas c,int mx,int my) {
        String tip=null;
        for(int id=0;id<25;id++)if(p.shown(id)&&(p.layout.buttons[id].contains(mx,my)||p.focusControl==id)) {
            tip=p.label(id);
            if(id==6)tip="Play selected track (or double-click a row)";
            if(id==8)tip="Stop custom music and return to Vanilla";
            if(id==0)tip="Previous track in playback history";
            if(!p.available(id))tip+=" (unavailable)";
            break;
        }
        if(!p.settingsOpen&&p.layout.volume.contains(mx,my))tip="Music volume - drag or use +/- keys";
        if(!p.settingsOpen&&p.layout.library.contains(mx,my)&&mx<p.layout.library.x+23)tip=p.library.catalog.preferences.vanilla()?"Vanilla is protected; choose a user playlist":"+ enabled / - disabled in this playlist";
        if(!p.settingsOpen&&p.layout.library.contains(mx,my)&&mx>=p.layout.library.x+23) {
            int index=p.list.offset+(my-p.layout.library.y)/p.layout.rowHeight;
            if(index<p.rows().size()) {
                Track track=p.rows().get(index);
                int w=Math.min(p.width-16,Math.max(180,Math.max(track.id.length(),track.event.length())*6+12));
                int x=Math.max(8,Math.min(mx+10,p.width-w-8)),y=Math.max(4,Math.min(my+15,p.height-48));
                p.theme.nine(c,x,y,w,44,1);
                c.text(p.library.catalog.preferences.displayName(track),x+6,y+6,w-12,p.theme.text);
                c.text(track.event,x+6,y+18,w-12,p.theme.muted);
                c.text(track.id,x+6,y+30,w-12,p.theme.muted);
                return;
            }
        }
        if(tip==null)return;
        int w=Math.min(p.width-16,Math.max(130,tip.length()*6+12)),x=Math.max(8,Math.min(mx+10,p.width-w-8)),y=Math.max(4,Math.min(my+15,p.height-25));
        p.theme.nine(c,x,y,w,21,1);c.text(tip,x+6,y+7,w-12,p.theme.text);
    }
}
