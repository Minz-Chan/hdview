package com.starnet.snview.protocol.message;


//u_int8  channelId;			//ͨ��ID
//u_int8  reserve;				//����
//u_int16 checksum;				//У���.ĿǰΪ0δ��
//u_int32 frameIndex;			//��Ƶ֡���
//u_int32 time;	
public class VideoFrameInfo {
	private int channelId;
	private int reserve;
	private int checkSum;
	private int frameIndex;
	private int time;
	
	
	public int getChannelId() {
		return channelId;
	}
	public void setChannelId(int channelId) {
		this.channelId = channelId;
	}
	public int getReserve() {
		return reserve;
	}
	public void setReserve(int reserve) {
		this.reserve = reserve;
	}
	public int getCheckSum() {
		return checkSum;
	}
	public void setCheckSum(int checkSum) {
		this.checkSum = checkSum;
	}
	public int getFrameIndex() {
		return frameIndex;
	}
	public void setFrameIndex(int frameIndex) {
		this.frameIndex = frameIndex;
	}
	public int getTime() {
		return time;
	}
	public void setTime(int time) {
		this.time = time;
	}
}
