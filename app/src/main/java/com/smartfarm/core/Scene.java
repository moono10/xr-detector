package com.smartfarm.core;

import com.google.ar.core.Camera;

import java.util.ArrayList;
import java.util.List;

public class Scene {

    private List<Node> nodes = new ArrayList<Node>();


    float[] projmtx = new float[16];
    float[] viewmtx = new float[16];

    public List<Node> getNodes() {
        return nodes;
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }

    public void draw(Camera camera) {
        camera.getProjectionMatrix(projmtx, 0, 0.001f, 100.0f);
        camera.getViewMatrix(viewmtx, 0);

        for (Node node : nodes) {
            for (Component component : node.getComponents()) {
                component.draw(viewmtx, projmtx);
            }
        }

    }

}
