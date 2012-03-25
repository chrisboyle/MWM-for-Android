                                                                     
                                                                     
                                                                     
                                             
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
  * CallStateListener.java                                                    *
  * CallStateListener                                                         *
  * Listener waiting for incoming call                                        *
  *                                                                           *
  *                                                                           *
  *****************************************************************************/

package org.metawatch.manager;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;


class CallStateListener extends PhoneStateListener {
	
	Context context;
	private int mPhoneState;
	private SignalStrength mSignalStrength;
	private int mNetworkType = 0;
	private boolean mAlwaysUseCdmaRssi = false;  // TODO?
	private boolean samsungCdma = false;  // TODO? SystemProperties.get("ro.ril.samsung_cdma").equals("true")
	private boolean mHspaDataDistinguishable = true;  // TODO?
	
	public CallStateListener(Context ctx) {
		super();
		context = ctx;
	}

	@Override
	public void onCallStateChanged(int state, String incomingNumber) {
		super.onCallStateChanged(state, incomingNumber);
		mPhoneState = state;
		
		if (!MetaWatchService.Preferences.notifyCall)
			return;
		
		if (incomingNumber == null)
			incomingNumber = "";

		switch (state) {
			case TelephonyManager.CALL_STATE_RINGING: 
				//String name = Utils.getContactNameFromNumber(context, incomingNumber);	
				//SendCommand.sendIncomingCallStart(incomingNumber, name, photo);
				Call.startCall(context, incomingNumber);
				break;
			case TelephonyManager.CALL_STATE_IDLE: 
				Call.endCall(context);
				break;
			case TelephonyManager.CALL_STATE_OFFHOOK: 
				Call.endCall(context);
				break;
		}

	}

	@Override
	public void onDataConnectionStateChanged(int state, int networkType)
	{
		mNetworkType = networkType;
		updateDataNetType();
		updateSignalStrength();
	}

	/** Extracted from CyanogenMod:
	 *  https://github.com/CyanogenMod/android_frameworks_base/blob/gingerbread-release/packages/SystemUI/src/com/android/systemui/statusbar/StatusBarPolicy.java */
	@Override
	public void onSignalStrengthsChanged(SignalStrength signalStrength)
	{
		mSignalStrength = signalStrength;
		updateSignalStrength();
	}

	/** Extracted from CyanogenMod:
	 *  https://github.com/CyanogenMod/android_frameworks_base/blob/gingerbread-release/packages/SystemUI/src/com/android/systemui/statusbar/StatusBarPolicy.java */
	@Override
	public void onServiceStateChanged(ServiceState state)
	{
		updateSignalStrength();
	}

	private final void updateDataNetType() {
		String label = "";
		switch (mNetworkType) {
			case TelephonyManager.NETWORK_TYPE_EDGE:
				label = "E";
				break;
			case TelephonyManager.NETWORK_TYPE_UMTS:
				label = "3G";
				break;
			case TelephonyManager.NETWORK_TYPE_HSDPA:
			case TelephonyManager.NETWORK_TYPE_HSUPA:
			case TelephonyManager.NETWORK_TYPE_HSPA:
				if (mHspaDataDistinguishable) {
					label = "H";
				} else {
					label = "3G";
				}
				break;
			case TelephonyManager.NETWORK_TYPE_CDMA:
				// display 1xRTT for IS95A/B
				label = "1X";
				break;
			case TelephonyManager.NETWORK_TYPE_1xRTT:
				label = "1X";
				break;
			case TelephonyManager.NETWORK_TYPE_EVDO_0: //fall through
			case TelephonyManager.NETWORK_TYPE_EVDO_A:
			case TelephonyManager.NETWORK_TYPE_EVDO_B:
			case TelephonyManager.NETWORK_TYPE_EHRPD:
				label = "3G";
				break;
			case TelephonyManager.NETWORK_TYPE_LTE:
			case TelephonyManager.NETWORK_TYPE_HSPAP:
				label = "4G";
				break;
			default:
				label = "G";
				break;
		}
		if (label != Monitors.SignalData.phoneDataType) {
			Monitors.SignalData.phoneDataType = label;
			Idle.updateIdle(context, true);
		}

	}

	private void updateSignalStrength()
	{
		int iconLevel = 0;
		boolean roaming = false;
		if (mSignalStrength == null) return;
		TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);

		if (mSignalStrength.isGsm()) {
			int asu = mSignalStrength.getGsmSignalStrength();

			// ASU ranges from 0 to 31 - TS 27.007 Sec 8.5
			// asu = 0 (-113dB or less) is very weak
			// signal, its better to show 0 bars to the user in such cases.
			// asu = 99 is a special case, where the signal strength is unknown.
			if (asu <= 2 || asu == 99) iconLevel = 0;
			else if (asu >= 12) iconLevel = 4;
			else if (asu >= 8)  iconLevel = 3;
			else if (asu >= 5)  iconLevel = 2;
			else iconLevel = 1;

			roaming = tm.isNetworkRoaming();
		} else {
			// If 3G(EV) and 1x network are available than 3G should be
			// displayed, displayed RSSI should be from the EV side.
			// If a voice call is made then RSSI should switch to 1x.

			// Samsung CDMA devices handle signal strength display differently
			// relying only on cdmaDbm - thanks Adr0it for the assistance here
			if (samsungCdma) {
				final int cdmaDbm = mSignalStrength.getCdmaDbm();
				if (cdmaDbm >= -75) iconLevel = 4;
				else if (cdmaDbm >= -85) iconLevel = 3;
				else if (cdmaDbm >= -95) iconLevel = 2;
				else if (cdmaDbm >= -100) iconLevel = 1;
				else iconLevel = 0;
			} else {
				if ((mPhoneState == TelephonyManager.CALL_STATE_IDLE) && isEvdo()
						&& !mAlwaysUseCdmaRssi) {
					iconLevel = getEvdoLevel(mSignalStrength);
				} else {
					if ((mPhoneState == TelephonyManager.CALL_STATE_IDLE) && isEvdo()){
						iconLevel = getEvdoLevel(mSignalStrength);
					} else {
						iconLevel = getCdmaLevel(mSignalStrength);
					}
				}
			}
		}
		if (iconLevel != Monitors.SignalData.phoneBars
				|| roaming != Monitors.SignalData.roaming) {
			Monitors.SignalData.phoneBars = iconLevel;
			Monitors.SignalData.roaming = roaming;
			Idle.updateIdle(context, true);
		}
	}

	private boolean isEvdo()
	{
		return mNetworkType == TelephonyManager.NETWORK_TYPE_EVDO_0
				|| mNetworkType == TelephonyManager.NETWORK_TYPE_EVDO_A
				|| mNetworkType == TelephonyManager.NETWORK_TYPE_EVDO_B;
	}

	/** Extracted from CyanogenMod:
	 *  https://github.com/CyanogenMod/android_frameworks_base/blob/gingerbread-release/packages/SystemUI/src/com/android/systemui/statusbar/StatusBarPolicy.java */
	private int getCdmaLevel(SignalStrength mSignalStrength) {
		final int cdmaDbm = mSignalStrength.getCdmaDbm();
		final int cdmaEcio = mSignalStrength.getCdmaEcio();
		int levelDbm = 0;
		int levelEcio = 0;

		if (cdmaDbm >= -75) levelDbm = 4;
		else if (cdmaDbm >= -85) levelDbm = 3;
		else if (cdmaDbm >= -95) levelDbm = 2;
		else if (cdmaDbm >= -100) levelDbm = 1;
		else levelDbm = 0;

		// Ec/Io are in dB*10
		if (cdmaEcio >= -90) levelEcio = 4;
		else if (cdmaEcio >= -110) levelEcio = 3;
		else if (cdmaEcio >= -130) levelEcio = 2;
		else if (cdmaEcio >= -150) levelEcio = 1;
		else levelEcio = 0;

		return (levelDbm < levelEcio) ? levelDbm : levelEcio;
	}

	/** Extracted from CyanogenMod:
	 *  https://github.com/CyanogenMod/android_frameworks_base/blob/gingerbread-release/packages/SystemUI/src/com/android/systemui/statusbar/StatusBarPolicy.java */
	private int getEvdoLevel(SignalStrength mSignalStrength) {
		int evdoDbm = mSignalStrength.getEvdoDbm();
		int evdoSnr = mSignalStrength.getEvdoSnr();
		int levelEvdoDbm = 0;
		int levelEvdoSnr = 0;

		if (evdoDbm >= -65) levelEvdoDbm = 4;
		else if (evdoDbm >= -75) levelEvdoDbm = 3;
		else if (evdoDbm >= -90) levelEvdoDbm = 2;
		else if (evdoDbm >= -105) levelEvdoDbm = 1;
		else levelEvdoDbm = 0;

		if (evdoSnr >= 7) levelEvdoSnr = 4;
		else if (evdoSnr >= 5) levelEvdoSnr = 3;
		else if (evdoSnr >= 3) levelEvdoSnr = 2;
		else if (evdoSnr >= 1) levelEvdoSnr = 1;
		else levelEvdoSnr = 0;

		return (levelEvdoDbm < levelEvdoSnr) ? levelEvdoDbm : levelEvdoSnr;
	}


}
