package frc.robot.commands.turret;

import edu.wpi.first.wpilibj.command.Command;
import frc.robot.DriveDirections;
import frc.robot.Robot;

public class StopTurret extends Command {

    public StopTurret() {
        // Use requires() here to declare subsystem dependencies
        // eg. requires(chassis);
        requires(Robot.turret);
    }

    // Called repeatedly when this Command is scheduled to run
    @Override
    protected void execute() {
        Robot.turret.turnTurret(DriveDirections.STOP);
    }

    // Make this return true when this Command no longer needs to run execute()
    @Override
    protected boolean isFinished() {
        return true;
    }
}
