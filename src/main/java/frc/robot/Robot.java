package frc.robot;

import edu.wpi.cscore.UsbCamera;
import edu.wpi.cscore.VideoCamera;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.vision.VisionThread;
import edu.wpi.first.wpilibj.Preferences;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.command.Scheduler;
import edu.wpi.first.wpilibj.livewindow.LiveWindow;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import frc.robot.commands.turret.StopTurret;
import frc.robot.subsystems.Drivetrain;
import frc.robot.subsystems.Lifter;
import frc.robot.subsystems.Shooter;
import frc.robot.subsystems.Turret;
import frc.robot.vision.GripPipeline;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the IterativeRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the manifest file in the resource
 * directory.
 */
public class Robot extends TimedRobot {
    /**
     * Instance of {@link Drivetrain}. use this for {@code requires()}
     * statements and such.
     */
    public static Drivetrain driveTrain;
    /**
     * Instance of {@link Lifter}. use this for {@code requires()} statements
     * and such.
     */
    public static Lifter lifter;
    /**
     * Instance of {@link Shooter}. use this for {@code requires()} statements
     * and such.
     */
    public static Shooter shooter;
    public static Turret turret;
    /**
     * Instance of {@link OI}. No subsystem should require this. However, you
     * can read button values from it.
     */
    public static OI oi;

    public static Preferences prefs;

    // vision
    private static final int IMG_WIDTH = 320;
    private static final int IMG_HEIGHT = 240;

    private edu.wpi.first.vision.VisionThread visionThread;
    private double centerX = 0.0;
    @SuppressWarnings("unused")
    private ArrayList<MatOfPoint> filteredContours;
    private ArrayList<Double> averages;

    private final Object imgLock = new Object();

    Command autonomousCommand;
    SendableChooser<Command> chooser = new SendableChooser<>();

    /**
     * This function is run when the robot is first started up and should be
     * used for any initialization code.
     */
    @Override
    public void robotInit() {
        System.out.println("Test print please ignore");
        // init subsystems
        driveTrain = new Drivetrain();
        lifter = new Lifter();
        shooter = new Shooter();
        turret = new Turret();
        oi = new OI();
        prefs = Preferences.getInstance();
        // start vision thread
        UsbCamera camera = CameraServer.getInstance().startAutomaticCapture();
        camera.setResolution(getImgWidth(), IMG_HEIGHT);
        camera.setExposureManual(15);
        camera.setWhiteBalanceManual(VideoCamera.WhiteBalance.kFixedIndoor);
        visionThread = new VisionThread(camera, new GripPipeline(), pipeline -> {
            if (!pipeline.filterContoursOutput().isEmpty()) {
                MatOfPoint firstCont = pipeline.filterContoursOutput().get(0);
                MatOfPoint secondCont = pipeline.filterContoursOutput().get(1);
                double average_y_one = 0;
                for (Point p : firstCont.toList()) {
                    average_y_one += p.y;
                }
                double average_y_two = 0;
                for (Point p : secondCont.toList()) {
                    average_y_two += p.y;
                }
                // divide by number of points to give actual average
                average_y_two = average_y_two / secondCont.toList().size();
                average_y_one = average_y_one / firstCont.toList().size();
                Rect r = Imgproc.boundingRect(pipeline.findContoursOutput().get(0));
                synchronized (imgLock) {
                    centerX = r.x + (r.width / 2.0);
                    filteredContours = pipeline.filterContoursOutput();
                    // add averages to list
                    averages.add(average_y_one);
                    averages.add(average_y_two);
                }
            }
        });
        if (prefs.getBoolean("cam", false)) {
            visionThread.start();
        }
        chooser.setDefaultOption("Default Auto", new StopTurret());
        SmartDashboard.putData("Auto mode", chooser);
    }

    /**
     * This function is called once each time the robot enters Disabled mode.
     * You can use it to reset any subsystem information you want to clear when
     * the robot is disabled.
     */
    @Override
    public void disabledInit() {

    }

    @Override
    public void disabledPeriodic() {
        Scheduler.getInstance().run();
    }

    /**
     * This autonomous (along with the chooser code above) shows how to select
     * between different autonomous modes using the dashboard. The sendable
     * chooser code works with the Java SmartDashboard. If you prefer the
     * LabVIEW Dashboard, remove all of the chooser code and uncomment the
     * getString code to get the auto name from the text box below the Gyro
     * <p>
     * You can add additional auto modes by adding additional commands to the
     * chooser code above (like the commented example) or additional comparisons
     * to the switch structure below with additional strings &amp; commands.
     */
    @Override
    public void autonomousInit() {
        try {
            autonomousCommand = chooser.getSelected();
            autonomousCommand.start();
        } catch (NullPointerException e) {
            System.out.println("Caught NPE in auto init. Is there a button chooser on the SmartDashboard?");
            autonomousCommand = null;
        }
    }

    /**
     * This function is called periodically during autonomous
     */
    @Override
    public void autonomousPeriodic() {
        if (autonomousCommand != null) {
            Scheduler.getInstance().run();
        } else {
            double centerX;
            synchronized (imgLock) {
                centerX = this.centerX;
            }
            double turn = centerX - (getImgWidth() / 2.0);
            // drive with turn
            Robot.driveTrain.arcadeDrive(0.5, (turn * 0.005) * -1, true);
        }
    }

    @Override
    public void teleopInit() {
        // This makes sure that the autonomous stops running when
        // teleop starts running. If you want the autonomous to
        // continue until interrupted by another command, remove
        // this line or comment it out.
        if (autonomousCommand != null)
            autonomousCommand.cancel();
    }

    /**
     * This function is called periodically during operator control
     */
    @Override
    public void teleopPeriodic() {
        double centerX;
        synchronized (imgLock) {
            centerX = this.centerX;
        }
        turret.setCenterX(centerX);
        Scheduler.getInstance().run();
    }

    /**
     * This function is called periodically during test mode
     */
    @Override
    public void testPeriodic() {
        LiveWindow.run();
    }

    public double getCenterX() {
        return centerX;
    }

    public static int getImgWidth() {
        return IMG_WIDTH;
    }
}
