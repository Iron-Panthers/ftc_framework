package org.firstinspires.ftc.team7316.commands;

import org.firstinspires.ftc.team7316.util.commands.Command;

public class UnturnCheddar extends Command {

    TurnGyroSimple turn;

    @Override
    public void init() {
        turn = new TurnGyroSimple((int)TurnTowardsCheddar.ANGLE_TURNED - 45);
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
        turn.end();
    }
}
