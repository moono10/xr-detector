package com.smartfarm.core;

import com.google.ar.core.Frame;

public abstract class Component {

    public abstract void draw(float[] viewmtx, float[] projmtx, Frame frame);
}
