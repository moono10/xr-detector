package com.smartfarm.core.components;

import android.content.Context;

import com.google.ar.core.Frame;
import com.smartfarm.ai.SharedCameraActivity;
import com.smartfarm.common.rendering.BackgroundRenderer;
import com.smartfarm.common.rendering.geometry.LineString;
import com.smartfarm.core.Component;

public class ARBackgroundComponent extends Component {
    // Renderers, see hello_ar_java sample to learn more.
    public final BackgroundRenderer backgroundRenderer = new BackgroundRenderer();

    public ARBackgroundComponent(SharedCameraActivity context) {
        try {
            backgroundRenderer.createOnGlThread(context);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void draw(float[] viewmtx, float[] projmtx, Frame frame) {
        // If frame is ready, render camera preview image to the GL surface.
        backgroundRenderer.draw(frame);
    }
}
