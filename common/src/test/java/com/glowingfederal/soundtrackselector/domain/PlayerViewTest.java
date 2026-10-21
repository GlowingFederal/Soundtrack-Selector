package com.glowingfederal.soundtrackselector.domain;

import java.io.*;
import java.util.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

/** Regression coverage for real shared actions, responsive hit regions, and playback-independent styling. */
public class PlayerViewTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private Library library;
    private PlayerView player;
    private Playback playback;
    private final Canvas canvas = new Canvas();
    @Before public void setup() throws Exception {
        library = new Library(temporary.newFolder(), new Library.Log() {
            public void info(String message) { }
            public void warning(String message,Throwable failure) { throw new AssertionError(message,failure); }
            public void debug(String message,Throwable failure) { }
        }, "test", Collections.<String>emptyList());
        playback=new Playback();library.playback=playback;
        List<Track> tracks=new ArrayList<Track>();
        for(int i=0;i<200;i++)tracks.add(new Track("minecraft:sounds/music/track"+i+".ogg","minecraft:music.game",120));
        library.catalog.replace(tracks);
        player=new PlayerView(library);player.resize(640,360);
    }
    @Test public void allThemesPreservePlaybackSelectionAndScroll() {
        library.createPlaylist("Travel");player.action(1);
        player.list.selected=player.rows().get(20);player.list.offset=15;
        player.action(6);player.action(7);
        Track current=playback.current;int starts=playback.starts,revision=library.catalog.revision();
        double elapsed=library.elapsed();
        for(int id:new int[]{21,22,21,22,23})player.action(id);
        assertSame(current,playback.current);assertSame(current,player.list.selected);
        assertEquals(15,player.list.offset);assertEquals(starts,playback.starts);
        assertEquals(revision,library.catalog.revision());assertTrue(playback.paused);
        assertEquals(elapsed,library.elapsed(),0.001);assertFalse(library.takeRefreshRequest());
    }
    @Test public void selectionMembershipAndDoubleClickAreIndependent() {
        library.createPlaylist("Travel");
        int x=player.layout.library.x,y=player.layout.library.y;
        player.click(x+65,y+10,canvas);
        assertNotNull(player.list.selected);assertEquals(0,playback.starts);
        assertFalse(library.catalog.preferences.enabled(player.list.selected));
        player.click(x+9,y+10,canvas);
        assertTrue(library.catalog.preferences.enabled(player.list.selected));assertEquals(0,playback.starts);
        player.click(x+65,y+10,canvas);player.click(x+65,y+10,canvas);
        assertEquals(1,playback.starts);
        assertSame(player.list.selected,playback.current);
    }
    @Test public void bulkActionsIncludeOffscreenRowsAndVanillaIsProtected() {
        player.action(1);assertEquals(0,library.catalog.enabledCount());
        library.createPlaylist("Travel");player.action(1);assertEquals(200,library.catalog.enabledCount());
        player.action(11);player.action(2);assertEquals(0,library.catalog.enabledCount());
        player.action(8);assertTrue(library.catalog.preferences.vanilla());assertFalse(player.available(1));
    }
    @Test public void previousUsesConfirmedPlaybackHistoryAndStopRestoresVanilla() {
        Track a=player.rows().get(0),b=player.rows().get(1);
        library.play(a);assertFalse(library.hasPrevious());library.play(b);assertTrue(library.hasPrevious());
        player.action(0);assertSame(a,playback.current);assertFalse(library.hasPrevious());
        player.action(8);assertNull(playback.current);assertTrue(library.catalog.preferences.vanilla());assertFalse(library.hasPrevious());
    }
    @Test public void scrollbarAndVolumeDragClampAndPersistOnRelease() {
        PlayerLayout.Box b=player.layout.library;
        player.click(b.x+b.w-3,b.y+2,canvas);player.drag(b.x+b.w-3,10000,canvas);player.release(canvas);
        assertEquals(player.rows().size()-player.visible(),player.list.offset);
        b=player.layout.volume;player.click(b.x,b.y,canvas);player.drag(10000,b.y,canvas);player.release(canvas);
        assertEquals(1,canvas.volume,0);assertEquals(1,canvas.saves);
    }
    @Test public void responsiveControlsNeverOverlapOrLeaveTheScreen() {
        for(boolean modern:new boolean[]{false,true})for(int[] size:new int[][]{{320,240},{426,240},{480,270},{640,360},{854,480},{960,540},{1280,720},{1920,1080},{2560,1440}}) {
            PlayerLayout l=new PlayerLayout(size[0],size[1],modern);
            assertTrue(l.library.y+l.library.h<=l.footer.y);
            for(int i=0;i<=20;i++) {
                PlayerLayout.Box a=l.buttons[i];if(a==null)continue;
                assertTrue(a.x>=0&&a.y>=0&&a.w>0&&a.x+a.w<=size[0]&&a.y+a.h<=size[1]);
                for(int j=i+1;j<=20;j++) { PlayerLayout.Box b=l.buttons[j];if(b==null)continue;
                    assertFalse("controls "+i+" / "+j+" at "+size[0],a.x<b.x+b.w&&a.x+a.w>b.x&&a.y<b.y+b.h&&a.y+a.h>b.y);
                }
            }
            assertEquals(modern&&size[0]>=820&&size[1]>=340,l.nowPlaying.w>0);
        }
    }
    @Test public void nicknameEditorRestoresSearchAndArtworkUsesKnownResourcesOnly() {
        player.query="track1";player.list.selected=player.rows().get(0);player.action(18);player.query="Journey";player.action(18);
        assertEquals("track1",player.query);assertEquals("Journey",library.catalog.preferences.displayName(player.list.selected));
        assertNull(new Track("example:sounds/music/cat.ogg","example:record.cat",60).disc);
        assertEquals("cat",new Track("minecraft:sounds/records/cat.ogg","minecraft:record.cat",60).disc);
        assertEquals(2,new Track("minecraft:sounds/music/nether/test.ogg","minecraft:music.nether",60).artwork);
    }
    @Test public void discFollowsPlaybackPauseStopAndReducedAnimations() throws Exception {
        library.play(player.rows().get(0));Thread.sleep(15);
        player.draw(canvas,0,0);assertTrue(canvas.angle>0);
        library.togglePause();player.draw(canvas,0,0);float held=canvas.angle;
        Thread.sleep(15);player.draw(canvas,0,0);assertEquals(held,canvas.angle,0);
        player.action(23);player.draw(canvas,0,0);assertEquals(0,canvas.angle,0);
        player.action(23);library.stop();player.draw(canvas,0,0);assertEquals(0,canvas.angle,0);
        assertEquals(0,canvas.clipDepth);
        assertTrue(canvas.needleInFront);
    }
    @Test public void lightInputScrollingMatchesNativeSelectionBoundaries() {
        String value="abcdefghijklmnopqrst";
        int start=GuiTheme.inputScroll(canvas,value,20,0,60);
        assertEquals(10,start);
        start=GuiTheme.inputScroll(canvas,value,10,start,60);assertEquals(0,start);
        start=GuiTheme.inputScroll(canvas,value,5,15,60);assertEquals(5,start);
        assertEquals(0,GuiTheme.inputScroll(canvas,"",0,15,60));
    }
    @Test public void appearancePersistsSeparatelyFromPlaylistData() throws Exception {
        library.appearance.modern=true;library.appearance.light=true;library.appearance.reducedMotion=true;library.appearance.save();
        File file=new File(temporary.getRoot(),"unused");
        // Locate the Library's unique fixture directory, never a game configuration.
        for(File dir:temporary.getRoot().listFiles())if(dir.isDirectory())file=new File(dir,"soundtrackselector.appearance.properties");
        long deadline=System.nanoTime()+3000000000L;
        while(!file.isFile()&&System.nanoTime()<deadline)Thread.sleep(10);
        assertTrue(file.isFile());
        Appearance loaded=new Appearance(file.getParentFile(),library.log());
        assertTrue(loaded.modern&&loaded.light&&loaded.reducedMotion);
        assertTrue(library.catalog.preferences.vanilla());
    }
    private final class Playback implements Library.Playback {
        Track current;int starts;boolean paused;
        public void play(Track track) { current=track;paused=false;starts++;library.played(track); }
        public void stop() { current=null;paused=false;library.relinquished(); }
        public Track current() { return current; }
        public void togglePause() { paused=!paused; }
        public boolean paused() { return paused; }
        public void release() { stop(); }
    }
    private static final class Canvas implements PlayerView.Canvas {
        float volume,angle;int saves,clipDepth;
        boolean lastWasDisc,needleInFront=true;
        public void rectangle(int a,int b,int c,int d,int color) { }
        public void text(String s,int x,int y,int w,int color) { }
        public int textWidth(String value) { return value.length()*6; }
        public void texture(String s,int x,int y,int w,int h,int u,int v,int rw,int rh,int tw,int th,float degrees) {
            if(lastWasDisc)needleInFront&=s.endsWith("/common/tonearm.png")&&degrees==0;
            lastWasDisc=s.endsWith("/common/disc.png");if(lastWasDisc)angle=degrees;
        }
        public void clip(int x,int y,int w,int h) { clipDepth++; }
        public void unclip() { clipDepth--; }
        public boolean disc(String name,int x,int y,int size) { return false; }
        public float musicVolume() { return volume; }
        public void musicVolume(float value,boolean save) { volume=value;if(save)saves++; }
    }
}
