package com.luoj.android.camera.view.multivideorecord;

/**
 * Created by 京 on 2016/11/16.
 */

public class VideoPart {
    public int index;
    public String path;
    public long duration;
    public VideoPart(int index, String path) {
        this.index = index;
        this.path = path;
    }
    @Override
    public String toString() {
        return "VideoPart{" +
                "index=" + index +
                ", path='" + path + '\'' +
                ", duration=" + duration +
                '}';
    }
}
