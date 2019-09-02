package frc.robot.commands.drive;

import edu.wpi.first.wpilibj.command.Command;
import frc.robot.Robot;

/**
 *
 */
public class TurnLeft extends Command {

    public TurnLeft() {
        // Use requires() here to declare subsystem dependencies
        // eg. requires(chassis);
        requires(Robot.driveTrain);
    }

    // Called just before this Command runs the first time
    @Override
    protected void initialize() {
    }

    // Called repeatedly when this Command is scheduled to run
    @Override
    protected void execute() {
        Robot.driveTrain.arcadeDrive(0.5, 0.5, true);
    }

    // Make this return true when this Command no longer needs to run execute()
    @Override
    protected boolean isFinished() {
        return !Robot.oi.getXbox_b().get();
    }

    // Called once after isFinished returns true
    @Override
    protected void end() {
    }

    // Called when another command which requires one or more of the same
    // subsystems is scheduled to run
    @Override
    protected void interrupted() {
    }
}
