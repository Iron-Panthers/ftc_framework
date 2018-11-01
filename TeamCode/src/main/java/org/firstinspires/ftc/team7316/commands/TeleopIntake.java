package org.firstinspires.ftc.team7316.commands;

import org.firstinspires.ftc.team7316.util.commands.Command;
import org.firstinspires.ftc.team7316.util.input.OI;
import org.firstinspires.ftc.team7316.util.subsystems.Subsystem;
import org.firstinspires.ftc.team7316.util.subsystems.Subsystems;

public class TeleopIntake extends Command {

    @Override
    public void init() {
        requires(Subsystems.instance.intakeSubsystem);
    }

    @Override
    public void loop() {
        Subsystems.instance.intakeSubsystem.setMotor(OI.instance.gp2.left_stick.getY());
    }

    @Override
    public boolean shouldRemove() {
        return false;
    }

    @Override
    protected void end() {

    }
}
