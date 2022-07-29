package com.smartfarm.core.components.objectDetect;

import android.graphics.RectF;
import android.util.Log;

import com.google.mlkit.vision.objects.DetectedObject;
import com.smartfarm.common.rendering.geometry.Ray;
import com.smartfarm.common.rendering.geometry.Vector3;

import java.sql.Array;
import java.util.ArrayList;
import java.util.List;

public class ObjectDetectResult {

    private DetectedObject detectedObject;
    private RectF screenRect;
    private List<Ray> rays = new ArrayList();
    private float[] anchorMatrix;
    private boolean check;
    private double avgDirectionLength;

    public ObjectDetectResult(DetectedObject detectedObject) {
        this.detectedObject = detectedObject;
    }

    public void setDetectedObject(DetectedObject detectedObject) {
        this.detectedObject = detectedObject;
    }

    public void setScreenRect(RectF screenRect) {
        this.screenRect = screenRect;
    }

    public DetectedObject getDetectedObject() {
        return detectedObject;
    }

    public RectF getScreenRect() {
        return screenRect;
    }

    public List<Ray> getRays() {
        return rays;
    }

    public void setRays(List<Ray> rays) {
        this.rays = rays;
    }

    public float[] getAnchorMatrix() {
        return anchorMatrix;
    }

    public void setAnchorMatrix(float[] anchorMatrix) {
        this.anchorMatrix = anchorMatrix;
    }

    public boolean isCheck() {
        return check;
    }

    public void setCheck(boolean check) {
        this.check = check;
    }

    public void addRay(Ray ray) {
        this.rays.add(ray);

        double min = Double.MAX_VALUE;
        Integer removeIndex = null;
        Vector3 center = new Vector3(0, 0, 0);
        int cnt = 0;
        double sumLen = 0.0;
        for (int i = 0; i < rays.size(); i++) {
            for (int j = i + 1; j < rays.size(); j++) {
                Ray ray1 = rays.get(i);
                Ray ray2 = rays.get(j);

                    double len = new Vector3(ray1.direction.x, ray1.direction.y, ray1.direction.z).sub(ray2.direction).length();
                    if (len < min) {
                        min = len;
                        if (i == rays.size() - 2) {
                            removeIndex = i + 1;
                        } else {
                            removeIndex = i;
                        }
                    }

                try {
                    Vector3[] ps = ray1.getSkewPoints(ray2);
                    ps[0].add(ps[1]).divideScalar(2.0f);
                    center.add(ps[0]);
                    sumLen += len;
                    cnt++;
                } catch (Exception e) {
                }
            }
        }
        center.divideScalar(cnt);
        avgDirectionLength = (sumLen / cnt);
        this.anchorMatrix = new float[]{1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
        anchorMatrix[12] = center.x;
        anchorMatrix[13] = center.y - 0.025f;
        anchorMatrix[14] = center.z;

        if (removeIndex != null && rays.size() > 50) {

            rays.remove(rays.get(removeIndex));
        }
    }

    public double getAvgDirectionLength() {
        return avgDirectionLength;
    }

    public void setAvgDirectionLength(double avgDirectionLength) {
        this.avgDirectionLength = avgDirectionLength;
    }
}
