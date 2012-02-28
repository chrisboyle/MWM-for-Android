package org.metawatch.manager.widgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;

public class NotificationsWidget implements InternalWidget
{
	public final static String id_0 = "notifications_96_32";
	final static String desc_0 = "Notifications (96x32)";

	private Context context;

	public void init(Context context, List<String> widgetIds)
	{
		this.context = context;
	}

	public void refresh(List<String> widgetIds)
	{
	}

	static int sum(int[] a)
	{
		int s = 0;
		for (int i : a) s += i;
		return s;
	}

	public void get(List<String> widgetIds, Map<String,WidgetData> result)
	{
		if(widgetIds != null && ! widgetIds.contains(id_0)) return;

		InternalWidget.WidgetData widget = new InternalWidget.WidgetData();

		widget.id = id_0;
		widget.description = desc_0;
		widget.width = 96;
		widget.height = 32;
		widget.priority = 1;
		widget.bitmap = Bitmap.createBitmap(96, 32, Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(bitmap);
		canvas.drawColor(Color.WHITE);
		int x=0, y=0;

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

		int pxRemain = 96 - y;
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

		return widget;
	}
}
