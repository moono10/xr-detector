package com.smartfarm.core.components;

import android.content.Context;

import com.google.ar.core.Frame;
import com.smartfarm.common.rendering.LineRenderer;
import com.smartfarm.common.rendering.geometry.LineString;
import com.smartfarm.core.Component;

public class LineRendererComponent extends Component {

    private final LineRenderer xAxislineRenderer = new LineRenderer();

    public LineRendererComponent(Context context, LineString lsx)  {
        try {
            xAxislineRenderer.createOnGlThread(context);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        xAxislineRenderer.update(lsx);
    }

    @Override
    public void draw(float[] viewmtx, float[] projmtx, Frame frame) {
        xAxislineRenderer.draw(viewmtx, projmtx);
    }
}
