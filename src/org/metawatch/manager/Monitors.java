                                                                     
                                                                     
                                                                     
                                             
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
  * Monitors.java                                                             *
  * Monitors                                                                  *
  * Starting notifications and updates                                        *
  *                                                                           *
  *                                                                           *
  *****************************************************************************/

package org.metawatch.manager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Date;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.anddev.android.weatherforecast.weather.GoogleWeatherHandler;
import org.anddev.android.weatherforecast.weather.WeatherCurrentCondition;
import org.anddev.android.weatherforecast.weather.WeatherForecastCondition;
import org.anddev.android.weatherforecast.weather.WeatherSet;
import org.anddev.android.weatherforecast.weather.WeatherUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.MetaWatchService.WeatherProvider;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;


public class Monitors {
	
	public static AlarmManager alarmManager;
	public static Intent intent;
	public static PendingIntent sender;
	
	static GmailMonitor gmailMonitor;
	
	private static ContentObserverMessages contentObserverMessages;
	static ContentResolver contentResolverMessages;
	
	private static ContentObserverCalls contentObserverCalls;
	static ContentResolver contentResolverCalls;
	
	private static ContentObserverAppointments contentObserverAppointments;
	static ContentResolver contentResolverAppointments;

  //public static int gmailCount = 0;
	static Hashtable<String, Integer> gmailUnreadCounts = new Hashtable<String, Integer>();
	
	public static LocationManager locationManager;
	public static String locationProvider;
	
	private static NetworkLocationListener networkLocationListener;
	
	private static BroadcastReceiver batteryLevelReceiver, wifiReceiver;
	
	public static boolean calendarChanged = false;

  public static class WeatherData {
		public static boolean updating = false;
		public static boolean received = false;
		public static String icon;
		public static String temp;
		public static String condition;
		public static String locationName;
		public static boolean celsius = false;
		
		public static int sunriseH = 7;
		public static int sunriseM = 0;
		
		public static int sunsetH = 19;
		public static int sunsetM = 0;
		
		public static int moonPercentIlluminated = -1;
		public static int ageOfMoon = -1;
		
		public static Forecast[] forecast = null;
		
		public static long timeStamp = 0;
		public static long forecastTimeStamp = 0;
	}
		
	public class Forecast {
		public String day;
		public String icon;
		public String tempHigh;
		public String tempLow;
	}
	
	public static class LocationData {
		public static boolean received = false;
	    public static double latitude;
	    public static double longitude;
	    
	    public static long timeStamp = 0;
	}
	
	public static class BatteryData {
		public static int level = -1;
		public static boolean charging = false;
	}
	
	public static class SignalData {
		public static int phoneBars = 0;  // 0-4
		public static int wifiBars  = 0;  // 0-4
		public static String phoneDataType = "";
		public static boolean roaming = false;
	}
	
	private static Monitors m = new Monitors(); // Static instance for new
	
	public static void updateGmailUnreadCount(String account, int count) {
		if (Preferences.logging) Log.d(MetaWatch.TAG, "Monitors.updateGmailUnreadCount(): account='"
				+ account + "' count='" + count + "'");
		gmailUnreadCounts.put(account, count);
		if (Preferences.logging) Log.d(MetaWatch.TAG,
				"Monitors.updateGmailUnreadCount(): new unread count is: "
						+ gmailUnreadCounts.get(account));
	}
	
	public static int getGmailUnreadCount() {
		if (Preferences.logging) Log.d(MetaWatch.TAG, "Monitors.getGmailUnreadCount()");
		int totalCount = 0;
		for (String key : gmailUnreadCounts.keySet()) {
			Integer accountCount = gmailUnreadCounts.get(key);
			totalCount += accountCount.intValue();
			if (Preferences.logging) Log.d(MetaWatch.TAG, "Monitors.getGmailUnreadCount(): account='"
					+ key + "' accountCount='" + accountCount
					+ "' totalCount='" + totalCount + "'");
		}
		return totalCount;
	}
	
	public static int getGmailUnreadCount(String account) {
		int count = gmailUnreadCounts.get(account);
		if (Preferences.logging) Log.d(MetaWatch.TAG, "Monitors.getGmailUnreadCount('"+account+"') returning " + count);
		return count;
	}
	
	public static void start(Context context, TelephonyManager telephonyManager) {
		// start weather updater
		
		if (Preferences.logging) Log.d(MetaWatch.TAG,
				"Monitors.start()");
		
		createBatteryLevelReciever(context);
		createWifiReceiver(context);
				
		if (Preferences.weatherGeolocation) {
			if (Preferences.logging) Log.d(MetaWatch.TAG,
					"Initialising Geolocation");
			
			try {
				locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
				locationProvider = LocationManager.NETWORK_PROVIDER;
				
				networkLocationListener = new NetworkLocationListener(context);
				
				locationManager.requestLocationUpdates(locationProvider, 30 * 60 * 1000, 500, networkLocationListener);
				
				RefreshLocation();
			}
			catch (IllegalArgumentException e) {
				if (Preferences.logging) Log.d(MetaWatch.TAG,"Failed to initialise Geolocation "+e.getMessage());
			}
		}
		else {
			if (Preferences.logging) Log.d(MetaWatch.TAG,"Geolocation disabled");
		}
		
		CallStateListener phoneListener = new CallStateListener(context);
		
		telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		int phoneEvents = PhoneStateListener.LISTEN_CALL_STATE
				| PhoneStateListener.LISTEN_SIGNAL_STRENGTHS
				| PhoneStateListener.LISTEN_DATA_CONNECTION_STATE;
		telephonyManager.listen(phoneListener, phoneEvents);
		
		if (Utils.isGmailAccessSupported(context)) {
			gmailMonitor = new GmailMonitor(context);
			gmailMonitor.startMonitor();
		}
		
		try {
			contentObserverMessages = new ContentObserverMessages(context);
			Uri uri = Uri.parse("content://mms-sms/conversations/");
			contentResolverMessages = context.getContentResolver();
			contentResolverMessages.registerContentObserver(uri, true, contentObserverMessages);
		} catch (Exception x) {
		}
		
		try {
			contentObserverCalls = new ContentObserverCalls(context);
			//Uri uri = Uri.parse("content://mms-sms/conversations/");
			contentResolverCalls = context.getContentResolver();
			contentResolverCalls.registerContentObserver(android.provider.CallLog.Calls.CONTENT_URI, true, contentObserverCalls);
		} catch (Exception x) {
		}
	
		try {
			contentObserverAppointments = new ContentObserverAppointments(context);
			Uri uri = Uri.parse("content://com.android.calendar/calendars/");
			contentResolverAppointments = context.getContentResolver();
			contentResolverAppointments.registerContentObserver(uri, true, contentObserverAppointments);
		} catch (Exception x) {
			}
		
		// temporary one time update
		updateWeatherData(context);
		
		startAlarmTicker(context);
	}
	
	public static void RefreshLocation() {
		if (locationManager==null)
			return;
		Location location = locationManager.getLastKnownLocation(locationProvider);
		
		if (location!=null) {
			LocationData.latitude = location.getLatitude();
			LocationData.longitude = location.getLongitude();
			
			LocationData.timeStamp = location.getTime();
			
			LocationData.received = true;
		}
	}
	
	public static void stop(Context context) {
		
		if (Preferences.logging) Log.d(MetaWatch.TAG,
				"Monitors.stop()");
		
		contentResolverMessages.unregisterContentObserver(contentObserverMessages);
		if (Preferences.weatherGeolocation & locationManager!=null) {
			if (locationManager!=null) {
				locationManager.removeUpdates(networkLocationListener);
			}
		}
		stopAlarmTicker();
		
		if (batteryLevelReceiver!=null) {
			context.unregisterReceiver(batteryLevelReceiver);
			batteryLevelReceiver=null;
		}
		if (wifiReceiver!=null) {
			context.unregisterReceiver(wifiReceiver);
			wifiReceiver=null;
		}
	}
	
	private static synchronized void updateWeatherDataGoogle(Context context) {
		try {

			if (WeatherData.updating)
				return;
			
			// Prevent weather updating more frequently than every 5 mins
			if (WeatherData.timeStamp!=0 && WeatherData.received) {
				long currentTime = System.currentTimeMillis();
				long diff = currentTime - WeatherData.timeStamp;
				
				if (diff < 5 * 60*1000) {
					if (Preferences.logging) Log.d(MetaWatch.TAG,
							"Skipping weather update - updated less than 5m ago");

            		//IdleScreenWidgetRenderer.sendIdleScreenWidgetUpdate(context);

					return;
				}
			}
			
			WeatherData.updating = true;
			
			if (Preferences.logging) Log.d(MetaWatch.TAG,
					"Monitors.updateWeatherDataGoogle(): start");
		
			String queryString;
			List<Address> addresses;
			if (Preferences.weatherGeolocation && LocationData.received) {
				Geocoder geocoder;
				String locality = "";
				String PostalCode="";
				try{
					geocoder = new Geocoder(context, Locale.getDefault());
					addresses = geocoder.getFromLocation(LocationData.latitude, LocationData.longitude, 1);

					for (Address address : addresses) {
						if (!address.getPostalCode().equalsIgnoreCase("")){
							PostalCode=address.getPostalCode();
							locality=address.getLocality();
							if (locality.equals("")) {
								locality=PostalCode;
							}
							else {
								PostalCode = locality+", "+PostalCode;
							}
								
						}
					}
				}
				catch (IOException e){
					if (Preferences.logging) Log.e(MetaWatch.TAG, "Exception while retreiving postalcode", e);
				}
				
				if (PostalCode.equals("")){
					PostalCode=Preferences.weatherCity;
				}
				if (locality.equals("")){
					WeatherData.locationName = PostalCode;
				}
				else{
					WeatherData.locationName = locality;
				}
				
				queryString = "http://www.google.com/ig/api?weather=" + PostalCode;
			}
			else{
				queryString = "http://www.google.com/ig/api?weather=" + Preferences.weatherCity;
				WeatherData.locationName = Preferences.weatherCity;
			}
						
			URL url = new URL(queryString.replace(" ", "%20"));			 
			
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser sp = spf.newSAXParser();
			XMLReader xr = sp.getXMLReader();

			GoogleWeatherHandler gwh = new GoogleWeatherHandler();
			xr.setContentHandler(gwh);
			xr.parse(new InputSource(url.openStream()));
			WeatherSet ws = gwh.getWeatherSet();
			WeatherCurrentCondition wcc = ws
					.getWeatherCurrentCondition();
			
			ArrayList<WeatherForecastCondition> conditions = ws.getWeatherForecastConditions();
			
			int days = conditions.size();
			WeatherData.forecast = new Forecast[days];
			
			for (int i=0; i<days; ++i) {
				WeatherForecastCondition wfc = conditions.get(i);
				
				WeatherData.forecast[i] = m.new Forecast();
				WeatherData.forecast[i].day = null;
				
				WeatherData.forecast[i].icon = getIconGoogleWeather(wfc.getCondition());
				WeatherData.forecast[i].day = wfc.getDayofWeek();
				
				if (Preferences.weatherCelsius) {
					WeatherData.forecast[i].tempHigh = wfc.getTempMaxCelsius().toString();
					WeatherData.forecast[i].tempLow = wfc.getTempMinCelsius().toString();
				} else {
					WeatherData.forecast[i].tempHigh = 
							  Integer.toString(WeatherUtils
									.celsiusToFahrenheit(wfc
											.getTempMaxCelsius()));
					WeatherData.forecast[i].tempLow = 
							  Integer.toString(WeatherUtils
									.celsiusToFahrenheit(wfc
											.getTempMinCelsius()));
				}
			}
			
			WeatherData.celsius = Preferences.weatherCelsius;
								
			String cond =  wcc.getCondition();
			WeatherData.condition = cond;
		
			if (Preferences.weatherCelsius) {
				WeatherData.temp = Integer.toString(wcc.getTempCelcius());
			} else {
				WeatherData.temp = Integer.toString(wcc.getTempFahrenheit());
			}

			cond = cond.toLowerCase();

			WeatherData.icon = getIconGoogleWeather(cond);
			WeatherData.received = true;
			WeatherData.timeStamp = System.currentTimeMillis();		

			Idle.updateLcdIdle(context);
			MetaWatchService.notifyClients();
			
		} catch (Exception e) {
			if (Preferences.logging) Log.e(MetaWatch.TAG, "Exception while retreiving weather", e);
		} finally {
			if (Preferences.logging) Log.d(MetaWatch.TAG,
					"Monitors.updateWeatherData(): finish");
		}
		
	}

	private static synchronized void updateWeatherDataWunderground(Context context) {
		try {

			if (WeatherData.updating)
				return;
			
			long currentTime = System.currentTimeMillis();
			
			// Prevent weather updating more frequently than every 5 mins
			if (WeatherData.timeStamp!=0 && WeatherData.received) {
				
				long diff = currentTime - WeatherData.timeStamp;
				if (diff < 5 * 60*1000) {
					if (Preferences.logging) Log.d(MetaWatch.TAG,
							"Skipping weather update - updated less than 5m ago");
					Idle.updateLcdIdle(context);
					return;
				}
			}
			
			WeatherData.updating = true;
			
			if (Preferences.logging) Log.d(MetaWatch.TAG,
					"Monitors.updateWeatherDataWunderground(): start");
			
			if (LocationData.received && Preferences.wundergroundKey != "") {
				
				String latLng = Double.toString(LocationData.latitude)+","+Double.toString(LocationData.longitude);
				String forecastQuery = "";
				boolean hasForecast = false;
				
				long diff = currentTime - WeatherData.forecastTimeStamp;
				if (WeatherData.forecast==null || (diff > 3 * 60*60*1000)) {
					// Only update forecast every three hours
					forecastQuery = "forecast10day/astronomy/";
					hasForecast = true;
				}
				
				String requestUrl =  "http://api.wunderground.com/api/"+Preferences.wundergroundKey+"/geolookup/conditions/"+forecastQuery+"q/"+latLng+".json";
				
				if (Preferences.logging) Log.d(MetaWatch.TAG,
						"Request: "+requestUrl);
				
				JSONObject json = getJSONfromURL( requestUrl );
				
				JSONObject location = json.getJSONObject("location");
				JSONObject current = json.getJSONObject("current_observation");
							
				if (hasForecast) {
					JSONObject moon = json.getJSONObject("moon_phase");
					JSONObject sunrise = moon.getJSONObject("sunrise");
					WeatherData.sunriseH = sunrise.getInt("hour");
					WeatherData.sunriseM = sunrise.getInt("minute");
					JSONObject sunset = moon.getJSONObject("sunset");
					WeatherData.sunsetH = sunset.getInt("hour");
					WeatherData.sunsetM = sunset.getInt("minute");
					
					WeatherData.moonPercentIlluminated = moon.getInt("percentIlluminated");
					WeatherData.ageOfMoon = moon.getInt("ageOfMoon");
				}
				
				boolean isDay = true;
				
				Date dt = new Date();
                int hours = dt.getHours();
                int minutes = dt.getMinutes();
                
                if ((hours<WeatherData.sunriseH) ||
                	(hours==WeatherData.sunriseH && minutes<WeatherData.sunriseM) ||
                	(hours>WeatherData.sunsetH) ||
                	(hours==WeatherData.sunsetH && minutes>WeatherData.sunsetM))
                {
                	isDay = false;
                }
                						
				WeatherData.locationName = location.getString("city");			
				WeatherData.condition = current.getString("weather");	
				WeatherData.icon = getIconWunderground(current.getString("icon"), isDay);
				
				if (Preferences.weatherCelsius) {
					WeatherData.temp = current.getString("temp_c");
				}
				else {
					WeatherData.temp = current.getString("temp_f");
				}
					
				if (hasForecast) {
					JSONObject forecast = json.getJSONObject("forecast");
					JSONArray forecastday = forecast.getJSONObject("simpleforecast").getJSONArray("forecastday");
				
					int days = forecastday.length();
					WeatherData.forecast = new Forecast[days];
					
					for (int i=0; i<days; ++i) {
						WeatherData.forecast[i] = m.new Forecast();
						JSONObject day = forecastday.getJSONObject(i);
						JSONObject date = day.getJSONObject("date");
						
						WeatherData.forecast[i].icon = getIconWunderground(day.getString("icon"), true);
						WeatherData.forecast[i].day = date.getString("weekday_short");
						if (Preferences.weatherCelsius) {
							WeatherData.forecast[i].tempLow = day.getJSONObject("low").getString("celsius");
							WeatherData.forecast[i].tempHigh= day.getJSONObject("high").getString("celsius");
						}
						else {
							WeatherData.forecast[i].tempLow = day.getJSONObject("low").getString("fahrenheit");
							WeatherData.forecast[i].tempHigh= day.getJSONObject("high").getString("fahrenheit");
						}			
					}
					
					WeatherData.forecastTimeStamp = System.currentTimeMillis();
				}
			
				WeatherData.celsius = Preferences.weatherCelsius;
				
				WeatherData.received = true;
				
				Idle.updateLcdIdle(context);
				MetaWatchService.notifyClients();
				WeatherData.timeStamp = System.currentTimeMillis();		
		    }

			
		} catch (Exception e) {
			if (Preferences.logging) Log.e(MetaWatch.TAG, "Exception while retreiving weather", e);
		} finally {
			if (Preferences.logging) Log.d(MetaWatch.TAG,
					"Monitors.updateWeatherData(): finish");
			
			WeatherData.updating = false;			
		}
	}
	
	private static String getIconGoogleWeather(String cond) {
		if (cond.equals("clear")
				|| cond.equals("sunny"))
			return "weather_sunny.bmp";
		else if (cond.equals("cloudy")
				|| cond.equals("overcast") )
			return "weather_cloudy.bmp";
		else if (cond.equals("mostly cloudy")
				|| cond.equals("partly cloudy")
				|| cond.equals("mostly sunny")
				|| cond.equals("partly sunny"))
			return "weather_partlycloudy.bmp";
		else if (cond.equals("light rain") || cond.equals("rain")
				|| cond.equals("rain showers")
				|| cond.equals("showers")
				|| cond.equals("chance of showers")
				|| cond.equals("scattered showers")
				|| cond.equals("freezing rain")
				|| cond.equals("freezing drizzle")
				|| cond.equals("rain and snow"))
			return "weather_rain.bmp";
		else if (cond.equals("thunderstorm")
				|| cond.equals("chance of storm")
				|| cond.equals("isolated thunderstorms"))
			return "weather_thunderstorm.bmp";
		else if (cond.equals("chance of snow")
				|| cond.equals("snow showers")
				|| cond.equals("ice/snow")
				|| cond.equals("flurries"))
			return "weather_snow.bmp";
		else
			return "weather_cloudy.bmp";
	}
	
	private static String getIconWunderground(String cond, boolean isDay) {

		if (cond.equals("clear") 
				|| cond.equals("sunny"))
			if (isDay)
				return "weather_sunny.bmp";
			else
				return "weather_nt_clear.bmp";
		else if (cond.equals("cloudy"))
			return "weather_cloudy.bmp";
		else if (cond.equals("partlycloudy")
				|| cond.equals("mostlycloudy")
				|| cond.equals("partlysunny")
				|| cond.equals("mostlysunny"))
			if (isDay)
				return "weather_partlycloudy.bmp";
			else
				return "weather_nt_partlycloudy.bmp";
		else if (cond.equals("rain") 
				|| cond.equals("chancerain"))
			return "weather_rain.bmp";
		else if (cond.equals("fog") 
				|| cond.equals("hazy"))
			return "weather_fog.bmp";
		else if (cond.equals("tstorms") 
				|| cond.equals("chancetstorms"))
			return "weather_thunderstorm.bmp";
		else if (cond.equals("snow") 
				|| cond.equals("chancesnow")
				|| cond.equals("sleet")
				|| cond.equals("chancesleet")
				|| cond.equals("flurries")
				|| cond.equals("chanceflurries"))
			return "weather_snow.bmp";
		else
			return "weather_cloudy.bmp";		
	}

	public static void updateWeatherData(final Context context) {
		Thread thread = new Thread("WeatherUpdater") {
			@Override
			public void run() {
				PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
				PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Weather");
			 	wl.acquire();
			 	
			 	switch (Preferences.weatherProvider) {
			 	case WeatherProvider.GOOGLE:
			 		updateWeatherDataGoogle(context);
			 		break;
			 		
			 	case WeatherProvider.WUNDERGROUND:
			 		updateWeatherDataWunderground(context);
			 		break;
			 	}
			 	
			 	wl.release();
			}
		};
		thread.start();
	}
	
	static void startAlarmTicker(Context context) {		
		if (Preferences.logging) Log.d(MetaWatch.TAG, "startAlarmTicker()");
		alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
		intent = new Intent(context, AlarmReceiver.class);
		intent.putExtra("action_update", "update");
		sender = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);

		alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, 0, AlarmManager.INTERVAL_HALF_HOUR, sender);  
	}
	
	static void stopAlarmTicker() {
		alarmManager.cancel(sender);
	}
	
	private static class ContentObserverMessages extends ContentObserver {

		Context context;
		
		public ContentObserverMessages(Context context) {
			super(null);
			this.context = context;			
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);			
			// change in SMS/MMS database			
			Idle.updateLcdIdle(context);
		}
	}
	
	private static class ContentObserverCalls extends ContentObserver {

		Context context;
		
		public ContentObserverCalls(Context context) {
			super(null);
			this.context = context;			
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);			
			// change in call history database
			if (Preferences.logging) Log.d(MetaWatch.TAG, "call history change");
			Idle.updateLcdIdle(context);
		}
	}
	
	private static class ContentObserverAppointments extends ContentObserver {

		Context context;

		public ContentObserverAppointments(Context context) {
			super(null);
			this.context = context;     
		}

		@Override
		public void onChange(boolean selfChange) {
			super.onChange(selfChange);     
			// change in calendar database
			if (Preferences.logging) Log.d(MetaWatch.TAG, "calendar change");
				calendarChanged = true;
				Idle.updateLcdIdle(context);
				calendarChanged = false;
			}
		}

		private static class NetworkLocationListener implements LocationListener {

		Context context;
		
		public NetworkLocationListener(Context context) {
			this.context = context;
		}
		
		public void onLocationChanged(Location location) {
					
			LocationData.latitude = location.getLatitude();
			LocationData.longitude = location.getLongitude();
			
			LocationData.timeStamp = location.getTime();
			
			if (Preferences.logging) Log.d(MetaWatch.TAG, "location changed "+location.toString() );
			
			LocationData.received = true;
			MetaWatchService.notifyClients();
			
			if (!WeatherData.received && !WeatherData.updating) {
				if (Preferences.logging) Log.d(MetaWatch.TAG, "First location - getting weather");
				Monitors.updateWeatherData(context);
			}
		}

		public void onProviderDisabled(String provider) {
		}

		public void onProviderEnabled(String provider) {
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
		}
	}
	
	//http://p-xr.com/android-tutorial-how-to-parse-read-json-data-into-a-android-listview/
	public static JSONObject getJSONfromURL(String url){

		//initialize
		InputStream is = null;
		String result = "";
		JSONObject jArray = null;

		//http post
		try{
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost(url);
			HttpResponse response = httpclient.execute(httppost);
			HttpEntity entity = response.getEntity();
			is = entity.getContent();

		}catch(Exception e){
			if (Preferences.logging) Log.e(MetaWatch.TAG, "Error in http connection "+e.toString());
		}

		//convert response to string
		if (is != null) {
			try {
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(is, "iso-8859-1"), 8);
				StringBuilder sb = new StringBuilder();
				String line = null;
				while ((line = reader.readLine()) != null) {
					sb.append(line + "\n");
				}
				is.close();
				result = sb.toString();
			} catch (Exception e) {
				if (Preferences.logging)
					Log.e(MetaWatch.TAG,
							"Error converting result " + e.toString());
			}

			// dump to sdcard for debugging
			File sdCard = Environment.getExternalStorageDirectory();
			File file = new File(sdCard, "weather.json");

			try {
				FileWriter writer = new FileWriter(file);
				writer.append(result);
				writer.flush();
				writer.close();
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// try parse the string to a JSON object
			try {
				jArray = new JSONObject(result);
			} catch (JSONException e) {
				if (Preferences.logging)
					Log.e(MetaWatch.TAG, "Error parsing data " + e.toString());
			}
		}
		return jArray;
	}
	


	private static void createBatteryLevelReciever(Context context) {
		if(batteryLevelReceiver!=null)
			return;
		
		batteryLevelReceiver = new BroadcastReceiver() {
			public void onReceive(Context context, Intent intent) {
				int rawlevel = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
				int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
				int level = -1;
				if (rawlevel >= 0 && scale > 0) {
					level = (rawlevel * 100) / scale;
				}
				boolean charging = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) > 0;
				if(BatteryData.level != level || BatteryData.charging != charging) {
					//if (Preferences.logging) Log.d(MetaWatch.TAG, "Battery level changed: "+rawlevel+"/"+scale+" - "+level+"%");
					BatteryData.level = level;
					BatteryData.charging = charging;
					Idle.updateLcdIdle(context);
				}
			}
		};
		context.registerReceiver(batteryLevelReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
	}

	private static void createWifiReceiver(final Context context)
	{
		if (wifiReceiver != null) return;
		WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = wm.getConnectionInfo();
		if (info != null) SignalData.wifiBars = 1 + WifiManager.calculateSignalLevel(info.getRssi(), 4);
		wifiReceiver = new BroadcastReceiver() {
			int wifiBars = 0;
			@Override public void onReceive(Context c, Intent intent) {
				String action = intent.getAction();
				if (action.equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
					if (intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
							WifiManager.WIFI_STATE_UNKNOWN) != WifiManager.WIFI_STATE_ENABLED) {
						wifiBars = 0;
					}
				} else if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
					if (! intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)) {
						wifiBars = 0;
					}
				} else if (action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
					NetworkInfo netInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
					if (netInfo.getState() != NetworkInfo.State.CONNECTED) {
						wifiBars = 0;
					} else {
						WifiInfo wifiInfo = intent.getParcelableExtra(WifiManager.EXTRA_WIFI_INFO);
						if (wifiInfo == null) {
							wifiBars = 0;
						} else {
							wifiBars = 1 + WifiManager.calculateSignalLevel(wifiInfo.getRssi(), 4);
						}
					}
				} else if (action.equals(WifiManager.RSSI_CHANGED_ACTION)) {
					final int newRssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, -200);
					wifiBars = 1 + WifiManager.calculateSignalLevel(newRssi, 4);
				}
				if (wifiBars != SignalData.wifiBars) {
					SignalData.wifiBars = wifiBars;
					Idle.updateLcdIdle(context);
				}
			}
		};
		IntentFilter f = new IntentFilter();
		f.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		f.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
		f.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		f.addAction(WifiManager.RSSI_CHANGED_ACTION);
		context.registerReceiver(wifiReceiver, f);
	}
}
