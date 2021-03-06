package com.spartronics4915.frc2020.commands;

import com.spartronics4915.frc2020.subsystems.Intake;

import edu.wpi.first.wpilibj2.command.FunctionalCommand;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.StartEndCommand;

public class IntakeCommands
{
    /**
     * Commands with simple logic statements should be implemented as a
     * {@link FunctionalCommand}. This saves the overhead of a full
     * {@link CommandBase}, but still allows us to deal with isFinished.
     * <p>
     * A FunctionalCommand takes five inputs:
     * @param Runnable onInit
     * @param Runnable onExecute
     * @param Consumer<Boolean> onEnd (boolean interrupted)
     * @param BooleanSupplier isFinished
     * @param Subsystem requirement For both the CommandScheduler and the above method references.
     * <p>
     * Each of these parameters corresponds with a method in the CommandBase class.
     */

    /**
     * This {@link FunctionalCommand} harvests balls by running {@link Intake}.intake continuously,
     * unless terminated by a second press of the Harvest button or
     * a positive reading from {@link Intake}.isBallHeld.
     */
    public class Harvest extends FunctionalCommand
    {
        public Harvest(Intake Intake)
        {
            super(() -> {}, Intake::intake, (Boolean b) -> Intake.stop(), Intake::isBallHeld, Intake);
        }
    }

    /**
     * An {@link InstantCommand} runs an action and immediately exits.
     * <p>
     * @param Runnable toRun A reference to a subsystem method
     * @param Subsystem requirement For both the CommandScheduler and the above method reference.
     */

    /**
     * This {@link InstantCommand} stops the intake by calling
     * {@link Intake}.stop once.
     * <p>
     * Note that the Intake only controls the front roller.
     */
    public class Stop extends InstantCommand
    {
        public Stop(Intake Intake)
        {
            super(Intake::stop, Intake);
        }
    }

    /**
     * A {@link StartEndCommand} allows us to specify an execute() and end()
     * condition, and runs until interrupted.
     *
     * @param Runnable onInit Runs over and over until interrupted
     * @param Runnable onEnd (boolean interrupted) Method to call once when ended
     * @param Subsystem requirement For both the CommandScheduler and the above method references.
     */

    /**
     * This {@link StartEndCommand} runs the intake motor backwards by calling
     * {@link Intake}.reverse repeatedly.
     * <p>
     * Note that this is not an Unjam command. The {@link Intake} subsystem only
     * controls the mechanical vector roller.
     */
    public class Eject extends StartEndCommand
    {
        public Eject(Intake Intake)
        {
            super(Intake::reverse, Intake::stop, Intake);
        }
    }
}
