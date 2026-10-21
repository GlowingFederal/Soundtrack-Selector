package com.glowingfederal.soundtrackselector.domain;

/** Logical GUI coordinates, shared by native input and rendering. Recomputed only on resize/style changes. */
public final class PlayerLayout {
    public static final class Box {
        public final int x, y, w, h;
        public Box(int x, int y, int w, int h) { this.x=x; this.y=y; this.w=w; this.h=h; }
        public boolean contains(double px, double py) { return px >= x && py >= y && px < x+w && py < y+h; }
    }
    public final Box[] buttons = new Box[25];
    public final Box search, name, library, libraryPanel, sidebar, nowPlaying, footer, volume, settings;
    public final int rowHeight;
    public PlayerLayout(int width, int height, boolean modern) {
        int margin = modern ? 8 : 10;
        int side = width >= 520 ? (modern ? 106 : 116) : 0;
        int extra = modern && width >= 820 && height >= 340 ? Math.min(210, width/4) : 0;
        int left = margin + (side == 0 ? 0 : side + 6);
        int right = width - margin - (extra == 0 ? 0 : extra + 6);
        int span = right-left;
        sidebar = new Box(margin, 36, side, height-124);
        nowPlaying = new Box(right+6, 36, extra, height-124);
        footer = new Box(margin, height-82, width-2*margin, 74);
        volume = new Box(width-margin-91, height-65, 80, 12);
        int fieldWidth = Math.max(44, (span-116)/2);
        buttons[13] = new Box(left, 40, fieldWidth, 20);
        name = new Box(left+fieldWidth+4, 40, fieldWidth, 20);
        buttons[14] = new Box(left+2*fieldWidth+8, 40, 48, 20);
        buttons[15] = new Box(left+2*fieldWidth+60, 40, span-2*fieldWidth-60, 20);
        search = new Box(left, 66, span-64, 20);
        buttons[19] = new Box(right-60, 66, 60, 20);
        int x=left;
        if (side == 0) { buttons[4]=new Box(x, 92, 77, 20); x+=81; }
        int sortWidth = Math.min(82, Math.max(64, span-(side == 0 ? 81 : 0)-152));
        buttons[5]=new Box(x,92,sortWidth,20); x+=sortWidth+4;
        buttons[1]=new Box(x,92,42,20); x+=46;
        buttons[2]=new Box(x,92,42,20); x+=46;
        buttons[18]=new Box(x,92,Math.max(46,right-x),20);
        if (side != 0) {
            for (int i=0;i<3;i++) buttons[10+i]=new Box(margin+8,62+i*(modern?26:30),side-16,24);
        }
        buttons[20]=new Box(width-margin-88,8,88,20);
        int[] transport={0,6,7,8,9,16,17};
        int bw=width>=620?42:26;
        for (int i=0;i<transport.length;i++) buttons[transport[i]]=new Box(margin+6+i*(bw+4),height-32,bw,20);
        buttons[3]=new Box(width-margin-48,height-32,42,20);
        rowHeight=modern?28:30;
        library=new Box(left,128,span,Math.max(rowHeight,((height-218)/rowHeight)*rowHeight));
        libraryPanel=new Box(left-4,34,span+8,library.y+library.h-30);
        settings=new Box((width-280)/2,Math.max(32,(height-154)/2),280,154);
        for(int i=0;i<3;i++) buttons[21+i]=new Box(settings.x+14,settings.y+32+i*30,252,22);
        buttons[24]=new Box(settings.x+184,settings.y+126,82,20);
    }
}
