package org.team100.lib.spline;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.team100.lib.geometry.GeometryUtil;
import org.team100.lib.geometry.Pose2dWithMotion;
import org.team100.lib.path.Path100;
import org.team100.lib.timing.ScheduleGenerator;
import org.team100.lib.timing.TimingConstraint;
import org.team100.lib.trajectory.Trajectory100;
import org.team100.lib.util.Util;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;

class HolonomicSplineTest {
    private static final boolean DEBUG = false;
    private static final double kDelta = 0.001;

    @Test
    void testStationary() {
        HolonomicSpline s = new HolonomicSpline(
                new Pose2d(),
                new Pose2d(),
                new Rotation2d(),
                new Rotation2d());
        Translation2d t = s.getPoint(0);
        assertEquals(0, t.getX(), kDelta);
        t = s.getPoint(1);
        assertEquals(0, t.getX(), kDelta);
        Pose2dWithMotion p = s.getPose2dWithMotion(0);
        assertEquals(0, p.getPose().getX(), kDelta);
        assertEquals(0, p.getPose().getRotation().getRadians(), kDelta);
        assertEquals(0, p.getHeadingRate(), kDelta);
        p = s.getPose2dWithMotion(1);
        assertEquals(0, p.getPose().getX(), kDelta);
        assertEquals(0, p.getPose().getRotation().getRadians(), kDelta);
        assertEquals(0, p.getHeadingRate(), kDelta);
    }

    @Test
    void testLinear() {
        HolonomicSpline s = new HolonomicSpline(
                new Pose2d(),
                new Pose2d(1, 0, new Rotation2d()),
                new Rotation2d(),
                new Rotation2d());
        Translation2d t = s.getPoint(0);
        assertEquals(0, t.getX(), kDelta);
        t = s.getPoint(1);
        assertEquals(1, t.getX(), kDelta);
        Pose2dWithMotion p = s.getPose2dWithMotion(0);
        assertEquals(0, p.getPose().getX(), kDelta);
        assertEquals(0, p.getPose().getRotation().getRadians(), kDelta);
        assertEquals(0, p.getHeadingRate(), kDelta);
        p = s.getPose2dWithMotion(1);
        assertEquals(1, p.getPose().getX(), kDelta);
        assertEquals(0, p.getPose().getRotation().getRadians(), kDelta);
        assertEquals(0, p.getHeadingRate(), kDelta);
    }

    @Test
    void testBounds() {
        {
            // allow 0 degrees
            Pose2d p0 = new Pose2d(0, 0, GeometryUtil.kRotationZero);
            Pose2d p1 = new Pose2d(1, 0, GeometryUtil.kRotationZero);
            assertDoesNotThrow(() -> HolonomicSpline.checkBounds(p0, p1));
        }
        {
            // allow 90 degrees
            Pose2d p0 = new Pose2d(0, 0, new Rotation2d(Math.PI / 2));
            Pose2d p1 = new Pose2d(1, 0, GeometryUtil.kRotationZero);
            assertDoesNotThrow(() -> HolonomicSpline.checkBounds(p0, p1));
        }
        {
            // allow 135 degrees
            Pose2d p0 = new Pose2d(0, 0, new Rotation2d(3 * Math.PI / 4));
            Pose2d p1 = new Pose2d(1, 0, GeometryUtil.kRotationZero);
            assertDoesNotThrow(() -> HolonomicSpline.checkBounds(p0, p1));
        }
        {
            // disallow u-turn; these are never what you want.
            Pose2d p0 = new Pose2d(0, 0, new Rotation2d(Math.PI));
            Pose2d p1 = new Pose2d(1, 0, GeometryUtil.kRotationZero);
            assertDoesNotThrow(() -> HolonomicSpline.checkBounds(p0, p1));
        }
    }

    @Test
    void testLinear2() {
        HolonomicSpline s = new HolonomicSpline(
                new Pose2d(),
                new Pose2d(2, 0, new Rotation2d()),
                new Rotation2d(),
                new Rotation2d());
        Translation2d t = s.getPoint(0);
        assertEquals(0, t.getX(), kDelta);
        t = s.getPoint(1);
        assertEquals(2, t.getX(), kDelta);
        Pose2dWithMotion p = s.getPose2dWithMotion(0);
        assertEquals(0, p.getPose().getX(), kDelta);
        assertEquals(0, p.getPose().getRotation().getRadians(), kDelta);
        assertEquals(0, p.getHeadingRate(), kDelta);
        p = s.getPose2dWithMotion(1);
        assertEquals(2, p.getPose().getX(), kDelta);
        assertEquals(0, p.getPose().getRotation().getRadians(), kDelta);
        assertEquals(0, p.getHeadingRate(), kDelta);
    }

    @Test
    void testRotation() {
        // the heading rate observed here used to be 1.875 in the middle, and zero at
        // the ends, which is bad. now it's constant.

        HolonomicSpline s = new HolonomicSpline(
                new Pose2d(),
                new Pose2d(),
                new Rotation2d(),
                new Rotation2d(1));
        Translation2d t = s.getPoint(0);
        assertEquals(0, t.getX(), kDelta);
        t = s.getPoint(1);
        assertEquals(0, t.getX(), kDelta);
        Pose2dWithMotion p = s.getPose2dWithMotion(0);
        assertEquals(0, p.getPose().getX(), kDelta);
        assertEquals(0, p.getPose().getRotation().getRadians(), kDelta);
        // yay, heading rate is now not zero :-)
        assertEquals(1, p.getHeadingRate(), kDelta);
        p = s.getPose2dWithMotion(1);
        assertEquals(0, p.getPose().getX(), kDelta);
        assertEquals(1, p.getPose().getRotation().getRadians(), kDelta);
        // yay, heading rate is now not zero :-)
        assertEquals(1, p.getHeadingRate(), kDelta);
        p = s.getPose2dWithMotion(0.5);
        assertEquals(0, p.getPose().getX(), kDelta);
        assertEquals(0.5, p.getPose().getRotation().getRadians(), kDelta);
        // this used to be 1.875 in the middle
        assertEquals(1, p.getHeadingRate(), kDelta);
    }

    @Test
    void testCircle() {
        // four splines that make a circle should have nice even curvature and velocity
        // throughout. The circle is centered at zero, the heading always points there.
        //
        // the spline code currently does not behave correctly, it wants the theta
        // speed to be zero at each of the spline endpoints below.
        //
        // magic number of 1 makes something about 1.5% from a circle, close enough.
        double magicNumber = 1.0;
        HolonomicSpline s0 = new HolonomicSpline(
                new Pose2d(1, 0, Rotation2d.kCCW_90deg),
                new Pose2d(0, 1, Rotation2d.k180deg),
                Rotation2d.k180deg,
                Rotation2d.kCW_90deg,
                magicNumber);
        HolonomicSpline s1 = new HolonomicSpline(
                new Pose2d(0, 1, Rotation2d.k180deg),
                new Pose2d(-1, 0, Rotation2d.kCW_90deg),
                Rotation2d.kCW_90deg,
                Rotation2d.kZero,
                magicNumber);
        HolonomicSpline s2 = new HolonomicSpline(
                new Pose2d(-1, 0, Rotation2d.kCW_90deg),
                new Pose2d(0, -1, Rotation2d.kZero),
                Rotation2d.kZero,
                Rotation2d.kCCW_90deg,
                magicNumber);
        HolonomicSpline s3 = new HolonomicSpline(
                new Pose2d(0, -1, Rotation2d.kZero),
                new Pose2d(1, 0, Rotation2d.kCCW_90deg),
                Rotation2d.kCCW_90deg,
                Rotation2d.k180deg,
                magicNumber);
        List<HolonomicSpline> splines = new ArrayList<>();
        splines.add(s0);
        splines.add(s1);
        splines.add(s2);
        splines.add(s3);

        // what do the unoptimized splines look like?
        // for (int i = 0; i < 4; ++i) {
        // HolonomicSpline s = splines.get(i);
        // for (double j = 0; j < 0.99; j += 0.1) {
        // Pose2d p = s.getPose2d(j);
        // Util.printf("%.1f, %.2f, %.2f, %.2f\n",
        // i + j, p.getX(), p.getY(), p.getRotation().getRadians());
        // }
        // }

        // optimization does not help, the individual splines are already "optimal."

        SplineUtil.forceC1(splines);

        SplineUtil.optimizeSpline(splines);

        // spline joints are C1 smooth (i.e. same slope)
        assertTrue(SplineUtil.verifyC1(splines));
        // spline joints are C2 smooth (i.e. same curvature)
        assertTrue(SplineUtil.verifyC2(splines));
        // spline joints are C3 smooth (i.e. same rate of change of curvature)
        // assertTrue(SplineUtil.verifyC3(splines));

        for (int i = 0; i < 4; ++i) {
            HolonomicSpline s = splines.get(i);
            for (double j = 0; j < 0.99; j += 0.1) {
                Pose2d p = s.getPose2d(j);
                // the heading should point to the origin all the time, more or less.

                Rotation2d angleFromOrigin = p.getTranslation().unaryMinus().getAngle();
                Rotation2d error = angleFromOrigin.minus(p.getRotation());
                // there's about 2 degrees of error here because the spline is not quite a
                // circle.
                assertEquals(0, error.getRadians(), 0.04);
                if (DEBUG)
                    Util.printf("%.1f, %.2f, %.2f, %.2f\n",
                            i + j, p.getX(), p.getY(), p.getRotation().getRadians());
            }
        }
    }

    @Test
    void testMismatchedThetaDerivatives() {
        // if the theta derivative and each endpoint is the average delta,
        // what happens when adjacent segments have different deltas?
        // the optimizer needs to make them the same.
        double magicNumber = 1.0;
        HolonomicSpline s0 = new HolonomicSpline(
                new Pose2d(0, 0, Rotation2d.kZero),
                new Pose2d(1, 0, Rotation2d.kZero),
                Rotation2d.kZero,
                new Rotation2d(1), // turn a bit to the left
                magicNumber);
        HolonomicSpline s1 = new HolonomicSpline(
                new Pose2d(1, 0, Rotation2d.kZero),
                new Pose2d(2, 0, Rotation2d.kZero),
                new Rotation2d(1),
                Rotation2d.k180deg, // turn much more to the left
                magicNumber);
        List<HolonomicSpline> splines = new ArrayList<>();
        splines.add(s0);
        splines.add(s1);

        // for (int i = 0; i < 2; ++i) {
        // HolonomicSpline s = splines.get(i);
        // for (double j = 0; j < 0.99; j += 0.1) {
        // Pose2d p = s.getPose2d(j);
        // if (DEBUG)
        // Util.printf("%.1f, %.2f, %.2f, %.2f\n",
        // i + j, p.getX(), p.getY(), p.getRotation().getRadians());
        // }
        // }

        SplineUtil.forceC1(splines);

        SplineUtil.optimizeSpline(splines);

        // spline joints are C1 smooth (i.e. same slope)
        assertTrue(SplineUtil.verifyC1(splines));
        // spline joints are C2 smooth (i.e. same curvature)
        assertTrue(SplineUtil.verifyC2(splines));
        // spline joints are C3 smooth (i.e. same rate of change of curvature)
        // assertTrue(SplineUtil.verifyC3(splines));

        for (int i = 0; i < 2; ++i) {
            HolonomicSpline s = splines.get(i);
            for (double j = 0; j < 0.99; j += 0.1) {
                Pose2d p = s.getPose2d(j);
                if (DEBUG)
                    Util.printf("%.1f, %.2f, %.2f, %.2f\n",
                            i + j, p.getX(), p.getY(), p.getRotation().getRadians());
            }
        }
    }

    @Test
    void testMismatchedXYDerivatives() {
        // corners seem to be allowed, but yield un-traversable trajectories
        // with infinite accelerations at the corners, so they should be prohibited.
        double magicNumber = 1.0;
        // this goes straight ahead to (1,0)
        HolonomicSpline s0 = new HolonomicSpline(
                // derivatives point straight ahead
                new Pose2d(0, 0, Rotation2d.kZero),
                new Pose2d(1, 0, Rotation2d.kZero),
                Rotation2d.kZero,
                Rotation2d.kZero,
                magicNumber);
        // this is a sharp turn to the left
        HolonomicSpline s1 = new HolonomicSpline(
                // derivatives point to the left
                new Pose2d(1, 0, Rotation2d.kCCW_90deg),
                new Pose2d(1, 1, Rotation2d.kCCW_90deg),
                Rotation2d.kZero,
                Rotation2d.kZero,
                magicNumber);
        List<HolonomicSpline> splines = new ArrayList<>();
        splines.add(s0);
        splines.add(s1);

        // for (int i = 0; i < 2; ++i) {
        // HolonomicSpline s = splines.get(i);
        // for (double j = 0; j < 0.99; j += 0.1) {
        // Pose2d p = s.getPose2d(j);
        // if (DEBUG)
        // Util.printf("%.1f, %.2f, %.2f, %.2f\n",
        // i + j, p.getX(), p.getY(), p.getRotation().getRadians());
        // }
        // }

        SplineUtil.forceC1(splines);

        SplineUtil.optimizeSpline(splines);

        for (HolonomicSpline s : splines) {
            if (DEBUG)
                Util.printf("spline %s\n", s);
        }

        // spline joints are C1 smooth (i.e. same slope)
        assertTrue(SplineUtil.verifyC1(splines));
        // spline joints are C2 smooth (i.e. same curvature)
        assertTrue(SplineUtil.verifyC2(splines));
        // spline joints are C3 smooth (i.e. same rate of change of curvature)
        // assertTrue(SplineUtil.verifyC3(splines));

        for (int i = 0; i < 2; ++i) {
            HolonomicSpline s = splines.get(i);
            for (double j = 0; j < 0.99; j += 0.1) {
                Pose2d p = s.getPose2d(j);
                if (DEBUG)
                    Util.printf("%.1f, %.2f, %.2f, %.2f\n",
                            i + j, p.getX(), p.getY(), p.getRotation().getRadians());
            }
        }

        Path100 path = new Path100(SplineGenerator.parameterizeSplines(splines, 0.05, 0.05, 0.05));
        Util.printf("path %s\n", path);
        List<TimingConstraint> constraints = new ArrayList<>();
        ScheduleGenerator scheduleGenerator = new ScheduleGenerator(constraints);
        Trajectory100 trajectory = scheduleGenerator.timeParameterizeTrajectory(path,
                0.05, 0, 0);
        Util.printf("trajectory %s\n", trajectory);

    }

}
