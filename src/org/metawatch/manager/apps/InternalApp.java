package org.metawatch.manager.apps;

import android.content.Context;
import android.graphics.Bitmap;

public interface InternalApp {

	
	public class AppData {
		public String id;
		public String name;
		
		boolean supportsDigital = false;
		boolean supportsAnalog = false;
	}
	
	public AppData getInfo();
	
	public void activate(int watchType);
	public void deactivate(int watchType);
	
	public Bitmap update(Context context, int watchType);

}
