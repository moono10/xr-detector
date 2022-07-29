package com.smartfarm.core.components.objectDetect;

import android.graphics.RectF;

import com.google.mlkit.vision.objects.DetectedObject;
import com.smartfarm.common.rendering.geometry.Ray;
import com.smartfarm.common.rendering.geometry.Vector3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ObjectDetectMap {

    private Map<Integer, ObjectDetectResult> map = new ConcurrentHashMap<>();

    public Map<Integer, ObjectDetectResult> getMap() {
        return map;
    }

    public void setMap(Map<Integer, ObjectDetectResult> map) {
        this.map = map;
    }

    public void detectedObjects(List<DetectedObject> detectedObjects, int imageWidth, int imageHeight, int viewWidth, int viewHeight, float[] projmtx, float[] viewmtx) {
        float viewAspectRatio = (float) viewWidth / viewHeight;             //0.4655172413793103
        float imageAspectRatio = (float) imageWidth / imageHeight;            // 0.75
        float scaleFactor = 0;
        float postScaleWidthOffset = 0;
        float postScaleHeightOffset = 0;
        if (viewAspectRatio > imageAspectRatio) {
            scaleFactor = (float) viewWidth / imageWidth;
            postScaleHeightOffset = ((float) viewWidth / imageAspectRatio - viewHeight) / 2;
        } else {
            scaleFactor = (float) viewHeight / imageHeight;
            postScaleWidthOffset = ((float) viewHeight * imageAspectRatio - viewWidth) / 2;
        }


        for (Integer key : map.keySet()) {
            map.get(key).setCheck(false);
        }
        for (DetectedObject detectedObject : detectedObjects) {
            detectedObject(detectedObject, viewWidth, viewHeight, scaleFactor, postScaleWidthOffset, postScaleHeightOffset, projmtx, viewmtx);
        }
        Set<Integer> keyset =  map.keySet();
        for (Integer key : keyset) {
            if (!map.get(key).isCheck()) {
                map.remove(key);
            }
        }
    }

    public void detectedObject(DetectedObject detectedObject, int viewWidth, int viewHeight, float scaleFactor, float postScaleWidthOffset, float postScaleHeightOffset, float[] projmtx, float[] viewmtx) {
        //if (detectedObject.getLabels().size() == 0 || !"Mouse".equals( detectedObject.getLabels().get(0).getText())) return;
        if (map.get(detectedObject.getTrackingId()) == null) {
            map.put(detectedObject.getTrackingId(), new ObjectDetectResult(detectedObject));
        }

        RectF rect = new RectF(detectedObject.getBoundingBox());
        float x0 = translateX(rect.left, scaleFactor, postScaleWidthOffset);
        float x1 = translateX(rect.right, scaleFactor, postScaleWidthOffset);
        rect.left = Math.min(x0, x1);
        rect.right = Math.max(x0, x1);
        rect.top = translateY(rect.top, scaleFactor, postScaleHeightOffset);
        rect.bottom = translateY(rect.bottom, scaleFactor, postScaleHeightOffset);

        float coordx = (float) (((rect.left + rect.right) / 2.0) / viewWidth * 2.0 - 1.0);
        float coordy = (float) (((rect.top + rect.bottom) / 2.0) / viewHeight * -2.0 + 1.0);

        Vector3 origin = new Vector3(0, 0, 0);
        origin.setFromMatrixPosition(viewmtx);
        Vector3 direction = new Vector3(0, 0, -1);
        direction.set(coordx, coordy, 0.5f).unproject(projmtx, viewmtx).sub(origin).normalize();

        ObjectDetectResult result = map.get(detectedObject.getTrackingId());
        result.setDetectedObject(detectedObject);
        result.setScreenRect(rect);
        result.setCheck(true);
        result.addRay(new Ray(new Vector3(origin.x, origin.y, origin.z), new Vector3(direction.x, direction.y, direction.z)));

    }

    public float translateX(float x, float scaleFactor, float postScaleWidthOffset) {
        // if (overlay.isImageFlipped) {
        //    return overlay.getWidth() - (scale(x) - overlay.postScaleWidthOffset);
        // } else {
        return scale(x ,scaleFactor) - postScaleWidthOffset;
        // }
    }

    public float translateY(float y, float scaleFactor, float postScaleHeightOffset) {
        return scale(y, scaleFactor) - postScaleHeightOffset;
    }

    public float scale(float imagePixel, float scaleFactor) {
        return imagePixel * scaleFactor;
    }
}
