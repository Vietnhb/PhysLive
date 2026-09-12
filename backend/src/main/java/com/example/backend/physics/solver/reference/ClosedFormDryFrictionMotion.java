package com.example.backend.physics.solver.reference;

import java.math.BigDecimal;

final class ClosedFormDryFrictionMotion {

    private ClosedFormDryFrictionMotion() {
    }

    static Outcome solve(
            BigDecimal initialPosition,
            BigDecimal initialVelocity,
            BigDecimal duration,
            BigDecimal mass,
            BigDecimal driveForce,
            BigDecimal frictionLimit) {
        BigDecimal position = initialPosition;
        BigDecimal velocity = initialVelocity;
        BigDecimal remaining = duration;

        if (velocity.compareTo(BigDecimal.ZERO) != 0) {
            BigDecimal acceleration = accelerationWhileMoving(velocity, mass, driveForce, frictionLimit);
            BigDecimal stopTime = stopTime(velocity, acceleration);
            if (stopTime == null || stopTime.compareTo(remaining) > 0) {
                return integrate(position, velocity, remaining, acceleration, frictionLimit);
            }
            Outcome untilStop = integrate(position, velocity, stopTime, acceleration, frictionLimit);
            position = untilStop.position();
            velocity = BigDecimal.ZERO;
            remaining = remaining.subtract(stopTime, ReferenceSupport.MATH_CONTEXT);
        }

        if (driveForce.abs().compareTo(frictionLimit) <= 0) {
            return new Outcome(position, BigDecimal.ZERO, BigDecimal.ZERO, driveForce.abs());
        }

        int direction = driveForce.signum();
        BigDecimal netForce = driveForce.subtract(
                frictionLimit.multiply(BigDecimal.valueOf(direction), ReferenceSupport.MATH_CONTEXT),
                ReferenceSupport.MATH_CONTEXT);
        BigDecimal acceleration = netForce.divide(mass, ReferenceSupport.MATH_CONTEXT);
        return integrate(position, velocity, remaining, acceleration, frictionLimit);
    }

    private static BigDecimal accelerationWhileMoving(
            BigDecimal velocity,
            BigDecimal mass,
            BigDecimal driveForce,
            BigDecimal frictionLimit) {
        BigDecimal signedFriction = frictionLimit.multiply(
                BigDecimal.valueOf(velocity.signum()), ReferenceSupport.MATH_CONTEXT);
        return driveForce.subtract(signedFriction, ReferenceSupport.MATH_CONTEXT)
                .divide(mass, ReferenceSupport.MATH_CONTEXT);
    }

    private static BigDecimal stopTime(BigDecimal velocity, BigDecimal acceleration) {
        if (acceleration.compareTo(BigDecimal.ZERO) == 0
                || velocity.signum() == acceleration.signum()) {
            return null;
        }
        BigDecimal candidate = velocity.negate().divide(acceleration, ReferenceSupport.MATH_CONTEXT);
        return candidate.compareTo(BigDecimal.ZERO) > 0 ? candidate : null;
    }

    private static Outcome integrate(
            BigDecimal position,
            BigDecimal velocity,
            BigDecimal duration,
            BigDecimal acceleration,
            BigDecimal frictionMagnitude) {
        BigDecimal finalVelocity = velocity.add(
                acceleration.multiply(duration, ReferenceSupport.MATH_CONTEXT),
                ReferenceSupport.MATH_CONTEXT);
        BigDecimal displacement = velocity.multiply(duration, ReferenceSupport.MATH_CONTEXT)
                .add(acceleration.multiply(duration.pow(2), ReferenceSupport.MATH_CONTEXT)
                        .divide(ReferenceSupport.TWO, ReferenceSupport.MATH_CONTEXT),
                        ReferenceSupport.MATH_CONTEXT);
        return new Outcome(
                position.add(displacement, ReferenceSupport.MATH_CONTEXT),
                finalVelocity,
                acceleration,
                frictionMagnitude);
    }

    record Outcome(
            BigDecimal position,
            BigDecimal velocity,
            BigDecimal acceleration,
            BigDecimal frictionMagnitude) {
    }
}
