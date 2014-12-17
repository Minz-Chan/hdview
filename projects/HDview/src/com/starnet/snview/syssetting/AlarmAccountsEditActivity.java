package com.starnet.snview.syssetting;

import java.util.ArrayList;
import java.util.List;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnCancelListener;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.baidu.android.pushservice.PushManager;
import com.starnet.snview.R;
import com.starnet.snview.component.BaseActivity;
import com.starnet.snview.util.MD5Utils;
import com.starnet.snview.util.NetWorkUtils;
import com.starnet.snview.util.ReadWriteXmlUtils;

@SuppressLint("HandlerLeak")
public class AlarmAccountsEditActivity extends BaseActivity {

	private int pos;
	private Context ctx;
//	private Button delBtn;
	private Button idtBtn;
	private EditText domnEdt;
	private EditText portEdt;
	private EditText userEdt;
	private EditText pswdEdt;
	private CloudAccount originCA;

	private CloudAccount idtCA;// 单击验证时的验证用户
	private ProgressDialog pro;
	private boolean isIdentify;// 是否验证
	private boolean isIdentifyPass;// 验证是否通过
	private List<CloudAccount> mList;
	private AlarmPushIdentifyTask identifyTask;
	private final int IDENTIFY_DIALOG = 0x0009;
	private final int REQUESTCODE_EDIT = 0x0004;
	private final int IDENTIFY_SUCCESS = 0x0001;
	private final int IDENTIFY_PSWD_ERR = 0x0006;
	private final int IDENTIFY_USER_ERR = 0x0007;
	private final int IDENTIFY_TIMEOUT_ERR = 0x0008;
	private final int IDENTIFY_DOMN_PORT_ERR = 0x0005;

	private Handler mHandler = new Handler() {
		@SuppressWarnings("deprecation")
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
			case IDENTIFY_SUCCESS:
				if (pro.isShowing()) {
					dismissDialog(IDENTIFY_DIALOG);
				}
				setIdentify(true, true);
				showToast(getString(R.string.system_setting_alarm_pushset_identify_suc));
				break;
			case IDENTIFY_DOMN_PORT_ERR:
				if (pro.isShowing()) {
					dismissDialog(IDENTIFY_DIALOG);
				}
				setIdentify(true, false);
				showToast(getString(R.string.system_setting_alarm_pushset_domain_port_wrong));
				break;
			case IDENTIFY_PSWD_ERR:
				if (pro.isShowing()) {
					dismissDialog(IDENTIFY_DIALOG);
				}
				setIdentify(true, false);
				showToast(getString(R.string.system_setting_alarm_pushset_pswd_wrong));
				break;
			case IDENTIFY_USER_ERR:
				if (pro.isShowing()) {
					dismissDialog(IDENTIFY_DIALOG);
				}
				setIdentify(true, false);
				showToast(getString(R.string.system_setting_alarm_pushset_user_wrong));
				break;
			case IDENTIFY_TIMEOUT_ERR:
				if (pro.isShowing()) {
					dismissDialog(IDENTIFY_DIALOG);
				}
				setIdentify(true, false);
				showToast(getString(R.string.system_setting_alarm_pushset_timout));
				break;
			default:
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.alarm_accounts_edit_activity);

		initViews();
		setListeners();
	}

	private void initViews() {

		super.hideExtendButton();
		super.setToolbarVisiable(false);
		ctx = AlarmAccountsEditActivity.this;
		super.setRightButtonBg(R.drawable.navigation_bar_savebtn_selector);
		super.setLeftButtonBg(R.drawable.navigation_bar_back_btn_selector);
		super.setTitleViewText(getString(R.string.system_setting_preview_user_info));

		Intent intent = getIntent();
		pos = intent.getIntExtra("pos", 0);
		originCA = (CloudAccount) intent.getSerializableExtra("cla");

//		delBtn = (Button) findViewById(R.id.delete_btn);
		idtBtn = (Button) findViewById(R.id.identify_btn);
		portEdt = (EditText) findViewById(R.id.port_edt);
		userEdt = (EditText) findViewById(R.id.user_edt);
		domnEdt = (EditText) findViewById(R.id.domain_edt);
		pswdEdt = (EditText) findViewById(R.id.password_edt);

		showContent();

	}

	private void showContent() {
		portEdt.setText(originCA.getPort());
		domnEdt.setText(originCA.getDomain());
		userEdt.setText(originCA.getUsername());
		pswdEdt.setText(originCA.getPassword());
	}

	private void setListeners() {

		super.getLeftButton().setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				AlarmAccountsEditActivity.this.finish();
			}
		});

		super.getRightButton().setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (NetWorkUtils.checkNetConnection(ctx)) {
					CloudAccount addAccount = getCloudAccount();
					mList = new ArrayList<CloudAccount>();
					mList = ReadWriteXmlUtils.getAlarmPushUsersFromXML();
					addBaiduTags(addAccount);
				} else {
					showToast(getString(R.string.system_setting_alarm_pushset_netnotopen));
				}
			}
		});

		idtBtn.setOnClickListener(new OnClickListener() {

			@SuppressWarnings("deprecation")
			@Override
			public void onClick(View v) {
				if (NetWorkUtils.checkNetConnection(ctx)) {
					showDialog(IDENTIFY_DIALOG);
					idtCA = getIdentifyCloudAccount();
					identifyTask = new AlarmPushIdentifyTask(mHandler, idtCA);
					identifyTask.start();
				} else {
					showToast(getString(R.string.system_setting_alarm_pushset_netnotopen));
				}
			}
		});

//		delBtn.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				// TODO Auto-generated method stub
//
//			}
//		});

	}

	private void addBaiduTags(CloudAccount addAccount) {
		if (!isIdentify) {
			popUpIdentifyDialog(R.string.system_setting_alarm_pushset_builer_identify_add);
			return;
		} else {
			if (isIdentifyPass) {
				if (checkSameCloudAccounts(idtCA, addAccount)) {
					try {
						addBaiduPushTags(addAccount);
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					popUpIdentifyDialog(R.string.system_setting_alarm_pushset_builer_identify_add);
				}
			} else {
				popUpIdentifyDialog(R.string.system_setting_alarm_pushset_builer_identify_add);
			}
		}
	}

	/** 增加百度标签(删除原来的，保留当前的) **/
	private void addBaiduPushTags(CloudAccount addAccount) throws Exception {
		boolean isExist = isExistUser(addAccount);
		if (!isExist) {
			List<String> delTags = new ArrayList<String>();
			List<String> regTags = new ArrayList<String>();
			delTags.add(originCA.getUsername()+ ""+ MD5Utils.createMD5(originCA.getPassword()));
			regTags.add(addAccount.getUsername() + ""+ MD5Utils.createMD5(addAccount.getPassword()));
			PushManager.delTags(ctx, delTags);
			PushManager.setTags(ctx, regTags);
			ReadWriteXmlUtils.replaceAlarmPushUserToXML(addAccount, pos);
			Intent intent = new Intent();
			intent.putExtra("position", pos);
			intent.putExtra("claa", addAccount);
			setResult(REQUESTCODE_EDIT, intent);
			this.finish();
		} else {
			jumpCoverDialog(addAccount);// 弹出对话框，询问用户是否覆盖
		}
	}

	private void jumpCoverDialog(CloudAccount ca) {
		Builder builder = new Builder(ctx);
		builder.setTitle(R.string.system_setting_alarm_pushset_exist_cover);
		String ok = getString(R.string.system_setting_alarm_pushset_builer_identify_add_ok);
		String cancel = getString(R.string.system_setting_alarm_pushset_builer_identify_add_cance);
		// builder.setNegativeButton(cancel,
		// new DialogInterface.OnClickListener() {
		// @Override
		// public void onClick(DialogInterface dialog, int which) {
		// AlarmAccountsAddActivity.this.finish();
		// }
		// });
		builder.setNegativeButton(cancel, null);
		final CloudAccount cla = ca;
		builder.setPositiveButton(ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent data = new Intent();
				data.putExtra("claa", cla);
				data.putExtra("cover", true);
				data.putExtra("position", pos);
				setResult(REQUESTCODE_EDIT, data);
				AlarmAccountsEditActivity.this.finish();
			}
		});
		builder.show();
	}

	private boolean isExistUser(CloudAccount account) {
		boolean isExist = false;
		if (mList != null) {
			for (int i = 0; i < mList.size(); i++) {
				if (account.getUsername().equals(mList.get(i).getUsername())) {
					isExist = true;
					break;
				}
			}
		}
		return isExist;
	}

	/** 判断两个用户的信息是否一致，如果一致，则返回true；否则，返回false **/
	private boolean checkSameCloudAccounts(CloudAccount iAcc, CloudAccount jAcc) {
		String iDomn = iAcc.getDomain();
		String iPswd = iAcc.getPassword();
		String iPort = iAcc.getPort();
		String iName = iAcc.getUsername();

		String jDomn = jAcc.getDomain();
		String jPswd = jAcc.getPassword();
		String jPort = jAcc.getPort();
		String jName = jAcc.getUsername();
		// 首先对密码进行处理
		if (iPswd.trim().length() > 0) {
			if (!iPswd.equals(jPswd)) {
				showToast(getString(R.string.system_setting_alarm_pushset_userchange_idn));
				return false;
			}
		} else {
			if (jPswd.trim().length() > 0) {
				showToast(getString(R.string.system_setting_alarm_pushset_userchange_idn));
				return false;
			} else {
				return true;
			}
		}
		boolean isNull = checkExistNull(jName, jDomn, jPort);
		if (isNull) {
			showToast(getString(R.string.system_setting_alarm_pushset_userchange_idn));
			return false;
		} else {
			if (iDomn.equals(jDomn) && iPort.equals(jPort)
					&& iName.equals(jName)) {
				return true;
			} else {
				showToast(getString(R.string.system_setting_alarm_pushset_userchange_idn));
				return false;
			}
		}
	}

	private void popUpIdentifyDialog(int resID) {
		Builder builder = new Builder(ctx);
		builder.setTitle(resID);
		String ok = getString(R.string.system_setting_alarm_pushset_builer_identify_add_ok);
		builder.setPositiveButton(ok, null);
		builder.show();
	}

	private void showToast(String content) {
		Toast.makeText(ctx, content, Toast.LENGTH_SHORT).show();
	}

	private void setIdentify(boolean isIdentify, boolean isIdentifyPass) {
		this.isIdentify = isIdentify;
		this.isIdentifyPass = isIdentifyPass;
	}

	/** 获取需要验证的账户 **/
	private CloudAccount getIdentifyCloudAccount() {
		idtCA = new CloudAccount();
		String port = portEdt.getText().toString();
		String pswd = pswdEdt.getText().toString();
		String uNam = userEdt.getText().toString();
		String domn = domnEdt.getText().toString();
		boolean isNull = checkExistNull(uNam, domn, port);
		if (!isNull) {
			idtCA.setPort(port);
			idtCA.setDomain(domn);
			idtCA.setPassword(pswd);
			idtCA.setUsername(uNam);
		}
		return idtCA;
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case IDENTIFY_DIALOG:
			String con = getString(R.string.system_setting_pushset_identify_wait);
			pro = ProgressDialog.show(this, "", con, true, true);
			pro.setOnCancelListener(new OnCancelListener() {
				@SuppressWarnings("deprecation")
				@Override
				public void onCancel(DialogInterface dialog) {
					if (pro.isShowing()) {
						dismissDialog(IDENTIFY_DIALOG);
					}
					if (identifyTask != null) {
						identifyTask.setCancel(true);
					}
				}
			});
			return pro;
		default:
			return null;
		}
	}

	/** 获取增加标签的账户 **/
	private CloudAccount getCloudAccount() {
		CloudAccount account = new CloudAccount();
		String port = portEdt.getText().toString();
		String pswd = pswdEdt.getText().toString();
		String uNam = userEdt.getText().toString();
		String domn = domnEdt.getText().toString();
		account.setPort(port);
		account.setDomain(domn);
		account.setPassword(pswd);
		account.setUsername(uNam);
		return account;
	}

	/** 检测用户的信息是否为空，如果为空，返回true；否则，返回false ***/
	private boolean checkExistNull(String uNam, String domn, String port) {
		boolean isNull = false;
		if (uNam.trim().length() == 0) {
			showToast(getString(R.string.system_setting_alarm_pushset_uName_null));
			return true;
		}
		if (domn.trim().length() == 0) {
			showToast(getString(R.string.system_setting_alarm_pushset_domn_null));
			return true;
		}
		if (port.trim().length() == 0) {
			showToast(getString(R.string.system_setting_alarm_pushset_port_null));
			return true;
		}
		return isNull;
	}
}