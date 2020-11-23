package com.example.crosssoftwaretakeword.data;

import android.graphics.Rect;

import java.util.List;

/**
 * 句子或者单词
 */
public class OcrContentBean {
    private String content;
    private Rect contentRect;

    private List<OcrContentBean> sentenceList;//每行都是这句的一部分    一个句子由多行组成

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Rect getContentRect() {
        return contentRect;
    }

    public void setContentRect(Rect contentRect) {
        this.contentRect = contentRect;
    }

    public void setSentenceList(List<OcrContentBean> sentenceList) {
        this.sentenceList = sentenceList;
    }

    public List<OcrContentBean> getSentenceList() {
        return sentenceList;
    }
}
