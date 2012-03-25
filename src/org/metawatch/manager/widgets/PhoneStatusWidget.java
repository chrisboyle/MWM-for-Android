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
	
	public final static String id_1 = "phoneStatus_16_16";
	final static String desc_1 = "Phone Battery Status (16x16)";
	
	public final static String id_2 = "phoneStatus_mini";
	final static String desc_2 = "Phone Status (24x13)";
	
	private Context context;
	private TextPaint paintSmall;
	private Paint paintXor;
	private TextPaint paintSmallNumerals;
		
	public void init(Context context, ArrayList<CharSequence> widgetIds) {
		this.context = context;
		
		paintSmall = new TextPaint();
		paintSmall.setColor(Color.BLACK);
		paintSmall.setTextSize(FontCache.instance(context).Small.size);
		paintSmall.setTypeface(FontCache.instance(context).Small.face);
		paintSmall.setTextAlign(Align.CENTER);
		
		paintSmallNumerals = new TextPaint();
		paintSmallNumerals.setColor(Color.BLACK);
		paintSmallNumerals.setTextSize(FontCache.instance(context).SmallNumerals.size);
		paintSmallNumerals.setTypeface(FontCache.instance(context).SmallNumerals.face);
		paintSmallNumerals.setTextAlign(Align.CENTER);

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
		
		if(widgetIds == null || widgetIds.contains(id_2)) {
			result.put(id_2, GenWidget(id_2));
		}
	}
	
	private InternalWidget.WidgetData GenWidget(String widget_id) {
		InternalWidget.WidgetData widget = new InternalWidget.WidgetData();
		
		String iconFile="";
		if( widget_id == id_0 ) {
			widget.id = id_0;
			widget.description = desc_0;
			widget.width = 24;
			widget.height = 32;
			iconFile = "idle_phone_status.bmp";
		}
		else if( widget_id == id_1 ) {
			widget.id = id_1;
			widget.description = desc_1;
			widget.width = 16;
			widget.height = 16; 
			iconFile = "idle_phone_status_10.bmp";
		}
		else if( widget_id == id_2 ) {
			widget.id = id_2;
			widget.description = desc_2;
			widget.width = 24;
			widget.height = 13;
			iconFile = "idle_phone_battery_11.png";
		}
		
		Bitmap icon = Utils.loadBitmapFromAssets(context, iconFile);

		int level = Monitors.BatteryData.level;
		String count = level==-1 ? "-" : level+"%";

		widget.priority = level==-1 ? 0 : 1;		
		widget.bitmap = Bitmap.createBitmap(widget.width, widget.height, Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(widget.bitmap);
		canvas.drawColor(Color.WHITE);
		
		if (widget_id == id_0 ) {
			canvas.drawBitmap(icon, 0, 3, null);
			canvas.drawText(count, 12, 30,  paintSmall);
		
			if(level>-1)
				canvas.drawRect(13, 8 + ((100-level)/10), 19, 18, paintSmall);
		}
		else if (widget_id == id_1 ) {
			canvas.drawBitmap(icon, 2, 0, null);
			canvas.drawText(count, 8, 15,  paintSmallNumerals);
		
			if(level>-1)
				canvas.drawRect(9, 1 + ((100-level)/12), 12, 8, paintSmall);	
		}
		else if( widget_id == id_2 ) {
			widget.priority = 10;
			canvas.drawBitmap(icon, 17, 1, null);
			if(level>-1)
				canvas.drawRect(18, Math.round(3 + ((100-level)*0.08d)), 22, 11, paintSmall);
			if (Monitors.BatteryData.charging) {
				Bitmap spark = Utils.loadBitmapFromAssets(context, "idle_phone_battery_charge_11.png");
				canvas.drawBitmap(spark, 17, 1, paintXor);
			}

			int phoneBars = Monitors.SignalData.phoneBars;
			if (phoneBars >= 1) canvas.drawLine( 9, 10,  9, 12, paintSmall);
			else                canvas.drawLine( 9, 11,  9, 12, paintSmall);
			if (phoneBars >= 2) canvas.drawLine(11,  8, 11, 12, paintSmall);
			else                canvas.drawLine(11, 11, 11, 12, paintSmall);
			if (phoneBars >= 3) canvas.drawLine(13,  6, 13, 12, paintSmall);
			else                canvas.drawLine(13, 11, 13, 12, paintSmall);
			if (phoneBars >= 4) canvas.drawLine(15,  4, 15, 12, paintSmall);
			else                canvas.drawLine(15, 11, 15, 12, paintSmall);

			paintSmall.setTextAlign(Align.RIGHT);
			if (Monitors.SignalData.wifiBars > 0) {
				String s;
				if (Monitors.SignalData.wifiBars >= 4) s = "idle_phone_wifi_4.png";
				else if (Monitors.SignalData.wifiBars >= 3) s = "idle_phone_wifi_3.png";
				else if (Monitors.SignalData.wifiBars >= 2) s = "idle_phone_wifi_2.png";
				else s = "idle_phone_wifi_1.png";
				canvas.drawBitmap(Utils.loadBitmapFromAssets(context, s), 1, 1, null);
				// overlap, but still clear
				if (Monitors.SignalData.roaming) canvas.drawText("R", 14, 6, paintSmall);
			} else if (! Monitors.SignalData.phoneDataType.isEmpty()) {
				canvas.drawText(Monitors.SignalData.phoneDataType, 14, 6, paintSmall);
				if (Monitors.SignalData.roaming) canvas.drawText("R", 9, 12, paintSmall);
			}
			paintSmall.setTextAlign(Align.CENTER);
		}
		
		return widget;
	}
}
	
