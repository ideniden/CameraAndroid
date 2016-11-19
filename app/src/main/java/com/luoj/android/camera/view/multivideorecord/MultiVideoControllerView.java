package com.luoj.android.camera.view.multivideorecord;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;

import com.tosee.android.util.LogUtil;
import com.tosee.android.util.MathUtil;

import java.util.ArrayList;

/**
 * Created by 京 on 2016/11/16.
 */

public class MultiVideoControllerView extends View implements MultiVideoRuntimeCallback{

    public MultiVideoControllerView(Context context) {
        super(context);
        init();
    }

    public MultiVideoControllerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MultiVideoControllerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    Handler handler=new Handler(){
        @Override
        public void handleMessage(Message msg) {

        }
    };

    Paint bgPaint;
    Paint bgRingPaint;
    Paint ringPaint;
    Paint ringCheckedPaint;

    int bgRadius=80;

    int ringPadding=10;
    int ringWidth=20;

    int ringRadius=bgRadius+ringPadding+ringWidth;

    /**
     * 绘制圆环部分需要间距表示多个块，
     */
    float partAngleMargin=1f;
    private ArrayList<RingPart> parts=new ArrayList<>();
    int checkedIndex=-1;

    private void init() {
        bgPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(Color.WHITE);
        bgPaint.setStyle(Paint.Style.FILL);

        bgRingPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
        bgRingPaint.setColor(Color.WHITE);
        bgRingPaint.setStyle(Paint.Style.STROKE);
        bgRingPaint.setStrokeWidth(ringWidth);

        ringPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
        ringPaint.setColor(Color.BLACK);
        ringPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(ringWidth);

        ringCheckedPaint=new Paint(Paint.ANTI_ALIAS_FLAG);
        ringCheckedPaint.setColor(Color.GRAY);
        ringCheckedPaint.setStyle(Paint.Style.STROKE);
        ringPaint.setStrokeWidth(ringWidth);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        //draw bg
        canvas.drawCircle(centerX,centerY,bgRadius,bgPaint);
        canvas.drawCircle(centerX,centerY,ringRadius,bgRingPaint);

        RectF arcRect=new RectF(centerX-ringRadius,centerY-ringRadius,centerX+ringRadius,centerY+ringRadius);

        //draw parts
        if(!parts.isEmpty()){
            for (int i = 0; i < parts.size(); i++) {
                float startAngle = getPart(i).startAngle;
                float sweepAngle = getPart(i).sweepAngle;
                if(sweepAngle>0)canvas.drawArc(arcRect,startAngle,sweepAngle,false,ringPaint);
            }
        }

        //draw checked
//        if(checkedIndex>-1){
//            RingPart rp=getPart(checkedIndex);
//            if(null!=rp)canvas.drawArc(arcRect,rp.startAngle,rp.sweepAngle,false,ringCheckedPaint);
//        }
    }

    public int getLastIndex(){
        return parts.size()-1;
    }

    public RingPart getLastOne(){
        return getPart(getLastIndex());
    }

    public boolean isChecked(){
        return checkedIndex!=-1;
    }

    public RingPart getPart(int index){
        if(index>=parts.size()){
            return null;
        }
        return parts.get(index);
    }

    public void addPart(){
        addPart(0f);
    }

    public void addPart(float sweepAngle){
        if(parts.isEmpty()){
            parts.add(new RingPart(-90f,sweepAngle));
        }else{
            RingPart last = getLastOne();
            float startAngle=last.startAngle+last.sweepAngle+partAngleMargin;
            parts.add(new RingPart(startAngle,sweepAngle));
        }
//        postInvalidate();
    }

    public boolean refreshSweepAngle(int index,float sweepAngle){
        RingPart ringPart = getPart(index);
        if(null==ringPart){
            return false;
        }
        ringPart.sweepAngle=sweepAngle;
        invalidate();
        return true;
    }

    public void removeLastOne(){
        if(parts.isEmpty()){
            parts.remove(getLastIndex());
        }
//        postInvalidate();
    }

    public void check(int index){
        checkedIndex=index;
//        postInvalidate();
    }

    public void checkLastOne(){
        check(getLastIndex());
    }

    public void clearChecked(){
        checkedIndex=-1;
//        postInvalidate();
    }

    public float computeAngle(long maxDuration,long duration){
        return MathUtil.mapping(duration,0f,maxDuration,0f,360f);
    }

    @Override
    public void onDeleted(MultiVideoRecordView recordView,int deleteIndex) {
//        if(deleteIndex==getLastIndex()){
//            removeLastOne();
//        }
    }

    @Override
    public void onRecordStarted(MultiVideoRecordView multiVideoRecordView) {
        addPart();
    }

    @Override
    public void onRecording(MultiVideoRecordView multiVideoRecordView, byte[] frameBytes) {
        LogUtil.d("onRecording");
        int currentIndex = multiVideoRecordView.getCurrentIndex();
//        RingPart ringPart = getPart(currentIndex);
//        if(null==ringPart){
//            addPart();
//        }else{
            long maxDuration = multiVideoRecordView.getMaxDuration();
            long duration = multiVideoRecordView.getVideoDuration();
            refreshSweepAngle(currentIndex,computeAngle(maxDuration,duration));
//        }
    }

    @Override
    public void onRecordCompleted(MultiVideoRecordView multiVideoRecordView) {
        int currentIndex = multiVideoRecordView.getCurrentIndex();
        long maxDuration = multiVideoRecordView.getMaxDuration();
        long duration = multiVideoRecordView.getCurrentPart().duration;
        refreshSweepAngle(currentIndex,computeAngle(maxDuration,duration));
    }

}
