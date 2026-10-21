package com.glowingfederal.soundtrackselector.domain;

import java.util.List;

/** Interaction state only. PlayerRenderer and GuiTheme own presentation; adapters own native APIs. */
public final class PlayerView {
    public interface Canvas {
        void rectangle(int left, int top, int right, int bottom, int color);
        void text(String value, int x, int y, int availableWidth, int color);
        int textWidth(String value);
        void texture(String resource, int x,int y,int width,int height,int u,int v,int regionWidth,int regionHeight,int textureWidth,int textureHeight,float degrees);
        void clip(int x,int y,int width,int height);
        void unclip();
        boolean disc(String name,int x,int y,int size);
        float musicVolume();
        void musicVolume(float value,boolean save);
    }
    public final ListView list;
    final Library library;
    public PlayerLayout layout;
    public GuiTheme theme;
    public int width,height;
    public String query="",playlistName="";
    public boolean editingNickname,settingsOpen;
    public int focusControl=-1;
    private String previousQuery,clickedTrack;
    private Track nicknameTrack;
    private boolean deleteArmed,draggingScroll,draggingVolume;
    private int scrollGrab;
    private long clickedAt;
    private final PlayerRenderer renderer = new PlayerRenderer();
    public PlayerView(Library library) { this.library=library; list=new ListView(library.catalog); }
    public void resize(int width,int height) {
        this.width=width; this.height=height;
        layout=new PlayerLayout(width,height,library.appearance.modern);
        theme=new GuiTheme(library.appearance);
    }
    public int left() { return layout.library.x; }
    public int visible() { return Math.max(1,layout.library.h/layout.rowHeight); }
    public List<Track> rows() { List<Track> rows=list.rows(editingNickname?previousQuery:query); list.clamp(visible()); return rows; }
    public boolean shown(int id) { return layout.buttons[id]!=null && (settingsOpen?id>=21&&id<=24:id<=20); }
    public boolean selected(int id) {
        return id>=10&&id<=12?list.filterIndex()==id-10:id==16?library.catalog.preferences.loop():id==17&&library.catalog.preferences.randomOrder();
    }
    public void action(int id) {
        clickedTrack = null;
        if (id == 20 || id == 24) { settingsOpen = !settingsOpen; return; }
        if (id >= 21 && id <= 23) {
            if (id == 21) library.appearance.modern = !library.appearance.modern;
            if (id == 22) library.appearance.light = !library.appearance.light;
            if (id == 23) library.appearance.reducedMotion = !library.appearance.reducedMotion;
            library.appearance.save(); resize(width,height); return;
        }
        if (id == 0) { library.previous(); return; }
        if (id == 19) { library.requestRefresh(); return; }
        if (id == 18) {
            if (editingNickname) {
                library.rename(nicknameTrack, query); query = previousQuery; editingNickname = false;
            } else if (list.selected != null) {
                nicknameTrack = list.selected; previousQuery = query;
                query = library.catalog.preferences.nickname(nicknameTrack); editingNickname = true;
            }
            return;
        }
        if (id != 15) deleteArmed = false;
        if (id == 13) library.nextPlaylist();
        if (id == 14) library.createPlaylist(playlistName);
        if (id == 15) { if (deleteArmed) { library.deletePlaylist(); deleteArmed = false; } else deleteArmed = true; }
        if (id == 1) library.setTracks(rows(), true);
        if (id == 2) library.setTracks(rows(), false);
        if (id == 4) list.cycleFilter();
        if (id == 5) list.cycleSort();
        if (id == 6) library.play(list.selected);
        if (id == 7) library.togglePause();
        if (id == 8) library.stop();
        if (id == 9) library.next();
        if (id == 16) library.toggleLoop();
        if (id == 17) library.toggleRandomOrder();
        if (id >= 10 && id <= 12) list.filter(id - 10);
    }
    public String label(int id) {
        if (id == 0) return "Previous";
        if (id == 3) return "Done";
        if (id == 6) return "Play selected";
        if (id == 8) return "Stop";
        if (id == 1) return "All on";
        if (id == 2) return "All off";
        if (id == 10) return "Music";
        if (id == 11) return "Soundtrack";
        if (id == 12) return "Long audio";
        if (id == 14) return "New";
        if (id == 19) return "Refresh";
        if (id == 20) return "Appearance";
        if (id == 21) return "Style: " + (library.appearance.modern ? "Modern" : "Classic");
        if (id == 22) return "Appearance: " + (library.appearance.light ? "Light" : "Dark");
        if (id == 23) return "Reduced animations: " + (library.appearance.reducedMotion ? "On" : "Off");
        if (id == 24) return "Back";
        if (id == 18) return editingNickname ? "Save name" : "Nickname";
        if (id == 9) return library.custom() ? "Next" : "Start";
        if (id == 13) return library.catalog.preferences.activePlaylist();
        if (id == 15) return deleteArmed ? "Sure?" : "Delete";
        if (id == 16) return "Loop: " + (library.catalog.preferences.loop() ? "On" : "Off");
        if (id == 17) return "Random: " + (library.catalog.preferences.randomOrder() ? "On" : "Off");
        if (id == 4) return list.filterLabel();
        if (id == 5) return "Sort: " + list.sortLabel();
        if (id == 7) return library.playback != null && library.playback.paused() ? "Resume" : "Pause";
        return "";
    }
    public boolean available(int id) {
        if (id >= 20) return true;
        if (id == 0) return !editingNickname && library.hasPrevious();
        if (id == 18) return editingNickname || list.selected != null;
        if (editingNickname && id != 3) return false;
        if (id == 1 || id == 2) return !library.catalog.preferences.vanilla();
        if (id == 15) return !library.catalog.preferences.vanilla() && !library.catalog.preferences.activePlaylist().equals("Default");
        if (id == 6) return list.selected != null && library.playback != null;
        if (id == 8) return library.custom();
        if (id == 7) return library.playback != null && library.playback.current() != null;
        if (id == 9) return library.playback != null && library.catalog.hasEnabled();
        return true;
    }
    public boolean click(double x,double y,Canvas canvas) {
        if(settingsOpen)return true;
        if(layout.volume.contains(x,y)) { draggingVolume=true; drag(x,y,canvas); return true; }
        PlayerLayout.Box b=layout.library;
        if(x>=b.x+b.w-8&&x<b.x+b.w&&y>=b.y&&y<b.y+b.h&&rows().size()>visible()) {
            draggingScroll=true; int thumb=thumbHeight(),top=thumbTop();
            scrollGrab=y>=top&&y<top+thumb?(int)y-top:thumb/2;
            drag(x,y,canvas);return true;
        }
        if(editingNickname||!b.contains(x,y)) { clickedTrack=null;return false; }
        List<Track> rows=rows();
        int index=list.offset+(int)((y-b.y)/layout.rowHeight);
        if(index>=rows.size()) { clickedTrack=null;return false; }
        Track track=rows.get(index); list.selected=track;
        if(x<b.x+23) { clickedTrack=null;library.toggle(track); }
        else {
            long now=System.nanoTime();
            if(track.id.equals(clickedTrack)&&now-clickedAt<=250000000L) { clickedTrack=null;library.play(track); }
            else { clickedTrack=track.id;clickedAt=now; }
        }
        return true;
    }
    public int thumbHeight() { return Math.max(12,layout.library.h*visible()/Math.max(1,rows().size())); }
    public int thumbTop() { return layout.library.y+(layout.library.h-thumbHeight())*list.offset/Math.max(1,rows().size()-visible()); }
    public boolean drag(double x,double y,Canvas c) {
        if(draggingVolume) { c.musicVolume((float)Math.max(0,Math.min(1,(x-layout.volume.x)/(layout.volume.w-1))),false);return true; }
        if(!draggingScroll)return false;
        int travel=layout.library.h-thumbHeight();
        list.offset=travel<=0?0:(int)Math.round((y-layout.library.y-scrollGrab)*Math.max(0,rows().size()-visible())/travel);
        list.clamp(visible());return true;
    }
    public void release(Canvas c) { if(draggingVolume)c.musicVolume(c.musicVolume(),true);draggingVolume=false;draggingScroll=false; }
    public void scroll(int delta) { if(settingsOpen||editingNickname)return;clickedTrack=null;rows();list.scroll(delta,visible()); }
    public void moveSelection(int delta) {
        if(settingsOpen||editingNickname)return;
        List<Track> tracks=rows();if(tracks.isEmpty())return;
        int index=list.selected==null?-1:tracks.indexOf(list.selected);
        index=Math.max(0,Math.min(tracks.size()-1,index+delta));list.selected=tracks.get(index);
        if(index<list.offset)list.offset=index;
        if(index>=list.offset+visible())list.offset=index-visible()+1;
    }
    public int nextFocus() {
        for(int n=0;n<25;n++) { focusControl=(focusControl+1)%25;if(shown(focusControl)&&available(focusControl))return focusControl; }
        return -1;
    }
    public void draw(Canvas c,int x,int y) { renderer.draw(this,c,x,y); }
    public void overlay(Canvas c,int x,int y) { renderer.overlay(this,c,x,y); }
    public void button(Canvas c,int id,boolean hover,boolean focus) { renderer.button(this,c,id,hover,focus); }
    public Track playing() { return library.playback==null?null:library.playback.current(); }
    public boolean paused() { return library.playback!=null&&library.playback.paused(); }
}
