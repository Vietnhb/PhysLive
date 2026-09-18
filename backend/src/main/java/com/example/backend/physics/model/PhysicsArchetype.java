package com.example.backend.physics.model;

import java.util.Locale;

public enum PhysicsArchetype {
    UNIFORM_MOTION("KINEMATICS"),
    CONSTANT_ACCELERATION("KINEMATICS"),
    PROJECTILE_MOTION("KINEMATICS"),
    HORIZONTAL_FORCE("DYNAMICS"),
    INCLINED_PLANE("DYNAMICS"),
    ELASTIC_COLLISION("DYNAMICS"),
    SPRING_OSCILLATION("DYNAMICS"),
    SERIES_CIRCUIT("CIRCUITS"),
    PARALLEL_CIRCUIT("CIRCUITS"),
    RC_CHARGING("CIRCUITS"),
    RC_DISCHARGING("CIRCUITS");

    private final String topicCode;

    PhysicsArchetype(String topicCode) {
        this.topicCode = topicCode;
    }

    public String topicCode() {
        return topicCode;
    }

    public boolean belongsTo(String topic) {
        return topic != null && topicCode.equals(topic.trim().toUpperCase(Locale.ROOT));
    }
}
