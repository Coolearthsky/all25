package org.team100.lib.motion.mechanism;

import org.team100.lib.config.Feedforward100;
import org.team100.lib.config.PIDConstants;
import org.team100.lib.encoder.CANSparkEncoder;
import org.team100.lib.encoder.SimulatedBareEncoder;
import org.team100.lib.logging.LoggerFactory;
import org.team100.lib.motor.BareMotor;
import org.team100.lib.motor.MotorPhase;
import org.team100.lib.motor.Neo550CANSparkMotor;
import org.team100.lib.motor.NeoCANSparkMotor;
import org.team100.lib.motor.NeutralMode;
import org.team100.lib.motor.SimulatedBareMotor;
import org.team100.lib.util.CanId;

/**
 * Factory for common mechanisms.
 */
public class LinearMechanismFactory {

        /** REV NEO motor with built-in encoder. */
        public static LinearMechanism neo(
                        LoggerFactory log,
                        int supplyLimit,
                        CanId id,
                        MotorPhase phase,
                        double gearRatio,
                        double wheelDiaM) {
                NeoCANSparkMotor left = new NeoCANSparkMotor(
                                log, id, NeutralMode.BRAKE,
                                phase, supplyLimit,
                                Feedforward100.makeNeo(),
                                PIDConstants.makeVelocityPID(0));
                CANSparkEncoder leftEncoder = new CANSparkEncoder(
                                log, left);
                LinearMechanism leftMech = new LinearMechanism(
                                log, left, leftEncoder,
                                gearRatio, wheelDiaM,
                                Double.NEGATIVE_INFINITY,
                                Double.POSITIVE_INFINITY);
                return leftMech;
        }

        /** REV NEO 550 motor with built-in encoder. */
        public static LinearMechanism neo550(
                        LoggerFactory log,
                        int supplyLimit,
                        CanId id,
                        MotorPhase phase,
                        double gearRatio,
                        double wheelDiaM) {
                Neo550CANSparkMotor left = new Neo550CANSparkMotor(
                                log, id, NeutralMode.BRAKE,
                                phase, supplyLimit,
                                Feedforward100.makeNeo550(),
                                PIDConstants.makeVelocityPID(0.1));
                CANSparkEncoder leftEncoder = new CANSparkEncoder(log, left);
                LinearMechanism leftMech = new LinearMechanism(
                                log, left, leftEncoder,
                                gearRatio, wheelDiaM,
                                Double.NEGATIVE_INFINITY,
                                Double.POSITIVE_INFINITY);
                return leftMech;
        }

        /** Simulation. */
        public static LinearMechanism sim(
                        LoggerFactory log,
                        double gearRatio,
                        double wheelDiaM) {
                BareMotor motor = new SimulatedBareMotor(log, 600);
                SimulatedBareEncoder encoder = new SimulatedBareEncoder(
                                log, motor);
                return new LinearMechanism(
                                log, motor, encoder,
                                gearRatio, wheelDiaM,
                                Double.NEGATIVE_INFINITY,
                                Double.POSITIVE_INFINITY);
        }
}
