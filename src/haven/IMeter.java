/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import java.awt.Color;
import java.awt.font.TextAttribute;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static haven.Utils.*;

public class IMeter extends LayerMeter {
    public static final Coord off = UI.scale(22, 7);
    public static final Coord fsz = UI.scale(101, 24);
    static Coord msz = UI.scale(75, 10);
    public final Indir<Resource> bg;
    
    private static final Pattern hppat = Pattern.compile("Health: (\\d+)/(\\d+)/(\\d+)/?(\\d+)?");
    private static final Pattern stampat = Pattern.compile("Stamina: (\\d+)");
    private static final Pattern energypat = Pattern.compile("Energy: (\\d+)");
    private static final RichText.Foundry fnd = new RichText.Foundry(TextAttribute.FAMILY, "Dialog", TextAttribute.SIZE, UI.scale(10));
    Text meterinfo = null;
    @RName("im")
    public static class $_ implements Factory {
	public Widget create(UI ui, Object[] args) {
	    Indir<Resource> bg = ui.sess.getres((Integer)args[0]);
	    List<Meter> meters = decmeters(args, 1);
	    return(new IMeter(bg, meters));
	}
    }

    public IMeter(Indir<Resource> bg, List<Meter> meters) {
	super(fsz);
	this.bg = bg;
	set(meters);
    }
    public static float getpreff(String prefname, float def) {
	try {
	    return (prefs().getFloat(prefname, def));
	} catch (SecurityException e) {
	    return (def);
	}
    }
    protected float scale = getpreff("scale-", 1f);
    
    protected void drawMeters(GOut g) {
	for (Meter m : meters) {
	    int w = msz.x;
	    w = (int) ((w * m.a) / 100);
	    g.chcolor(m.c);
	    g.frect(off.mul(this.scale), Coord.of(w, msz.y).mul(this.scale));
	}
    }
    public void draw(GOut g) {
	try {
	    Tex bg = this.bg.get().flayer(Resource.imgc).tex();
	    g.chcolor(0, 0, 0, 255);
	    g.frect(off, msz);
	    g.chcolor();
	    drawMeters(g);
	    g.chcolor();
	    g.image(bg, Coord.z);
	    if (meterinfo != null)
		g.aimage(meterinfo.tex(), sz.div(2).add((int) (UI.scale(10) * this.scale), (int) (-1 * this.scale)), 0.5, 0.5);
	} catch(Loading l) {
	}
    }
    public void uimsg(String msg, Object... args) {
	if (msg == "set") {
	    List<Meter> meters = new LinkedList<>();
	    for (int i = 0; i < args.length; i += 2)
		meters.add(new Meter((Integer) args[i + 1], (Color) args[i]));
	    this.meters = meters;
	    
	} else {
	    super.uimsg(msg, args);
	    if (msg.equals("tip")) {
		final String tt = (String) args[0];
		Matcher matcher = hppat.matcher(tt);
		String meterinfo = null;
		if (matcher.find()) {
		    if (matcher.group(4) != null) {
			meterinfo = matcher.group(1);
		    } else {
			ui.sess.details.shp = Integer.parseInt(matcher.group(1));
			ui.sess.details.hhp = Integer.parseInt(matcher.group(2));
			ui.sess.details.mhp = Integer.parseInt(matcher.group(3));
			if (ui.sess.details.shp < ui.sess.details.hhp && ui.sess.details.hhp < ui.sess.details.mhp)
			    meterinfo = ui.sess.details.shp + "/" + ui.sess.details.hhp + "/" + ui.sess.details.mhp;
			else if ((ui.sess.details.shp < ui.sess.details.hhp && ui.sess.details.hhp == ui.sess.details.mhp) || (ui.sess.details.shp == ui.sess.details.hhp && ui.sess.details.hhp < ui.sess.details.mhp))
			    meterinfo = ui.sess.details.shp + "/" + ui.sess.details.mhp;
			else if (ui.sess.details.shp == ui.sess.details.hhp && ui.sess.details.hhp == ui.sess.details.mhp)
			    meterinfo = ui.sess.details.mhp + "";
		    }
		} else {
		    matcher = stampat.matcher(tt);
		    if (matcher.find()) {
			ui.sess.details.stam = Integer.parseInt(matcher.group(1));
			meterinfo = ui.sess.details.stam + "%";
		    } else {
			matcher = energypat.matcher(tt);
			if (matcher.find()) {
			    ui.sess.details.energy = Integer.parseInt(matcher.group(1));
			    meterinfo = ui.sess.details.energy + "%";
			} else {
			    meterinfo = tt;
			    if (meterinfo.contains("ow")) {
				meterinfo = tt.split(" ")[2];
			    } else {
				meterinfo = tt.split(" ")[1];
			    }
			    if (meterinfo.contains("/")) {
				String[] hps = meterinfo.split("/");
				meterinfo = hps[0] + "/" + hps[hps.length - 1];
			    }
			}
		    }
		}
		if (meterinfo == null)
		    meterinfo = tt;
		updatemeterinfo(meterinfo);
	    }
	}
    }
    protected void updatemeterinfo(String str) {
	if (str == null || str.isEmpty()) return;
	Text meterinfo = this.meterinfo;
	if (meterinfo == null || !meterinfo.text.equals(str)) {
	    this.meterinfo = create(str, strokeImg(fnd.render(str, -1, TextAttribute.SIZE, UI.scale(10))));
	}
    }
    public static BufferedImage strokeImg(Text text) {
	return (strokeImg(text.img, 1, 1, Color.BLACK));
    }
    public static BufferedImage strokeImg(BufferedImage img, int grad, int brad, Color color) {
	return (rasterimg(blurmask2(img.getRaster(), grad, brad, color)));
    }
    public static WritableRaster blurmask(Raster img, int grad, int brad, Color col) {
	Coord marg = new Coord(grad + brad, grad + brad), sz = imgsz(img).add(marg.mul(2));
	return (alphadraw(imgraster(sz), imgblur(imggrow(copyband(alpharaster(sz), 0, marg, img, 3), grad), brad, brad), Coord.z, col));
    }
    public static WritableRaster copyband(WritableRaster dst, int dband, Coord doff, Raster src, int sband, Coord soff, Coord sz) {
	dst.setSamples(doff.x, doff.y, sz.x, sz.y, dband, src.getSamples(soff.x, soff.y, sz.x, sz.y, sband, (int[]) null));
	return (dst);
    }
    
    public static WritableRaster copyband(WritableRaster dst, int dband, Coord doff, Raster src, int sband) {
	return (copyband(dst, dband, doff, src, sband, Coord.z, imgsz(src)));
    }
    
    public static WritableRaster copyband(WritableRaster dst, int dband, Raster src, int sband) {
	return (copyband(dst, dband, Coord.z, src, sband));
    }
    public static WritableRaster imggrow(WritableRaster img, int rad) {
	int h = img.getHeight(), w = img.getWidth();
	int[] buf = new int[w * h];
	int o = 0;
	for (int y = 0; y < h; y++) {
	    for (int x = 0; x < w; x++) {
		int m = 0;
		int u = Math.max(0, y - rad), b = Math.min(h - 1, y + rad);
		int l = Math.max(0, x - rad), r = Math.min(w - 1, x + rad);
		for (int y2 = u; y2 <= b; y2++) {
		    for (int x2 = l; x2 <= r; x2++) {
			m = Math.max(m, img.getSample(x2, y2, 0));
		    }
		}
		buf[o++] = m;
	    }
	}
	img.setSamples(0, 0, w, h, 0, buf);
	return (img);
    }
    public static Coord imgsz(BufferedImage img) {
	return (new Coord(img.getWidth(), img.getHeight()));
    }
    
    public static Coord imgsz(Raster img) {
	return (new Coord(img.getWidth(), img.getHeight()));
    }
    
    public static WritableRaster byteraster(Coord sz, int bands) {
	return (Raster.createInterleavedRaster(DataBuffer.TYPE_BYTE, sz.x, sz.y, bands, null));
    }
    
    public static WritableRaster alpharaster(Coord sz) {
	return (byteraster(sz, 1));
    }
    
    public static WritableRaster imgraster(Coord sz) {
	return (byteraster(sz, 4));
    }
    public static WritableRaster imgblur(WritableRaster img, int rad, double var) {
	int h = img.getHeight(), w = img.getWidth();
	double[] gk = new double[(rad * 2) + 1];
	for (int i = 0; i <= rad; i++)
	    gk[rad + i] = gk[rad - i] = Math.exp(-0.5 * Math.pow(i / var, 2.0));
	double s = 0;
	for (double cw : gk) s += cw;
	s = 1.0 / s;
	for (int i = 0; i <= rad * 2; i++)
	    gk[i] *= s;
	int[] buf = new int[w * h];
	for (int band = 0; band < img.getNumBands(); band++) {
	    int o;
	    o = 0;
	    for (int y = 0; y < h; y++) {
		for (int x = 0; x < w; x++) {
		    double v = 0;
		    int l = Math.max(0, x - rad), r = Math.min(w - 1, x + rad);
		    for (int x2 = l, ks = l - (x - rad); x2 <= r; x2++, ks++)
			v += img.getSample(x2, y, band) * gk[ks];
		    buf[o++] = (int) v;
		}
	    }
	    img.setSamples(0, 0, w, h, band, buf);
	    o = 0;
	    for (int y = 0; y < h; y++) {
		for (int x = 0; x < w; x++) {
		    double v = 0;
		    int u = Math.max(0, y - rad), b = Math.min(h - 1, y + rad);
		    for (int y2 = u, ks = u - (y - rad); y2 <= b; y2++, ks++)
			v += img.getSample(x, y2, band) * gk[ks];
		    buf[o++] = (int) v;
		}
	    }
	    img.setSamples(0, 0, w, h, band, buf);
	}
	return (img);
    }
    
    public static WritableRaster alphadraw(WritableRaster dst, Raster alpha, Coord ul, Color col) {
	int r = col.getRed(), g = col.getGreen(), b = col.getBlue(), ba = col.getAlpha();
	int w = alpha.getWidth(), h = alpha.getHeight();
	for (int y = 0; y < h; y++) {
	    for (int x = 0; x < w; x++) {
		int a = (alpha.getSample(x, y, 0) * ba) / 255;
		int dx = x + ul.x, dy = y + ul.y;
		dst.setSample(dx, dy, 0, ((r * a) + (dst.getSample(dx, dy, 0) * (255 - a))) / 255);
		dst.setSample(dx, dy, 1, ((g * a) + (dst.getSample(dx, dy, 1) * (255 - a))) / 255);
		dst.setSample(dx, dy, 2, ((b * a) + (dst.getSample(dx, dy, 2) * (255 - a))) / 255);
		dst.setSample(dx, dy, 3, Math.max((ba * a) / 255, dst.getSample(dx, dy, 3)));
	    }
	}
	return (dst);
    }
    
    public static WritableRaster blurmask2(Raster img, int grad, int brad, Color col) {
	return (alphablit(blurmask(img, grad, brad, col), img, new Coord(grad + brad, grad + brad)));
    }
    public static WritableRaster alphablit(WritableRaster dst, Raster src, Coord off) {
	int w = src.getWidth(), h = src.getHeight();
	for (int y = 0; y < h; y++) {
	    for (int x = 0; x < w; x++) {
		int a = src.getSample(x, y, 3);
		int dx = x + off.x, dy = y + off.y;
		dst.setSample(dx, dy, 0, ((src.getSample(x, y, 0) * a) + (dst.getSample(dx, dy, 0) * (255 - a))) / 255);
		dst.setSample(dx, dy, 1, ((src.getSample(x, y, 1) * a) + (dst.getSample(dx, dy, 1) * (255 - a))) / 255);
		dst.setSample(dx, dy, 2, ((src.getSample(x, y, 2) * a) + (dst.getSample(dx, dy, 2) * (255 - a))) / 255);
		dst.setSample(dx, dy, 3, Math.max(src.getSample(x, y, 3), dst.getSample(dx, dy, 3)));
	    }
	}
	return (dst);
    }
    public static BufferedImage rasterimg(WritableRaster img) {
	return (new BufferedImage(TexI.glcm, img, false, null));
    }
    public static Text create(String text, BufferedImage img) {
	return (new Text(text, img));
    }
}
