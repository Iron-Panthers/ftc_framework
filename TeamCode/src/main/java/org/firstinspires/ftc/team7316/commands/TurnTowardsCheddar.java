package org.firstinspires.ftc.team7316.commands;

import org.firstinspires.ftc.team7316.util.Constants;
import org.firstinspires.ftc.team7316.util.Hardware;
import org.firstinspires.ftc.team7316.util.commands.Command;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

public class TurnTowardsCheddar extends Command {

    double pixelPos = 0;
    public static double ANGLE_TURNED = 0;
    int angleWanted = 0;

    TurnGyroSimple turn;

    @Override
    public void init() {
        Moments M = Imgproc.moments(CameraUntilCheddar.contour);
        pixelPos = M.m01 / M.m00;
        angleWanted = (int) (pixelPos * Constants.PIXELS_TO_DEGREES);
        turn = new TurnGyroSimple(angleWanted);

        turn.init();
    }

    @Override
    public void loop() {
        turn.loop();
    }

    @Override
    public boolean shouldRemove() {
        return turn.shouldRemove();
    }

    @Override
    protected void end() {
        ANGLE_TURNED += Hardware.instance.gyroWrapper.angles().heading;
        turn.end();
    }
}