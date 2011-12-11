package org.metawatch.manager;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

public class NotificationIconShrinker
{
	static double luminance(int color)
	{
		return Color.alpha(color) * (
				0.299*Color.red(color) +
				0.587*Color.green(color) +
				0.114*Color.blue(color));
	}

	public static Bitmap shrink(Resources r, int iconId)
	{
		// Get an icon appropriate for small screens
		Drawable d = r.getDrawable(iconId);
		if (d == null) return null;

		// Coerce to bitmap
		Bitmap in;
		boolean inIsOurs;
		if (d instanceof BitmapDrawable) {
			in = ((BitmapDrawable)d).getBitmap();
			inIsOurs = false;
		} else {
			int iw = d.getIntrinsicWidth();
			int ih = d.getIntrinsicHeight();
			if (iw <= 0) { iw = 18; ih = 18; }
			in = Bitmap.createBitmap(iw, ih, Bitmap.Config.ARGB_8888);
			inIsOurs = true;
			d.setBounds(0,0,iw,ih);
			d.draw(new Canvas(in));
		}

		// Scale it to 18 pixels high
		int h = 18;
		int w = (int)Math.round((((double)in.getWidth())/in.getHeight())*h);
		Bitmap out = Bitmap.createScaledBitmap(in, w, h, true);
		if (out != in && inIsOurs) in.recycle();

		// Threshold to monochrome
		double maxLum = 0;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				maxLum = Math.max(maxLum, luminance(out.getPixel(x,y)));
			}
		}
		double midLum = maxLum/2;
		int minX = w, maxX = 0;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				int c = out.getPixel(x, y);
				if (luminance(c) >= midLum) {
					out.setPixel(x, y, Color.BLACK);
					minX = Math.min(minX, x);
					maxX = Math.max(maxX, x);
				} else {
					out.setPixel(x, y, Color.WHITE);
				}
			}
		}

		// Some icons come with a lot of empty/dim space, get rid of it
		int usefulWidth = maxX - minX;
		if (usefulWidth >= 5) {
			Bitmap cropped = Bitmap.createBitmap(out, minX, 0, (maxX-minX)+1, h);
			if (cropped != out) {
				out.recycle();
				out = cropped;
			}
		}
		return out;
	}
}
