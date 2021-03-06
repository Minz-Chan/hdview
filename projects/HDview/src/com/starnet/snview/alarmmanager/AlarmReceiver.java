package com.starnet.snview.alarmmanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Message;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;

import com.baidu.android.pushservice.PushManager;
import com.baidu.android.pushservice.PushMessageReceiver;
import com.starnet.snview.R;
import com.starnet.snview.component.SnapshotSound;
import com.starnet.snview.syssetting.AnotherAlarmPushManagerActivity;
import com.starnet.snview.util.Base64Util;
import com.starnet.snview.util.ReadWriteXmlUtils;

/**
 * Push消息处理receiver。请编写您需要的回调函数， 一般来说： onBind是必须的，用来处理startWork返回值；
 * onMessage用来接收透传消息； onSetTags、onDelTags、onListTags是tag相关操作的回调；
 * onNotificationClicked在通知被点击时回调； onUnbind是stopWork接口的返回值回调
 * 
 * 返回值中的errorCode，解释如下： 0 - Success 10001 - Network Problem 30600 - Internal
 * Server Error 30601 - Method Not Allowed 30602 - Request Params Not Valid
 * 30603 - Authentication Failed 30604 - Quota Use Up Payment Required 30605 -
 * Data Required Not Found 30606 - Request Time Expires Timeout 30607 - Channel
 * Token Timeout 30608 - Bind Relation Not Found 30609 - Bind Number Too Many
 * 
 * 当您遇到以上返回错误时，如果解释不了您的问题，请用同一请求的返回值requestId和errorCode联系我们追查问题。
 * 
 */
@SuppressLint({ "SdCardPath" })
public class AlarmReceiver extends PushMessageReceiver {
	public static final String TAG = "AlarmReceiver";
	
	public static boolean serviceOpenStatus = false;  // true, opened; false, closed
	public static HashMap<String, Boolean> tagsStatus = new HashMap<String, Boolean>(); // <tagString, tagRegisterStatus>
	
	public static int ERROR_CODE;
	public static boolean applicationOver = false;
	
	
	
	public static final int NOTIFICATION_ID = 0x00001234;
	public static AnotherAlarmPushManagerActivity mActivity;
	public static NetDetectionReceiver mNetDetectionReceiver;
	
	public static final int SUCCESS = 1;
	public static final int FAILURE = 0;
	
	public static final int SERVICE_RSP_START = 0x99990001;
	public static final int SERVICE_RSP_STOP = 0x99990002;
	public static final int SERVICE_RSP_SET_TAG = 0x99990003;
	public static final int SERVICE_RSP_DEL_TAG = 0x99990004;
	public static final int SERVICE_RSP_NULL_ALARM_TAGLIST = 0x99990005;
	
	// 当onSetTags()或onDelTags()的触发 非用户间接触发时，此值为true
	private boolean callFromInternal = false;
	
	/**
	 * 调用PushManager.startWork后，sdk将对push
	 * server发起绑定请求，这个过程是异步的。绑定请求的结果通过onBind返回。 如果您需要用单播推送，需要把这里获取的channel
	 * id和user id上传到应用server中，再调用server接口用channel id和user id给单个手机或者用户推送。
	 * 
	 * @param context
	 *            BroadcastReceiver的执行Context
	 * @param errorCode
	 *            绑定接口返回值，0 - 成功
	 * @param appid
	 *            应用id。errorCode非0时为null
	 * @param userId
	 *            应用user id。errorCode非0时为null
	 * @param channelId
	 *            应用channel id。errorCode非0时为null
	 * @param requestId
	 *            向服务端发起的请求id。在追查问题时有用；
	 * @return none
	 */
	@Override
	public void onBind(Context context, int errorCode, String appid, String userId, String channelId, String requestId) {
		String responseString = "onBind errorCode=" + errorCode + " appid="
				+ appid + " userId=" + userId + " channelId=" + channelId
				+ " requestId=" + requestId;
		Log.d(TAG, responseString);
		
		// 如果返回0，绑定成功；否则，绑定失败
		if (errorCode == 0) {
			// 更新服务状态
			serviceOpenStatus = true;
			
			callFromInternal = true;
			PushManager.listTags(context); // 触发onListTags()进行注册或删除
		} else {
			// 更新服务状态
			serviceOpenStatus = false;
		}
		
		// 传入服务状态信息，通知UI更新
		Message msg = Message.obtain();
		msg.what = SERVICE_RSP_START;
		msg.arg1 = serviceOpenStatus ? SUCCESS : FAILURE;
		AlarmSettingUtils.getInstance().notifyUIChanges(msg);
	}
	
	
	/**
	 * PushManager.stopWork() 的回调函数。
	 * 
	 * @param context
	 *            上下文
	 * @param errorCode
	 *            错误码。0表示从云推送解绑定成功；非0表示失败。
	 * @param requestId
	 *            分配给对云推送的请求的id
	 */
	@Override
	public void onUnbind(Context context, int errorCode, String requestId) {
		String responseString = "onUnbind errorCode=" + errorCode
				+ " requestId = " + requestId;
		Log.d(TAG, responseString);
		
		// 如果返回0，绑定成功；否则，绑定失败
		if (errorCode == 0) {
			// 更新服务状态
			serviceOpenStatus = false;
		} else {
			// 更新服务状态
			serviceOpenStatus = true;
		}
		
		// 传入服务状态信息，通知UI更新
		Message msg = Message.obtain();
		msg.what = SERVICE_RSP_STOP;
		msg.arg1 = serviceOpenStatus ? FAILURE : SUCCESS;
		AlarmSettingUtils.getInstance().notifyUIChanges(msg);
	}
	
	/**
	 * listTags() 的回调函数。
	 * 
	 * @param context
	 *            上下文
	 * @param errorCode
	 *            错误码。0表示列举tag成功；非0表示失败。
	 * @param tags
	 *            当前应用设置的所有tag。
	 * @param requestId
	 *            分配给对云推送的请求的id
	 */
	@Override
	public void onListTags(Context context, int errorCode, List<String> tags, String requestId) {
		String responseString = "onListTags errorCode=" + errorCode + " tags="
				+ tags;
		Log.d(TAG, responseString);
		
		if (!AlarmSettingUtils.getInstance().isInitalized()) {
			return;
		}
		
		List<String> scheduleTags = new ArrayList<String>();  	// 计划注册的用户tag列表
		List<String> toRegisterTags = new ArrayList<String>(); 	// 将要注册的tag列表：in {计划注册tag列表} and not in {已注册tag列表}
		List<String> toDelTags = new ArrayList<String>(); 		// 将要删除的tag列表：not in {计划注册tag列表} and in {已注册tag列表}
		boolean isUserAlarmOpen = AlarmSettingUtils.getInstance()
				.isUserAlarmOpen();
		
		Log.d(TAG, "isUserAlarmOpen:" + isUserAlarmOpen);
		
		if (isUserAlarmOpen) {
			// 获取已启用星云账户tag列表和报警账户tag列表
			scheduleTags = AlarmSettingUtils.getInstance().getAllUserTags();
		} else {
			// 获取已启用星云账户tag列表
			scheduleTags = AlarmSettingUtils.getInstance().getStarnetAccountsTags();
		}
		
		Log.d(TAG, "schemeTags.size():" + scheduleTags.size());
		
		// 如果schemeTags为空，发送消息到ui，且此时若tags不为空，tags中的tag应被请求删除
		if (scheduleTags.size() ==  0) {
			// 通知ui进行按钮状态更新，并根据实际情况决定是否提示
			Message msg = Message.obtain();
			msg.what = SERVICE_RSP_NULL_ALARM_TAGLIST;
			AlarmSettingUtils.getInstance().notifyUIChanges(msg);

			if (tags != null && tags.size() > 0) {
				callFromInternal = true;
				Log.d(TAG, "1Call delTags:" + tags);
				PushManager.delTags(context, tags);
			}
			
			return;
		}
		
		if (tags != null && tags.size() > 0) {
			for (String scheduleTag : scheduleTags) {
				// 如果已有tag列表中没有此用户tag，则加入待注册列表
				if (!tags.contains(scheduleTag)) {
					toRegisterTags.add(scheduleTag);
				}
			}
			// 注册tag列表
			if (toRegisterTags.size() > 0) {
				Log.d(TAG, "2Call setTags:" + toRegisterTags);
				PushManager.setTags(context, toRegisterTags);
			}
			
			for (String registeredTag : tags) {
				// 如果计划注册的用户tag列表中没有此注册过的tag，则加入待删除列表
				if (!scheduleTags.contains(registeredTag)) {
					toDelTags.add(registeredTag);
				}
			}
			// 删除tag列表
			if (toDelTags.size() > 0) {
				Log.d(TAG, "3Call delTags:" + toDelTags);
				PushManager.delTags(context, toDelTags);
			}
			
			// 若tags与scheduleTags包含相同的结果集，则无待注册或待删除的tag，此时应发送空消息通知界面取消加载框
			if (toRegisterTags.size() == 0 && toDelTags.size() == 0) {
				AlarmSettingUtils.getInstance().notifyUIChanges(Message.obtain());
			}
			
		} else {
			// 所有计划注册的用户tag均需注册，无多余用户tag需要删除
			Log.d(TAG, "4Call delTags:" + toDelTags);
			PushManager.setTags(context, scheduleTags);
		}
	}
	
	/**
	 * setTags() 的回调函数。
	 * 
	 * @param context
	 *            上下文
	 * @param errorCode
	 *            错误码。0表示某些tag已经设置成功；非0表示所有tag的设置均失败。
	 * @param successTags
	 *            设置成功的tag
	 * @param failTags
	 *            设置失败的tag
	 * @param requestId
	 *            分配给对云推送的请求的id
	 */
	@Override
	public void onSetTags(Context context, int errorCode, List<String> sucessTags, List<String> failTags, String requestId) {
		String responseString = "onSetTags errorCode=" + errorCode
				+ " sucessTags=" + sucessTags + " failTags=" + failTags
				+ " requestId=" + requestId;
		Log.d(TAG, responseString);
		
		// 如果函数调用由onBind()间接触发，则不另作提示
		if (callFromInternal) {
			callFromInternal = false;
			return;
		}
		
		int result; // SUCCESS：成功；FAILURE，失败；－1；部分设置成功
		if (errorCode == 0) {
			if (failTags == null || 
					(failTags != null && failTags.size() == 0)) {
				// tag全部设置成功，不作处理
				result = SUCCESS;
			} else {
				// TODO tag部分设置成功
				result = -1;
			}
		} else {
			// tag全部设置失败
			result = FAILURE;

		}
		
		// 通知ui进行相应提示
		Message msg = Message.obtain();
		msg.what = SERVICE_RSP_SET_TAG;
		msg.arg1 = result;
		AlarmSettingUtils.getInstance().notifyUIChanges(msg);
	}

	/**
	 * delTags() 的回调函数。
	 * 
	 * @param context
	 *            上下文
	 * @param errorCode
	 *            错误码。0表示某些tag已经删除成功；非0表示所有tag均删除失败。
	 * @param successTags
	 *            成功删除的tag
	 * @param failTags
	 *            删除失败的tag
	 * @param requestId
	 *            分配给对云推送的请求的id
	 */
	@Override
	public void onDelTags(Context context, int errorCode, List<String> sucessTags, List<String> failTags, String requestId) {
		String responseString = "onDelTags errorCode=" + errorCode + " sucessTags=" + sucessTags + " failTags=" + failTags + " requestId=" + requestId;
		Log.d(TAG, responseString);
		
		// 如果函数调用由onBind()间接触发，则不另作提示
		if (callFromInternal) {
			callFromInternal = false;
			return;
		}
		
		int result; // SUCCESS：成功；FAILURE，失败；－1；部分设置成功 
		if (errorCode == 0) {
			if (failTags != null && failTags.size() > 0) {
				// TODO 部分删除失败
				result = -1;
			} else {
				// 全部删除成功
				result = SUCCESS;
			}
		} else {
			// 全部删除失败
			result = FAILURE;
		}
		
		// 通知ui进行相应提示
		Message msg = Message.obtain();
		msg.what = SERVICE_RSP_DEL_TAG;
		msg.arg1 = result;
		AlarmSettingUtils.getInstance().notifyUIChanges(msg);
	}

	/**
	 * 接收透传消息的函数。
	 * 
	 * @param context
	 *            上下文
	 * @param message
	 *            推送的消息
	 * @param customContentString
	 *            自定义内容,为空或者json字符串
	 */
	@Override
	public void onMessage(Context context, String message, String customContentString) {
		String messageString = "透传消息 message=\"" + message + "\" customContentString=" + customContentString;
		Log.d(TAG, messageString);

		if (!TextUtils.isEmpty(message)) {
			try {
				String deviceName = null;
				String alarmTime = null;
				String alarmType = null;
				String alarmContent = null;
				String imageUrl = null;
				String videoUrl = null;
				String pushUserUrl = null;
				JSONObject messageJsonObj = new JSONObject(message);
				JSONObject customContentJsonObj = null;

				if (messageJsonObj.isNull("custom_content") || TextUtils.isEmpty(messageJsonObj.getString("custom_content"))) {
					return;
				} else {
					customContentJsonObj = new JSONObject(messageJsonObj.getString("custom_content"));
				}

				if (!customContentJsonObj.isNull("device_name")) {
					String de = customContentJsonObj.getString("device_name");
					deviceName = Base64Util.snDecode(de); // 需先BASE64解密
				}
				if (!customContentJsonObj.isNull("alarm_time")) {
					alarmTime = Base64Util.snDecode(customContentJsonObj.getString("alarm_time"));
				}
				if (!customContentJsonObj.isNull("alarm_type")) {
					alarmType = Base64Util.snDecode(customContentJsonObj.getString("alarm_type"));
				}
				if (!customContentJsonObj.isNull("alarm_content")) {
					alarmContent = Base64Util.snDecode(customContentJsonObj.getString("alarm_content"));
				}
				if (!customContentJsonObj.isNull("push_user")) {
					pushUserUrl = Base64Util.snDecode(customContentJsonObj
							.getString("push_user"));
				}
				if (!customContentJsonObj.isNull("image_path")) {
					imageUrl = Base64Util.snDecode(customContentJsonObj.getString("image_path"));
				}
				if (!customContentJsonObj.isNull("video_url")) {
					videoUrl = Base64Util.snDecode(customContentJsonObj.getString("video_url"));
				}

				AlarmDevice ad = new AlarmDevice();
				ad.setDeviceName(deviceName);
				ad.setAlarmTime(alarmTime);
				ad.setAlarmType(alarmType);
				ad.setAlarmContent(alarmContent);
				parseImageUrl(ad, imageUrl);
				parseVideoUrl(ad, videoUrl);
				parsePushUser(ad, pushUserUrl);

				ReadWriteXmlUtils.writeAlarm(ad); // 持久化报警信息到文件中

				showNotification(context, ad.getAlarmContent(), deviceName, ad.getIp() + ":" + ad.getPort());
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (Exception e) {
				// error processing
				e.printStackTrace();
			}
		}
	}

	/**
	 * 解析ImageUrl
	 * 
	 * @param ad
	 *            被填充的对象，接收格式化后的信息
	 * @param imageUrl
	 *            ImageUrl字符串
	 * @return 更新后的AlarmDevice对象
	 */
	private AlarmDevice parseImageUrl(AlarmDevice ad, String imageUrl) {
		if (TextUtils.isEmpty(imageUrl)) {
			return ad;
		}

		/*
		 * example:
		 * why:1@http://xy.star-netsecurity.com/p/jtpt/2014-11-24/1416790466581653.
		 * jpg
		 */
		String userPattern = "([a-zA-Z0-9]{1,16})";
		String passwordPattern = "([a-zA-Z0-9]{0,32})";
		String imageUrlPattern = "([a-zA-z]+://[^\\s]*)";
		Pattern pattern = Pattern.compile(userPattern + ":" + passwordPattern + "@" + imageUrlPattern);
		Matcher matcher = pattern.matcher(imageUrl);
		if (matcher.find()) {
			ad.setImageUrl(matcher.group(3));
		} else {
			throw new IllegalStateException("Invalid image url, which should match pattern" + "[username]:[password]@[url]");
		}

		return ad;
	}

	/**
	 * 解析VideoUrl
	 * 
	 * @param ad
	 *            被填充的对象，接收格式化后的信息
	 * @param videoUrl
	 *            VideoUrl字符串
	 * @return 更新后的AlarmDevice对象
	 */
	private AlarmDevice parseVideoUrl(AlarmDevice ad, String videoUrl) {
		if (TextUtils.isEmpty(videoUrl)) {
			return ad;
		}

		/*
		 * example: why:1@owsp://106.91.60.24:8080/chn2
		 * owsp://106.91.60.24:8080/chn2
		 */
		String userPattern = "([a-zA-Z0-9]{1,16})";
		String passwordPattern = "([a-zA-Z0-9]{0,32})";
		String ipPattern = "([\\d]+\\.[\\d]+\\.[\\d]+\\.[\\d]+)";
		String portPattern = "([\\d]+)";
		String channelPattern = "chn([\\d]+)";
		String schemePattern = "owsp";
		boolean isExistUserPass = videoUrl.indexOf("@") != -1;
		Pattern pattern;

		if (isExistUserPass) { // there exists user and password
			pattern = Pattern.compile(userPattern + ":" + passwordPattern + "@"
					+ schemePattern + "://" + ipPattern + ":" + portPattern
					+ "/" + channelPattern);
		} else { // no user and password
			pattern = Pattern.compile(schemePattern + "://" + ipPattern + ":" + portPattern + "/" + channelPattern);
		}
		Matcher matcher = pattern.matcher(videoUrl);
		if (matcher.find()) {
			if (isExistUserPass) {
				ad.setUserName(matcher.group(1)); // user
				ad.setPassword(matcher.group(2)); // password
				ad.setIp(matcher.group(3)); // ip
				ad.setPort(Integer.valueOf(matcher.group(4))); // port
				ad.setChannel(Integer.valueOf(matcher.group(5))); // channel
			} else {
				ad.setIp(matcher.group(1)); // ip
				ad.setPort(Integer.valueOf(matcher.group(2))); // port
				ad.setChannel(Integer.valueOf(matcher.group(3))); // channel
			}
		} else {
			throw new IllegalStateException("Invalid video url, which should match pattern" + "[username]:[password]@[scheme]://[ip]:[port]/[channel]" + " or [scheme]://[ip]:[port]/[channel]");
		}

		return ad;
	}

	/**
	 * 解析PushUser
	 * 
	 * @param ad
	 *            被填充的对象，接收格式化后的信息
	 * @param pushUser
	 *            PushUser字符串
	 * @return 更新后的AlarmDevice对象
	 */
	private AlarmDevice parsePushUser(AlarmDevice ad, String pushUser) {
		if (TextUtils.isEmpty(pushUser)) {
			return ad;
		}

		/*
		 * example: why:1@xy.star-netsecurity.com
		 */
		String pusherUsernamePattern = "([a-zA-Z0-9]{1,32})";
		String pusherPasswordPattern = "([a-zA-Z0-9]{0,128})";
		String pusherDomainPattern = "(([\\w-]+\\.)+[\\w-]+)";
		Pattern pattern = Pattern.compile(pusherUsernamePattern + ":"
				+ pusherPasswordPattern + "@" + pusherDomainPattern);
		Matcher matcher = pattern.matcher(pushUser);
		if (matcher.find()) {
			ad.setPusherUserName(matcher.group(1));
			ad.setPusherPassword(matcher.group(2));
			ad.setPusherDomain(matcher.group(3));
		} else {
			throw new IllegalStateException("Invalid PushUser url, which should match pattern" + "[username]:[password]@[Domain/IP]");
		}
		return ad;
	}

	@SuppressWarnings("deprecation")
	private void showNotification(Context context, String title, String contentTitle, String contentText) {
		Intent intent = new Intent(OnAlarmMessageArrivedReceiver.ACTION);
		PendingIntent contentIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
		Notification notification = new Notification(R.drawable.ic_launcher, title, System.currentTimeMillis());
		notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.notify(NOTIFICATION_ID, notification);
		controlSoundAndShake(context);
	}

	/**
	 * 提醒用户（声音、震动）
	 * @param context 当前环境上下文
	 */
	private void controlSoundAndShake(final Context context) {
		boolean isShakeOpen = AlarmSettingUtils.getInstance().isShakeOpen();
		boolean isSoundOpen = AlarmSettingUtils.getInstance().isSoundOpen();
		if (isSoundOpen) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					SnapshotSound s = new SnapshotSound(context);
					s.playPushSetSound();
				}
			}).start();
		}
		if (isShakeOpen) {
			Vibrator vibrator = (Vibrator) context
					.getSystemService(Context.VIBRATOR_SERVICE);
			long[] pattern = { 50, 200, 50, 200 };
			vibrator.vibrate(pattern, -1);
		}
	}

	/**
	 * 接收通知点击的函数。注：推送通知被用户点击前，应用无法通过接口获取通知的内容。
	 * 
	 * @param context
	 *            上下文
	 * @param title
	 *            推送的通知的标题
	 * @param description
	 *            推送的通知的描述
	 * @param customContentString
	 *            自定义内容，为空或者json字符串
	 */
	@Override
	public void onNotificationClicked(Context context, String title, String description, String customContentString) {
		String notifyString = "通知点击 title=\"" + title + "\" description=\"" + description + "\" customContent=" + customContentString;
		Log.d(TAG, notifyString);
	}
	

//	private SharedPreferences sp;
//	private static Intent intentService;
//	private static boolean started = false;
//	private final String PSXML = "PSXMLFILE";
//
//	/** 保存注册/删除的标志位的值isSucOrFail **/
//	private void saveTagSuccOrFail(Context ctx, boolean isSucOrFail) {
//		sp = ctx.getSharedPreferences(PSXML, Context.MODE_PRIVATE);
//		Editor edt = sp.edit();
//		edt.putBoolean("isRegOrDelSuc", isSucOrFail);
//		edt.commit();
//	}
//
//	/** 关闭服务进程 **/
//	private void closeRegOrDelService(Context ctx) {
//		if (started) {
//			ctx.getApplicationContext().stopService(intentService);
//		}
//	}
//	
//	private static boolean isFirstStart = true;
//	
//	private void updateAlarmPushManagerActivityUI(Context context,int errorCode){
//		Message msg = new Message();
//		Bundle data = new Bundle();
//		data.putInt("errorCode",errorCode);
//		data.putBoolean("remind_push_all_accept", false);
//		msg.setData(data);
//		if(mActivity != null){
//			mActivity.handler.sendMessage(msg);
//		}
//		if(isFirstStart){
//			isFirstStart = false;
//		}
//	}
//	private void updateAlarmPushManagerActivityUIWithSetOrDelTags(Context context, List<String> sucessTags, List<String> failTags,int errorCode) {
//		
//		if(errorCode == 0){
//			if((failTags!=null)&&(failTags.size()>0)){
//				errorCode = -1;
//			}
//		}
//		Message msg = new Message();
//		Bundle data = new Bundle();
//		data.putInt("errorCode",errorCode);
//		data.putBoolean("remind_push_all_accept", true);
//		msg.setData(data);
////		AnotherAlarmPushManagerActivity.mHandler.sendMessage(msg);
//		if(mActivity != null){
//			mActivity.handler.sendMessage(msg);
//		}
//		if(isFirstStart){
//			isFirstStart = false;
//		}
//	}

	@Override
	public void onNotificationArrived(Context arg0, String arg1, String arg2,
			String arg3) {
		
	}
}