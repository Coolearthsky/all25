package org.team100.lib.timing;

import java.util.List;

import org.team100.lib.motion.drivetrain.kinodynamics.SwerveKinodynamics;

public class TimingConstraintFactory {
    private final SwerveKinodynamics m_limits;

    public TimingConstraintFactory(SwerveKinodynamics limits) {
        m_limits = limits;
    }

    /**
     * Return wheel speed, yaw rate, and centripetal acceleration constraints with
     * reasonable scale to prevent too-fast spinning and to slow down in sharp
     * curves. The velocity is set to half of maximum.
     * 
     * If you want to adjust these scales, make a new factory method while you
     * figure out what you need, don't just change them here.
     */
    public List<TimingConstraint> allGood() {
        return scaled(0.5, 0.5, 0.2, 0.2);
    }

    /** Absolute maximum. Probably too fast to actually use. */
    public List<TimingConstraint> fast() {
        return scaled(1.0, 1.0, 1.0, 0.25);
    }

    /** Very slow, 25% speed. */
    public List<TimingConstraint> slow() {
        return scaled(0.25, 0.25, 0.25, 0.25);
    }

    /** Maybe unrealistically fast? */
    public List<TimingConstraint> medium() {
        return scaled(0.75, 1, 0.75, 0.25);
    }

    /** Maybe unrealistically fast? */
    public List<TimingConstraint> auto() {
        return scaled(0.77, 0.6, 0.75, 0.5);
    }

    /** see TrajectoryVelocityProfileTest.testAuto() */
    public List<TimingConstraint> testAuto() {
        return scaled(1, 1, 1, 1);
    }

    /**
     * Use absolute max as the constraints. Shouldn't be used on a real robot.
     */
    public List<TimingConstraint> forTest() {
        return scaled(1.0, 1.0, 1.0, 1.0);
    }

    private List<TimingConstraint> scaled(
            double vScale,
            double aScale,
            double centripetalScale,
            double yawRateScale) {
        return List.of(
                new ConstantConstraint(vScale, aScale, m_limits),
                new SwerveDriveDynamicsConstraint(m_limits, vScale, aScale),
                new YawRateConstraint(m_limits, yawRateScale),
                new CapsizeAccelerationConstraint(m_limits, centripetalScale));
    }

}
