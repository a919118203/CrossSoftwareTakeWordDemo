package com.example.crosssoftwaretakeword;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.RadioGroup;

import com.blankj.utilcode.util.ToastUtils;
import com.example.crosssoftwaretakeword.base.Consts;
import com.example.crosssoftwaretakeword.basefloat.FloatWindowParamManager;
import com.example.crosssoftwaretakeword.basefloat.FloatWindowService;
import com.example.crosssoftwaretakeword.basefloat.RomUtils;
import com.example.crosssoftwaretakeword.util.LogUtils;
import com.example.crosssoftwaretakeword.util.NotchUtil;
import com.example.crosssoftwaretakeword.util.SPUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MainActivity extends AppCompatActivity {

    private RadioGroup group;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        NotchUtil.hasNotch = NotchUtil.hasNotchInScreen(this);
    }

    private void initView(){
        group = findViewById(R.id.radio_group);
        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int id) {
                int mode = Consts.MODE_TAKE_WORD;

                if(id == R.id.takw_sentence){
                    mode = Consts.MODE_TAKE_SENTENCE;
                }

                SPUtil.savePref(Consts.TAKE_MODE, mode);
            }
        });

        findViewById(R.id.init_dict).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isAllMustPermissioned()){
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE}, 12345);
                    return ;
                }

                initTraineddata();
            }
        });

        findViewById(R.id.open_take_word).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!isAllMustPermissioned()){
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                    Manifest.permission.READ_EXTERNAL_STORAGE}, 12345);
                    return ;
                }

                Intent intent = new Intent(MainActivity.this, FloatWindowService.class);

                if(Build.VERSION.SDK_INT >= 18 && Build.VERSION.SDK_INT < 21){
                    //TODO 不支持的版本
                    return ;
                }

                boolean permission = FloatWindowParamManager.checkPermission(MainActivity.this);
                if (permission&&!RomUtils.isVivoRom()) {
                    startService(intent);
                } else {
                    FloatWindowParamManager.tryJumpToPermissionPage(MainActivity.this);
                }
            }
        });

        findViewById(R.id.close_take_word).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, FloatWindowService.class);
                stopService(intent);
            }
        });
    }

    /**
     * 解压ocr词典
     */
    private void initTraineddata(){
        ToastUtils.showShort("正在初始化。。。");
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //将assets中的词典复制出来
                    String toDir = getExternalFilesDir("") + "/tessdata";
                    File dir = new File(toDir);
                    if(!dir.exists()){
                        dir.mkdirs();
                    }

                    byte[] buffer = new byte[1024];
                    int len;
                    String[] fileNames = getAssets().list("traineddata");
                    for(String fileName : fileNames){
                        InputStream is = getAssets().open("traineddata/" + fileName);
                        ZipInputStream zipInputStream = new ZipInputStream(is);
                        // 读取一个进入点
                        ZipEntry zipEntry = zipInputStream.getNextEntry();

                        while (zipEntry != null) {
                            if (!zipEntry.isDirectory()) {  //如果是一个文件
                                // 如果是文件
                                String realFileName = zipEntry.getName();
                                realFileName = realFileName.substring(realFileName.lastIndexOf("/") + 1);  //截取文件的名字 去掉原文件夹名字
                                File file = new File(toDir + "/" + realFileName);  //放到新的解压的文件路径

                                file.createNewFile();
                                FileOutputStream fileOutputStream = new FileOutputStream(file);
                                while ((len = zipInputStream.read(buffer)) > 0) {
                                    fileOutputStream.write(buffer, 0, len);
                                }
                                fileOutputStream.close();
                            }

                            // 定位到下一个文件入口
                            zipEntry = zipInputStream.getNextEntry();
                        }

                        zipInputStream.close();
                    }
                    LogUtils.logE("初始化完成");
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ToastUtils.showShort("初始化结束");
                        }
                    });
                }
            }
        }).start();
    }

    private boolean isAllMustPermissioned() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }
}