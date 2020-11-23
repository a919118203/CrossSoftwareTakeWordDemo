package com.example.crosssoftwaretakeword.basefloat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.crosssoftwaretakeword.R;
import com.jaeger.library.StatusBarUtil;

/**
 * 用于跨屏取词申请截图权限
 */
public class RequestPermissionActivity extends AppCompatActivity {

    private static final int REQUEST_MEDIA_PROJECTION = 1421;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request_permission);

        StatusBarUtil.setTranslucentForImageViewInFragment(this, 0, null);
        StatusBarUtil.setLightMode(this);

        if (Build.VERSION.SDK_INT >= 21) {
            startActivityForResult(
                    ((MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE))
                            .createScreenCaptureIntent(),REQUEST_MEDIA_PROJECTION);
        }else{
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_MEDIA_PROJECTION && resultCode == Activity.RESULT_OK && data != null) {
            FloatTakeWordView.setResultData(data);
        }
        finish();
    }
}