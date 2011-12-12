package org.metawatch.manager;

import java.util.Iterator;
import java.util.LinkedList;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import android.view.accessibility.AccessibilityEvent;

public class MetaWatchAccessibilityService extends AccessibilityService {

	@Override
	protected void onServiceConnected() {
		super.onServiceConnected();
		AccessibilityServiceInfo asi = new AccessibilityServiceInfo();
		asi.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED
				| AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
		asi.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
		asi.flags = AccessibilityServiceInfo.DEFAULT;
		asi.notificationTimeout = 100;
		setServiceInfo(asi);

		// ArrayList<PInfo> apps = getInstalledApps(true);
		// for (PInfo pinfo : apps) {
		// appsByPackage.put(pinfo.pname, pinfo);
		// }
	}

	static LinkedList<Pair<String,Bitmap>> notificationIcons;
	static {
		notificationIcons = new LinkedList<Pair<String,Bitmap>>();
	}

	void addPersistentNotification(Notification n, PackageManager pm, PackageInfo pi)
	{
		if (n.icon == 0) return;
		try {
			Bitmap b = NotificationIconShrinker.shrink(
					pm.getResourcesForApplication(pi.applicationInfo), n.icon);
			if (b == null) return;
			synchronized(notificationIcons) {
				removePersistentNotifications(pi.packageName, false);
				Log.d(MetaWatch.TAG,
						"MetaWatchAccessibilityService.onAccessibilityEvent(): Adding notification for "+pi.packageName);
				notificationIcons.addFirst(new Pair<String,Bitmap>(pi.packageName, b));
			}
			Idle.updateLcdIdle(this);
			MetaWatchService.notifyClients();
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
	}

	void removePersistentNotifications(String packageName, boolean notify)
	{
		Log.d(MetaWatch.TAG,
				"MetaWatchAccessibilityService.onAccessibilityEvent(): Removing notifications for "+packageName);
		synchronized(notificationIcons) {
			Iterator<Pair<String, Bitmap>> li = notificationIcons.iterator();
			while (li.hasNext()) {
				if (li.next().first.equals(packageName)) {
					li.remove();
					if (notify) {
						Idle.updateLcdIdle(this);
						MetaWatchService.notifyClients();
					}
					break;
				}
			}
		}
	}

	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {

		/* Acquire details of event. */
		CharSequence packageName = event.getPackageName();
		CharSequence className = event.getClassName();
		Log.d(MetaWatch.TAG,
				"MetaWatchAccessibilityService.onAccessibilityEvent(): Received event, packageName = '"
						+ packageName + "' className = '" + className + "'");

		if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
			removePersistentNotifications(event.getPackageName().toString(), true);
			return;
		}

		Parcelable p = event.getParcelableData();
		if (p instanceof android.app.Notification == false) {
			Log.d(MetaWatch.TAG,
					"MetaWatchAccessibilityService.onAccessibilityEvent(): Not a real notification, ignoring.");
			return;
		}

		android.app.Notification notification = (android.app.Notification) p;
		Log.d(MetaWatch.TAG,
				"MetaWatchAccessibilityService.onAccessibilityEvent(): notification text = '"
						+ notification.tickerText + "' flags = "
						+ notification.flags + " ("
						+ Integer.toBinaryString(notification.flags) + ")");

		if ((notification.flags & android.app.Notification.FLAG_ONGOING_EVENT) > 0) {
			/* Ignore updates to ongoing events. */
			Log.d(MetaWatch.TAG,
					"MetaWatchAccessibilityService.onAccessibilityEvent(): Ongoing event, ignoring.");
			return;
		}

		if (packageName.equals("com.google.android.gsf")) {
			// GSF might notify for other reasons, but this is a very common case:
			// Get a better label, and match the package later to clear the icon when Talk is opened
			packageName = "com.google.android.talk";
		}

		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		PackageManager pm = getPackageManager();
		PackageInfo packageInfo = null;
		String appName = null;
		try {
			packageInfo = pm.getPackageInfo(packageName.toString(), 0);
			addPersistentNotification(notification, pm, packageInfo);
			appName = packageInfo.applicationInfo.loadLabel(pm).toString();
		} catch (NameNotFoundException e) {
			/* OK, appName is null */
		}

		if (notification.tickerText == null
				|| notification.tickerText.toString().trim().length() == 0) {
			Log.d(MetaWatch.TAG,
					"MetaWatchAccessibilityService.onAccessibilityEvent(): Empty text, ignoring.");
			return;
		}

		/* Forward calendar event */
		if (packageName.equals("com.android.calendar")) {
			if (sharedPreferences.getBoolean("NotifyCalendar", true)) {
				Log.d(MetaWatch.TAG,
						"onAccessibilityEvent(): Sending calendar event: '"
								+ notification.tickerText + "'.");
				NotificationBuilder.createCalendar(this,
						notification.tickerText.toString());
				return;
			}
		}

		/* Some other notification */
		if (sharedPreferences.getBoolean("NotifyOtherNotification", true)) {

			String appBlacklist = sharedPreferences.getString("appBlacklist",
					AppBlacklist.DEFAULT_BLACKLIST);

			/* Ignore if on blacklist */
			if (appBlacklist.contains(packageName)) {
				Log.d(MetaWatch.TAG,
						"onAccessibilityEvent(): App is blacklisted, ignoring.");
				return;
			}

			if (appName == null) {
				Log.d(MetaWatch.TAG,
						"onAccessibilityEvent(): Unknown app -- sending notification: '"
								+ notification.tickerText + "'.");
				NotificationBuilder.createOtherNotification(this,
						"Notification", notification.tickerText.toString());
			} else {
				Log.d(MetaWatch.TAG,
						"onAccessibilityEvent(): Sending notification: app='"
								+ appName + "' notification='"
								+ notification.tickerText + "'.");
				NotificationBuilder.createOtherNotification(this, appName,
						notification.tickerText.toString());
			}
		}
	}

	@Override
	public void onInterrupt() {
		/* Do nothing */
	}

}
