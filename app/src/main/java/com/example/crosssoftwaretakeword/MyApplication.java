package com.example.crosssoftwaretakeword;

import android.app.Application;

public class MyApplication extends Application {

    public static Application application;

    @Override
    public void onCreate() {
        super.onCreate();

        application = this;
    }

    public static Application getInstance(){
        return application;
    }
}
