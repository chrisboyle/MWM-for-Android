package org.metawatch.manager;

import java.util.Iterator;
import java.util.LinkedList;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.StaticLayout;
import android.util.Log;

class LCDNotification {
	static LinkedList<LCDNotification> iconNotifications;
	static LinkedList<LCDNotification> ongoingNotifications;
	String packageName;
	Bitmap icon;
	String text;
	StaticLayout _staticLayout;

	static {
		iconNotifications = new LinkedList<LCDNotification>();
		ongoingNotifications = new LinkedList<LCDNotification>();
	}

	static boolean isMusic(CharSequence packageName)
	{
		String p = packageName.toString();
		return p.equals("com.android.music")
				|| p.equals("com.google.android.music")
				|| p.equals("com.htc.music")
				|| p.equals("com.nullsoft.winamp");
	}

	static boolean useAppIconInstead(CharSequence packageName)
	{
		return isMusic(packageName);
	}

	static boolean shouldSkipFirstExpandedLine(CharSequence packageName) {
		return packageName.toString().equals("com.google.android.apps.maps");
	}

	public LCDNotification(String p, Bitmap i, String t) {
		packageName = p;
		icon = i;
		text = t;
	}

	static void addPersistentNotification(Context context, boolean ongoing,
			String packageName, Bitmap b, String s)
	{
		LinkedList<LCDNotification> l = ongoing ? ongoingNotifications : iconNotifications;
		synchronized(l) {
			removePersistentNotifications(context, ongoing, packageName, false);
			Log.d(MetaWatch.TAG,
					"MetaWatchAccessibilityService.onAccessibilityEvent(): Adding notification for "+packageName);
			l.addFirst(new LCDNotification(packageName, b, s));
		}
		Idle.updateLcdIdle(context);
		MetaWatchService.notifyClients();
	}

	static void removePersistentNotifications(Context context, boolean ongoing,
			String packageName, boolean notify)
	{
		Log.d(MetaWatch.TAG,
				"MetaWatchAccessibilityService.onAccessibilityEvent(): Removing notifications for "+packageName);
		LinkedList<LCDNotification> l = ongoing ? ongoingNotifications : iconNotifications;
		synchronized(l) {
			Iterator<LCDNotification> li = l.iterator();
			while (li.hasNext()) {
				if (li.next().packageName.equals(packageName)) {
					li.remove();
					if (notify) {
						Idle.updateLcdIdle(context);
						MetaWatchService.notifyClients();
					}
					break;
				}
			}
		}
	}
}