package com.smartfarm.common.rendering.geometry;

import com.smartfarm.core.Color4;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

public class LineString {

    private List<Vector3> pointList = new ArrayList<Vector3>();
    private Color4 color4;
    public java.nio.FloatBuffer getPoints() {
        FloatBuffer buf = FloatBuffer.allocate (pointList.size() * 4);

        for (Vector3 v : pointList) {
            buf.put(v.getX());
            buf.put(v.getY());
            buf.put(v.getZ());
            buf.put(0.0f);
        }
        buf.rewind();

        return buf;
    }

    public List<Vector3> getPointList() {
        return pointList;
    }

    public void setPointList(List<Vector3> pointList) {
        this.pointList = pointList;
    }

    public Color4 getColor4() {
        return color4;
    }

    public void setColor4(Color4 color4) {
        this.color4 = color4;
    }
}
