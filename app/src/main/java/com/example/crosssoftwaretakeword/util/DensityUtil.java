package com.example.crosssoftwaretakeword.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.view.Display;

import com.example.crosssoftwaretakeword.MyApplication;

public class DensityUtil {

	/**
	 * 根据手机的分辨率从 dip 的单位 转成为 px(像素)
	 */
	public static int dip2px(Context context, float dpValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f);
	}

	public static int dip2px(float dpValue){
		final float scale = MyApplication.getInstance().getResources().getDisplayMetrics().density;
		return (int) (dpValue * scale + 0.5f);
	}

	/**
	 * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
	 */
	public static int px2dip(Context context, float pxValue) {
		final float scale = context.getResources().getDisplayMetrics().density;
		return (int) (pxValue / scale + 0.5f);
	}

	/**
	 * 获取屏幕宽度
	 * @param activity  活动
	 * @return          屏幕宽度
	 */
	public static int getDisplayWidth(Activity activity){
		Display defaultDisplay = activity.getWindowManager().getDefaultDisplay();
		Point point = new Point();
		defaultDisplay.getSize(point);
		return point.x;
	}

	/**
	 * 获取屏幕高度
	 * @param activity  活动
	 * @return          屏幕高度
	 */
	public static int getDisplayHeight(Activity activity){
		Display defaultDisplay = activity.getWindowManager().getDefaultDisplay();
		Point point = new Point();
		defaultDisplay.getSize(point);
		return point.y;
	}
}