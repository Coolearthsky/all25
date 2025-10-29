package org.team100.lib.examples.tank;

import org.team100.lib.config.Identity;
import org.team100.lib.logging.LoggerFactory;
import org.team100.lib.motion.mechanism.LinearMechanismFactory;
import org.team100.lib.motor.MotorPhase;
import org.team100.lib.util.CanId;

public class TankDriveFactory {

    public static TankDrive make(
            LoggerFactory fieldLogger,
            LoggerFactory parent,
            int supplyLimit,
            CanId left,
            CanId right,
            double gearRatio,
            double wheelDiaM) {
        return switch (Identity.instance) {
            case BLANK -> sim(
                    fieldLogger,
                    parent,
                    gearRatio,
                    wheelDiaM);
            default -> realRobot(
                    fieldLogger,
                    parent,
                    supplyLimit,
                    left,
                    right,
                    gearRatio,
                    wheelDiaM);
        };
    }

    private static TankDrive realRobot(
            LoggerFactory fieldLogger,
            LoggerFactory parent,
            int supplyLimit,
            CanId left,
            CanId right,
            double gearRatio,
            double wheelDiaM) {
        LoggerFactory log = parent.name("Tank Drive");
        return new TankDrive(
                fieldLogger,
                LinearMechanismFactory.neo(
                        log.name("left"),
                        supplyLimit,
                        left,
                        MotorPhase.REVERSE,
                        gearRatio,
                        wheelDiaM),
                LinearMechanismFactory.neo(
                        log.name("right"),
                        supplyLimit,
                        right,
                        MotorPhase.FORWARD,
                        gearRatio,
                        wheelDiaM));
    }

    private static TankDrive sim(
            LoggerFactory fieldLogger,
            LoggerFactory parent,
            double gearRatio,
            double wheelDiaM) {
        LoggerFactory log = parent.name("Tank Drive");
        return new TankDrive(
                fieldLogger,
                LinearMechanismFactory.sim(
                        log.name("left"),
                        gearRatio,
                        wheelDiaM),
                LinearMechanismFactory.sim(
                        log.name("right"),
                        gearRatio,
                        wheelDiaM));
    }
}
