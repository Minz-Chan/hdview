package com.starnet.snview.test;

import com.starnet.snview.util.Base64Util;

import junit.framework.Assert;
import android.test.AndroidTestCase;

public class Base64UtilTester extends AndroidTestCase {
	
	public void testDecode() throws Exception{
		String result = Base64Util.decode("uuy608zVtMnNqLXAM7aoyrHXpcXE");
		Assert.assertEquals("����մ�ͨ��3��ʱץ��", result);
	}
}
