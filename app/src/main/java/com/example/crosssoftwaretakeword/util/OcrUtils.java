package com.example.crosssoftwaretakeword.util;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.crosssoftwaretakeword.data.OcrContentBean;
import com.googlecode.leptonica.android.Pixa;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.util.ArrayList;
import java.util.List;

public class OcrUtils {

    /**
     * 获取截图中的文本信息
     * @param words           获取到的单词会放到这个集合中
     * @param sentences       获取到的句子会放到这个集合中
     * @param bitmap          截图
     * @param tessBaseAPI     ocr
     */
    public static void paresBitmap(
            List<OcrContentBean> words,
            List<OcrContentBean> sentences,
            Bitmap bitmap,
            TessBaseAPI tessBaseAPI,
            int screenWidth,
            int screenHeight){

        tessBaseAPI.setImage(bitmap);
        //获取所有的ocr结果
//                String result = baseApi.getUTF8Text();
//                String[] words = result.split("[\n ]+");

        //获取单词的矩形
        Pixa pixes = tessBaseAPI.getWords();

        if (pixes.size() == 0) {
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                @Override
                public void run() {
                    //TODO　当前截图未识别到文本
                    LogUtils.logE("当前截图未识别到文本");
                }
            });
        }

        for (int i = 0; i < pixes.size(); i++) {
            Rect rect = pixes.getBoxRect(i);

            //这个ocr有时候会识别出一个 矩形为全屏的矩阵   过滤这种情况
            if (rect.left == 0 && rect.top == 0 && rect.right == screenWidth && rect.bottom == screenHeight) {
                continue;
            }

            OcrContentBean bean = new OcrContentBean();
            bean.setContentRect(rect);
            words.add(bean);
        }

        //计算句子的多边形
        List<List<OcrContentBean>> map = new ArrayList<>();

        //将同一行单词bean归到一起
        OcrContentBean lastBean = null;
        for (OcrContentBean bean : words) {
            if (map.size() == 0) {
                map.add(new ArrayList<OcrContentBean>());
                map.get(0).add(bean);
            } else {
                //如果bottom相差不到30px  就认为这两个单词在同一行
                if (Math.abs(lastBean.getContentRect().bottom - bean.getContentRect().bottom) <= 30 ||
                        Math.abs(lastBean.getContentRect().top - bean.getContentRect().top) <= 30) {
                    map.get(map.size() - 1).add(bean);
                } else {
                    map.add(new ArrayList<OcrContentBean>());
                    map.get(map.size() - 1).add(bean);
                }
            }
            lastBean = bean;
        }

        //将每行中，如果单词和单词之间的横向间隔比较小，就认为是同一句话
        List<List<OcrContentBean>> sentenceMap = new ArrayList<>();
        for (List<OcrContentBean> beans : map) {
            List<OcrContentBean> sentenceList = new ArrayList<>();
            lastBean = null;
            OcrContentBean sentenceBean = null;
            //ocr识别出来的单词矩形  是从左到右 从上到下排列的
            for (OcrContentBean bean : beans) {
                if (lastBean != null) {
                    //如果单词和单词之间的间隔小于30  就将其合并
                    if (Math.abs(bean.getContentRect().left - lastBean.getContentRect().right) <= 30) {
                        mergeRect(sentenceBean.getContentRect(), bean.getContentRect());
                    } else {
                        //如果离太远  就算是新的一句话
                        sentenceList.add(sentenceBean);
                        sentenceBean = new OcrContentBean();
                        sentenceBean.setContentRect(new Rect(
                                bean.getContentRect().left,
                                bean.getContentRect().top,
                                bean.getContentRect().right,
                                bean.getContentRect().bottom));
                    }
                } else {
                    //lastBean为null  说明是第一个
                    sentenceBean = new OcrContentBean();
                    sentenceBean.setContentRect(new Rect(
                            bean.getContentRect().left,
                            bean.getContentRect().top,
                            bean.getContentRect().right,
                            bean.getContentRect().bottom));
                }
                lastBean = bean;
            }

            //处理最后一个单词没有单词能和他做比较的情况
            if (sentenceBean != null) {
                sentenceList.add(sentenceBean);
            }
            sentenceMap.add(sentenceList);
        }

//                int index = 1;                              用于debug
//                OcrContentBean lb = null;
//                List<Rect> rects = new ArrayList<>();
//                for(List<OcrContentBean> beans : sentenceMap){
//                    OcrContentBean bean = beans.get(0);
//                    Log.d(TAG, "onShotOk: 第" + index + "行");
//                    if(lb != null){
//                        Log.d(TAG, "onShotOk: 第" + (index - 1) + "行和第" + index
//                                + "行之间的距离：" + (bean.getContentRect().top - lb.getContentRect().bottom));
//                    }
//
//                    index ++;
//                    lb = bean;
//                    rects.add(lb.getContentRect());
//                }
//                new Handler(Looper.getMainLooper()).post(new Runnable() {
//                    @Override
//                    public void run() {
//                        maskShowRectView.setRects(rects);
//                    }
//                });

        //如果句子和句子之间的纵向距离比较小，也算是同一句话
        //只识别左对齐的句子
        //这里相当于分段   可以在此基础上做分句功能
        if (sentenceMap.size() != 0) {
            int row = 0;  //第几行
            int startIndex = 0; //记录从上往下数  第一个还有item的list的下标
            lastBean = null;
            OcrContentBean lastSentenceBean = null;
            while (true) {
                if (row == startIndex) {
                    //如果这一行的句子用完了  找下一个没用完的
                    while (startIndex < sentenceMap.size() &&
                            sentenceMap.get(startIndex).isEmpty()) {
                        startIndex++;
                    }

                    //说明map里的全部句子都remove完了  结束
                    if (startIndex == sentenceMap.size()) {
                        sentences.add(lastSentenceBean);
                        break;
                    }

                    row = startIndex;

                    lastBean = sentenceMap.get(row).remove(0);
                    lastSentenceBean = new OcrContentBean();
                    //这个矩形用于显示在屏幕上
                    lastSentenceBean.setContentRect(new Rect(
                            lastBean.getContentRect().left,
                            lastBean.getContentRect().top,
                            lastBean.getContentRect().right,
                            lastBean.getContentRect().bottom));
                    //多个句子矩形组成的多边形才是句子本来的形状
                    lastSentenceBean.setSentenceList(new ArrayList<OcrContentBean>());
                    lastSentenceBean.getSentenceList().add(lastBean);
                    row++;
                } else {
                    //已经是最后一行
                    if (row == sentenceMap.size()) {
                        sentences.add(lastSentenceBean);
                        row = startIndex;
                    } else {
                        //是否有句子和lastBean的句子离的很近
                        boolean hasMatches = false;
                        for (OcrContentBean bean : sentenceMap.get(row)) {
                            //左对齐  所以判断两矩形的left是否够近
                            //而且判断两个矩形的纵向距离是否够近
                            if (Math.abs(bean.getContentRect().left - lastBean.getContentRect().left) <= 30 &&
                                    Math.abs(bean.getContentRect().top - lastBean.getContentRect().bottom) <= 60) {
                                hasMatches = true;
                                lastSentenceBean.getSentenceList().add(bean);
                                lastBean = bean;

                                //合并两个句子矩形
                                mergeRect(lastSentenceBean.getContentRect(), bean.getContentRect());
                                break;
                            }
                        }

                        if (hasMatches) {
                            sentenceMap.get(row).remove(lastBean);
                            row++;
                        } else {
                            //如果这一行没有离得近的句子  就结束这一次
                            sentences.add(lastSentenceBean);
                            row = startIndex;
                        }
                    }
                }
            }
        }
    }

    /**
     * 合并两个区间
     * 合并到第一个矩形中
     *
     * @param r1
     * @param r2
     * @return
     */
    private static void mergeRect(Rect r1, Rect r2) {
        r1.left = Math.min(r1.left, r2.left);
        r1.top = Math.min(r1.top, r2.top);
        r1.right = Math.max(r1.right, r2.right);
        r1.bottom = Math.max(r1.bottom, r2.bottom);
    }
}
