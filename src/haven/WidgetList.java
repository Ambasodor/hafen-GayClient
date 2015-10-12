package haven;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;

public class WidgetList<T extends Widget> extends ListWidget<T> {
    public static final IBox BOX = new IBox("gfx/hud/box", "tl", "tr", "bl", "br", "extvl", "extvr", "extht", "exthb");
    public static final Coord BTLOFF = BOX.btloff();
    private final List<T> list = new LinkedList<>();
    private final Scrollbar sb;
    private final Coord itemsz;
    private final int h;
    private Color bgcolor = new Color(0, 0, 0, 96);
    private T over;

    public WidgetList(Coord itemsz, int h) {
	super(itemsz, itemsz.y);
	this.itemsz = itemsz;
	this.h = h;
	sz = BOX.bisz().add(itemsz.x, h * itemsz.y);
	sb = add(new Scrollbar(sz.y, 0, 20), sz.x, 0);
	pack();
    }

    public T additem(T item) {
	list.add(item);
	add(item, BTLOFF.add(0, (listitems() - 1) * itemsz.y));
	return item;
    }

    protected void drawbg(GOut g) {
	if(bgcolor != null) {
	    g.chcolor(bgcolor);
	    g.frect(Coord.z, sz);
	    g.chcolor();
	}
	BOX.draw(g, Coord.z, sz);
    }

    protected void drawsel(GOut g, Color color) {
	g.chcolor(color);
	g.frect(Coord.z, g.sz);
	g.chcolor();
    }

    @Override
    public void draw(GOut g) {
	drawbg(g);

	int n = listitems();
	for(int i = 0; i < h; i++) {
	    int idx = i + sb.val;
	    if(idx >= n)
		break;
	    T item = listitem(idx);
	    GOut ig = g.reclip(BTLOFF.add(0, itemsz.y * i), itemsz);
	    if(item == sel) {
		drawsel(ig, Listbox.selc);
	    } else if(item == over) {
		drawsel(ig, Listbox.overc);
	    }
	    drawitem(ig, item, idx);
	}

	sb.max = n - h;
	sb.draw(g.reclip(xlate(sb.c, true), sb.sz));
    }

    @Override
    protected T listitem(int idx) {
	return list.get(idx);
    }

    @Override
    protected int listitems() {
	return list.size();
    }

    @Override
    protected void drawitem(GOut g, T item, int i) {
	item.draw(g);
    }

    public T itemat(Coord c) {
	int idx = (c.y / itemsz.y) + sb.val;
	if(idx >= listitems())
	    return (null);
	return (listitem(idx));
    }

    protected void itemclick(T item, int button) {
	if(button == 1)
	    change(item);
    }

    @Override
    public boolean mousedown(Coord c, int button) {
	if(c.x < sb.c.x) {
	    c.y += sb.val * itemh;
	}
	if(super.mousedown(c, button))
	    return (true);
	T item = itemat(c);
	if((item == null) && (button == 1))
	    change(null);
	else if(item != null)
	    itemclick(item, button);
	return (true);
    }

    @Override
    public void mousemove(Coord c) {
	super.mousemove(c);
	if(c.isect(Coord.z, sz)) {
	    over = itemat(c);
	} else {
	    over = null;
	}
    }

    @Override
    public boolean mousewheel(Coord c, int amount) {
	sb.ch(amount);
	return (true);
    }
}