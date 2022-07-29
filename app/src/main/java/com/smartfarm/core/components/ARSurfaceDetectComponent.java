package com.smartfarm.core.components;

import com.google.ar.core.Frame;
import com.google.ar.core.Plane;
import com.smartfarm.core.activity.DefaultAREngineActivity;
import com.smartfarm.common.rendering.PlaneRenderer;
import com.smartfarm.core.Component;

public class ARSurfaceDetectComponent extends Component {

    private final PlaneRenderer planeRenderer = new PlaneRenderer();
    private DefaultAREngineActivity activity;

    public ARSurfaceDetectComponent(DefaultAREngineActivity activity) {
        this.activity = activity;
        try{
            planeRenderer.createOnGlThread(activity, "models/trigrid.png");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public void draw(float[] viewmtx, float[] projmtx, Frame frame) {
        // Visualize planes.
        planeRenderer.drawPlanes(activity.sharedSession.getAllTrackables(Plane.class), frame.getCamera().getDisplayOrientedPose(), projmtx);
    }
}
