package org.metawatch.manager;

import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;

import org.metawatch.manager.MetaWatchService.Preferences;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

public class MetaWatchAccessibilityService extends AccessibilityService {

	// in patched Android, probably not in your SDK
	static final int TYPE_NOTIFICATION_REMOVED = 0x4000;

	@Override
	protected void onServiceConnected() {
		super.onServiceConnected();
		AccessibilityServiceInfo asi = new AccessibilityServiceInfo();
		asi.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
				| AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
				| TYPE_NOTIFICATION_REMOVED;
		asi.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
		asi.flags = AccessibilityServiceInfo.DEFAULT;
		asi.notificationTimeout = 100;
		setServiceInfo(asi);

		// ArrayList<PInfo> apps = getInstalledApps(true);
		// for (PInfo pinfo : apps) {
		// appsByPackage.put(pinfo.pname, pinfo);
		// }
	}

	// These apps should mostly be using the sync adapter framework insteadof
	// pointlessly notifying me...
	static Pattern excludeTicker = Pattern.compile(".*\\b((updat|sync(h(roni[sz])?)?|refresh|sign)(ing|ed)|online|running)\\b.*",
			Pattern.CASE_INSENSITIVE);
	static Pattern excludeLine1 = Pattern.compile("AndroIRC|ConnectBot|(USB|Car mode|RssDemon|RSS|Select input|Connected as a) .*", 0);
	static Pattern progressLike = Pattern.compile("\\d\\d?%|\\d\\d?:\\d\\d", 0);

	static boolean haveCMHack = false;

	private String currentActivity = "";
	public static boolean accessibilityReceived = false;
	
	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {

		if(!accessibilityReceived) {
			accessibilityReceived = true;
			MetaWatchService.notifyClients();
		}
		
		/* Acquire details of event. */
		int eventType = event.getEventType();
		CharSequence packageName = event.getPackageName();
		CharSequence className = event.getClassName();
				
		Parcelable p = event.getParcelableData();
		if (p instanceof android.app.Notification == false) {
			if (Preferences.logging && event.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
				Log.d(MetaWatch.TAG,
					"MetaWatchAccessibilityService.onAccessibilityEvent(): Not a real notification, ignoring."
					+ "type: "+event.getEventType()+" recvd: "+p);
			}
			return;
		}

		if (packageName.equals("com.google.android.gsf")) {
			// GSF might notify for other reasons, but this is a very common case:
			// Get a better label, and match the package later to clear the icon when Talk is opened
			packageName = "com.google.android.talk";
		} else if (packageName.equals("org.metawatch.manager")
				|| packageName.equals("com.twofortyfouram.locale")
				|| packageName.equals("net.dinglisch.android.taskerm")
				|| packageName.equals("com.google.android.carhome")) {
			return;
		}

		Bitmap icon = null;
		android.app.Notification notification = (android.app.Notification) p;
		boolean isOngoing = (notification.flags & android.app.Notification.FLAG_ONGOING_EVENT) > 0;
		if (event.getEventType() == TYPE_NOTIFICATION_REMOVED) {
			// This is my hacked CyanogenMod telling us a Notification went away
			LCDNotification.removePersistentNotifications(this, isOngoing, packageName.toString(), true);
			haveCMHack = true;
			return;
		}
		PackageManager pm = getPackageManager();
		PackageInfo packageInfo = null;
		String appName = null;
		try {
			packageInfo = pm.getPackageInfo(packageName.toString(), 0);
			appName = packageInfo.applicationInfo.loadLabel(pm).toString();
			int iconId = (LCDNotification.useAppIconInstead(packageName) || notification.icon == 0)
					? packageInfo.applicationInfo.icon
					: notification.icon;
			icon = NotificationIconShrinker.shrink(
					pm.getResourcesForApplication(packageInfo.applicationInfo),
					iconId, packageName.toString(), NotificationIconShrinker.ICON_SIZE);
		} catch (NameNotFoundException e) {
			/* OK, appName is null */
		}

		if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
			if (Preferences.logging) Log.d(MetaWatch.TAG,
					"MetaWatchAccessibilityService.onAccessibilityEvent(): Received event, packageName = '"
							+ packageName + "' className = '" + className + "'");
	
			if (Preferences.logging) Log.d(MetaWatch.TAG,
					"MetaWatchAccessibilityService.onAccessibilityEvent(): notification text = '"
							+ notification.tickerText + "' flags = "
							+ notification.flags + " ("
							+ Integer.toBinaryString(notification.flags) + ")");

			if (isOngoing) {
				String ticker = (notification.tickerText == null) ? null : notification.tickerText.toString();
				if (ticker != null && excludeTicker.matcher(ticker).matches()) return;

				List<CharSequence> l = event.getText();
				String text = "";
				boolean big = false;
				if (LCDNotification.isMusic(packageName) && MediaControl.lastTrack.length() > 0) {
					text = MediaControl.lastTrack + " (" + MediaControl.lastArtist + ")";
				} else {
					int firstNotTicker = (notification.tickerText != null) ? 1 : 0;
					if (firstNotTicker < l.size()) haveCMHack = true;
					if (LCDNotification.isNavigation(packageName) && Preferences.bigNavigation) {
						big = true;
					}
					if (LCDNotification.shouldSkipFirstExpandedLine(packageName)) firstNotTicker++;
					Vector<String> filtered = new Vector<String>();
					for (int i=firstNotTicker; i<l.size(); i++) {
						String s = l.get(i).toString().trim();
						if (! s.isEmpty()) filtered.add(s);
					}
					for (int i=0; i<filtered.size(); i++) {
						String s = filtered.get(i);
						if (i == 0 && excludeLine1.matcher(s).matches()) return;
						if (i+1 < filtered.size() && filtered.get(i+1).toString().trim().startsWith(s)) {
							// { "ConnectBot", "ConnectBot is running" }
							continue;
						}
						if (progressLike.matcher(s).matches()) return;
						if (text.length() > 0 && s.length() > 0 && ! text.endsWith(". ")) {
							text += text.endsWith(".") ? " " : ". ";
						}
						text += s;
					}
					if (text.length() == 0 && ticker != null) {
						text = ticker;
					}
					if (LCDNotification.isNavigation(packageName)) {
						text = LCDNotification.abbreviateNavigation(text);
					}
				}
				if (text.length() > 0) {
					LCDNotification.addPersistentNotification(this, true,
							packageName.toString(), icon, text, big);
				}
				return;
			} else {
				if (LCDNotification.isGmail(packageName) && MetaWatchService.Preferences.notifyGmail) {
					int firstNotTicker = (notification.tickerText != null) ? 1 : 0;
					List<CharSequence> l = event.getText();
					if (firstNotTicker < l.size()) haveCMHack = true;
					if (firstNotTicker + 1 < l.size()) {
						if (progressLike.matcher(l.get(firstNotTicker+1)).matches()) {
							// ICS
							String title = "GMail";
							String subject = l.get(firstNotTicker).toString();
							int colonPos = subject.indexOf(':');
							if (colonPos > 0) {
								title = subject.substring(0, colonPos);
								subject = subject.substring(colonPos + 2);
							}
							NotificationBuilder.createSmart(this, title,
									subject, icon, NotificationBuilder.createVibratePatternFromPreference(
											this, "settingsGmailNumberBuzzes"));
						} else {
							NotificationBuilder.createSmart(this,
									l.get(firstNotTicker+1).toString(),
									l.get(firstNotTicker).toString(), icon,
									NotificationBuilder.createVibratePatternFromPreference(
											this, "settingsGmailNumberBuzzes"));
						}
					}
				}
				LCDNotification.addPersistentNotification(this, false,
						packageName.toString(), icon, null, false);
			}
	
			if (notification.tickerText == null
					|| notification.tickerText.toString().trim().length() == 0) {
				if (Preferences.logging) Log.d(MetaWatch.TAG,
						"MetaWatchAccessibilityService.onAccessibilityEvent(): Empty text, ignoring.");
				return;
			}
	
			SharedPreferences sharedPreferences = PreferenceManager
					.getDefaultSharedPreferences(this);
	
			/* Forward calendar event */
			if (packageName.equals("com.android.calendar")) {
				if (sharedPreferences.getBoolean("NotifyCalendar", true)) {
					if (Preferences.logging) Log.d(MetaWatch.TAG,
							"onAccessibilityEvent(): Sending calendar event: '"
									+ notification.tickerText + "'.");
					NotificationBuilder.createCalendar(this,
							notification.tickerText.toString() );
					return;
				}
			}
			
			/* Forward google chat or voice event */
			if (packageName.equals("com.google.android.gsf") || packageName.equals("com.google.android.apps.googlevoice")) {
				if (sharedPreferences.getBoolean("notifySMS", true)) {
					if (Preferences.logging) Log.d(MetaWatch.TAG,
							"onAccessibilityEvent(): Sending SMS event: '"
									+ notification.tickerText + "'.");
					NotificationBuilder.createSMS(this,"Google Message" ,notification.tickerText.toString());
					return;
				}
			}
			
			
			/* Deezer track notification */
			if (packageName.equals("deezer.android.app")) {
				
				String text = notification.tickerText.toString().trim();
				
				int truncatePos = text.indexOf(" - ");
				if (truncatePos>-1)
				{
					String artist = text.substring(0, truncatePos);
					String track = text.substring(truncatePos+3);
					
					MediaControl.updateNowPlaying(this, artist, "", track, packageName.toString());
					
					return;
				}
				
				return;
			}
			
			if ((notification.flags & android.app.Notification.FLAG_ONGOING_EVENT) > 0) {
				/* Ignore updates to ongoing events. */
				if (Preferences.logging) Log.d(MetaWatch.TAG,
						"MetaWatchAccessibilityService.onAccessibilityEvent(): Ongoing event, ignoring.");
				return;
			}
			
			/* Some other notification */
			if (sharedPreferences.getBoolean("NotifyOtherNotification", true)) {
	
				String appBlacklist = sharedPreferences.getString("appBlacklist",
						AppBlacklist.DEFAULT_BLACKLIST);
	
				/* Ignore if on blacklist */
				if (appBlacklist.contains(packageName)) {
					if (Preferences.logging) Log.d(MetaWatch.TAG,
							"onAccessibilityEvent(): App is blacklisted, ignoring.");
					return;
				}
	
				try {
					packageInfo = pm.getPackageInfo(packageName.toString(), 0);
					appName = packageInfo.applicationInfo.loadLabel(pm).toString();
					if (icon == null) {
						Drawable d = packageInfo.applicationInfo.loadIcon(pm);
						icon = NotificationIconShrinker.shrink(d, packageName.toString(),
								NotificationIconShrinker.ICON_SIZE);
					}
	
				} catch (NameNotFoundException e) {
					/* OK, appName is null */
				}
	
				if (appName == null) {
					if (Preferences.logging) Log.d(MetaWatch.TAG,
							"onAccessibilityEvent(): Unknown app -- sending notification: '"
									+ notification.tickerText + "'.");
					NotificationBuilder.createOtherNotification(this,
							"Notification", notification.tickerText.toString(), icon);
				} else {
					if (Preferences.logging) Log.d(MetaWatch.TAG,
							"onAccessibilityEvent(): Sending notification: app='"
									+ appName + "' notification='"
									+ notification.tickerText + "'.");
					NotificationBuilder.createOtherNotification(this, appName,
							notification.tickerText.toString(), icon);
				}
			}
		}
		else if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
		{
			if (! haveCMHack) {
				// In the absence of a patched Android, just guess that notifications
				// probably go away when you enter the app that sent them
				LCDNotification.removePersistentNotifications(this, false, event.getPackageName().toString(), true);
			}

			String newActivity = className.toString();
			if (currentActivity.startsWith("com.fsck.k9")) {
				if (!newActivity.startsWith("com.fsck.k9")) {
					// User has switched away from k9, so refresh the read count
					Utils.refreshUnreadK9Count(this);
					Idle.updateIdle(this, true);
				}
			}
			
			currentActivity = newActivity;
		}
	}

	@Override
	public void onInterrupt() {
		/* Do nothing */
	}

}
