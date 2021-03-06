package com.spartronics4915.frc2020.commands;

import com.spartronics4915.frc2020.Constants;
import com.spartronics4915.frc2020.RobotContainer;
import com.spartronics4915.frc2020.subsystems.Climber;

import edu.wpi.first.wpilibj2.command.FunctionalCommand;
import edu.wpi.first.wpilibj2.command.StartEndCommand;

public class ClimberCommands
{
    /**
     * A {@link StartEndCommand} allows us to specify an execute() and end()
     * condition, and runs until interrupted.
     *
     * @param Runnable onInit Runs over and over until interrupted
     * @param Runnable onEnd (boolean interrupted) Method to call once when ended
     * @param Subsystem requirement For both the CommandScheduler and the above method references.
     */

    /**
     * This {@link StartEndCommand} extends the "lightsaber" while held down.
     * <p>
     * It does so by calling {@link Climber}.extend until stopped.
     * There is no end condition aside from direct termination.
     * <p>
     * Note that the "while held down" functionality is defined in {@link RobotContainer}.
     */
    public class Extend extends StartEndCommand
    {
        public Extend(Climber Climber)
        {
            super(Climber::extend, Climber::stop, Climber);
        }
    }

    /**
     * This {@link StartEndCommand} extends the "lightsaber" while held down.
     * <p>
     * It does so by calling {@link Climber}.retract until stopped.
     * There is no end condition aside from direct termination.
     * <p>
     * Note that the "while held down" functionality is defined in {@link RobotContainer}.
     */
    public class Retract extends StartEndCommand
    {
        public Retract(Climber Climber)
        {
            super(Climber::retract, Climber::stop, Climber);
        }
    }

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
     * The {@link FunctionalCommand} WinchPrimary is part of a two-step winching Command chain.
     * <p>
     * This primary functionality activates first, winching the rope by turning the motor a direction
     * (! {@link Constants}.Climber.kStalled) until it detects a stall ({@link Climber.isStalled}),
     * in which case {@link WinchSecondary} follows.
     * <p>
     * A switch controls this Command, and will always run it first. In the event the Climber is
     * already winched in this motor direction, it will quickly detect the stall and go to
     * {@link WinchSecondary}.
     */
    public class WinchPrimary extends FunctionalCommand
    {
        public WinchPrimary(Climber Climber)
        {
            super(() -> {}, () -> Climber.winch(!Constants.Climber.kStalled),
                (Boolean b) -> Climber.stop(), Climber::isStalled, Climber);
        }
    }

    /**
     * The {@link FunctionalCommand} WinchSecondary is part of a two-step winching Command chain.
     * <p>
     * This secondary functionality activates after {@link WinchPrimary}, winching the rope by turning
     * the motor the opposite direction as {@link WinchPrimary} ({@link Constants}.Climber.kStalled).
     * <p>
     * A switch controls this Command, and will always run it after {@link WinchPrimary}, even if
     * {@link WinchPrimary} is already winched enough in that direction.
     */
    public class WinchSecondary extends StartEndCommand
    {
        public WinchSecondary(Climber Climber)
        {
            super(() -> Climber.winch(Constants.Climber.kStalled), Climber::stop, Climber);
        }
    }
}
