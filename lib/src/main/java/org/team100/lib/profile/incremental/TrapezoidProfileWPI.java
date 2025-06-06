package org.team100.lib.profile.incremental;

import org.team100.lib.state.Control100;
import org.team100.lib.state.Model100;

import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.math.trajectory.TrapezoidProfile.Constraints;
import edu.wpi.first.math.trajectory.TrapezoidProfile.State;

/**
 * Wrap the WPI profile.
 * 
 * note: both the wpi and 100 profiles fail to produce useful feedforward when
 * the distance is reachable in one time step, i.e. high accel and velocity
 * limits.
 */
public class TrapezoidProfileWPI implements Profile100 {
    private final Constraints m_constraints;
    private final TrapezoidProfile m_profile;
    private final double m_maxVel;

    public TrapezoidProfileWPI(double maxVel, double maxAccel) {
        m_constraints = new Constraints(maxVel, maxAccel);
        m_profile = new TrapezoidProfile(m_constraints);
        m_maxVel = maxVel;
    }

    @Override
    public Control100 calculate(double dt, Control100 initial, Model100 goal) {
        State result = m_profile.calculate(dt, new State(initial.x(), initial.v()), new State(goal.x(), goal.v()));
        // WPI State doesn't have accel, so we calculate it.
        double accel = (result.velocity - initial.v()) / dt;
        return new Control100(result.position, result.velocity, accel);
    }

    @Override
    public ResultWithETA calculateWithETA(double dt, Control100 initial, Model100 goal) {
        Control100 result100 = calculate(dt, initial, goal);
        double eta = m_profile.totalTime();
        return new ResultWithETA(result100, eta);
    }

    @Override
    public TrapezoidProfileWPI scale(double s) {
        return new TrapezoidProfileWPI(m_constraints.maxVelocity, s * m_constraints.maxAcceleration);
    }

    @Override
    public double getMaxVelocity() {
        return m_maxVel;
    }

    @Override
    public double solve(
            double dt,
            Control100 i,
            Model100 g,
            double eta,
            double etaTolerance) {
        return Profile100.solveForSlowerETA(
                dt,
                i,
                g,
                eta,
                etaTolerance,
                (s) -> new TrapezoidProfileWPI(
                        m_constraints.maxVelocity,
                        s * m_constraints.maxAcceleration));
    }
}
