package com.luoj.android.camera.view.multivideorecord;

import android.content.Context;
import android.hardware.Camera;
import android.text.TextUtils;
import android.util.AttributeSet;

import com.luoj.android.camera.util.FileUtil;
import com.luoj.android.camera.util.LogUtil;
import com.luoj.android.camera.view.VideoRecordView;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by 京 on 2016/11/14.
 */

public class MultiVideoRecordView extends VideoRecordView implements VideoRecordView.OnVideoRecordListener {

    public MultiVideoRecordView(Context context) {
        super(context);
    }

    public MultiVideoRecordView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MultiVideoRecordView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    final String videoFileTypeSuffix=".mp4";

    /**
     * 参数
     */
    String mCacheDir;
    private String concatScriptFilePath;
    String mergeFilePath;
    long mMinMillisecond,mMaxMillisecond;

    /**
     * 缓存
     */
    private int index=-1;//录制索引(开始录制之后增一，可以表示当前录制索引)
    LinkedHashMap<String,VideoPart> videos=new LinkedHashMap<>();//路径为key

    MultiVideoRecordCallback recordCallback;

    MultiVideoRuntimeCallback runtimeCallback;

    public void init(String cacheDir,long minMillisecond,long maxMillisecond,MultiVideoRecordCallback callback){
        this.recordCallback=callback;
        setOnVideoRecordListener(this);
        if(TextUtils.isEmpty(cacheDir)){
            throw new RuntimeException("cacheDir is null");
        }
        if(!cacheDir.endsWith(File.separator)){
            mCacheDir=cacheDir+File.separator;
        }else{
            mCacheDir=cacheDir;
        }
        concatScriptFilePath =mCacheDir+"config.txt";
        mergeFilePath=mCacheDir+"merge"+videoFileTypeSuffix;

        mMinMillisecond=minMillisecond;
        mMaxMillisecond=maxMillisecond;

        FileUtil.DeleteFolder(mCacheDir);
        new File(mCacheDir).mkdirs();
        LogUtil.d("init MultiVideoRecordView");
        LogUtil.d("cache dir->"+mCacheDir);
        LogUtil.d("min duration->"+mMinMillisecond);
        LogUtil.d("max duration->"+mMaxMillisecond);
    }

    public void setRuntimeCallback(MultiVideoRuntimeCallback runtimeCallback){
        this.runtimeCallback=runtimeCallback;
    }

    void indexforward(){
        if(!isStarted()){
            index+=1;
        }
    }

    void indexbackward(){
        if(!isStarted()){
            index-=1;
        }
    }

    @Override
    public boolean record() {
        indexforward();
        return super.record();
    }

    @Override
    public void onRecordStart() {
        append(getCurrentIndex(),getVideoFilePath());
        if(null!=recordCallback){
            recordCallback.onRecordStarted(this);
        }
        if(null!=runtimeCallback){
            runtimeCallback.onRecordStarted(this);
        }
    }

    @Override
    public void onRecordStop(long videoDuration) {
        LogUtil.d("onRecordStop start");
        VideoPart part = getCurrentPart();
        LogUtil.d("onRecordStop getCurrentPart"+part.toString());
        part.duration=videoDuration;
        LogUtil.d("onRecordStop set duration->"+videoDuration);
        if(null!=recordCallback){
            recordCallback.onRecordCompleted(this);
            LogUtil.d("onRecordStop call onRecordCompleted");
        }
        if(null!=runtimeCallback){
            runtimeCallback.onRecordCompleted(this);
        }
        LogUtil.d("onRecordStop stop");
    }

    @Override
    public void onError(Exception e) {

    }

    @Override
    protected String generateFilePath(){
        return mCacheDir + getCurrentIndex() + videoFileTypeSuffix;
    }

    public long getMaxDuration(){
        return mMaxMillisecond;
    }

    private void append(int indx,String filePath){
        VideoPart part=new VideoPart(indx,filePath);
        videos.put(part.path,part);
    }

    public int getSize(){
        return videos.size();
    }

    public boolean isEmpty(){
        return videos.isEmpty();
    }

    /**
     * 获取总时长
     * @return 单位毫秒
     */
    public long getTotalDuration(){
        long total=0l;
        for (VideoPart part :videos.values()) {
            total+=part.duration;
        }
        //如果正在录制中，录制部分的duration是没有存入数据的，需要通过函数获取
        if(isStarted()){
            total+=getVideoDuration();
        }
        return total;
    }

    public int getCurrentIndex(){
        return index;
    }

    public VideoPart getCurrentPart(){
        return getPart(getCurrentIndex());
    }

    public VideoPart getPart(int index){
        Iterator<Map.Entry<String, VideoPart>> iterator = videos.entrySet().iterator();
        VideoPart tail = null;
        int i=0;
        while (iterator.hasNext()) {
            Map.Entry<String, VideoPart> next = iterator.next();
            if(i==index){
                tail = next.getValue();
                break;
            }
            i+=1;
        }
        return tail;
    }

    public boolean delete(int index){
        if(isStarted()){
            return false;
        }
        return delete(getPart(index));
    }

    public boolean delete(VideoPart part){
        if(isStarted()){
            return false;
        }
        FileUtil.deleteFile(part.path);
        videos.remove(part.path);
        int currentIndex = getCurrentIndex();
        if(currentIndex>=0){
            indexbackward();
        }
        if(null!=runtimeCallback){
            runtimeCallback.onDeleted(this,currentIndex);
        }
        return true;
    }

    public boolean  deleteAll(){
        if(isStarted()){
            return false;
        }
        FileUtil.DeleteFolder(mCacheDir);
        videos.clear();
        return true;
    }

    //TODO 设计合成接口，可通过不同方式实现
//    public String merge(ExecuteBinaryResponseHandler handler){
//        String[] partFilesPath=new String[getSize()];
//        int i=0;
//        for (VideoPart part : videos.values()) {
//            partFilesPath[i]=part.path;
//            i+=1;
//        }
//        try {
//            FFmpegUtil.generateConcatScriptFile(concatScriptFilePath,partFilesPath);
//            FFmpegUtil.mergeVideo(concatScriptFilePath,mergeFilePath,handler);
//        } catch (IOException e) {
//            e.printStackTrace();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return mergeFilePath;
//    }

    public String getMergeVideoFilePath(){
        return mergeFilePath;
    }

    @Override
    public void onPreviewFrame(byte[] bytes, Camera camera) {
        if(isStarted()){
            getCurrentPart().duration=getVideoDuration();
            if(null!=recordCallback){
                recordCallback.onRecording(this,bytes);
            }
            if(null!=runtimeCallback){
                runtimeCallback.onRecording(this,bytes);
            }
        }
    }

}
