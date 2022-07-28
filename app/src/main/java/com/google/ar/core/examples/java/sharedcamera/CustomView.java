package com.google.ar.core.examples.java.sharedcamera;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.google.mlkit.vision.objects.DetectedObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomView extends View {

    private Paint paint;
    private int imageWidth;
    private int imageHeight;
    private List<DetectedObject> detectedObjects = new ArrayList<DetectedObject>();

    public CustomView(Context context) {
        super(context);
    }

    public CustomView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private static final float STROKE_WIDTH = 4.0f;
    private static final float TEXT_SIZE = 54.0f;
    private static final String LABEL_FORMAT = "%.2f%% confidence (index: %d)";
    private  float postScaleWidthOffset;
    private  float postScaleHeightOffset;
    private  float scaleFactor;
    /**
     * 뷰가 화면에 디스플레이 될때 자동으로 호출
     * @param canvas
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float viewAspectRatio = (float) getWidth() / getHeight();             //0.4655172413793103
        float imageAspectRatio = (float) imageWidth / imageHeight;            // 0.75
        postScaleWidthOffset = 0;
        postScaleHeightOffset = 0;
        if (viewAspectRatio > imageAspectRatio) {

            scaleFactor = (float) getWidth() / imageWidth;
            postScaleHeightOffset = ((float) getWidth() / imageAspectRatio - getHeight()) / 2;
         } else {

           scaleFactor = (float) getHeight() / imageHeight;
           postScaleWidthOffset = ((float) getHeight() * imageAspectRatio - getWidth()) / 2;
         }



        Paint paint = new Paint(); // 페인트 객체 생성
        paint.setColor(Color.RED); // 빨간색으로 설정
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6.0f);


        Paint paint2 = new Paint(); // 페인트 객체 생성
        paint2.setColor(Color.WHITE); // 빨간색으로 설정
        paint2.setStyle(Paint.Style.FILL);
        paint2.setTextSize(TEXT_SIZE);


        for (DetectedObject o : this.detectedObjects) {

            float textWidth = paint.measureText("Tracking ID: " + o.getTrackingId());
            float lineHeight = TEXT_SIZE + STROKE_WIDTH;
            float yLabelOffset = -lineHeight;



            RectF rect = new RectF(o.getBoundingBox());
            // If the image is flipped, the left will be translated to right, and the right to left.
            float x0 = translateX(rect.left);
            float x1 = translateX(rect.right);
            rect.left = Math.min(x0, x1);
            rect.right = Math.max(x0, x1);
            rect.top = translateY(rect.top);
            rect.bottom = translateY(rect.bottom);


            Log.d("", "rect: " + rect);




           //
            // Log.d(TAG,  "--------------------------------" + image.getWidth() + ", " + image.getHeight());
            //float width =  rect.bottom - rect.top;
            canvas.drawRect(  rect.left , rect.top, rect.right, rect.bottom, paint);


            // Draws other object info.
            canvas.drawRect(
                    rect.left - STROKE_WIDTH,
                    rect.top + yLabelOffset,
                    rect.left + textWidth + (2 * STROKE_WIDTH),
                    rect.top,
                    paint);
            yLabelOffset += TEXT_SIZE;
            canvas.drawText(
                    "Tracking ID: " + o.getTrackingId(),
                    rect.left,
                    rect.top + yLabelOffset,
                    paint2);
            yLabelOffset += lineHeight;

            for (DetectedObject.Label label : o.getLabels()) {
                canvas.drawText(label.getText(), rect.left, rect.top + yLabelOffset, paint2);
                yLabelOffset += lineHeight;
                canvas.drawText(
                        String.format(Locale.US, LABEL_FORMAT, label.getConfidence() * 100, label.getIndex()),
                        rect.left,
                        rect.top + yLabelOffset,
                        paint2);

                yLabelOffset += lineHeight;
            }
        }


       // Log.d("", "w : " + getWidth());
       // Log.d("", "h : " + getHeight());
       // Log.d("", "s : " + scaleFactor);
       // Log.d("", "O : " + postScaleWidthOffset);







    }
    public float translateX(float x) {
       // if (overlay.isImageFlipped) {
        //    return overlay.getWidth() - (scale(x) - overlay.postScaleWidthOffset);
       // } else {
            return scale(x) - postScaleWidthOffset;
       // }
    }

    /**
     * Adjusts the y coordinate from the image's coordinate system to the view coordinate system.
     */
    public float translateY(float y) {
        return scale(y) - postScaleHeightOffset;
    }

    public float scale(float imagePixel) {

        return imagePixel * scaleFactor;
    }
    public void setDetectedObjects(List<DetectedObject> detectedObjects, int width, int height) {
        this.detectedObjects = detectedObjects;
        this.imageWidth = height > width ? width : height;
        this.imageHeight = height > width ? height : width;
        this.invalidate();
       // Log.d("", detectedObjects.size() + "----111인식");

        //for (DetectedObject o : detectedObjects) {
        //    Log.d("", "ID : " + o.getTrackingId());
        //    Log.d("", "LABEL : " + (o.getLabels().size() > 0 ? o.getLabels().get(0).getText() : ""));
        //    Log.d("", "BBOX : " + o.getBoundingBox());
        //}

    }
}