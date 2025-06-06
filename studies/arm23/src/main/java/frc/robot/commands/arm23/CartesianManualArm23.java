package org.team100.lib.commands.arm23;

import java.util.Optional;
import java.util.function.DoubleSupplier;

import org.team100.lib.motion.arm23.ArmAngles23;
import org.team100.lib.motion.arm23.ArmKinematics23;
import org.team100.lib.motion.arm23.ArmSubsystem23;

import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj2.command.Command;

/**
 * Manual arm control in cartesian coordinates.
 * 
 * There are no safety features like soft limits or current limits or anything,
 * this is direct manual control using inverse kinematics.
 */
public class CartesianManualArm23 extends Command {
    private static final double kMaxDutyCycle = 0.5;

    private final ArmSubsystem23 m_arm;
    private final ArmKinematics23 m_kinematics;
    private final DoubleSupplier m_dx;
    private final DoubleSupplier m_dy;

    public CartesianManualArm23(
            ArmSubsystem23 arm,
            ArmKinematics23 kinematics,
            DoubleSupplier dx,
            DoubleSupplier dy) {
        m_arm = arm;
        m_kinematics = kinematics;
        m_dx = dx;
        m_dy = dy;
        addRequirements(arm);
    }

    /** Use inverse kinematics to transform the manual input into joint space. */
    @Override
    public void execute() {
        Translation2d cartesianVelocity = new Translation2d(m_dx.getAsDouble(), m_dy.getAsDouble());
        Optional<ArmAngles23> position = m_arm.getPosition();
        if (position.isEmpty())
            return;
        ArmAngles23 jointVelocity = m_kinematics.inverseVel(position.get(), cartesianVelocity);
        m_arm.set(kMaxDutyCycle * jointVelocity.th1(), kMaxDutyCycle * jointVelocity.th2());
    }

    @Override
    public void end(boolean interrupted) {
        m_arm.set(0, 0);
    }
}
