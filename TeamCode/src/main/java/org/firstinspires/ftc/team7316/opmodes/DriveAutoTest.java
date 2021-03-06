package org.firstinspires.ftc.team7316.opmodes;

import android.util.Log;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.team7316.commands.CameraUntilCheddar;
import org.firstinspires.ftc.team7316.commands.DriveDistance;
import org.firstinspires.ftc.team7316.util.Constants;
import org.firstinspires.ftc.team7316.util.GyroAngles;
import org.firstinspires.ftc.team7316.util.Hardware;
import org.firstinspires.ftc.team7316.util.Scheduler;
import org.firstinspires.ftc.team7316.util.commands.AutoCodes;
import org.firstinspires.ftc.team7316.util.modes.AutoBaseOpMode;
import org.firstinspires.ftc.team7316.util.subsystems.Subsystems;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import ftc.vision.ImageProcess;

@Autonomous(name="Auto Long")
public class DriveAutoTest extends AutoBaseOpMode {

    double runningAverage1 = 0;
    double runningAverage2 = 0;
    int count = 0;
    ElapsedTime timer = new ElapsedTime();
    ElapsedTime timer2 = new ElapsedTime();

    long ticksLeft = 0;
    long ticksRight = 0;
    double power = 1.0;
    double dp = 0.1;

    @Override
    public void onInit() {
//        Scheduler.instance.add(new TurnGyroSimple(90));
//        Scheduler.instance.add(AutoCodes.blueDoubleCheddarSequence());
//        Hardware.instance.gyroWrapper.resetHeading(Hardware.instance.gyroWrapper.angles().yaw);
//        timer.reset();
//        timer2.reset();
//
//        runningAverage1 = 0;
//        runningAverage2 = 0;
//
//        ticksLeft = Hardware.instance.leftmotor.getCurrentPosition();
//        ticksRight = Hardware.instance.rightmotor.getCurrentPosition();
        Subsystems.instance.driveSubsystem.driveMotorSet(-1,-1);
    }

    @Override
    public void onLoop() {
        testDriveF();
    }

    private void testTurnF() {
        dp = -0.1;

        GyroAngles angles = Hardware.instance.gyroWrapper.angles();
        Hardware.instance.gyroWrapper.resetHeading(angles.yaw);

        runningAverage1 += Math.abs(angles.heading) / timer.seconds();
        timer.reset();
        count++;

        Hardware.instance.rightmotor.setPower(-power);
        Hardware.instance.leftmotor.setPower(power);
        Hardware.instance.centermotor.setPower(-power);

        Hardware.log("power", power);

        if (timer2.seconds() > 5) {
            Log.d("used power", String.valueOf(power));
            Log.d("real speed", String.valueOf(runningAverage1 / count));
            runningAverage1 = 0;
            count = 0;
            power += dp;
            timer2.reset();
        }
    }

    private void testDriveF() {
        dp = -0.1;

        runningAverage1 += Math.abs(Hardware.instance.leftmotor.getCurrentPosition() - ticksLeft) / timer.seconds();
        ticksLeft = Hardware.instance.leftmotor.getCurrentPosition();
        runningAverage2 += Math.abs(Hardware.instance.rightmotor.getCurrentPosition() - ticksRight) / timer.seconds();
        ticksRight = Hardware.instance.rightmotor.getCurrentPosition();
        timer.reset();
        count++;

        Hardware.instance.rightmotor.setPower(-power);
        Hardware.instance.leftmotor.setPower(-power);

        Hardware.log("right power",  power);
        Hardware.log("left power", power);

        Hardware.log("left ticks", Hardware.instance.leftmotor.getCurrentPosition());
        Hardware.log("right ticks", Hardware.instance.rightmotor.getCurrentPosition());

        if (timer2.seconds() > 3) {
            Log.d("used power left", String.valueOf(power));
            Log.d("real speed left", String.valueOf(runningAverage1 / count));
            Log.d("used power right", String.valueOf( power));
            Log.d("real speed right", String.valueOf(runningAverage2 / count));
            runningAverage1 = 0;
            runningAverage2 = 0;
            count = 0;
            power += dp;
            timer2.reset();
        }
    }

    private void testCamera() {
        double x = 0;

        if (CameraUntilCheddar.contour != null) {
            Moments M = Imgproc.moments(CameraUntilCheddar.contour);
            x = Math.floor(M.m10 /  M.m00);
            ImageProcess.stop();
        }

        Hardware.log("cheddar x", x);
    }

}
