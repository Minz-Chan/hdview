/*
 * FileName:DataProcessService.java
 * 
 * Package:com.starsecurity.service
 * 
 * Date:2013-04-15
 * 
 * Copyright: Copyright (c) 2013 Minz.Chan
 */
package com.starnet.snview.playback.utils;

import java.util.ArrayList;

/**
 * @function     功能	  	数据处理接口
 * @author       创建人              陈明珍
 * @date        创建日期           2013-04-15
 * @author       修改人              陈明珍
 * @date        修改日期           2013-04-15
 * @description 修改说明	          首次增加
 */
public interface DataProcessService {
	public int process(byte[] data, int length);
	/**获取搜索记录列表**/
	public ArrayList<TLV_V_RecordInfo> getRecordInfos();
//	/**暂停标志设置**/
//	public void setPause(boolean isPause);
//	/**继续标志设置**/
//	public void setResume(boolean isResume);
}
