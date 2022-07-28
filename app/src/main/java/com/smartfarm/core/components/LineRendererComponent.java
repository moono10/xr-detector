package com.smartfarm.core.components;

import android.content.Context;

import com.smartfarm.common.rendering.LineRenderer;
import com.smartfarm.common.rendering.geometry.LineString;
import com.smartfarm.common.rendering.geometry.Vector3;
import com.smartfarm.core.Color4;
import com.smartfarm.core.Component;

import java.io.IOException;

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
    public void draw(float[] viewmtx, float[] projmtx) {
        xAxislineRenderer.draw(viewmtx, projmtx);
    }
}
