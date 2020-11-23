package com.example.crosssoftwaretakeword.basefloat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;

public class ScreenShotUtil {
    private static final String TAG = "ScreentShotUtil";
    private static final String CLASS1_NAME = "android.view.SurfaceControl";
    private static final String CLASS2_NAME = "android.view.Surface";
    private static final String METHOD_NAME = "screenshot";
    private static ScreenShotUtil instance;
    private Display mDisplay;
    private DisplayMetrics mDisplayMetrics;
    private Matrix mDisplayMatrix;
    private WindowManager wm;
    private SimpleDateFormat format;

    private ScreenShotUtil() {
    }

    public static ScreenShotUtil getInstance() {
        Class var0 = ScreenShotUtil.class;
        synchronized(ScreenShotUtil.class) {
            if (instance == null) {
                instance = new ScreenShotUtil();
            }
        }

        return instance;
    }

    private Bitmap screenShot(int width, int height) {
        Class<?> surfaceClass = null;
        Method method = null;

        try {
            if (Build.VERSION.SDK_INT >= 18) {
                surfaceClass = Class.forName(CLASS1_NAME);
            } else {
                surfaceClass = Class.forName(CLASS2_NAME);
            }

            method = surfaceClass.getDeclaredMethod(METHOD_NAME, Integer.TYPE, Integer.TYPE);
            method.setAccessible(true);
            return (Bitmap)method.invoke((Object)null, width, height);
        } catch (NoSuchMethodException var6) {
            Log.e("ScreentShotUtil", var6.toString());
        } catch (IllegalArgumentException var7) {
            Log.e("ScreentShotUtil", var7.toString());
        } catch (IllegalAccessException var8) {
            Log.e("ScreentShotUtil", var8.toString());
        } catch (InvocationTargetException var9) {
            Log.e("ScreentShotUtil", var9.toString());
        } catch (ClassNotFoundException var10) {
            Log.e("ScreentShotUtil", var10.toString());
        }

        return null;
    }

    @SuppressLint({"NewApi"})
    public Bitmap takeScreenshot(Context context) {
        if (Build.VERSION.SDK_INT < 18 && Build.VERSION.SDK_INT >= 14) {
            this.wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
            this.mDisplay = this.wm.getDefaultDisplay();
            this.mDisplayMatrix = new Matrix();
            this.mDisplayMetrics = new DisplayMetrics();
            this.mDisplay.getRealMetrics(this.mDisplayMetrics);
            float[] dims = new float[]{(float)this.mDisplayMetrics.widthPixels, (float)this.mDisplayMetrics.heightPixels};
            float degrees = this.getDegreesForRotation(this.mDisplay.getRotation());
            boolean requiresRotation = degrees > 0.0F;
            if (requiresRotation) {
                this.mDisplayMatrix.reset();
                this.mDisplayMatrix.preRotate(-degrees);
                this.mDisplayMatrix.mapPoints(dims);
                dims[0] = Math.abs(dims[0]);
                dims[1] = Math.abs(dims[1]);
            }

            Bitmap mScreenBitmap = this.screenShot((int)dims[0], (int)dims[1]);
            if (requiresRotation) {
                Bitmap ss = Bitmap.createBitmap(this.mDisplayMetrics.widthPixels, this.mDisplayMetrics.heightPixels, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(ss);
                c.translate((float)(ss.getWidth() / 2), (float)(ss.getHeight() / 2));
                c.rotate(degrees);
                c.translate(-dims[0] / 2.0F, -dims[1] / 2.0F);
                c.drawBitmap(mScreenBitmap, 0.0F, 0.0F, (Paint)null);
                c.setBitmap((Bitmap)null);
                mScreenBitmap = ss;
                if (ss != null && !ss.isRecycled()) {
                    ss.recycle();
                }
            }

            if (mScreenBitmap == null) {
                Log.e(TAG, "takeScreenshot: screen shot fail");
                return null;
            }

            mScreenBitmap.setHasAlpha(false);
            mScreenBitmap.prepareToDraw();

            return mScreenBitmap;
        }

        return null;
    }

    private float getDegreesForRotation(int value) {
        switch(value) {
            case 1:
                return 270.0F;
            case 2:
                return 180.0F;
            case 3:
                return 90.0F;
            default:
                return 0.0F;
        }
    }
}
