package com.smartfarm.core.components;

import android.content.Context;

import com.google.ar.core.Frame;
import com.google.ar.core.PointCloud;
import com.smartfarm.common.rendering.PointCloudRenderer;
import com.smartfarm.common.rendering.geometry.LineString;
import com.smartfarm.core.Component;

public class ARTrackedPointComponent extends Component {
    private final PointCloudRenderer pointCloudRenderer = new PointCloudRenderer();
    public ARTrackedPointComponent(Context context)  {
        try {
            pointCloudRenderer.createOnGlThread(context);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public void draw(float[] viewmtx, float[] projmtx, Frame frame) {
        // Visualize tracked points.
        // Use try-with-resources to automatically release the point cloud.
        try (PointCloud pointCloud = frame.acquirePointCloud()) {
            pointCloudRenderer.update(pointCloud);
            pointCloudRenderer.draw(viewmtx, projmtx);
        }
    }
}
