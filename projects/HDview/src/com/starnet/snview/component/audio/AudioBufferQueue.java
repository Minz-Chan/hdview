package com.starnet.snview.component.audio;

import java.util.LinkedList;
import java.util.Queue;

import android.os.Handler;
import android.os.Message;

public class AudioBufferQueue {
	public static final int BUFFER_SIZE = 4160;  // 520ms, 8000Hz, 16bit PCM
	
	
	private Queue<AudioBuffer> writeBufferQueue;
	private Queue<AudioBuffer> readBufferQueue;
	
	private Handler audioHandler;
	
	public AudioBufferQueue(Handler audioHandler) {
		this.audioHandler = audioHandler;
		
		writeBufferQueue = new LinkedList<AudioBuffer>();
		for (int i = 0; i < 5; i++) {
			writeBufferQueue.offer(new AudioBuffer());
		}	
		
		readBufferQueue = new LinkedList<AudioBuffer>();
	}
	
	public int write(byte[] src) {
		return write(src, 0, src.length);
	}
	
	public int write(byte[] src, int offset, int size) {
		if (!writeBufferQueue.isEmpty()) {
			AudioBuffer buf = writeBufferQueue.peek();
			int written = buf.put(src, offset, size);
			if (buf.getState() == AudioBufferState.FULL) {
				readBufferQueue.offer(writeBufferQueue.poll());
				
				Message msg = Message.obtain();
				msg.what = AudioHandler.MSG_BUFFER_FULL;
				audioHandler.sendMessage(msg);
			}
			
			return written;
		}
		
		return 0;
	}
	
	public int read(byte[] des) {
		return read(des, 0, des.length);
	}
	
	public int read(byte[] des, int offset, int size) {
		if (!readBufferQueue.isEmpty()) {
			AudioBuffer buf = readBufferQueue.peek();
			int read = buf.read(des, offset, size);
			if (buf.getState() == AudioBufferState.EMPTY) {
				writeBufferQueue.offer(readBufferQueue.poll());
			}
			
			return read;
		}
		
		return 0;
	}
	
	
	/**
	 * This buffer should only do reading/writing until buffer is empty/full.
	 * @author minz
	 *
	 */
	private class AudioBuffer {
		private byte[] data;
		private int position;  // Used for only one complete reading or writing process
		private AudioBufferState state;
		
		private AudioBuffer() {
			position = 0;
			data = new byte[BUFFER_SIZE];
			state = AudioBufferState.EMPTY;
		}
		
		public int put(byte[] src, int offset, int size) {
			if (state == AudioBufferState.FULL) {
				return 0;
			} else if (position+size > BUFFER_SIZE) {
				throw new IllegalStateException("Need " + size
						+ " byte(s) space, but only " + (BUFFER_SIZE - position)
						+ " byte(s) space is left.");
			}
			
			System.arraycopy(src, offset, data, position, size);
			position += size;
			if (position == BUFFER_SIZE) {
				state = AudioBufferState.FULL;
				position = 0;
			}
			
			return size;
		}
		
		public int read(byte[]des, int offset, int size) {
			if (state ==  AudioBufferState.EMPTY) {
				return 0;
			} else if (position+size > BUFFER_SIZE) {
				throw new IllegalStateException("Can not read " + size
						+ " byte(s) from buffer, only " + (BUFFER_SIZE - position)
						+ " byte(s)is left.");
			}
			
			System.arraycopy(data, position, des, offset, size);
			position+=size;
			if (position == BUFFER_SIZE) {
				state = AudioBufferState.EMPTY;
				position = 0;
			}
			
			return size;
		}
 		
//		public byte[] getData() {
//			return data;
//		}
//		
//		public int getPosition() {
//			return position;
//		}
		
		public AudioBufferState getState() {
			return state;
		}
	}
	
	private enum AudioBufferState {
		EMPTY,
		FULL,
		READING,  // be in reading
		WRITING   // be in writing
	}
	
}