package org.metawatch.manager;

import java.util.Iterator;
import java.util.LinkedList;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Log;

public class LCDNotification {
	public static LinkedList<LCDNotification> iconNotifications;
	public static LinkedList<LCDNotification> ongoingNotifications;
	String packageName;
	public Bitmap icon;
	public String text;
	public boolean big;
	private StaticLayout staticLayout;

	static {
		iconNotifications = new LinkedList<LCDNotification>();
		ongoingNotifications = new LinkedList<LCDNotification>();
	}

	static boolean isGmail(CharSequence packageName)
	{
		String p = packageName.toString();
		return p.equals("com.google.android.gm");
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

	static String abbreviateNavigation(String text)
	{
		return text.replace("Turn left", "Left")
				.replace("Turn right", "Right")
				.replace(" the "," ")
				.replace(" roundabout", " rdb")
				.replace(" onto ", " on ");
	}

	static boolean isNavigation(CharSequence packageName) {
		return packageName.toString().equals("com.google.android.apps.maps");
	}

	static boolean shouldSkipFirstExpandedLine(CharSequence packageName) {
		return packageName.toString().equals("com.google.android.apps.maps");
	}

	public int makeTextLayout(int len, TextPaint p)
	{
		String s = (len < 0 || len >= text.length()) ? text : text.substring(0, len-3)+"...";
		staticLayout = new StaticLayout(s, p, 80,
				android.text.Layout.Alignment.ALIGN_NORMAL, 1.1f,
				0, false);
		return staticLayout.getHeight();
	}

	public int getTextHeight() { return staticLayout.getHeight(); }

	public void drawText(Canvas c)
	{
		staticLayout.draw(c);
	}

	public LCDNotification(String p, Bitmap i, String t, boolean big_) {
		packageName = p;
		icon = i;
		text = t;
		big = big_;
	}

	static void addPersistentNotification(Context context, boolean ongoing,
			String packageName, Bitmap b, String s, boolean big)
	{
		LinkedList<LCDNotification> l = ongoing ? ongoingNotifications : iconNotifications;
		synchronized(l) {
			removePersistentNotifications(context, ongoing, packageName, false);
			Log.d(MetaWatch.TAG,
					"MetaWatchAccessibilityService.onAccessibilityEvent(): Adding notification for "+packageName);
			l.addFirst(new LCDNotification(packageName, b, s, big));
		}
		Idle.updateIdle(context, true);
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
						Idle.updateIdle(context, true);
						MetaWatchService.notifyClients();
					}
					break;
				}
			}
		}
	}
}