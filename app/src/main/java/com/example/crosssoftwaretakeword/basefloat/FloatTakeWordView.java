package com.example.crosssoftwaretakeword.basefloat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.RequiresApi;

import com.blankj.utilcode.util.ToastUtils;
import com.example.crosssoftwaretakeword.R;
import com.example.crosssoftwaretakeword.base.Consts;
import com.example.crosssoftwaretakeword.data.OcrContentBean;
import com.example.crosssoftwaretakeword.util.DensityUtil;
import com.example.crosssoftwaretakeword.util.LogUtils;
import com.example.crosssoftwaretakeword.util.NotchUtil;
import com.example.crosssoftwaretakeword.util.OcrUtils;
import com.example.crosssoftwaretakeword.util.SPUtil;
import com.example.crosssoftwaretakeword.util.StatusBarUtils;
import com.googlecode.leptonica.android.Pixa;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.example.crosssoftwaretakeword.base.Consts.MODE_TAKE_WORD;

/**
 * 萝卜词典悬浮窗
 */
public class FloatTakeWordView extends AbsFloatBase {
    private static Intent resultData;
    private final String popupWindowLock = "popupWindowLock";
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private ImageReader mImageReader;
    private int mScreenWidth;
    private int mScreenHeight;
    private int mScreenDensity;
    //前景  用于获取用户的触摸事件
    private View searchView;
    private View mainView;
    private View mainViewLayout;
    private View.OnTouchListener onTouchListener;
    private int lastX;
    private int lastY;
    private CopyOnWriteArrayList<OcrContentBean> wordBeans = new CopyOnWriteArrayList<>();
    private CopyOnWriteArrayList<OcrContentBean> sentenceBeans = new CopyOnWriteArrayList<>();
    private Bitmap shotBitmap;  //截屏bitmap
    private OcrContentBean lastBean;
    private int lastBeanCount;
    private Thread mQueryThread;
    private Timer mThreadCreateTimer;
    private TessBaseAPI baseApi;
    private AtomicBoolean baseLock = new AtomicBoolean();//相当于baseApi的锁

    private Timer timer;

    private int takeMode;

    public FloatTakeWordView(Context context) {
        super(context);
        initOcr();

        takeMode = SPUtil.getPref(Consts.TAKE_MODE, MODE_TAKE_WORD);
    }

    /**
     * 保存用户的授权
     *
     * @param intent
     */
    public static void setResultData(Intent intent) {
        resultData = intent;
    }

    /**
     * 初始化ocr
     */
    private void initOcr() {
        baseApi = new TessBaseAPI();
        //字典库
        baseApi.init(mContext.getExternalFilesDir("") + "/", "eng");
        //设置设别模式
        baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);
    }

    /**
     * 获取检索框现在正在覆盖的区域
     *
     * @return
     */
    public Rect getSearchRect() {
        int left = lastX + DensityUtil.dip2px(2);
        int top = lastY;
        int right = left + searchView.getWidth();
        int bottom = top + searchView.getHeight();
        return new Rect(left, top, right, bottom);
    }

    @Override
    public void create() {
        super.create();

        mViewMode = WRAP_CONTENT_TOUCHABLE;

        mGravity = Gravity.START | Gravity.TOP;

        inflate(R.layout.float_take_word_layout);

        init();
        initView();
        initListener();
    }

    @Override
    public void destroy() {
        super.destroy();

        resultData = null;

        try {
            if (baseApi != null) {
                baseApi.end();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized void show() {
        super.show();

        //将图标移到屏幕中间靠下的位置
        lastY = (int) (mScreenHeight * 0.6);
        move(0, lastY);
        move(0, 0);
    }

    @Override
    public void remove() {
        super.remove();

        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
    }

    /**
     * 寻找当前检索框的中点所在的单词框  高亮这个单词框
     */
    private void showRect() {
        Rect searchRect;

        //如果没有ocr结果  或者 现在没有框住东西    就不高亮
        boolean noData = wordBeans.isEmpty()
                || sentenceBeans.isEmpty();

        if ((searchRect = getSearchRect()) == null || noData) {
            LogUtils.logE("状态：ocr还没完成");
            return;
        }

        OcrContentBean targetBean = null;

        int x = (searchRect.left + searchRect.right) / 2;
        int y = (searchRect.top + searchRect.bottom) / 2;
        //寻找包含(x,y)的矩形
        try {//防止concurrentModificationException
            if (takeMode == MODE_TAKE_WORD) {
                for (OcrContentBean bean : wordBeans) {
                    Rect rect = bean.getContentRect();
                    if (rect.left <= x && rect.right >= x
                            && rect.top <= y && rect.bottom >= y) {
                        targetBean = bean;
                        break;
                    }
                }
            } else {
                for (OcrContentBean bean : sentenceBeans) {
                    for (OcrContentBean sentence : bean.getSentenceList()) {
                        Rect rect = sentence.getContentRect();
                        if (rect.left <= x && rect.right >= x
                                && rect.top <= y && rect.bottom >= y) {
                            targetBean = bean;
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        //停留200ms才开始ocr
        if (targetBean != lastBean) {
            lastBean = targetBean;
            lastBeanCount = 0;
        } else {
            lastBeanCount++;
        }

        if (lastBeanCount <= 2) {
            return;
        }

        //该区域还没去获取内容
        if (targetBean != null && targetBean.getContent() == null && shotBitmap != null) {
            try {
                //设为true  不让其他地方使用baseApi
                if (!baseLock.compareAndSet(false, true)) {
                    return;
                }

                if (takeMode == MODE_TAKE_WORD) {
                    targetBean.setContent(ocrRect(targetBean.getContentRect()));
                } else {
                    //句子是多边形 需要一个一个识别
                    StringBuilder sb = new StringBuilder();
                    for (OcrContentBean bean : targetBean.getSentenceList()) {
                        sb.append(ocrRect(bean.getContentRect()));
                        sb.append(" ");
                    }
                    targetBean.setContent(sb.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(TAG, "showRect: ocr失败");
                return;
            } finally {
                baseLock.set(false);
            }
        }

        //ocr完了以后不在这个矩形了 就不显示
        searchRect = getSearchRect();
        if (searchRect == null) {
            return;
        }
        x = (searchRect.left + searchRect.right) / 2;
        y = (searchRect.top + searchRect.bottom) / 2;
        if (!(targetBean != null &&
                targetBean.getContentRect().left <= x && targetBean.getContentRect().right >= x
                && targetBean.getContentRect().top <= y && targetBean.getContentRect().bottom >= y)) {
            return;
        }

        final OcrContentBean finalBean = targetBean;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                //TODO 显示获取文本的结果
                LogUtils.logE("获取的内容：" + finalBean.getContent());
            }
        });
    }

    /**
     * 指定矩形中的内容
     */
    private String ocrRect(Rect rect) {
        if (shotBitmap == null || shotBitmap.isRecycled() || baseApi == null) {
            return "";
        }

        int x = Math.max(rect.left - 10, 0);
        int y = Math.max(rect.top - 10, 0);
        int width = Math.min(rect.right - rect.left + 20, shotBitmap.getWidth() - x);
        int height = Math.min(rect.bottom - rect.top + 20, shotBitmap.getHeight() - y);

        //加大一点矩形  好识别一点
        Bitmap contentBitmap = Bitmap.createBitmap(shotBitmap,
                x,
                y,
                width,
                height);

        baseApi.setImage(contentBitmap);

        return baseApi.getUTF8Text();
    }

    private void initListener() {
        onTouchListener = new View.OnTouchListener() {
            int lastX, lastY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int x = (int) event.getRawX();
                int y = (int) event.getRawY();
                switch (event.getAction()) {
                    case MotionEvent.ACTION_UP:
                        move(0, 0);

                        //清空ocr结果
                        wordBeans.clear();
                        sentenceBeans.clear();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int offsetX = x - lastX;
                        int offsetY = y - lastY;

                        int afterX = offsetX + FloatTakeWordView.this.lastX;
                        int afterY = offsetY + FloatTakeWordView.this.lastY;

                        //判断是否出界
                        if (afterX < 0) {
                            offsetX = -FloatTakeWordView.this.lastX;
                        } else if (afterX + mainViewLayout.getWidth() > mScreenWidth) {
                            offsetX = mScreenWidth - mainViewLayout.getWidth() - FloatTakeWordView.this.lastX;
                        }

                        //判断是否出界
                        if (afterY < 0) {
                            offsetY = -FloatTakeWordView.this.lastY;
                        } else if (afterY + mainViewLayout.getHeight() > mScreenHeight) {
                            offsetY = mScreenHeight - mainViewLayout.getHeight() - FloatTakeWordView.this.lastY;
                        }

                        move(offsetX, offsetY);
                        move(0, 0);

                        lastX = x;
                        lastY = y;

                        FloatTakeWordView.this.lastX += offsetX;
                        FloatTakeWordView.this.lastY += offsetY;
                        break;
                    case MotionEvent.ACTION_DOWN:
                        lastX = x;
                        lastY = y;

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && resultData == null) {  //如果还没获取权限
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    setUpMediaProjection();
                                }
                            });
                        } else {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    LogUtils.logE("状态：开始截图");
                                    if (Build.VERSION.SDK_INT >= 16 && Build.VERSION.SDK_INT < 18 ||
                                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                startScreenShot();
                                            }
                                        }, 300);
                                    }
                                }
                            });
                        }
                        break;
                }
                return true;
            }
        };
        mainView.setOnTouchListener(onTouchListener);
    }

    private void initView() {
        mainViewLayout = findView(R.id.rlContent);
        mainView = findView(R.id.iv_bg);
        searchView = findView(R.id.searchView);
    }

    private void init() {
        DisplayMetrics metrics = new DisplayMetrics();
        mWindowManager.getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;

        final Display display = mWindowManager.getDefaultDisplay();
        Point outPoint = new Point();
        if (Build.VERSION.SDK_INT >= 19) {
            // 可能有虚拟按键的情况
            display.getRealSize(outPoint);
        } else {
            // 不可能有虚拟按键
            display.getSize(outPoint);
        }
        mScreenHeight = outPoint.y;
        mScreenWidth = outPoint.x;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            createImageReader();
        }

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                showRect();
            }
        }, 100, 100);
    }

    @Override
    protected void onAddWindowFailed(Exception e) {
        ToastUtils.showLong("添加悬浮窗失败！！！！！！请检查悬浮窗权限");
    }

    @SuppressLint("WrongConstant")
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void createImageReader() {
        mImageReader = ImageReader.newInstance(mScreenWidth, mScreenHeight, PixelFormat.RGBA_8888, 1);
    }

    /**
     * 将截图保存到本地
     * 用于自己看
     */
    private void saveShot(Bitmap bitmap, String dirPath) {
        File dir = new File(dirPath);
        dir.mkdirs();

        File file = new File(dirPath + "/" + System.currentTimeMillis() + ".png");
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "saveShot: 保存失败");
        }

        Log.d(TAG, "saveShot: 保存成功");
    }

    /**
     * 完成截图
     * 解析截图
     *
     * @param bitmap
     */
    private synchronized void onShotOk(Bitmap bitmap) {
        LogUtils.logE("状态：截图结束开始解析截图（仅获取每个文本的矩形，不解析矩形中的内容）");

        if (Thread.currentThread().isInterrupted()) {
            return;
        }

        try {
            if (baseApi != null) {
                //如果其他地方在用  就等着
                while (!baseLock.compareAndSet(false, true)) {
                    Thread.sleep(200);
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                }

                shotBitmap = convertGray(bitmap);

                //保存当前屏幕的截图到本地
                //saveShot(shotBitmap);

                wordBeans.clear();
                sentenceBeans.clear();
                OcrUtils.paresBitmap(wordBeans, sentenceBeans, shotBitmap, baseApi,
                        mScreenWidth, mScreenHeight);

                baseLock.set(false);

                LogUtils.logE("状态：解析结束");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            mQueryThread = null;
        }
    }

    /**
     * 灰度化处理
     */
    public Bitmap convertGray(Bitmap bitmap3) {
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);

        Paint paint = new Paint();
        paint.setColorFilter(filter);
        Bitmap result = Bitmap.createBitmap(bitmap3.getWidth(), bitmap3.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        canvas.drawBitmap(bitmap3, 0, 0, paint);
        return result;
    }

    /**
     * 开始截图
     * 5.0以上使用google提供的接口截屏
     * 4.3 4.4暂无办法截屏
     */

    private void startScreenShot() {
        //限制只有一个访问线程
        if (mQueryThread != null) {
            if (mThreadCreateTimer != null) {
                mThreadCreateTimer.cancel();
                mThreadCreateTimer = null;
            }

            //将线程状态改为中断状态
            mQueryThread.interrupt();
            mThreadCreateTimer = new Timer();
            mThreadCreateTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    //检查是否已中断
                    if (mQueryThread == null || !mQueryThread.isAlive()) {
                        mQueryThread = null;

                        //一有机会就release掉VirtualDisplay   防止手机变卡
                        if (mVirtualDisplay != null) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                                mVirtualDisplay.release();
                            }
                            mVirtualDisplay = null;
                        }

                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                //中断了才进行新的一次访问
                                startScreenShot();
                            }
                        });

                        mThreadCreateTimer.cancel();
                        mThreadCreateTimer = null;
                    }
                }
            }, 100, 100);

            return;
        }

        mQueryThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    startVirtual();
                } else if (Build.VERSION.SDK_INT >= 16 && Build.VERSION.SDK_INT < 18) {
                    Bitmap bitmap = ScreenShotUtil.getInstance().takeScreenshot(mContext);
                    onShotOk(bitmap);
                } else {
                    mQueryThread = null;
                }
            }
        });
        mQueryThread.start();
    }

    /**
     * 初始化virtualDisplay
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void startVirtual() {
        if (mMediaProjection == null) {
            setUpMediaProjection();
        }

        if (mMediaProjection == null) {
            return;
        }

        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
        try {
            mVirtualDisplay = mMediaProjection.createVirtualDisplay("screen-mirror",
                    mScreenWidth, mScreenHeight, mScreenDensity, DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    mImageReader.getSurface(), null, null);
        } catch (Exception e) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    mQueryThread = null;
                    mMediaProjection = null;
                }
            });
        }

        startCapture();
    }

    /**
     * 申请权限初始化MediaProjection
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void setUpMediaProjection() {
        if (resultData == null) {
            Intent intent = new Intent(mContext, RequestPermissionActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            ((Service) mContext).startActivity(intent);


            mQueryThread = null;
        } else {
            mMediaProjection = getMediaProjectionManager().getMediaProjection(Activity.RESULT_OK, resultData);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private MediaProjectionManager getMediaProjectionManager() {
        return (MediaProjectionManager) mContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    /**
     * 截图
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void startCapture() {
        if (Thread.currentThread().isInterrupted()) {
            return;
        }

        Image image = mImageReader.acquireNextImage();
        if (image == null) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            startCapture();
        } else {
            //关闭虚拟显示器
            mVirtualDisplay.release();
            mVirtualDisplay = null;

            int width = image.getWidth();
            int height = image.getHeight();
            final Image.Plane[] planes = image.getPlanes();
            final ByteBuffer buffer = planes[0].getBuffer();
            //每个像素的间距
            int pixelStride = planes[0].getPixelStride();
            //总的间距
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * width;
            Bitmap bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
            bitmap.copyPixelsFromBuffer(buffer);
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height);
            image.close();

            //防止截屏的时候出现两次截屏是一样的图的情况
            image = mImageReader.acquireNextImage();
            if (image != null) {
                image.close();
            }

            if (bitmap != null) {
                onShotOk(bitmap);
            }
        }
    }
}
