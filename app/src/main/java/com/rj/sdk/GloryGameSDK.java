package com.rj.sdk;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.NativeActivity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.UUID;

public class GloryGameSDK {
    private static Activity _gameActivity = null;

	public static void setCurrentActivity(Activity gameActivity)
    {
        _gameActivity = gameActivity;
    }
	
	public static String getLanguage()
	{
		Locale locale =  _gameActivity.getResources().getConfiguration().locale;
        String language = locale.getLanguage();
        return language;
	}
	public static String getCountry()
	{
		Locale locale = _gameActivity.getResources().getConfiguration().locale;
        String language = locale.getCountry();
        return language;
	}
	public static String  getIMEI() {
		
		TelephonyManager TelephonyMgr = (TelephonyManager)_gameActivity.getBaseContext().getSystemService(Context.TELEPHONY_SERVICE); 
		String szImei = TelephonyMgr.getDeviceId();
		return szImei;
	}
	public static String getIpAddress() {  
	    try {  
	        for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {  
	            NetworkInterface intf = en.nextElement();  
	            for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {  
	                InetAddress inetAddress = enumIpAddr.nextElement();  
	                if (!inetAddress.isLoopbackAddress()) {  
	                    return inetAddress.getHostAddress().toString();  
	                }  
	            }  
	        }  
	    } catch (SocketException ex) {  
	        Log.e("", ex.toString());  
	    }  
	    return "";  
	}  
	public static String getUUID(){
		 
        final TelephonyManager tm = (TelephonyManager) _gameActivity.getBaseContext().getSystemService(NativeActivity.TELEPHONY_SERVICE);   
 
        final String tmDevice, tmSerial, tmPhone, androidId;   
        tmDevice = "" + tm.getDeviceId();  
        tmSerial = "" + tm.getSimSerialNumber();   
        androidId = "" + android.provider.Settings.Secure.getString(_gameActivity.getContentResolver(),android.provider.Settings.Secure.ANDROID_ID);   
        UUID deviceUuid = new UUID(androidId.hashCode(), ((long)tmDevice.hashCode() << 32) | tmSerial.hashCode());   
        String uniqueId = deviceUuid.toString();
        Log.d("debug","uuid="+uniqueId);
 
        return uniqueId;
 
       }
	public static String getTelcoOper(){
		TelephonyManager telManager = (TelephonyManager) _gameActivity.getSystemService(Context.TELEPHONY_SERVICE);
		String operator = telManager.getSimOperator();
		if(operator!=null){
		if(operator.equals("46000") || operator.equals("46002")|| operator.equals("46007")){
			return "Mobile";
		//中国移动

		}else if(operator.equals("46001")){
			return "Unicom";
		//中国联通

		}else if(operator.equals("46003")){
			return "Telecom";
		//中国电信
		}else {
			return "";
		}
		}
		return "";
	}
	
	public static String getProduct(){
		return Build.PRODUCT;
	}
	public static String GetNetworkType()
	{
	    String strNetworkType = "";
	    
	    NetworkInfo networkInfo = ((ConnectivityManager) _gameActivity.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
	    if (networkInfo != null && networkInfo.isConnected())
	    {
	        if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI)
	        {
	            strNetworkType = "WIFI";
	        }
	        else if (networkInfo.getType() == ConnectivityManager.TYPE_MOBILE)
	        {
	            String _strSubTypeName = networkInfo.getSubtypeName();
	            
	            Log.e("cocos2d-x", "Network getSubtypeName : " + _strSubTypeName);
	            
	            // TD-SCDMA   networkType is 17
	            int networkType = networkInfo.getSubtype();
	            switch (networkType) {
	                case TelephonyManager.NETWORK_TYPE_GPRS:
	                case TelephonyManager.NETWORK_TYPE_EDGE:
	                case TelephonyManager.NETWORK_TYPE_CDMA:
	                case TelephonyManager.NETWORK_TYPE_1xRTT:
	                case TelephonyManager.NETWORK_TYPE_IDEN: //api<8 : replace by 11
	                    strNetworkType = "2G";
	                    break;
	                case TelephonyManager.NETWORK_TYPE_UMTS:
	                case TelephonyManager.NETWORK_TYPE_EVDO_0:
	                case TelephonyManager.NETWORK_TYPE_EVDO_A:
	                case TelephonyManager.NETWORK_TYPE_HSDPA:
	                case TelephonyManager.NETWORK_TYPE_HSUPA:
	                case TelephonyManager.NETWORK_TYPE_HSPA:
	                case TelephonyManager.NETWORK_TYPE_EVDO_B: //api<9 : replace by 14
	                case TelephonyManager.NETWORK_TYPE_EHRPD:  //api<11 : replace by 12
	                case TelephonyManager.NETWORK_TYPE_HSPAP:  //api<13 : replace by 15
	                    strNetworkType = "3G";
	                    break;
	                case TelephonyManager.NETWORK_TYPE_LTE:    //api<11 : replace by 13
	                    strNetworkType = "4G";
	                    break;
	                default:
	                    // http://baike.baidu.com/item/TD-SCDMA 中国移动 联通 电信 三种3G制式
	                    if (_strSubTypeName.equalsIgnoreCase("TD-SCDMA") || _strSubTypeName.equalsIgnoreCase("WCDMA") || _strSubTypeName.equalsIgnoreCase("CDMA2000")) 
	                    {
	                        strNetworkType = "3G";
	                    }
	                    else
	                    {
	                        strNetworkType = _strSubTypeName;
	                    }
	                    
	                    break;
	             }
	             
	            Log.e("cocos2d-x", "Network getSubtype : " + Integer.valueOf(networkType).toString());
	        }
	    }
	    
	    Log.e("cocos2d-x", "Network Type : " + strNetworkType);
	    
	    return strNetworkType;
	}
	@TargetApi(23) public static int checkSelfPermission(String permission)
	{
		if(Build.VERSION.SDK_INT <23){
			return 1;
		}
		try{
			if(PackageManager.PERMISSION_GRANTED==_gameActivity.getBaseContext().checkSelfPermission(permission))
			{
				return 2;
			}
		}catch (Exception e){
			return 0;
		}
		
		return 0;
	}
}
