                                                                     
                                                                     
                                                                     
                                             
 /*****************************************************************************
  *  Copyright (c) 2011 Meta Watch Ltd.                                       *
  *  www.MetaWatch.org                                                        *
  *                                                                           *
  =============================================================================
  *                                                                           *
  *  Licensed under the Apache License, Version 2.0 (the "License");          *
  *  you may not use this file except in compliance with the License.         *
  *  You may obtain a copy of the License at                                  *
  *                                                                           *
  *    http://www.apache.org/licenses/LICENSE-2.0                             *
  *                                                                           *
  *  Unless required by applicable law or agreed to in writing, software      *
  *  distributed under the License is distributed on an "AS IS" BASIS,        *
  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
  *  See the License for the specific language governing permissions and      *
  *  limitations under the License.                                           *
  *                                                                           *
  *****************************************************************************/

 /*****************************************************************************
  * Idle.java                                                                 *
  * Idle                                                                      *
  * Idle watch mode                                                           *
  *                                                                           *
  *                                                                           *
  *****************************************************************************/

package org.metawatch.manager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.Notification.VibratePattern;
import org.metawatch.manager.widgets.InternalWidget.WidgetData;
import org.metawatch.manager.widgets.WidgetManager;
import org.metawatch.manager.widgets.WidgetRow;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;

public class Idle {
	
	final static byte IDLE_NEXT_PAGE = 60;
	final static byte IDLE_OLED_DISPLAY = 61;

	static int currentPage = 0;
	
	static boolean widgetsInitialised = false;
		
	static int mediaPlayerPage = -1;
	
	public static void nextPage() {
		toPage(currentPage+1);
	}
	
	public static void toPage(int page) {
		
		if(currentPage==mediaPlayerPage) {
			Protocol.disableMediaButtons();
			if (Preferences.logging) Log.d(MetaWatch.TAG, "Leaving media mode");
			MediaControl.mediaPlayerActive = false;
		}
		
		currentPage = (page) % numPages();
		
		if(currentPage==mediaPlayerPage) {
			Protocol.enableMediaButtons();
			if (Preferences.logging) Log.d(MetaWatch.TAG, "Entering media mode");
			MediaControl.mediaPlayerActive = true;
		}
	}
	
	public static int numPages() {	
		int pages = (widgetScreens==null || widgetScreens.size()==0) ? 1 : widgetScreens.size();
		if(Preferences.idleMusicControls) {
			mediaPlayerPage = pages;
			pages++;
		}
		return pages;
	}
	
	private static ArrayList<ArrayList<WidgetRow>> widgetScreens = null;
	private static Map<String,WidgetData> widgetData = null;
	
	public static synchronized void updateWidgetPages(Context context, boolean refresh)
	{
		if(!widgetsInitialised) {
			WidgetManager.initWidgets(context, null);
			widgetsInitialised = true;
		}
		
		List<WidgetRow> rows = WidgetManager.getDesiredWidgetsFromPrefs(context);
		
		ArrayList<CharSequence> widgetsDesired = new ArrayList<CharSequence>();
		for(WidgetRow row : rows) {
			widgetsDesired.addAll(row.getIds());
		}			
		
		if (refresh)
			widgetData = WidgetManager.refreshWidgets(context, widgetsDesired);
		else
			widgetData = WidgetManager.getCachedWidgets(context, widgetsDesired);
		
		for(WidgetRow row : rows) { 
			row.doLayout(widgetData);
		}
		
		int maxScreenSize = 0;
		
		if (MetaWatchService.watchType == MetaWatchService.WatchType.DIGITAL)
			maxScreenSize = 96;
		else if (MetaWatchService.watchType == MetaWatchService.WatchType.ANALOG)
			maxScreenSize = 32;
		
		// Bucket rows into screens
		ArrayList<ArrayList<WidgetRow>> screens = new ArrayList<ArrayList<WidgetRow>>();
	
		int screenSize = 0;
		if (MetaWatchService.watchType == MetaWatchService.WatchType.DIGITAL)
			screenSize = 30; // Initial screen has top part used by the fw clock
		
		ArrayList<WidgetRow> screen = new ArrayList<WidgetRow>();
		for(WidgetRow row : rows) { 
			if(screenSize+row.getHeight() > maxScreenSize) {
				screens.add(screen);
				screen = new ArrayList<WidgetRow>();
				screenSize = 0;
			}
			screen.add(row);
			screenSize += row.getHeight();
		}
		screens.add(screen);
		
		widgetScreens = screens;
	}

	static synchronized Bitmap createLcdIdle(Context context) {
		return createLcdIdle(context, false, currentPage);
	}

	static synchronized Bitmap createLcdIdle(Context context, boolean preview, int page) {
		
		Bitmap bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(bitmap);
		
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
		
		canvas.drawColor(Color.WHITE);	
		
		if( page != mediaPlayerPage ) {
		
			if(preview && page==0) {
				canvas.drawBitmap(Utils.loadBitmapFromAssets(context, "dummy_clock.png"), 0, 0, null);
			} 
	
			if(widgetScreens.size() > page)
			{
				ArrayList<WidgetRow> rowsToDraw = widgetScreens.get(page);
				
				int totalHeight = 0;
				for(WidgetRow row : rowsToDraw) {
					totalHeight += row.getHeight();
				}
							
				int space = (((page==0 ? 64:96) - totalHeight) / (rowsToDraw.size()+1));
				int yPos = (page==0 ? 30:0) + space;
				
				for(WidgetRow row : rowsToDraw) {
					row.draw(widgetData, canvas, yPos);
					yPos += row.getHeight() + space;
				}

				if (Preferences.displayWidgetRowSeparator) {
					int i = (page==0 ? -1:0);
					yPos = 0 + space;
					for(WidgetRow row : rowsToDraw) {
						yPos += row.getHeight() + space;
						i++;
						if (i!=rowsToDraw.size())
							drawLine(canvas, yPos);
					}
				}

			}

		}
		else {
			
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
		}
		
		return bitmap;
	}
	
	static synchronized Bitmap createOledIdle(Context context, boolean preview, int page) {
		
		Bitmap bitmap = Bitmap.createBitmap(80, 32, Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(bitmap);
		
		canvas.drawColor(Color.WHITE);	
				
		if(widgetScreens.size() > page)
		{
			ArrayList<WidgetRow> rowsToDraw = widgetScreens.get(page);
			
			//int totalHeight = 0;
			//for(WidgetRow row : rowsToDraw) {
			//	totalHeight += row.getHeight();
			//}
						
			//int space = (32 - totalHeight) / (rowsToDraw.size()+1);
			int space = 0;
			int yPos = space;
			
			for(WidgetRow row : rowsToDraw) {
				row.draw(widgetData, canvas, yPos);
				yPos += row.getHeight() + space;
			}

			if (Preferences.displayWidgetRowSeparator) {
				int i = (page==0 ? -1:0);
				yPos = 0 + space;
				for(WidgetRow row : rowsToDraw) {
					yPos += row.getHeight() + space;
					i++;
					if (i!=rowsToDraw.size())
						drawLine(canvas, yPos);
				}
			}

		}
				
		return bitmap;
	}
	
	public static Canvas drawLine(Canvas canvas, int y) {
	  Paint paint = new Paint();
	  paint.setColor(Color.BLACK);

	  int left = 3;

	  for (int i = 0+left; i < 96-left; i += 3)
	    canvas.drawLine(i, y, i+2, y, paint);
	
	  return canvas;
	}
	
	private static synchronized void sendLcdIdle(Context context, boolean refresh) {
		if(MetaWatchService.watchState != MetaWatchService.WatchStates.IDLE) {
			if (Preferences.logging) Log.d(MetaWatch.TAG, "Ignoring sendLcdIdle as not in idle");
			return;
		}
		
		updateWidgetPages(context, refresh);
		
		final int mode = currentPage==mediaPlayerPage ? MetaWatchService.WatchBuffers.APPLICATION : MetaWatchService.WatchBuffers.IDLE;
		
		Protocol.sendLcdBitmap(createLcdIdle(context), mode);
		Protocol.configureIdleBufferSize(currentPage==0);
		Protocol.updateDisplay(mode);
	}
	
//	private static synchronized void sendOledIdle(Context context, boolean refresh) {
//		if(MetaWatchService.watchState != MetaWatchService.WatchStates.IDLE) {
//			if (Preferences.logging) Log.d(MetaWatch.TAG, "Ignoring sendLcdIdle as not in idle");
//			return;
//		}
//		
//		updateWidgetPages(context, refresh);
//		
//		for (int i=0;i<4;++i) {
//			Protocol.sendOledBitmap(createOledIdle(context, false, i), MetaWatchService.WatchBuffers.IDLE, i);
//		}	
//	}
	
	public static boolean toIdle(Context context) {
		
		MetaWatchService.WatchModes.IDLE = true;
		MetaWatchService.watchState = MetaWatchService.WatchStates.IDLE;
		
		if (MetaWatchService.watchType == MetaWatchService.WatchType.DIGITAL) {
			sendLcdIdle(context, true);
				
			if (numPages()>1) {
				Protocol.enableButton(0, 0, IDLE_NEXT_PAGE, 0); // Right top immediate
				Protocol.enableButton(0, 0, IDLE_NEXT_PAGE, 1); // Right top immediate
			}
		
		}
		else if (MetaWatchService.watchType == MetaWatchService.WatchType.ANALOG) {
			Protocol.enableButton(1, 0, IDLE_OLED_DISPLAY, 0); // Middle immediate

			//Protocol.enableButton(1, 1, IDLE_OLED_TEMP1, 0); // Middle release
			//Protocol.enableButton(1, 2, IDLE_NEXT_PAGE, 0); // Middle short hold
			//Protocol.enableButton(1, 3, IDLE_OLED_TEMP3, 0); // Middle long hold
			
			//sendOledIdle(context, true);
		}

		return true;
	}
	
	public static void updateIdle(Context context, boolean refresh) {
		if (MetaWatchService.watchState == MetaWatchService.WatchStates.IDLE )
			if (MetaWatchService.watchType == MetaWatchService.WatchType.DIGITAL)
				sendLcdIdle(context, refresh);
			//else if (MetaWatchService.watchType == MetaWatchService.WatchType.ANALOG)
			//	sendOledIdle(context, refresh);
	}
	
	// Send oled widgets view as a notification (until I can work out how to get the proper idle to work)
	public static void oledWidgetNotification(Context context) {
		Idle.updateWidgetPages(context, true);
				
		// get the 32px full screen
		Bitmap bmpPage = Idle.createOledIdle(context, false, currentPage);
		
		// Split into top/bottom, and send
		for(int i=0; i<2; ++i) {
			Bitmap bitmap = Bitmap.createBitmap(80, 16, Bitmap.Config.RGB_565);
			Canvas canvas = new Canvas(bitmap);
			canvas.drawBitmap(bmpPage, 0, -(i*16), null);
			Protocol.sendOledBitmap(bitmap, MetaWatchService.WatchBuffers.NOTIFICATION, i);
		}
					
	}
	
	public static void oledTest(Context context, String msg) {
		VibratePattern vibratePattern = new VibratePattern(false, 0, 0, 1);
		Notification.addOledNotification(context, Protocol.createOled1line(context, null, "Testing"), Protocol.createOled1line(context, null, msg), null, 0, vibratePattern);
	}
	
}
