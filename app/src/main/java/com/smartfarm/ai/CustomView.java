package com.smartfarm.ai;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.google.mlkit.vision.objects.DetectedObject;
import com.smartfarm.core.components.objectDetect.ObjectDetectMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomView extends View {

    private Paint paint;
    private ObjectDetectMap odMap;

    public CustomView(Context context) {
        super(context);
    }

    public CustomView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private static final float STROKE_WIDTH = 4.0f;
    private static final float TEXT_SIZE = 32.0f;
    private static final String LABEL_FORMAT = "%.2f%% confidence (index: %d)";


    /**
     * 뷰가 화면에 디스플레이 될때 자동으로 호출
     *
     * @param canvas
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);


        Paint paint = new Paint(); // 페인트 객체 생성
        paint.setColor(Color.RED); // 빨간색으로 설정
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6.0f);


        Paint paint2 = new Paint(); // 페인트 객체 생성
        paint2.setColor(Color.WHITE); // 빨간색으로 설정
        paint2.setStyle(Paint.Style.FILL);
        paint2.setTextSize(TEXT_SIZE);

    if (this.odMap == null) return;
        for (Integer key : this.odMap.getMap().keySet()) {

            RectF rect = this.odMap.getMap().get(key).getScreenRect();
            DetectedObject o = this.odMap.getMap().get(key).getDetectedObject();

            float textWidth = paint.measureText("Tracking ID: " + o.getTrackingId());
            float lineHeight = TEXT_SIZE + STROKE_WIDTH;
            float yLabelOffset = -lineHeight;

            canvas.drawRect(rect.left, rect.top, rect.right, rect.bottom, paint);

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
    }
    public void render(ObjectDetectMap odMap) {
        this.odMap = odMap;

        this.invalidate();
    }

}