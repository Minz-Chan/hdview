package com.starnet.snview.alarmmanager;

import java.io.File;

import com.starnet.snview.util.SDCardUtils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

@SuppressLint("SdCardPath")
public class AlarmImageFileCache {

	public static Context context;

	/** 从外置SD卡中获取图片 **/
	public static Bitmap getImageFromExternal(String url) {
		String appName = getApplicationName();
		String[] urls = url.split("/");
		boolean isAvailable = SDCardUtils.isAvailableForExternalSDCard();
		if (!isAvailable) {
			return null;
		}
		
		String tempPath = SDCardUtils.getExternalSDCardPath();
		if(tempPath==null||tempPath.equals("")){
			return null;
		}
		
		String path = tempPath + appName + urls[urls.length - 1];
		File file = new File(path);
		if (file.exists()) {
			Bitmap bmp = BitmapFactory.decodeFile(path);
			return bmp;
		} else {
			return null;
		}
	}
	
	/** 从缓存中获取图片 **/
	public static Bitmap getImageInternal(String url) {
		String appName = getApplicationName();
		String[] urls = url.split("/");
		String path = SDCardUtils.getInternalSDCardPath() + appName + urls[urls.length - 1];
		File file = new File(path);
		if (file.exists()) {
			Bitmap bmp = BitmapFactory.decodeFile(path);
			return bmp;
		} else {
			return null;
		}
	}

	/*** 判断图像文件是否在外置SDCard上 **/
	public static boolean isExistImageFileInExternal(String imgUrl) {
		String appName = getApplicationName();
		boolean isExist = false;
		String[] urls = imgUrl.split("/");
		boolean isAvailable = SDCardUtils.isAvailableForExternalSDCard();
		if (!isAvailable) {
			return false;
		}
		
		String externalPath = SDCardUtils.getExternalSDCardPath();
		if(externalPath==null||externalPath.equals("")){
			return false;
		}
		
		String path = externalPath + appName + urls[urls.length - 1];
		File file = new File(path);
		if (file.exists()) {
			isExist = true;
		}
		return isExist;
	}

	/** 检测内存中是否包含该图像 **/
	public static boolean isExistImgFileInternal(String imgUrl) {
		String appName = getApplicationName();
		String[] urls = imgUrl.split("/");
		boolean isExist = false;
		String path = SDCardUtils.getInternalSDCardPath() + appName + urls[urls.length - 1];
		File file = new File(path);
		if (file.exists()) {
			isExist = true;
		}
		return isExist;
	}

	public static String getApplicationName() {
		PackageManager pkManager = null;
		ApplicationInfo appInfo = null;
		try {
			Context ctx = context.getApplicationContext();
			pkManager = ctx.getPackageManager();
			appInfo = pkManager.getApplicationInfo(context.getPackageName(), 0);
		} catch (PackageManager.NameNotFoundException e) {
			appInfo = null;
		}
		String applicationName = (String) pkManager
				.getApplicationLabel(appInfo);
		return applicationName + "/alarmImg/";
	}
	
	public static String getApplicationName2() {
		PackageManager pkManager = null;
		ApplicationInfo appInfo = null;
		try {
			Context ctx = context.getApplicationContext();
			pkManager = ctx.getPackageManager();
			appInfo = pkManager.getApplicationInfo(context.getPackageName(), 0);
		} catch (PackageManager.NameNotFoundException e) {
			appInfo = null;
		}
		String applicationName = (String) pkManager
				.getApplicationLabel(appInfo);
		return applicationName + "/alarmImg";
	}


}