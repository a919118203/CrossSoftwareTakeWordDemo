package com.example.crosssoftwaretakeword.util;

import android.content.Context;
import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

public class SharedPreferencesHelper {
    private SharedPreferencesHelper() {}
    public static final String OLD_PREFERENCE_NAME = "mo"+"pubSettings";
    public static final String SHARE_PREFERENCE_NAME = "shareadsdk";

    public static SharedPreferences getSharedPreferences(Context context) {
        String pkgName = context.getPackageName();
        String PREFERENCE_NAME = pkgName.substring(pkgName.lastIndexOf(".") + 1);
        return context.getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE);
    }

    public static SharedPreferences getOldSharedPreferences(Context context) {
        return context.getSharedPreferences(OLD_PREFERENCE_NAME, MODE_PRIVATE);
    }


    public static SharedPreferences getAdSdkSharedPreferences(Context context) {
        return context.getSharedPreferences(SHARE_PREFERENCE_NAME, MODE_PRIVATE);
    }
}
