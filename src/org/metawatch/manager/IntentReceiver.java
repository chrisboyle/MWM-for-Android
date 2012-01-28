                                                                     
                                                                     
                                                                     
                                             
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
  * IntentReceiver.java                                                       *
  * IntentReceiver                                                            *
  * Notifications receiver                                                    *
  *                                                                           *
  *                                                                           *
  *****************************************************************************/

package org.metawatch.manager;

import org.metawatch.manager.MetaWatchService.Preferences;
import org.metawatch.manager.Notification.VibratePattern;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.telephony.SmsMessage;
import android.util.Log;

public class IntentReceiver extends BroadcastReceiver {
		
	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();		
		Log.d(MetaWatch.TAG, "IntentReceiver.onReceive(): received intent, action='"+action+"'");
		
		Bundle b = intent.getExtras();
		if (b != null) {
			for (String key : b.keySet()) {
				Log.d(MetaWatch.TAG,
						"extra: " + key + " = '" + b.get(key) + "'");
			}
			String dataString = intent.getDataString();
			Log.d(MetaWatch.TAG, "dataString: "
					+ (dataString == null ? "null" : "'" + dataString + "'"));
		}
		
		if (action.equals("android.intent.action.PROVIDER_CHANGED")) {

			if (!MetaWatchService.Preferences.notifyGmail)
				return;

			if (!Utils.isGmailAccessSupported(context) && ! MetaWatchAccessibilityService.haveCMHack) {
				Bundle bundle = intent.getExtras();

				/* Get recipient and count */
				String recipient = "You";
				if (bundle.containsKey("account"))
					recipient = bundle.getString("account");
				int count = bundle.getInt("count");

				/* What kind of update is this? */
				String tagLabel = bundle.getString("tagLabel");
				if (tagLabel.equals("^^unseen-^i")) {

					/* This is a new message notification. */
					if (count > 0) {
						NotificationBuilder.createGmailBlank(context,
								recipient, count);
						Log.d(MetaWatch.TAG,
								"Received Gmail new message notification; "
										+ count + " new message(s).");
					} else {
						Log.d(MetaWatch.TAG,
								"Ignored Gmail new message notification; no new messages.");
					}

				} else if (tagLabel.equals("^^unseen-^iim")) {

					/* This is a total unread count notification. */
					Log.d(MetaWatch.TAG,
							"IntentReceiver.onReceive(): Received Gmail notification: total unread count for '"
									+ recipient + "' is " + count + ".");

				} else {
					/* I have no idea what this is. */
					Log.d(MetaWatch.TAG,
							"Unknown Gmail notification: tagLabel is '"+tagLabel+"'");
				}

				Monitors.updateGmailUnreadCount(recipient, count);
				Log.d(MetaWatch.TAG,
						"IntentReceiver.onReceive(): Cached Gmail unread count for account '"
								+ recipient + "' is "
								+ Monitors.getGmailUnreadCount(recipient));
				
				if (MetaWatchService.watchType == MetaWatchService.WatchType.DIGITAL) {
					Idle.updateLcdIdle(context);
				}
				
				return;
			}
		}
		else if (action.equals("android.provider.Telephony.SMS_RECEIVED")) {		
			if (!MetaWatchService.Preferences.notifySMS)
				return;
			
			Bundle bundle = intent.getExtras();
			if (bundle.containsKey("pdus")) {
				Object[] pdus = (Object[]) bundle.get("pdus");
				SmsMessage[] smsMessage = new SmsMessage[pdus.length];
				for (int i = 0; i < smsMessage.length; i++) {
					smsMessage[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
					String number = smsMessage[i].getOriginatingAddress();
					String body = smsMessage[i].getDisplayMessageBody();
					
					NotificationBuilder.createSMS(context, number, body);
				}
			}
			return;
		}
		else if (action.equals("com.fsck.k9.intent.action.EMAIL_RECEIVED")) {
			
			if (MetaWatchService.Preferences.notifyK9) {				
				Bundle bundle = intent.getExtras();				
				String subject = bundle.getString("com.fsck.k9.intent.extra.SUBJECT");
				String sender = bundle.getString("com.fsck.k9.intent.extra.FROM");
				String account = bundle.getString("com.fsck.k9.intent.extra.ACCOUNT");
				String folder = bundle.getString("com.fsck.k9.intent.extra.FOLDER");
				NotificationBuilder.createK9(context, sender, subject, account+":"+folder);
			}
			if( MetaWatchService.Preferences.showK9Unread) {
				Utils.refreshUnreadK9Count(context);
				Idle.updateLcdIdle(context);
			}
			return;
		}	
		else if (action.equals("com.android.alarmclock.ALARM_ALERT")
				|| action.equals("com.htc.android.worldclock.ALARM_ALERT")
				|| action.equals("com.android.deskclock.ALARM_ALERT")
				|| action.equals("com.motorola.blur.alarmclock.ALARM_ALERT")
				|| action.equals("com.motorola.blur.alarmclock.COUNT_DOWN")
				|| action.equals("com.sonyericsson.alarm.ALARM_ALERT")) {
			
			if (!MetaWatchService.Preferences.notifyAlarm)
				return;
			
			NotificationBuilder.createAlarm(context);
			return;
		}
		else if (action.equals("android.intent.action.BATTERY_LOW") ) {
			
			if (!MetaWatchService.Preferences.notifyBatterylow)
				return;
			
			NotificationBuilder.createBatterylow(context);
			return;
		}
		else if (action.equals("android.intent.action.TIME_SET") ) {
			
			Log.d(MetaWatch.TAG, "IntentReceiver.onReceive(): Received time set intent.");
			
			/* The time has changed, so notify the watch. */
			//Protocol.setNvalTime(context);
			Protocol.sendRtcNow(context);
			return;
		}		
		else if (action.equals("android.intent.action.TIMEZONE_CHANGED") ) {
			
			Log.d(MetaWatch.TAG, "IntentReceiver.onReceive(): Received timezone changed intent.");
			
			/*
			 * If we're in a new time zone, then the time has probably changed.
			 * Notify the watch.
			 */
			Protocol.sendRtcNow(context);

			SharedPreferences sharedPreferences = PreferenceManager
					.getDefaultSharedPreferences(context);
			if (sharedPreferences.getBoolean("settingsNotifyTimezoneChange",
					false)) {
				NotificationBuilder.createTimezonechange(context);
			}
			return;
		}
		else if (action.equals("com.android.music.metachanged")
				|| action.equals("com.android.music.playbackcomplete")
				|| action.equals("com.android.music.playbackstatechanged")
				|| action.equals(
						"mobi.beyondpod.action.PLAYBACK_STATUS")
				|| intent.getAction().equals("com.htc.music.metachanged")
				|| intent.getAction().equals("com.nullsoft.winamp.metachanged")
				|| intent.getAction().equals("com.sonyericsson.music.playbackcontrol.ACTION_TRACK_STARTED")) {

			PackageManager pm = context.getPackageManager();
			String likelyPackage = action;
			likelyPackage = likelyPackage.substring(0, likelyPackage.lastIndexOf("."));
			if (likelyPackage.endsWith(".action")) {
				likelyPackage = likelyPackage.substring(0, likelyPackage.length() - 7);
			}
			if (likelyPackage.equals("com.android.music")) {
				try {
					pm.getPackageInfo("com.google.android.music", 0);
					likelyPackage = "com.google.android.music";
				} catch (NameNotFoundException e) {}
			}

			/* If the intent specifies a "playing" extra, use it. */
			boolean playing = true;
			if (action.endsWith(".playbackcomplete")) {
				playing = false;
			} else if (intent.hasExtra("playing")) {
				playing = intent.getBooleanExtra("playing", false);
			}
			if (playing == false) {
				LCDNotification.removePersistentNotifications(
						context, true, likelyPackage, true);
				/* Ignore stop events. */
				return;
			}
			
			String artist = "";
			String track = "";
			String album = "";

			if (intent.hasExtra("artist"))
				artist = intent.getStringExtra("artist");
			else if (intent.hasExtra("ARTIST_NAME"))
				artist = intent.getStringExtra("ARTIST_NAME");
			if (intent.hasExtra("track"))
				track = intent.getStringExtra("track");
			else if (intent.hasExtra("TRACK_NAME"))
				track = intent.getStringExtra("TRACK_NAME");
			if (intent.hasExtra("album"))
				album = intent.getStringExtra("album");
			else if (intent.hasExtra("ALBUM_NAME"))
				album = intent.getStringExtra("ALBUM_NAME");
			
			/* Ignore if track info hasn't changed. */
			if (artist.equals(MediaControl.lastArtist) && track.equals(MediaControl.lastTrack) && album.equals(MediaControl.lastAlbum)) {
				Log.d(MetaWatch.TAG, "IntentReceiver.onReceive(): Track info hasn't changed, ignoring");
				return;
			} else {
				MediaControl.lastArtist = artist;
				MediaControl.lastTrack = track;
				MediaControl.lastAlbum = album;
			}

			Bitmap icon = null;
			try {
				PackageInfo packageInfo = pm.getPackageInfo(likelyPackage, 0);
				icon = NotificationIconShrinker.shrink(
						pm.getResourcesForApplication(packageInfo.applicationInfo),
						packageInfo.applicationInfo.icon, likelyPackage,
						NotificationIconShrinker.ICON_SIZE);
			} catch (NameNotFoundException e) {}
			if (icon == null) {
				icon = Utils.loadBitmapFromAssets(context, "play11.bmp");
			}
			//LCDNotification.addPersistentNotification(context, true, likelyPackage,
			//		icon, track+" ("+artist+")");

			if (!MetaWatchService.Preferences.notifyMusic)
				return;

			if(MediaControl.mediaPlayerActive) {
				VibratePattern vibratePattern = NotificationBuilder.createVibratePatternFromPreference(context, "settingsMusicNumberBuzzes");				
	
				Idle.updateLcdIdle(context);
				
				if (vibratePattern.vibrate)
					Protocol.vibrate(vibratePattern.on,
							vibratePattern.off,
							vibratePattern.cycles);
				
				if (Preferences.notifyLight)
					Protocol.ledChange(true);
				
			}
			else {
				if (intent.getAction().equals("com.nullsoft.winamp.metachanged")) {
					NotificationBuilder.createWinamp(context, artist, track, album);				
				} else {
					NotificationBuilder.createMusic(context, artist, track, album);
				}
			}
			
		}
		
	}

}

