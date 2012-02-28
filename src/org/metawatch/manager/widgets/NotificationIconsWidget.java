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

		synchronized (LCDNotification.iconNotifications) {
			for (LCDNotification n : LCDNotification.iconNotifications) {
				// They're already scaled to (mostly) 13 pixels high; width varies
				int w = n.icon.getWidth();
				int h = n.icon.getHeight();
				int iy = y + 5 - h/2;
				canvas.drawBitmap(n.icon, null, new Rect(x, iy, x+w, iy+h), null);
				x += w + 1;
				if (x > 96) break;
			}
		}



		return widget;
	}
}
