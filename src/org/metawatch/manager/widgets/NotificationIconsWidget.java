package org.metawatch.manager.widgets;

import java.util.List;
import java.util.Map;

import org.metawatch.manager.LCDNotification;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;

public class NotificationIconsWidget implements InternalWidget
{
	public final static String id_0 = "notification_icons_x_11";
	final static String desc_0 = "Notification icons (...x11)";
	static final int TEXT_H = 6, LINE_SP = 1, LINE_H = TEXT_H + LINE_SP;

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
		widget.priority = 1;
		widget.width = 1;
		widget.height = 1;
		synchronized (LCDNotification.iconNotifications) {
			if(LCDNotification.iconNotifications.size() == 0) {
				widget.bitmap = null;
				result.put(id_0, widget);
				return;
			}
			for (LCDNotification n : LCDNotification.iconNotifications) {
				widget.width += n.icon.getWidth() + 1;
				widget.height = Math.max(widget.height, n.icon.getHeight());
			}
			widget.bitmap = Bitmap.createBitmap(widget.width, widget.height, Bitmap.Config.RGB_565);
			Canvas canvas = new Canvas(widget.bitmap);
			canvas.drawColor(Color.WHITE);
			int x=0, y=0;

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

		result.put(id_0, widget);
	}

	public void shutdown()
	{
		// TODO Auto-generated method stub
		
	}
}
