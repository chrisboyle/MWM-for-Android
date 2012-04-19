package org.metawatch.manager.apps;

import org.metawatch.manager.FontCache;
import org.metawatch.manager.MediaControl;
import org.metawatch.manager.MetaWatch;
import org.metawatch.manager.Utils;
import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.MetaWatchService.WatchType;
import org.metawatch.manager.Protocol;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;

public class MediaPlayerApp implements InternalApp {

	static AppData appData = new AppData() {{
		id = "org.metawatch.manager.apps.MediaPlayerApp";
		name = "Media Player";
		
		supportsAnalog = true;
		supportsDigital = false;
	}};
	
	public final static byte VOLUME_UP = 10;
	public final static byte VOLUME_DOWN = 11;
	public final static byte NEXT = 15;
	public final static byte PREVIOUS = 16;
	public final static byte HEADSET = 17;
	public final static byte TOGGLE = 20;
	
	public AppData getInfo() {
		return appData;
	}

	public void activate(int watchType) {
		if (Preferences.logging) Log.d(MetaWatch.TAG, "Entering media mode");
		
		MediaControl.mediaPlayerActive = true;
		
		if (watchType == WatchType.DIGITAL) {
			Protocol.enableButton(1, 0, TOGGLE, 1); // right middle - immediate
	
			Protocol.enableButton(5, 1, VOLUME_DOWN, 1); // left middle - press
			Protocol.enableButton(5, 2, PREVIOUS, 1); // left middle - hold
			Protocol.enableButton(5, 3, PREVIOUS, 1); // left middle - long hold
			
			Protocol.enableButton(6, 1, VOLUME_UP, 1); // left top - press
			Protocol.enableButton(6, 2, NEXT, 1); // left top - hold
			Protocol.enableButton(6, 3, NEXT, 1); // left top - long hold
		}
		else if (watchType == WatchType.ANALOG) {
			Protocol.enableButton(0, 1, TOGGLE, 1); // top - press
			Protocol.enableButton(2, 1, NEXT, 1); // bottom - press			
		}
	}
	
	public void deactivate(int watchType) {
		if (Preferences.logging) Log.d(MetaWatch.TAG, "Leaving media mode");
		
		MediaControl.mediaPlayerActive = false;
		
		if (watchType == WatchType.DIGITAL) {
			Protocol.disableButton(1, 0, 1);
			
			Protocol.disableButton(5, 0, 1);
			//Protocol.disableButton(5, 1, 1);
			Protocol.disableButton(5, 2, 1);
			Protocol.disableButton(5, 3, 1);
	
			Protocol.disableButton(6, 0, 1);
			//Protocol.disableButton(6, 1, 1);
			Protocol.disableButton(6, 2, 1);
			Protocol.disableButton(6, 3, 1);
		}
		else if (watchType == WatchType.ANALOG) {
			Protocol.disableButton(0, 1, 1); 
			Protocol.disableButton(2, 1, 1); 				
		}
	}
	
	public Bitmap update(Context context, int watchType) {
		
		TextPaint paintSmall = new TextPaint();
		paintSmall.setColor(Color.BLACK);
		paintSmall.setTextSize(FontCache.instance(context).Small.size);
		paintSmall.setTypeface(FontCache.instance(context).Small.face);
		
		TextPaint paintSmallOutline = new TextPaint();
		paintSmallOutline.setColor(Color.WHITE);
		paintSmallOutline.setTextSize(FontCache.instance(context).Small.size);
		paintSmallOutline.setTypeface(FontCache.instance(context).Small.face);
		
		TextPaint paintLarge = new TextPaint();
		paintLarge.setColor(Color.BLACK);
		paintLarge.setTextSize(FontCache.instance(context).Large.size);
		paintLarge.setTypeface(FontCache.instance(context).Large.face);
		
		TextPaint paintLargeOutline = new TextPaint();
		paintLargeOutline.setColor(Color.WHITE);
		paintLargeOutline.setTextSize(FontCache.instance(context).Large.size);
		paintLargeOutline.setTypeface(FontCache.instance(context).Large.face);
		

		
		if (watchType == WatchType.DIGITAL) {
			
			Bitmap bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.RGB_565);
			Canvas canvas = new Canvas(bitmap);
			canvas.drawColor(Color.WHITE);	
			
			if(MediaControl.lastTrack=="") {
				canvas.drawBitmap(Utils.loadBitmapFromAssets(context, "media_player_idle.png"), 0, 0, null);				
			}
			else {	
				canvas.drawBitmap(Utils.loadBitmapFromAssets(context, "media_player.png"), 0, 0, null);
				
				
				TextPaint tp = null;
				if( paintLarge.measureText(MediaControl.lastTrack) < 170) {
					tp = paintLarge;
				}
				else {
					tp = paintSmall;
				}
				
				canvas.save();			
				StaticLayout layout = new StaticLayout(MediaControl.lastTrack, tp, 96, Layout.Alignment.ALIGN_CENTER, 1.2f, 0, false);
				int height = layout.getHeight();
				int textY = 26 - (height/2);
				if(textY<8) {
					textY=8;
				}
				canvas.translate(0, textY); //position the text
				canvas.clipRect(0,0,96,35);
				layout.draw(canvas);
				canvas.restore();	
				
				canvas.save();			
				StringBuilder lowerText = new StringBuilder();
				if(!MediaControl.lastArtist.equals("")) {
					lowerText.append(MediaControl.lastArtist);
				}
				if(!MediaControl.lastAlbum.equals("")) {
					if(lowerText.length()>0)
						lowerText.append("\n\n");
					lowerText.append(MediaControl.lastAlbum);
				}
				layout = new StaticLayout(lowerText.toString(), paintSmall, 96, Layout.Alignment.ALIGN_CENTER, 1.0f, 0, false);
				height = layout.getHeight();
				textY = 70 - (height/2);
				if(textY<54) {
					textY=54;
				}
				canvas.translate(0, textY); //position the text
				canvas.clipRect(0,0,96,35);
				layout.draw(canvas);
				canvas.restore();	
			}
			
			return bitmap;
		}
		else if (watchType == WatchType.ANALOG) {
			Bitmap bitmap = Bitmap.createBitmap(80, 32, Bitmap.Config.RGB_565);
			Canvas canvas = new Canvas(bitmap);
			canvas.drawColor(Color.WHITE);	
	
			if(MediaControl.lastTrack=="") {
				canvas.drawBitmap(Utils.loadBitmapFromAssets(context, "media_player_idle_oled.png"), 0, 0, null);				
			}
			else {	
				canvas.drawBitmap(Utils.loadBitmapFromAssets(context, "media_player_oled.png"), 0, 0, null);
								
				TextPaint tp = null;
				if( paintLarge.measureText(MediaControl.lastTrack) < 75) {
					tp = paintLarge;
				}
				else {
					tp = paintSmall;
				}
				
				canvas.save();			
				StaticLayout layout = new StaticLayout(MediaControl.lastTrack, tp, 75, Layout.Alignment.ALIGN_CENTER, 1.2f, 0, false);
				int height = layout.getHeight();
				int textY = 8 - (height/2);
				if(textY<0) {
					textY=0;
				}
				canvas.translate(0, textY); //position the text
				canvas.clipRect(0,0,75,16);
				layout.draw(canvas);
				canvas.restore();	
				
				canvas.save();			
				StringBuilder lowerText = new StringBuilder();
				if(!MediaControl.lastArtist.equals("")) {
					lowerText.append(MediaControl.lastArtist);
				}
				if(!MediaControl.lastAlbum.equals("")) {
					if(lowerText.length()>0)
						lowerText.append("\n\n");
					lowerText.append(MediaControl.lastAlbum);
				}
				
				if( paintLarge.measureText(lowerText.toString()) < 75) {
					tp = paintLarge;
				}
				else {
					tp = paintSmall;
				}
				
				layout = new StaticLayout(lowerText.toString(), tp, 75, Layout.Alignment.ALIGN_CENTER, 1.0f, 0, false);
				height = layout.getHeight();
				textY = 24 - (height/2);
				if(textY<16) {
					textY=16;
				}
				canvas.translate(0, textY); //position the text
				canvas.clipRect(0,0,75,16);
				layout.draw(canvas);
				canvas.restore();	
				
			}
			
			return bitmap;
		}
		
		return null;
	}

}
