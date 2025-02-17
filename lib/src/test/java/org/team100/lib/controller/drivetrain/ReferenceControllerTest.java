package org.team100.lib.controller.drivetrain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.team100.lib.geometry.GeometryUtil;
import org.team100.lib.logging.LoggerFactory;
import org.team100.lib.logging.TestLoggerFactory;
import org.team100.lib.logging.primitive.TestPrimitiveLogger;
import org.team100.lib.motion.drivetrain.Fixtured;
import org.team100.lib.motion.drivetrain.MockDrive;
import org.team100.lib.motion.drivetrain.SwerveDriveSubsystem;
import org.team100.lib.motion.drivetrain.SwerveModel;
import org.team100.lib.motion.drivetrain.kinodynamics.FieldRelativeVelocity;
import org.team100.lib.motion.drivetrain.kinodynamics.SwerveKinodynamics;
import org.team100.lib.motion.drivetrain.kinodynamics.SwerveKinodynamicsFactory;
import org.team100.lib.reference.TrajectoryReference;
import org.team100.lib.testing.Timeless;
import org.team100.lib.timing.TimingConstraint;
import org.team100.lib.timing.TimingConstraintFactory;
import org.team100.lib.trajectory.Trajectory100;
import org.team100.lib.trajectory.TrajectoryMaker;

import edu.wpi.first.math.geometry.Pose2d;

public class ReferenceControllerTest extends Fixtured implements Timeless {
    private static final double kDelta = 0.001;
    private static final LoggerFactory logger = new TestLoggerFactory(new TestPrimitiveLogger());
    SwerveKinodynamics swerveKinodynamics = SwerveKinodynamicsFactory.get();
    List<TimingConstraint> constraints = new TimingConstraintFactory(swerveKinodynamics).allGood();
    TrajectoryMaker maker = new TrajectoryMaker(constraints);

    @Test
    void testTrajectoryStart() {
        Trajectory100 t = maker.restToRest(
                new Pose2d(0, 0, GeometryUtil.kRotationZero),
                new Pose2d(1, 0, GeometryUtil.kRotationZero));
        // first state is motionless
        assertEquals(0, t.sample(0).velocityM_S(), kDelta);
        SwerveController controller = SwerveControllerFactory.test(logger);

        MockDrive drive = new MockDrive();
        // initially at rest
        drive.m_state = new SwerveModel();
        drive.m_aligned = false;

        ReferenceController c = new ReferenceController(
                drive,
                controller,
                new TrajectoryReference(t), false);

        // Initially unaligned so steer at rest
        stepTime();
        c.execute();
        assertEquals(0.098, drive.m_atRestSetpoint.x(), kDelta);
        assertEquals(0, drive.m_atRestSetpoint.y(), kDelta);
        assertEquals(0, drive.m_atRestSetpoint.theta(), kDelta);

        // we don't advance because we're still steering.
        // this next-setpoint is from "preview"
        // and our current setpoint is equal to the measurement.
        stepTime();
        c.execute();
        assertEquals(0.098, drive.m_atRestSetpoint.x(), kDelta);
        assertEquals(0, drive.m_atRestSetpoint.y(), kDelta);
        assertEquals(0, drive.m_atRestSetpoint.theta(), kDelta);

        drive.m_aligned = true;
        // now aligned, so we drive normally, using the same setpoint as above
        stepTime();
        c.execute();
        assertEquals(0.098, drive.m_setpoint.x(), kDelta);
        assertEquals(0, drive.m_setpoint.y(), kDelta);
        assertEquals(0, drive.m_setpoint.theta(), kDelta);

        // more normal driving
        stepTime();
        c.execute();
        assertEquals(0.199, drive.m_setpoint.x(), kDelta);
        assertEquals(0, drive.m_setpoint.y(), kDelta);
        assertEquals(0, drive.m_setpoint.theta(), kDelta);

        // etc
        stepTime();
        c.execute();
        assertEquals(0.306, drive.m_setpoint.x(), kDelta);
        assertEquals(0, drive.m_setpoint.y(), kDelta);
        assertEquals(0, drive.m_setpoint.theta(), kDelta);
    }

    @Test
    void testTrajectoryDone() {
        Trajectory100 t = maker.restToRest(
                new Pose2d(0, 0, GeometryUtil.kRotationZero),
                new Pose2d(1, 0, GeometryUtil.kRotationZero));
        // first state is motionless
        assertEquals(0, t.sample(0).velocityM_S(), kDelta);
        SwerveController controller = SwerveControllerFactory.test(logger);

        MockDrive drive = new MockDrive();
        // initially at rest
        drive.m_state = new SwerveModel();
        // for this test we don't care about steering alignment.
        drive.m_aligned = true;

        ReferenceController c = new ReferenceController(
                drive,
                controller,
                new TrajectoryReference(t),
                false);

        // the measurement never changes but that doesn't affect "done" as far as the
        // trajectory is concerned.
        for (int i = 0; i < 48; ++i) {
            stepTime();
            c.execute();
        }
        assertTrue(c.isDone());

    }

    /** Use a real drivetrain to observe the effect on the motors etc. */
    @Test
    void testRealDrive() {
        // 1m along +x, no rotation.
        Trajectory100 trajectory = maker.restToRest(
                new Pose2d(0, 0, GeometryUtil.kRotationZero),
                new Pose2d(1, 0, GeometryUtil.kRotationZero));
        // first state is motionless
        assertEquals(0, trajectory.sample(0).velocityM_S(), kDelta);
        SwerveController controller = SwerveControllerFactory.test(logger);

        SwerveDriveSubsystem drive = fixture.drive;

        // initially at rest
        assertEquals(0, fixture.collection.states().frontLeft().speedMetersPerSecond(), kDelta);
        assertEquals(0, fixture.collection.states().frontLeft().angle().get().getRadians(), kDelta);

        // initial state is wheels pointing +x
        assertTrue(drive.aligned(new FieldRelativeVelocity(1, 0, 0)));

        ReferenceController command = new ReferenceController(
                drive,
                controller,
                new TrajectoryReference(trajectory),
                false);
        stepTime();

        // command has not checked yet
        assertFalse(command.is_aligned());

        // here it notices that we're aligned and produces a +x output
        // so we never steer at rest
        command.execute();
        // but that output is not available until after takt.
        assertEquals(0, fixture.collection.states().frontLeft().speedMetersPerSecond(), kDelta);
        assertEquals(0, fixture.collection.states().frontLeft().angle().get().getRadians(), kDelta);

        // the side-effect is to set the "aligned" flag.
        assertTrue(command.is_aligned());
        // and we are actually aligned (as we have been the whole time)
        assertTrue(drive.aligned(new FieldRelativeVelocity(1, 0, 0)));

        // drive normally more
        stepTime();
        command.execute();
        // this is the output from the previous takt
        assertEquals(0.02, fixture.collection.states().frontLeft().speedMetersPerSecond(), kDelta);
        assertEquals(0, fixture.collection.states().frontLeft().angle().get().getRadians(), kDelta);

        // etc
        stepTime();
        command.execute();
        assertEquals(0.04, fixture.collection.states().frontLeft().speedMetersPerSecond(), kDelta);
        assertEquals(0, fixture.collection.states().frontLeft().angle().get().getRadians(), kDelta);
    }

    /** Use a real drivetrain to observe the effect on the motors etc. */
    @Test
    void testRealDriveUnaligned() {
        // 1m along +y, no rotation.
        Trajectory100 trajectory = maker.restToRest(
                new Pose2d(0, 0, GeometryUtil.kRotationZero),
                new Pose2d(0, 1, GeometryUtil.kRotationZero));
        // first state is motionless
        assertEquals(0, trajectory.sample(0).velocityM_S(), kDelta);
        SwerveController controller = SwerveControllerFactory.test(logger);

        SwerveDriveSubsystem drive = fixture.drive;

        // initially at rest
        assertEquals(0, fixture.collection.states().frontLeft().speedMetersPerSecond(), kDelta);
        assertEquals(0, fixture.collection.states().frontLeft().angle().get().getRadians(), kDelta);
        // d.m_state = new SwerveModel();
        // d.m_aligned = false;
        // initial state is wheels pointing +x
        assertTrue(drive.aligned(new FieldRelativeVelocity(1, 0, 0)));

        ReferenceController command = new ReferenceController(
                drive,
                controller,
                new TrajectoryReference(trajectory),
                false);
        // always start unaligned
        assertFalse(command.is_aligned());

        // steering at rest for awhile, for the wheels to turn 90 degrees.
        // this is 16 cycles of 20 ms which is 0.32 sec, a long time. the profile
        // here is acceleration-limited, 20pi/s^2, which is just what the limits say.
        // https://docs.google.com/spreadsheets/d/19N-rQBjlWM-fb_Hd5wJeKuihzoglmGVK7MT7RYU-9bY
        for (int i = 0; i < 17; ++i) {
            // Util.printf("************ %d **********", i);
            // command thinks we're not aligned
            assertFalse(command.is_aligned());
            // drive thinks we're not aligned to the target (0,1)
            assertFalse(drive.aligned(new FieldRelativeVelocity(0, 1, 0)));

            stepTime();
            fixture.drive.periodic();
            // this calls steerAtRest, using the previewed state.
            command.execute();
            // assertEquals(0,
            // fixture.collection.states().frontLeft().speedMetersPerSecond(), kDelta);
            // assertEquals(0,
            // fixture.collection.states().frontLeft().angle().get().getRadians(), kDelta);
            // Util.printf("angle %s\n",
            // fixture.collection.states().frontLeft().angle().get());
            // Util.printf("p %s\n", fixture.collection.turningPosition()[0].getAsDouble());
            // Util.printf("v %s\n", fixture.collection.turningVelocity()[0].getAsDouble());
        }
        // the last call to execute discovers that the modules are already at the goal,
        // so it actuates +y
        //
        // Util.println("aligned?");
        assertTrue(command.is_aligned());
        assertTrue(drive.aligned(new FieldRelativeVelocity(0, 1, 0)));
        //
        // the actuation is not visible until after takt.
        assertEquals(0, fixture.collection.states().frontLeft().speedMetersPerSecond(), kDelta);
        // This overshoots slightly due to the clock granularity
        assertEquals(1.573, fixture.collection.states().frontLeft().angle().get().getRadians(), kDelta);
        assertEquals(1.573, fixture.collection.turningPosition()[0].getAsDouble(), kDelta);
        assertEquals(0, fixture.collection.turningVelocity()[0].getAsDouble(), kDelta);

        // advance the clock, so we can see the previous cycle's output
        stepTime();
        assertEquals(0.02, fixture.collection.states().frontLeft().speedMetersPerSecond(), kDelta);
        assertEquals(1.572, fixture.collection.states().frontLeft().angle().get().getRadians(), kDelta);
        assertEquals(1.572, fixture.collection.turningPosition()[0].getAsDouble(), kDelta);
        assertEquals(-0.039, fixture.collection.turningVelocity()[0].getAsDouble(), kDelta);

        command.execute();

        // now the velocity measurement reflects the previous actuation
        stepTime();
        assertEquals(0.04, fixture.collection.states().frontLeft().speedMetersPerSecond(), kDelta);
        assertEquals(1.572, fixture.collection.states().frontLeft().angle().get().getRadians(), kDelta);
        assertEquals(1.572, fixture.collection.turningPosition()[0].getAsDouble(), kDelta);
        assertEquals(-0.031, fixture.collection.turningVelocity()[0].getAsDouble(), kDelta);

        command.execute();

        // now the velocity measurement reflects the previous actuation
        stepTime();
        assertEquals(0.06, fixture.collection.states().frontLeft().speedMetersPerSecond(), kDelta);
        assertEquals(1.572, fixture.collection.states().frontLeft().angle().get().getRadians(), kDelta);
        assertEquals(1.572, fixture.collection.turningPosition()[0].getAsDouble(), kDelta);
        assertEquals(-0.017, fixture.collection.turningVelocity()[0].getAsDouble(), kDelta);

    }

}
