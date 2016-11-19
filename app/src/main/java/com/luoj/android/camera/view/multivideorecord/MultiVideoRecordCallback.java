package com.luoj.android.camera.view.multivideorecord;

/**
 * Created by 京 on 2016/11/16.
 */

public interface MultiVideoRecordCallback {
    void onRecordStarted(MultiVideoRecordView multiVideoRecordView);
    void onRecording(MultiVideoRecordView multiVideoRecordView, byte[] frameBytes);
    void onRecordCompleted(MultiVideoRecordView multiVideoRecordView);
}
