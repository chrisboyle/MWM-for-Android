                                                                     
                                                                     
                                                                     
                                             
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
  * Settings.java                                                             *
  * Settings                                                                  *
  * Preference activity                                                       *
  *                                                                           *
  *                                                                           *
  *****************************************************************************/

package org.metawatch.manager;

import org.metawatch.manager.MetaWatchService.Preferences;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceScreen;
import android.util.Log;

public class Settings extends PreferenceActivity {
	
	
	
	Context context;
	
	PreferenceScreen preferenceScreen;
	Preference discovery;
	Preference appBlacklist;
	
	EditTextPreference editTextMac;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		context = this;
		
		addPreferencesFromResource(R.layout.settings);

		preferenceScreen = getPreferenceScreen();
		
	}

	@Override
	protected void onStart() {

		editTextMac = (EditTextPreference) preferenceScreen.findPreference("MAC");
		editTextMac.setText(MetaWatchService.Preferences.watchMacAddress);
		
		discovery = preferenceScreen.findPreference("Discovery");
		discovery.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			public boolean onPreferenceClick(Preference arg0) {
				
				if (Preferences.logging) Log.d(MetaWatch.TAG, "discovery click");
				
				startActivity(new Intent(context, DeviceSelection.class));
				
				return false;
			}
		});
		
		
		appBlacklist = preferenceScreen.findPreference("appBlacklist");
		appBlacklist.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference arg0) {
				startActivity(new Intent(context, AppBlacklist.class));
				return false;
			}
		});

		preferenceScreen.findPreference("RTMAPISignup")
				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference arg0) {
				startActivity(new Intent(Intent.ACTION_VIEW,
						Uri.parse(getString(R.string.rmilk_api_signup_url))));
				return false;
			}
		});

		super.onStart();
	}
	
	
	
	

}
