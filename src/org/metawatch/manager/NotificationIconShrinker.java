package org.metawatch.manager;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;

public class NotificationIconShrinker
{
	static final int ICON_SIZE = 11;

	static double luminance(int color)
	{
		return Color.alpha(color) * (
				0.299*Color.red(color) +
				0.587*Color.green(color) +
				0.114*Color.blue(color));
	}

	public static double chooseThreshold(String packageName)
	{
		return (packageName.equals("com.google.android.music")
				|| packageName.equals("com.android.music")
				|| packageName.equals("com.google.android.apps.maps"))
				? 0.1 : 0.65;
	}

	public static Bitmap shrink(Resources r, int iconId, String packageName, int maxSize)
	{
		Drawable d = null;
		try {
			d = r.getDrawable(iconId);
		} catch (Exception e) {}
		if (d == null) return null;

		// Coerce to Bitmap - might already be a BitmapDrawable, but make
		// sure it's mutable and doesn't have unhelpful density info attached
		int iw = d.getIntrinsicWidth();
		int ih = d.getIntrinsicHeight();
		if (iw <= 0) { iw = maxSize; ih = maxSize; }
		Bitmap icon = Bitmap.createBitmap(iw, ih, Bitmap.Config.ARGB_8888);
		d.setBounds(0,0,iw,ih);
		d.draw(new Canvas(icon));

		// Threshold to monochrome (for LCD), and create a bounding box
		double maxLum = 0;
		iw = icon.getWidth(); ih = icon.getHeight();
		for (int y = 0; y < ih; y++) {
			for (int x = 0; x < iw; x++) {
				maxLum = Math.max(maxLum, luminance(icon.getPixel(x,y)));
			}
		}
		double thresholdLum = maxLum * chooseThreshold(packageName);
		int minX = iw, maxX = 0, minY = ih, maxY = 0;
		for (int y = 0; y < ih; y++) {
			for (int x = 0; x < iw; x++) {
				if (luminance(icon.getPixel(x,y)) >= thresholdLum) {
					minX = Math.min(minX, x);
					maxX = Math.max(maxX, x);
					minY = Math.min(minY, y);
					maxY = Math.max(maxY, y);
					icon.setPixel(x, y, Color.BLACK);
				} else {
					icon.setPixel(x, y, Color.WHITE);
				}
			}
		}

		// Crop to remove all blank space around the thresholded icon
		if (maxX-minX >= 5 && maxY-minY >= 5) {
			icon = Bitmap.createBitmap(icon, minX, minY, (maxX-minX)+1, (maxY-minY)+1);
			iw = icon.getWidth();
			ih = icon.getHeight();
		}

		// Scale it to maxSize pixels high
		int w, h;
		if (iw > ih) {
			w = maxSize;
			h = (int)Math.round((((double)ih)/iw)*w);
		} else {
			h = maxSize;
			w = (int)Math.round((((double)iw)/ih)*h);
		}
		return Bitmap.createScaledBitmap(icon, w, h, true);
	}
}
