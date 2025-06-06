package org.team100.lib.commands.drivetrain;

import org.team100.lib.controller.drivetrain.ReferenceController;
import org.team100.lib.controller.drivetrain.SwerveController;
import org.team100.lib.dashboard.Glassy;
import org.team100.lib.motion.drivetrain.DriveSubsystemInterface;
import org.team100.lib.reference.TrajectoryReference;
import org.team100.lib.trajectory.Trajectory100;
import org.team100.lib.visualization.TrajectoryVisualization;

import edu.wpi.first.wpilibj2.command.Command;

/**
 * Follow a single trajectory.
 * 
 * The starting point is fixed, obviously, so this command can
 * only be used from that point. It's kinda just for testing.
 */
public class DriveWithTrajectory extends Command implements Glassy {
    private final DriveSubsystemInterface m_drive;
    private final SwerveController m_controller;
    private final Trajectory100 m_trajectory;
    private final TrajectoryVisualization m_viz;

    private ReferenceController m_referenceController;

    public DriveWithTrajectory(
            DriveSubsystemInterface drive,
            SwerveController controller,
            Trajectory100 trajectory,
            TrajectoryVisualization viz) {
        m_drive = drive;
        m_controller = controller;
        m_trajectory = trajectory;
        m_viz = viz;
        addRequirements(m_drive);
    }

    @Override
    public void initialize() {
        m_referenceController = new ReferenceController(
                m_drive,
                m_controller,
                new TrajectoryReference(m_trajectory),
                false);
        m_viz.setViz(m_trajectory);
        m_drive.resetLimiter();
    }

    @Override
    public void execute() {
        m_referenceController.execute();
    }

    @Override
    public boolean isFinished() {
        return m_referenceController.isFinished();
    }

    boolean isDone() {
        return m_referenceController.isDone();
    }

    @Override
    public void end(boolean interrupted) {
        m_drive.stop();
        m_viz.clear();
    }
}
