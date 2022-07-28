package com.smartfarm.core;

import java.util.ArrayList;
import java.util.List;

public class Node {

    private List<Component> components = new ArrayList<>();

    public void addComponent(Component c) {
        components.add(c);
    }

    public List<Component> getComponents() {
        return components;
    }

    public void setComponents(List<Component> components) {
        this.components = components;
    }
}
