package com.example.crosssoftwaretakeword.util;

import android.content.SharedPreferences;
import android.text.TextUtils;

import com.example.crosssoftwaretakeword.MyApplication;

public class SPUtil {
    public static void savePref(String key, String value) {
        SharedPreferences.Editor editor = SharedPreferencesHelper.getSharedPreferences(MyApplication.getInstance()).edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static String getPref(String key, String value) {
        String value1 = SharedPreferencesHelper.getSharedPreferences(MyApplication.getInstance()).getString(key, value);
        if (TextUtils.isEmpty(value1) || value1.equals(value)) {
            value1 = SharedPreferencesHelper.getOldSharedPreferences(MyApplication.getInstance()).getString(key, value);
        }
        return value1;
    }

    public static void savePref(String key, Long value) {
        SharedPreferences.Editor editor = SharedPreferencesHelper.getSharedPreferences(MyApplication.getInstance()).edit();
        editor.putLong(key, value);
        editor.commit();
    }

    public static void removePref(String key) {
        SharedPreferences.Editor editor = SharedPreferencesHelper.getSharedPreferences(MyApplication.getInstance()).edit();
        editor.remove(key);
        editor.commit();
        SharedPreferences.Editor editor1 = SharedPreferencesHelper.getOldSharedPreferences(MyApplication.getInstance()).edit();
        editor1.remove(key);
        editor1.commit();
    }

    public static Long getPref(String key, Long value) {
        return SharedPreferencesHelper.getSharedPreferences(MyApplication.getInstance()).getLong(key, value);
    }

    public static void savePref(String key, Integer value) {
        SharedPreferences.Editor editor = SharedPreferencesHelper.getSharedPreferences(MyApplication.getInstance()).edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public static Integer getPref(String key, Integer value) {
        Integer value1 = SharedPreferencesHelper.getSharedPreferences(MyApplication.getInstance()).getInt(key, value);
        if (value1 == value) {
            value1 = SharedPreferencesHelper.getOldSharedPreferences(MyApplication.getInstance()).getInt(key, value);
        }
        return value1;
    }

    public static void savePref(String key, boolean value) {
        SharedPreferences.Editor editor = SharedPreferencesHelper.getSharedPreferences(MyApplication.getInstance()).edit();
        editor.putBoolean(key, value);
        editor.commit();
    }

    public static boolean getPref(String key, boolean value) {
        return SharedPreferencesHelper.getSharedPreferences(MyApplication.getInstance()).getBoolean(key, value);
    }
}
