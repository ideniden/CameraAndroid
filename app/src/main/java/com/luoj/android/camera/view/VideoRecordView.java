package com.luoj.android.camera.view;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.luoj.android.camera.util.LogUtil;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by 京 on 2016/7/22.
 */
public class VideoRecordView extends SurfaceView implements SurfaceHolder.Callback, MediaRecorder.OnErrorListener, Camera.PreviewCallback {

    public VideoRecordView(Context context) {
        super(context);
        init();
    }
    public VideoRecordView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    public VideoRecordView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private SurfaceHolder mSurfaceHolder;
    private MediaRecorder mMediaRecorder;
    protected Camera mCamera;
    Camera.Parameters mParams;
    int curCameraId= Camera.CameraInfo.CAMERA_FACING_BACK;

    long startTime;
    long stopTime;

    boolean isPreviewing;
    boolean openFlash;
    protected boolean recordStarted;
    protected OnVideoRecordListener mOnVideoRecordListener;

    File mVideoFile = null;
    String outputFilePath;

    private int mWidth=240;// 视频分辨率宽度
    private int mHeight=320;// 视频分辨率高度

    void init(){
        mSurfaceHolder = getHolder();
        mSurfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);//setType需放在addCallback前面，网上说的，不然有的手机崩溃
        mSurfaceHolder.addCallback(this);
        mSurfaceHolder.setKeepScreenOn(true);
        LogUtil.d("init");
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        LogUtil.d("surfaceCreated");
        try {
            openCamera();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        LogUtil.d("surfaceChanged");
    }
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        LogUtil.d("surfaceChanged");
        closeCamera();
    }

    /**
     * 初始化摄像头
     * @throws IOException
     */
    protected void openCamera() throws IOException {
        if (mCamera != null) {
            closeCamera();
        }
        try {
            mCamera = Camera.open(curCameraId);
        } catch (Exception e) {
            e.printStackTrace();
            closeCamera();
        }
        if (mCamera == null)
            return;

        setCameraParams();
        mCamera.setDisplayOrientation(90);
        mCamera.setPreviewDisplay(mSurfaceHolder);
        mCamera.setPreviewCallback(this);
        mCamera.startPreview();
//        mCamera.unlock();
//        mParams = mCamera.getParameters();
        isPreviewing = true;
        LogUtil.d("openCamera("+curCameraId+")");
    }

    /**
     * 设置摄像头为竖屏
     */
    protected void setCameraParams() {
        if (mCamera != null) {
            mParams = mCamera.getParameters();
            //设置颜色效果
            //EFFECT_NONE|EFFECT_MONO|EFFECT_NEGATIVE|EFFECT_SOLARIZE|EFFECT_SEPIA
            //EFFECT_POSTERIZE|EFFECT_WHITEBOARD|EFFECT_BLACKBOARD|EFFECT_AQUA
//            mParams.setColorEffect(Camera.Parameters.EFFECT_SEPIA);

            //设置显示方向
            if (isPortrait(getContext())) {
                mParams.set("orientation", "portrait");
                mCamera.setDisplayOrientation(90);
                LogUtil.d("setCameraParams set orientation portrait");
            } else {// 如果是横屏
                mParams.set("orientation", "landscape");
                mCamera.setDisplayOrientation(0);
                LogUtil.d("setCameraParams set orientation landscape");
            }
//            List<String> focusModes = mParams.getSupportedFocusModes();
//            if (focusModes.contains("continuous-video")) {
//                mParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
//            }
            List<String> focusModes = mParams.getSupportedFocusModes();
            if(focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)){
                mParams.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mCamera.autoFocus(null);
                    }
                });
                LogUtil.d("setCameraParams set focus mode FOCUS_MODE_AUTO");
            }

            Camera.Size closelyPreSize = getCloselyPreSize(isPortrait(getContext()),getMeasuredWidth(), getMeasuredHeight(), mParams.getSupportedPreviewSizes());
            LogUtil.d("setCameraParams set preview size->"+closelyPreSize.width+"*"+closelyPreSize.height);
            mParams.setPreviewSize(closelyPreSize.width,closelyPreSize.height);

            mCamera.setParameters(mParams);
        }
    }

    /**
     * 释放摄像头资源
     *
     * @author liuyinjun
     * @date 2015-2-5
     */
    protected void closeCamera() {
        if (mCamera != null) {
            setOnClickListener(null);
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
//            mCamera.lock();
            mCamera.release();
            mCamera = null;
            isPreviewing = false;
        }
        LogUtil.d("closeCamera");
    }

    protected void createRecordDir(String filePath) {
        if(TextUtils.isEmpty(filePath)){
            this.outputFilePath=generateFilePath();
        }else{
            this.outputFilePath=filePath;
        }
        File recordFile=new File(this.outputFilePath);
        if(!recordFile.getParentFile().exists()){
            recordFile.getParentFile().mkdirs();
        }
        if(recordFile.exists()){
            recordFile.delete();
        }
        LogUtil.d("createRecordDir->"+outputFilePath);
    }

    protected String generateFilePath(){
        //TODO 文件路径
        return "";
    }

    protected void startRecord() throws IOException {
        if(mCamera!=null)mCamera.unlock();
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.reset();
        if (mCamera != null){
            mMediaRecorder.setCamera(mCamera);
        }
        mMediaRecorder.setOnErrorListener(this);
        mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface());
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);// 视频源
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);// 音频源
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_480P));
        LogUtil.d("initRecord CamcorderProfile->QUALITY_480P");
        mMediaRecorder.setOrientationHint(curCameraId== Camera.CameraInfo.CAMERA_FACING_BACK?90:270);// 输出旋转90度，保持竖屏录制

//        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);// 视频输出格式
//                mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);// 视频录制格式
//        mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);// 音频格式
//        mMediaRecorder.setVideoSize(mWidth, mHeight);// 设置分辨率：
//         mMediaRecorder.setVideoFrameRate(20);//
//        mMediaRecorder.setVideoEncodingBitRate(1 * 1024 * 1024);// 设置帧频率，然后就清晰了
        // mediaRecorder.setMaxDuration(Constant.MAXVEDIOTIME * 1000);

        mMediaRecorder.setOutputFile(outputFilePath);
        mMediaRecorder.prepare();
        mMediaRecorder.start();
        startTime=System.currentTimeMillis();
        recordStarted=true;
    }

    protected void stopRecord() {
        LogUtil.d("stopRecord start");
        if(mCamera!=null)mCamera.lock();
        LogUtil.d("stopRecord lock");
        if (mMediaRecorder != null) {
            // 设置后不会崩
            mMediaRecorder.setOnErrorListener(null);
            mMediaRecorder.setPreviewDisplay(null);
            try {
                mMediaRecorder.stop();
                LogUtil.d("stopRecord mMediaRecorder.stop");
                mMediaRecorder.release();
                LogUtil.d("stopRecord mMediaRecorder.release");
                mMediaRecorder = null;
                stopTime=System.currentTimeMillis();
                recordStarted=false;
                LogUtil.d("stopRecord sucess");
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
        LogUtil.d("stopRecord stop");
    }

    /**
     * 开始录制视频
     * 视频储存位置
     * 达到指定时间之后回调接口
     */
    public boolean record(String filePath) {
        if(isStarted()){
            return false;
        }
        createRecordDir(filePath);
        try {
            startRecord();
        } catch (IOException e) {
            if(null!=mOnVideoRecordListener){
                mOnVideoRecordListener.onError(e);
            }
            LogUtil.d("record started failed_>"+e.toString());
            return false;
        }
        if(null!= mOnVideoRecordListener) mOnVideoRecordListener.onRecordStart();
        LogUtil.d("record started");
        return true;
    }

    public boolean record(){
        return record(null);
    }

    /**
     * 停止拍摄
     */
    public void stop() {
        stopRecord();
        if(null!= mOnVideoRecordListener) mOnVideoRecordListener.onRecordStop(getVideoDuration());
        LogUtil.d("record stop call onRecordStop");
        LogUtil.d("record stoped");
    }

    public boolean isStarted(){
        return recordStarted;
    }

    /**
     * @return the mVideoFile
     */
    public String getVideoFilePath() {
        return outputFilePath;
    }

    public long getVideoDuration(){
        if(isStarted()){
            return System.currentTimeMillis()-startTime;
        }else{
            return stopTime-startTime;
        }
    }

    public void setOnVideoRecordListener(OnVideoRecordListener listener) {
        this.mOnVideoRecordListener = listener;
    }

    public boolean setFlashMode(String mode){
        if (curCameraId== Camera.CameraInfo.CAMERA_FACING_BACK&&isPreviewing && (mCamera != null)) {
            Camera.Parameters mParams = mCamera.getParameters();
            mParams.setFlashMode(mode);
            mCamera.setParameters(mParams);
            LogUtil.d("setFlashMode mode->"+mode);
            return true;
        }
        return false;
    }

    public void openFlash(){
        if(setFlashMode(Camera.Parameters.FLASH_MODE_TORCH)){
            openFlash=true;
            LogUtil.d("openFlash");
        }
    }

    public void closeFlash(){
        if(setFlashMode(Camera.Parameters.FLASH_MODE_OFF)){
            openFlash=false;
            LogUtil.d("closeFlash");
        }
    }

    public boolean isFlashOpen(){
        return openFlash;
    }

    public int toggleCamera(){
        if(recordStarted){
            return curCameraId;
        }
        if(curCameraId== Camera.CameraInfo.CAMERA_FACING_BACK){
            curCameraId=Camera.CameraInfo.CAMERA_FACING_FRONT;
        }else{
            curCameraId=Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        try {
            openCamera();
        } catch (IOException e) {
            e.printStackTrace();
        }
        LogUtil.d("toggleCamera("+curCameraId+")");
        return curCameraId;
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {

    }

    /**
     * 录制完成回调接口
     */
    public interface OnVideoRecordListener {
        void onRecordStart();
        void onRecordStop(long videoDuration);
        void onError(Exception e);
    }

    @Override
    public void onError(MediaRecorder mr, int what, int extra) {
        try {
            if (mr != null)
                mr.reset();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    boolean initialized;//

    public void onActivityResume(){
        if (initialized) {
            try {
                openCamera();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        initialized =true;
    }

    public void onActivityPause(){
        closeCamera();
    }

    public boolean isPortrait(Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    public Camera.Size getCloselyPreSize(boolean isPortrait, int surfaceWidth, int surfaceHeight, List<Camera.Size> preSizeList) {

        int ReqTmpWidth;
        int ReqTmpHeight;
        // 当屏幕为垂直的时候需要把宽高值进行调换，保证宽大于高
        if (isPortrait) {
            ReqTmpWidth = surfaceHeight;
            ReqTmpHeight = surfaceWidth;
        } else {
            ReqTmpWidth = surfaceWidth;
            ReqTmpHeight = surfaceHeight;
        }
        //先查找preview中是否存在与surfaceview相同宽高的尺寸
        for (Camera.Size size : preSizeList) {
            if ((size.width == ReqTmpWidth) && (size.height == ReqTmpHeight)) {
                return size;
            }
        }

        // 得到与传入的宽高比最接近的size
        float reqRatio = ((float) ReqTmpWidth) / ReqTmpHeight;
        float curRatio, deltaRatio;
        float deltaRatioMin = Float.MAX_VALUE;
        Camera.Size retSize = null;
        for (Camera.Size size : preSizeList) {
            curRatio = ((float) size.width) / size.height;
            deltaRatio = Math.abs(reqRatio - curRatio);
            if (deltaRatio < deltaRatioMin) {
                deltaRatioMin = deltaRatio;
                retSize = size;
            }
        }

        return retSize;
    }

}
