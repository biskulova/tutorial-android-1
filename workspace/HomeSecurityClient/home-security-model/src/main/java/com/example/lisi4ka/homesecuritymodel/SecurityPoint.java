package com.example.lisi4ka.homesecuritymodel;

import org.apache.thrift.TSerializer;

/**
 * Created by lisi4ka on 1/9/16.
 */
public class SecurityPoint {
    private String name;
    private boolean selected;

    //TODO: add type - locker, movement_sensor, volume_sensor, fire_sensor

    public SecurityPoint(String name, boolean isSelected) {
        this.name = name;
        selected = isSelected;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
