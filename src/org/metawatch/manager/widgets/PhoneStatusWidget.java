package org.metawatch.manager.widgets;

import java.util.ArrayList;
import java.util.Map;

import org.metawatch.manager.FontCache;
import org.metawatch.manager.Monitors;
import org.metawatch.manager.Utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelXorXfermode;
import android.graphics.Paint.Align;
import android.text.TextPaint;

public class PhoneStatusWidget implements InternalWidget {

	public final static String id_0 = "phoneStatus_24_32";
	final static String desc_0 = "Phone Battery Status (24x32)";
	public final static String id_1 = "phoneStatus_24_11";
	final static String desc_1 = "Phone Status (24x11)";
	
	private Context context;
	private TextPaint paintSmall;
	private Paint paintXor;
		
	public void init(Context context, ArrayList<CharSequence> widgetIds) {
		this.context = context;
		
		paintSmall = new TextPaint();
		paintSmall.setColor(Color.BLACK);
		paintSmall.setTextSize(FontCache.instance(context).Small.size);
		paintSmall.setTypeface(FontCache.instance(context).Small.face);
		paintSmall.setTextAlign(Align.CENTER);

		paintXor = new Paint();
		paintXor.setXfermode(new PixelXorXfermode(Color.BLACK));
	}

	public void shutdown() {
		paintSmall = null;
	}

	public void refresh(ArrayList<CharSequence> widgetIds) {
	}

	public void get(ArrayList<CharSequence> widgetIds, Map<String,WidgetData> result) {

		if(widgetIds == null || widgetIds.contains(id_0)) {		
			result.put(id_0, GenWidget(id_0));
		}
		if(widgetIds == null || widgetIds.contains(id_1)) {
			result.put(id_1, GenWidget(id_1));
		}
	}
	
	private InternalWidget.WidgetData GenWidget(String widget_id) {
		InternalWidget.WidgetData widget = new InternalWidget.WidgetData();

		widget.id = widget_id;
		int level = Monitors.BatteryData.level;
		widget.priority = level==-1 ? 0 : 1;

		if (widget_id == id_0) {
			widget.description = desc_0;
			widget.width = 24;
			widget.height = 32;
	
			Bitmap icon = Utils.loadBitmapFromAssets(context, "idle_phone_status.bmp");

			String count = level==-1 ? "-" : level+"%";

			widget.bitmap = Bitmap.createBitmap(widget.width, widget.height, Bitmap.Config.RGB_565);
			Canvas canvas = new Canvas(widget.bitmap);
			canvas.drawColor(Color.WHITE);
		
			canvas.drawBitmap(icon, 0, 3, null);
			canvas.drawText(count, 12, 29,  paintSmall);
	
			if(level>-1)
				canvas.drawRect(13, 8 + ((100-level)/10), 19, 18, paintSmall);
		} else {
			widget.description = desc_1;
			widget.width = 24;
			widget.height = 11;

			Bitmap icon = Utils.loadBitmapFromAssets(context, "idle_phone_battery_11.png");
			widget.bitmap = Bitmap.createBitmap(widget.width, widget.height, Bitmap.Config.RGB_565);
			Canvas canvas = new Canvas(widget.bitmap);
			canvas.drawColor(Color.WHITE);

			canvas.drawBitmap(icon, 18, 0, null);
			if(level>-1)
				canvas.drawRect(19, 2 + ((100-level)/8), 23, 10, paintSmall);
			if (Monitors.BatteryData.charging) {
				Bitmap spark = Utils.loadBitmapFromAssets(context, "idle_phone_battery_charge_11.png");
				canvas.drawBitmap(spark, 18, 0, paintXor);
			}

			int phoneBars = Monitors.SignalData.phoneBars;
			if (phoneBars >= 1) canvas.drawLine(10, 8, 10, 11, paintSmall);
			if (phoneBars >= 2) canvas.drawLine(12, 6, 12, 11, paintSmall);
			if (phoneBars >= 3) canvas.drawLine(14, 4, 14, 11, paintSmall);
			if (phoneBars >= 4) canvas.drawLine(16, 2, 16, 11, paintSmall);
			if (Monitors.SignalData.roaming) canvas.drawText("R", 12, 5, paintSmall);

			if (Monitors.SignalData.wifiBars > 0) {
				String s;
				if (Monitors.SignalData.wifiBars >= 4) s = "idle_phone_wifi_4.png";
				else if (Monitors.SignalData.wifiBars >= 3) s = "idle_phone_wifi_3.png";
				else if (Monitors.SignalData.wifiBars >= 2) s = "idle_phone_wifi_2.png";
				else s = "idle_phone_wifi_1.png";
				canvas.drawBitmap(Utils.loadBitmapFromAssets(context, s), 0, 0, null);
			} else if (! Monitors.SignalData.phoneDataType.isEmpty()) {
				canvas.drawBitmap(Utils.loadBitmapFromAssets(context, "idle_phone_data.png"), 1, 0, null);
				canvas.drawText(Monitors.SignalData.phoneDataType, 5, 11, paintSmall);
			}
		}
		
		return widget;
	}
}
	