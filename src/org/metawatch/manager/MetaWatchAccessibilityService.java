package org.metawatch.manager;

import java.util.regex.Pattern;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.util.Log;
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

	// These apps should all be using the sync adapter framework instead of
	// pointlessly notifying me...
	static Pattern sillyOngoings = Pattern.compile(".*\\b(updat|sync(h(roni[sz])?)?|refresh)ing\\b.*",
			Pattern.CASE_INSENSITIVE);

	@Override
	public void onAccessibilityEvent(AccessibilityEvent event) {

		/* Acquire details of event. */
		CharSequence packageName = event.getPackageName();
		CharSequence className = event.getClassName();
		Log.d(MetaWatch.TAG,
				"MetaWatchAccessibilityService.onAccessibilityEvent(): Received event, packageName = '"
						+ packageName + "' className = '" + className + "'");

		if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
			LCDNotification.removePersistentNotifications(this, false, event.getPackageName().toString(), true);
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

		if (packageName.equals("com.google.android.gsf")) {
			// GSF might notify for other reasons, but this is a very common case:
			// Get a better label, and match the package later to clear the icon when Talk is opened
			packageName = "com.google.android.talk";
		} else if (packageName.equals("org.metawatch.manager")) {
			// Hey, that's me!
			return;
		}

		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		PackageManager pm = getPackageManager();
		PackageInfo packageInfo = null;
		String appName = null;
		boolean isOngoing = (notification.flags & android.app.Notification.FLAG_ONGOING_EVENT) > 0;
		Bitmap icon = null;
		if (event.getRemovedCount() > 0) {
			// This is my hacked CyanogenMod telling us a Notification went away
			LCDNotification.removePersistentNotifications(this, isOngoing, event.getPackageName().toString(), true);
			return;
		}
		try {
			packageInfo = pm.getPackageInfo(packageName.toString(), 0);
			if (notification.icon != 0) {
				icon = NotificationIconShrinker.shrink(
						pm.getResourcesForApplication(packageInfo.applicationInfo),
						notification.icon, NotificationIconShrinker.ICON_SIZE,
						NotificationIconShrinker.needsLowThreshold(packageName.toString()));
				if (icon != null && ! isOngoing) {
					String t = (notification.tickerText == null) ? null : notification.tickerText.toString();
					LCDNotification.addPersistentNotification(this, false, packageName.toString(), icon, t);
				}
			}
			if (isOngoing && notification.tickerText != null
					&& ! sillyOngoings.matcher(notification.tickerText).matches()) {
				LCDNotification.addPersistentNotification(this, true,
						packageName.toString(), icon, notification.tickerText.toString());
			}
			appName = packageInfo.applicationInfo.loadLabel(pm).toString();
		} catch (NameNotFoundException e) {
			/* OK, appName is null */
		}

		if (isOngoing) {
			/* Ignore updates to ongoing events. */
			Log.d(MetaWatch.TAG,
					"MetaWatchAccessibilityService.onAccessibilityEvent(): Ongoing event, ignoring.");
			return;
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
						"Notification", notification.tickerText.toString(),
						icon);
			} else {
				Log.d(MetaWatch.TAG,
						"onAccessibilityEvent(): Sending notification: app='"
								+ appName + "' notification='"
								+ notification.tickerText + "'.");
				NotificationBuilder.createOtherNotification(this, appName,
						notification.tickerText.toString(), icon);
			}
		}
	}

	@Override
	public void onInterrupt() {
		/* Do nothing */
	}

}
