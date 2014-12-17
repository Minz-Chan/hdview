package com.starnet.snview.syssetting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.starnet.snview.R;
import com.starnet.snview.component.BaseActivity;
import com.starnet.snview.util.ReadWriteXmlUtils;

public class AlarmPushManagerActivity extends BaseActivity {

	private Context ctx;

	boolean isAcc;
	boolean isShake;
	boolean isSound;
	boolean isAllAcc;
	

	private final int REQUESTCODE = 0x0001;
	private List<HashMap<String, Object>> list;
	private AalarmNotifyAdapter alarmNotifyAdapter;
	private CornerListView alarmNotifyListView;// 报警通知（接收、声音和震动的设置）
	private CornerListView alarmUserListView;// 报警账户的listView
	private AlarmUserAdapter alarmUserAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.alarm_push_manager_activity);

		intialViews();
		setListeners();

	}

	private void setListeners() {
		super.getLeftButton().setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				AlarmPushManagerActivity.this.finish();
			}
		});

		super.getRightButton().setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				isAcc = alarmUserAdapter.isClickFlag();
				isShake = alarmNotifyAdapter.isClickFlagSha();
				isSound = alarmNotifyAdapter.isClickFlagSou();
				isAllAcc = alarmNotifyAdapter.isClickFlagAcc();

				Editor editor = ctx.getSharedPreferences("ALARM_PUSHSET_FILE",
						Context.MODE_PRIVATE).edit();
				editor.putBoolean("isAccept", isAcc);
				editor.putBoolean("isShake", isShake);
				editor.putBoolean("isSound", isSound);
				editor.putBoolean("isAllAccept", isAllAcc);
				editor.commit();
				AlarmPushManagerActivity.this.finish();
			}
		});

		alarmUserListView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				if (position == 1) {
					Intent intent = new Intent();
					intent.setClass(ctx, AlarmAccountsPreviewActivity.class);
					startActivityForResult(intent, REQUESTCODE);
				}
			}
		});
	}

	private List<HashMap<String, Object>> settingList;
	private HashMap<String, Object> map;

	private void intialViews() {
		super.hideExtendButton();
		super.getToolbarContainer().setVisibility(View.GONE);
		super.setRightButtonBg(R.drawable.navigation_bar_savebtn_selector);
		super.setLeftButtonBg(R.drawable.navigation_bar_back_btn_selector);
		super.setTitleViewText(getString(R.string.system_setting_alarm_pushset));

		ctx = AlarmPushManagerActivity.this;
		alarmUserListView = (CornerListView) findViewById(R.id.alarmUserListView);
		alarmNotifyListView = (CornerListView) findViewById(R.id.alarmNotifyListView);

		SharedPreferences sp = ctx
				.getSharedPreferences("ALARM_PUSHSET_FILE", 0);
		isAcc = sp.getBoolean("isAccept", true);
		isShake = sp.getBoolean("isShake", true);
		isSound = sp.getBoolean("isSound", true);
		isAllAcc = sp.getBoolean("isAllAccept", true);

		setAalarmNotifyAdapter();
		setAlarmUserAdapter(isAcc);

	}

	private void setAlarmUserAdapter(boolean isAccept) {
		list = new ArrayList<HashMap<String, Object>>();
		HashMap<String, Object> map1 = new HashMap<String, Object>();
		map1.put("text", getString(R.string.system_setting_alarmuser_accept));
		list.add(map1);

		HashMap<String, Object> map2 = new HashMap<String, Object>();
		List<CloudAccount> accounts = new ArrayList<CloudAccount>();
		accounts = ReadWriteXmlUtils.getAlarmPushUsersFromXML();
		String content = "";
		if (accounts == null || (accounts != null && accounts.size() == 0)) {
			content = getString(R.string.system_setting_alarmuser_null);
		} else if (accounts != null && accounts.size() > 0) {
			for (int i = 0; i < accounts.size(); i++) {
				String result = accounts.get(i).getUsername() + "/";
				content += result;
			}
		}
		map2.put("text", content);
		list.add(map2);
		alarmUserAdapter = new AlarmUserAdapter(this, list, isAccept);
		alarmUserListView.setAdapter(alarmUserAdapter);
	}

	private void setAalarmNotifyAdapter() {
		settingList = new ArrayList<HashMap<String, Object>>();
		map = new HashMap<String, Object>();
		map.put("text", getString(R.string.system_setting_alarminfo_accept));
		settingList.add(map);
		map = new HashMap<String, Object>();
		map.put("text",
				getString(R.string.system_setting_alarminfo_remind_shake));
		settingList.add(map);
		map = new HashMap<String, Object>();
		map.put("text",
				getString(R.string.system_setting_alarminfo_remind_sound));
		settingList.add(map);
		alarmNotifyAdapter = new AalarmNotifyAdapter(this, settingList,
				isAllAcc, isShake, isSound);
		alarmNotifyListView.setAdapter(alarmNotifyAdapter);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		HashMap<String, Object> map2 = new HashMap<String, Object>();
		List<CloudAccount> accounts = ReadWriteXmlUtils
				.getAlarmPushUsersFromXML();
		String content = "";
		if (accounts == null || (accounts != null && accounts.size() == 0)) {
			content = getString(R.string.system_setting_alarmuser_null);
		} else if (accounts != null && accounts.size() > 0) {
			for (int i = 0; i < accounts.size(); i++) {
				String result = accounts.get(i).getUsername() + "/";
				content += result;
			}
		}
		map2.put("text", content);
		list.set(1, map2);
		alarmUserAdapter = new AlarmUserAdapter(this, list, isAcc);
		alarmUserListView.setAdapter(alarmUserAdapter);
	}
}