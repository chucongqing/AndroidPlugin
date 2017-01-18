/*
 * Copyright 2013 nishino.keiichiro@gmail.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mobirix.battlefieldcommander.activity;

import android.app.NativeActivity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.mobirix.battlefieldcommander.BillingPlugin;
//import com.mobirix.battlefieldcommander.QuickstartPreferences;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.rj.sdk.GloryGameSDK;
import com.unity3d.player.UnityPlayer;

import java.util.HashMap;
import java.util.Iterator;


public class BillingNativeActivity extends NativeActivity
{
	private static final String TAG = "activity";

	private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	private static final int RC_SIGN_IN = 9001;

	private BroadcastReceiver mRegistrationBroadcastReceiver;
	private boolean isReceiverRegistered;

	private FirebaseAnalytics mFirebaseAnalytics;

	private GoogleApiClient mGoogleApiClient;
	private BillingPlugin mBillingPlugin;
	private boolean isGoogleInited = false;
	public void setBillingPlugin(BillingPlugin plugin) {
		mBillingPlugin = plugin;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(" + requestCode + "," + resultCode + "," + data);
		if(requestCode == RC_SIGN_IN)
		{
			super.onActivityResult(requestCode, resultCode, data);
			GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
			handleSignInResult(result);
		}
        else if ((mBillingPlugin != null) && !mBillingPlugin.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data);
        }
        else {
            Log.d(TAG, "onActivityResult handled by IABUtil.");
        }
	}

	public void sendNotification(int id,String title,String message) {
		Intent intent = new Intent(this, BillingNativeActivity.class);
		//intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
				PendingIntent.FLAG_ONE_SHOT);

		Resources res = this.getResources();
		int icon = res.getIdentifier("ic_stat_ic_notification","drawable",this.getPackageName());

		Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
				.setSmallIcon(icon)  //todo
				.setContentTitle(title)
				.setContentText(message)
				.setAutoCancel(true)
				.setSound(defaultSoundUri)
				.setContentIntent(pendingIntent);

		NotificationManager notificationManager =
				(NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

		notificationManager.notify(id /* ID of notification */, notificationBuilder.build());
	}
	
    // Unity Original.
	protected UnityPlayer mUnityPlayer;		// don't change the name of this variable; referenced from native code

	// UnityPlayer.init() should be called before attaching the view to a layout. 
	// UnityPlayer.quit() should be the last thing called; it will terminate the process and not return.
	protected void onCreate (Bundle savedInstanceState)
	{
		mUnityPlayer = new UnityPlayer(this);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		
		getWindow().takeSurface(null);
		getWindow().setFormat(PixelFormat.RGBX_8888);// <--- This makes xperia play happy

		if (mUnityPlayer.getSettings ().getBoolean ("hide_status_bar", true))
			getWindow ().setFlags (WindowManager.LayoutParams.FLAG_FULLSCREEN,
			                       WindowManager.LayoutParams.FLAG_FULLSCREEN);

		View playerView = mUnityPlayer.getView();
		setContentView(playerView);
		playerView.requestFocus();

		Log.d(TAG, "Init firebase");
		mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
		GloryGameSDK.setCurrentActivity(this);
		InitGoogleSignIn();

	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		/* 根据 Deep Link 类型，参考下面内容进行实现 */

		/* 1. 实现 App Scheme Type(myApp://deepLinkAction) 的 Deep Link
		 *
		 * 通过 Deep Link 运行传达的 App Scheme Action
		 * IgawLiveOps.onNewIntent(MainActivity.this, intent);
		 */

		/* 2. 实现 Json 字符串形式({“url”:”deepLinkAction”}) 的 Deep Link
		 *
		 * 通过 Deep Link 提取 Json 字符串，转换为 Json Object
		 * String jsonStr = intent.getStringExtra("com.igaworks.liveops.deepLink");
		 * JSONObject jsonObj;
		 * try {
		 *     jsonObj = new JSONObject(jsonStr);
		 *     //Parsing Json Object，实现需要执行的动作
		 * } catch (Exception e) {
		 *     // TODO: handle exception
		 * }
		 */
	}

	private  void InitGoogleSignIn()
	{
		//The client ID of the server that will verify the integrity of the token
		String serverClientId = "989440815757-2dldn6urmctrav7bfv13rfdomjlumilh.apps.googleusercontent.com";//"989440815757-3skvp7duacbak8ig1p2mm8100i4k4i1a.apps.googleusercontent.com";

		GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
				.requestIdToken(serverClientId)

				.build();

		//manually manage the GoogleApiClient connection lifecycle.
		mGoogleApiClient = new GoogleApiClient.Builder(this)
				.addApi(Auth.GOOGLE_SIGN_IN_API, gso)
				.build();

		Log.d(TAG, "InitGoogleSignIn");
	}

	public void GoogleSignIn()
	{
		Log.d(TAG, "GoogleSignIn Begin");
		Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
		startActivityForResult(signInIntent, RC_SIGN_IN);
	}


	public void FirebaseAnalyticsLogEvent(HashMap<String, String> hmParams, String event)
	{
		/*
		String string = "004-034556";
		String[] parts = string.split("-");
		String part1 = parts[0]; // 004
		String part2 = parts[1]; // 034556
		*/
		Log.d(TAG, "FirebaseAnalyticsLogEvent "+event);
		Bundle bundle = new Bundle();
		Iterator it = hmParams.keySet().iterator();

		while(it.hasNext()) {
			String key = (String)it.next();
			Log.d(TAG, "k= "+ key +" "+" v= " + hmParams.get(key));
			bundle.putString(key,hmParams.get(key));
		}
		mFirebaseAnalytics.logEvent(event, bundle);


/*		bundle.putString(FirebaseAnalytics.Param.ITEM_ID, id);
		bundle.putString(FirebaseAnalytics.Param.ITEM_NAME,name);
		bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE,content_type);
		bundle.putString(FirebaseAnalytics.Param.ITEM_NAME,name);
		mFirebaseAnalytics.logEvent(event, bundle);*/

	}

	//delete information obtained from Google
	private void GoogleRevokeAccess() {
		if(!isGoogleInited)
		{
			return;
		}

		Auth.GoogleSignInApi.revokeAccess(mGoogleApiClient).setResultCallback(
				new ResultCallback<Status>() {
					@Override
					public void onResult(Status status) {
						// [START_EXCLUDE]

						// [END_EXCLUDE]
					}
				});
	}


	public void onConnectionFailed(ConnectionResult connectionResult) {
		// An unresolvable error has occurred and Google APIs (including Sign-In) will not
		// be available.
		Log.d(TAG, "onConnectionFailed:" + connectionResult);
	}

	//You must confirm that GoogleApiClient.onConnected has been called before you call signOut.
	public void GoogleSignOut()
	{
		if (isGoogleInited)
		{
			Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
					new ResultCallback<Status>() {
						@Override
						public void onResult(Status status) {
							if(status.isSuccess())
							{
								isGoogleInited = false;
							}
						}
					});
		}

	}

	private  void SendToUnity(String msg_type,String msg)
	{
		//GetGcmMessage
		msg = msg_type + "|" + msg;
		UnityPlayer.UnitySendMessage("PlatFormWWW","GetJavaMsg",msg);
	}

	private void handleSignInResult(GoogleSignInResult result) {
		Log.d(TAG, "handleSignInResult:" + result.isSuccess());
		if (result.isSuccess()) {
			// Signed in successfully, show authenticated UI.
			GoogleSignInAccount acct = result.getSignInAccount();
			String idToken = acct.getIdToken();
            //TODDO 将Token发送到服务器端进行处理
			isGoogleInited = true;
			Log.d(TAG, "token = :" + idToken);
			int len = idToken.length();
			int bk = len/2;

			SendToUnity("GS_TOKEN1",idToken.substring(0,bk));
			SendToUnity("GS_TOKEN2",idToken.substring(bk,len));

		} else {
			// Signed out, show unauthenticated UI.
			isGoogleInited = false;
			SendToUnity("GS_TOKEN2","0");
		}
	}
	/**
	 * Check the device to make sure it has the Google Play Services APK. If
	 * it doesn't, display a dialog that allows users to download the APK from
	 * the Google Play Store or enable it in the device's system settings.
	 */
	private boolean checkPlayServices() {
		GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
		int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (apiAvailability.isUserResolvableError(resultCode)) {
				apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
						.show();
			} else {
				Log.i(TAG, "This device is not supported.");
				finish();
			}
			return false;
		}
		return true;
	}


//	private void registerReceiver(){
//		if(!isReceiverRegistered) {
//			LocalBroadcastManager.getInstance(this).registerReceiver(mRegistrationBroadcastReceiver,
//					new IntentFilter(QuickstartPreferences.REGISTRATION_COMPLETE));
//			isReceiverRegistered = true;
//		}
//	}
	protected void onDestroy ()
	{
		super.onDestroy();
		mUnityPlayer.quit();

		// Add Billing.
        Log.d(TAG, "Destroying billing plugin.");
        if (mBillingPlugin != null) {
        	mBillingPlugin.dispose();
            mBillingPlugin = null;
        }
	}

	// onPause()/onResume() must be sent to UnityPlayer to enable pause and resource recreation on resume.
	protected void onPause()
	{
//		LocalBroadcastManager.getInstance(this).unregisterReceiver(mRegistrationBroadcastReceiver);
//		isReceiverRegistered = false;

		super.onPause();
		mUnityPlayer.pause();
		if (isFinishing())
			mUnityPlayer.quit();
	}
	protected void onResume()
	{
		super.onResume();
		//registerReceiver();
		mUnityPlayer.resume();
	}
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		mUnityPlayer.configurationChanged(newConfig);
	}
	public void onWindowFocusChanged(boolean hasFocus)
	{
		super.onWindowFocusChanged(hasFocus);
		mUnityPlayer.windowFocusChanged(hasFocus);
	}
	public boolean dispatchKeyEvent(KeyEvent event)
	{
		if (event.getAction() == KeyEvent.ACTION_MULTIPLE)
			return mUnityPlayer.onKeyMultiple(event.getKeyCode(), event.getRepeatCount(), event);
		return super.dispatchKeyEvent(event);
	}

	// Pass any events not handled by (unfocused) views straight to UnityPlayer
	@Override public boolean onKeyUp(int keyCode, KeyEvent event)     { return mUnityPlayer.injectEvent(event); }
	@Override public boolean onKeyDown(int keyCode, KeyEvent event)   { return mUnityPlayer.injectEvent(event); }
	@Override public boolean onTouchEvent(MotionEvent event)          { return mUnityPlayer.injectEvent(event); }
	/*API12*/ public boolean onGenericMotionEvent(MotionEvent event)  { return mUnityPlayer.injectEvent(event); }
}
