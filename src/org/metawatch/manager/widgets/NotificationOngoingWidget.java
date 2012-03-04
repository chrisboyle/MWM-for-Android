package org.metawatch.manager.widgets;

import java.util.ArrayList;
import java.util.Map;

import org.metawatch.manager.FontCache;
import org.metawatch.manager.LCDNotification;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.text.StaticLayout;
import android.text.TextPaint;

public class NotificationOngoingWidget implements InternalWidget
{
	public final static String id_0 = "notification_ongoing";
	final static String desc_0 = "Notifications ongoing (96x51)";
	static final int TEXT_H = 6, LINE_SP = 1, LINE_H = TEXT_H + LINE_SP;

	private Context context;
	private TextPaint paintSmall;
	private TextPaint paintLarge;

	public void init(Context context, ArrayList<CharSequence> widgetIds)
	{
		this.context = context;
		paintSmall = new TextPaint();
		paintSmall.setColor(Color.BLACK);
		paintSmall.setTextSize(FontCache.instance(context).Small.size);
		paintSmall.setTypeface(FontCache.instance(context).Small.face);
		paintLarge = new TextPaint();
		paintLarge.setColor(Color.BLACK);
		paintLarge.setTextSize(FontCache.instance(context).Large.size);
		paintLarge.setTypeface(FontCache.instance(context).Large.face);
	}

	public void refresh(ArrayList<CharSequence> widgetIds)
	{
	}

	static int sum(int[] a)
	{
		int s = 0;
		for (int i : a) s += i;
		return s;
	}

	public void get(ArrayList<CharSequence> widgetIds, Map<String,WidgetData> result)
	{
		if(widgetIds != null && ! widgetIds.contains(id_0)) return;

		InternalWidget.WidgetData widget = new InternalWidget.WidgetData();

		widget.id = id_0;
		widget.description = desc_0;
		widget.width = 96;
		widget.height = 51;
		widget.priority = 1;
		widget.bitmap = Bitmap.createBitmap(96, 51, Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(widget.bitmap);
		canvas.drawColor(Color.WHITE);
		int x=0, y=0;

		// Draw any prioritised "big" notifications
		for (LCDNotification n : LCDNotification.ongoingNotifications) {
			if (! n.big) continue;
			canvas.save();
			TextPaint paint = new TextPaint(paintLarge);
			StaticLayout layout = new StaticLayout(n.text, paint, 94,
					android.text.Layout.Alignment.ALIGN_NORMAL, 0.9f, 0, false);
			canvas.translate(1, y);
			layout.draw(canvas);
			canvas.restore();
			y += layout.getHeight() + 1;
		}

		// Now the rest
		int pxRemain = 51 - y;  // TODO! pass down to here the amount of space available
		synchronized (LCDNotification.ongoingNotifications) {
			int maxW = 13;
			int[] heights = new int[LCDNotification.ongoingNotifications.size()];
			int i=0;
			TextPaint paint = new TextPaint(paintSmall);
			for (LCDNotification n : LCDNotification.ongoingNotifications) {
				if (n.big) continue;
				int h = 0;
				if (n.icon != null) {
					maxW = Math.max(maxW, n.icon.getWidth());
					h = n.icon.getHeight();
				}
				int lh = n.makeTextLayout(-1, paint);
				heights[i++] = Math.max(h,Math.min(lh, pxRemain));
			}
			int maxLines = pxRemain/LINE_H + 1;
			while (maxLines > 2 && sum(heights) > pxRemain) {
				maxLines--;
				for (i=0; i<heights.length; i++) {
					heights[i] = Math.min(heights[i], maxLines*LINE_H);
				}
			}
			i = 0;
			for (LCDNotification n : LCDNotification.ongoingNotifications) {
				if (n.big) continue;
				int ih = n.icon.getHeight();
				int th = Math.max(ih, heights[i++]);
				if (n.icon != null) {
					int w = n.icon.getWidth();
					x = 1 + maxW/2 - w/2;
					canvas.drawBitmap(n.icon, null, new Rect(x, y, x+w, y+ih), null);
				}
				canvas.save();
				canvas.translate(maxW + 2, y); //position the text
				canvas.clipRect(0, 0, 80, th);
				if (n.getTextHeight() > th) {
					int left = 3, right = n.text.length()-1, len = (left+right)/2;
					while (left < right) {
						int h = n.makeTextLayout(len, paint);
						if (h > th) {
							right = len-1;
						} else {
							if (left == len) break;
							left = len;
						}
						len = (left+right)/2;
					}
					n.makeTextLayout(len, paint);
				}
				n.drawText(canvas);
				canvas.restore();
				y += th + 1;
				if (y > 96) break;
			}
		}

		result.put(id_0, widget);
	}

	public void shutdown()
	{
		// TODO Auto-generated method stub
		
	}
}
