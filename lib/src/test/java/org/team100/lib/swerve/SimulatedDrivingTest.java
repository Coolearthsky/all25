package org.team100.lib.swerve;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.team100.lib.geometry.GeometryUtil;
import org.team100.lib.localization.SwerveDrivePoseEstimator100;
import org.team100.lib.localization.VisionData;
import org.team100.lib.logging.LoggerFactory;
import org.team100.lib.logging.TestLoggerFactory;
import org.team100.lib.logging.primitive.TestPrimitiveLogger;
import org.team100.lib.motion.drivetrain.SwerveDriveSubsystem;
import org.team100.lib.motion.drivetrain.SwerveLocal;
import org.team100.lib.motion.drivetrain.kinodynamics.FieldRelativeVelocity;
import org.team100.lib.motion.drivetrain.kinodynamics.SwerveKinodynamics;
import org.team100.lib.motion.drivetrain.kinodynamics.SwerveKinodynamicsFactory;
import org.team100.lib.motion.drivetrain.kinodynamics.SwerveModuleDeltas;
import org.team100.lib.motion.drivetrain.kinodynamics.SwerveModulePosition100;
import org.team100.lib.motion.drivetrain.kinodynamics.SwerveModulePositions;
import org.team100.lib.motion.drivetrain.kinodynamics.SwerveModuleStates;
import org.team100.lib.motion.drivetrain.module.SimulatedSwerveModule100;
import org.team100.lib.motion.drivetrain.module.SwerveModuleCollection;
import org.team100.lib.sensors.Gyro;
import org.team100.lib.sensors.SimulatedGyro;
import org.team100.lib.testing.Timeless;
import org.team100.lib.util.DriveUtil;
import org.team100.lib.util.Takt;
import org.team100.lib.util.Util;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Twist2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;

public class SimulatedDrivingTest implements Timeless {
    LoggerFactory fieldLogger = new TestLoggerFactory(new TestPrimitiveLogger());
    LoggerFactory logger = new TestLoggerFactory(new TestPrimitiveLogger());
    SwerveKinodynamics swerveKinodynamics = SwerveKinodynamicsFactory.forRealisticTest();
    SwerveModuleCollection collection = new SwerveModuleCollection(
            SimulatedSwerveModule100.withInstantaneousSteering(logger, swerveKinodynamics),
            SimulatedSwerveModule100.withInstantaneousSteering(logger, swerveKinodynamics),
            SimulatedSwerveModule100.withInstantaneousSteering(logger, swerveKinodynamics),
            SimulatedSwerveModule100.withInstantaneousSteering(logger, swerveKinodynamics));

    Gyro gyro = new SimulatedGyro(swerveKinodynamics, collection);
    AsymSwerveSetpointGenerator setpointGenerator = new AsymSwerveSetpointGenerator(
            logger,
            swerveKinodynamics,
            () -> 12);
    SwerveDrivePoseEstimator100 poseEstimator = swerveKinodynamics.newPoseEstimator(
            logger,
            gyro,
            collection.positions(),
            GeometryUtil.kPoseZero,
            0);
    SwerveLocal swerveLocal = new SwerveLocal(logger, swerveKinodynamics, setpointGenerator, collection);
    VisionData visionData = new VisionData() {
        @Override
        public void update() {
        }
    };
    SwerveDriveSubsystem drive = new SwerveDriveSubsystem(
            fieldLogger,
            logger,
            gyro,
            poseEstimator,
            swerveLocal,
            visionData);

    @Test
    void testSteps() {
        FieldRelativeVelocity input = new FieldRelativeVelocity(2, 0, 3.5);
        Rotation2d theta = new Rotation2d();
        ChassisSpeeds targetChassisSpeeds = SwerveKinodynamics.toInstantaneousChassisSpeeds(input, theta);
        SwerveModuleStates states = swerveKinodynamics.toSwerveModuleStates(targetChassisSpeeds);

        // mmmm the angles start as zero? does this matter? no?
        SwerveModulePositions startPositions = new SwerveModulePositions(
                new SwerveModulePosition100(),
                new SwerveModulePosition100(),
                new SwerveModulePosition100(),
                new SwerveModulePosition100());

        // say each module proceeds at its setpoint speed and angle (i.e. starting angle
        // is irrelevant)

        double dt = 0.02;
        SwerveModulePositions endPositions = new SwerveModulePositions(
                new SwerveModulePosition100(
                        states.frontLeft().speedMetersPerSecond() * dt,
                        states.frontLeft().angle()),
                new SwerveModulePosition100(
                        states.frontRight().speedMetersPerSecond() * dt,
                        states.frontRight().angle()),
                new SwerveModulePosition100(
                        states.rearLeft().speedMetersPerSecond() * dt,
                        states.rearLeft().angle()),
                new SwerveModulePosition100(
                        states.rearRight().speedMetersPerSecond() * dt,
                        states.rearRight().angle()));

        SwerveModuleDeltas modulePositionDelta = DriveUtil.modulePositionDelta(
                startPositions,
                endPositions);
        Util.printf("%s\n", modulePositionDelta);

        Twist2d twist = swerveKinodynamics.getKinematics().toTwist2d(modulePositionDelta);

        Pose2d deltaPose = GeometryUtil.sexp(twist);
        ChassisSpeeds continuousSpeeds = new ChassisSpeeds(
                deltaPose.getX(),
                deltaPose.getY(),
                deltaPose.getRotation().getRadians()).div(dt);

        // to pass, this requires the "veering correction" to be zero.
        assertEquals(0, continuousSpeeds.vyMetersPerSecond, 1e-12);
    }

    @Test
    void testStraight() {
        // just +x
        FieldRelativeVelocity input = new FieldRelativeVelocity(2, 0, 0);
        double start = Takt.get();
        for (int i = 0; i < 100; ++i) {
            stepTime();
            drive.driveInFieldCoords(input);
            Util.printf("%.2f %s\n", Takt.get() - start, drive.getPose());
        }
    }

    @Test
    void testStraightVerbatim() {
        // just +x
        // this accelerates infinitely, immediately to the requested speed.
        FieldRelativeVelocity input = new FieldRelativeVelocity(2, 0, 0);
        double start = Takt.get();
        for (int i = 0; i < 100; ++i) {
            stepTime();
            drive.driveInFieldCoordsVerbatim(input);
            Util.printf("%.2f %s\n", Takt.get() - start, drive.getPose());
        }
    }

    /** The simulation veers, which is weird, it should be perfect. */
    @Test
    void testVeering() {
        // +x and spinning
        FieldRelativeVelocity input = new FieldRelativeVelocity(2, 0, 3.5);
        for (int i = 0; i < 5; ++i) {
            stepTime();
            drive.driveInFieldCoords(input);
            Util.printf("takt: %.2f pose: %s\n", Takt.get(), drive.getPose());
        }
    }

    /** This seems like the simplest case: no setpoint generator here. */
    @Test
    void testVeeringVerbatim() {
        collection.reset();
        // +x and spinning
        FieldRelativeVelocity input = new FieldRelativeVelocity(2, 0, 3.5);
        for (int i = 0; i < 5; ++i) {
            Util.printf("step time ...\n");
            stepTime();
            drive.driveInFieldCoordsVerbatim(input);
            Util.printf("takt: %.2f pose: %s\n", Takt.get(), drive.getPose());
        }
    }

    /** Is the gyro in sync with the estimated pose? Yes. */
    @Test
    void testGyro() {
        // spin fast
        FieldRelativeVelocity input = new FieldRelativeVelocity(0, 0, 4);
        Util.printf("pose %s, gyro %s, rate %f\n",
                drive.getPose(),
                gyro.getYawNWU(),
                gyro.getYawRateNWU());
        drive.driveInFieldCoordsVerbatim(input);
        Util.printf("pose %s, gyro %s, rate %f\n",
                drive.getPose(),
                gyro.getYawNWU(),
                gyro.getYawRateNWU());
        stepTime();
        Util.printf("pose %s, gyro %s, rate %f\n",
                drive.getPose(),
                gyro.getYawNWU(),
                gyro.getYawRateNWU());
        drive.driveInFieldCoordsVerbatim(input);
        Util.printf("pose %s, gyro %s, rate %f\n",
                drive.getPose(),
                gyro.getYawNWU(),
                gyro.getYawRateNWU());
        stepTime();
        Util.printf("pose %s, gyro %s, rate %f\n",
                drive.getPose(),
                gyro.getYawNWU(),
                gyro.getYawRateNWU());

    }
}
