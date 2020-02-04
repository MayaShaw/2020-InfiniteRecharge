package com.spartronics4915.lib.hardware.motors;

import com.revrobotics.CANAnalog;
import com.revrobotics.CANEncoder;
import com.revrobotics.CANError;
import com.revrobotics.CANPIDController;
import com.revrobotics.CANSparkMax;
import com.revrobotics.ControlType;
import com.revrobotics.CANAnalog.AnalogMode;
import com.revrobotics.CANSparkMax.IdleMode;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;
import com.spartronics4915.lib.util.Logger;

import edu.wpi.first.wpilibj.RobotBase;

public class SpartronicsMax implements SpartronicsMotor
{

    private static final int kVelocitySlotIdx = 0;
    private static final int kPositionSlotIdx = 1;

    private final double kRPMtoRPS = 1 / 60;

    private final CANSparkMax mSparkMax;
    private final SpartronicsEncoder mEncoder;
    private final SensorModel mSensorModel;
    private final boolean mHadStartupError;

    private boolean mBrakeMode = false;
    /** Volts */
    private double mVoltageCompSaturation = 12.0;
    /** Native units/sec, converted to meters on get and set */
    private double mMotionProfileCruiseVelocity = 0.0;
    /** Native units/sec^2, converted to meters on get and set */
    private double mMotionProfileAcceleration = 0.0;
    private boolean mUseMotionProfileForPosition = false;

    private CANEncoder mEncoderSensor;
    private CANAnalog mAnalogSensor;
    private CANPIDController mPIDController;

    private AnalogMode mAnalogMode;

    private FeedbackSensorType mFeedbackSensor;


    public class SpartronicsMaxPWMEncoder implements SpartronicsEncoder
    {

        @Override
        public double getVelocity()
        {
            return mSensorModel.toCustomUnits(mEncoderSensor.getVelocity());
        }

        @Override
        public double getPosition()
        {
            return mSensorModel.toCustomUnits(mEncoderSensor.getPosition());
        }

        @Override
        public void setPhase(boolean isReversed)
        {
            mSparkMax.setInverted(isReversed);
        }

        @Override
        public boolean setPosition(double position) {
            mEncoderSensor.setPosition(position);
            return true;
        }
    }

    public class SpartronicsMaxAnalogEncoder implements SpartronicsEncoder
    {

        @Override
        public double getVelocity()
        {
            return mSensorModel.toCustomUnits(mAnalogSensor.getVelocity());
        }

        @Override
        public double getPosition()
        {
            return mSensorModel.toCustomUnits(mAnalogSensor.getPosition());
        }

        @Override
        public void setPhase(boolean isReversed)
        {
            mSparkMax.setInverted(isReversed);
        }

        @Override
        public boolean setPosition(double position) {
            return false;
        }
    }

    public static enum FeedbackSensorType {
        kPWM,
        kAnalogRelative,
        kAnalogAbsolute
    }

    public static SpartronicsMotor makeMotor(int deviceNumber, SensorModel sensorModel, FeedbackSensorType feedbackSensor)
    {
        if (RobotBase.isSimulation())
        {
            return new SpartronicsSimulatedMotor();
        }
        return new SpartronicsMax(new CANSparkMax(deviceNumber, MotorType.kBrushless), sensorModel, feedbackSensor);
    }

    public static SpartronicsMotor makeMotor(int deviceNumber, SensorModel sensorModel)
    {
        return makeMotor(deviceNumber, sensorModel, FeedbackSensorType.kPWM);
    }

    public static SpartronicsMotor makeMotor(int deviceNumber, SensorModel sensorModel,
            FeedbackSensorType feedbackSensor, int followerDeviceNumber)
    {
        if (RobotBase.isSimulation())
        {
            return new SpartronicsSimulatedMotor();
        }

        // We only use SPARK MAXes for brushless motors
        // If that changes we can make motor type configurable
        var master = new CANSparkMax(deviceNumber, MotorType.kBrushless);
        CANSparkMax follower = new CANSparkMax(deviceNumber, MotorType.kBrushless);
        follower.follow(master);
        follower.close(); // Gave a warning for a resource leak.
        return new SpartronicsMax(master, sensorModel, feedbackSensor);
    }

    public static SpartronicsMotor makeMotor(int deviceNumber, SensorModel sensorModel, int followerDeviceNumber)
    {
        return makeMotor(deviceNumber, sensorModel, FeedbackSensorType.kPWM, followerDeviceNumber);
    }

    private SpartronicsMax(CANSparkMax spark, SensorModel sensorModel, FeedbackSensorType feedbackSensor)
    {
        mSparkMax = spark;
        mSensorModel = sensorModel;
        mPIDController = mSparkMax.getPIDController();
        mFeedbackSensor = feedbackSensor;

        CANError err;
        switch (feedbackSensor) {
            case kPWM:
                mEncoderSensor = mSparkMax.getEncoder();
                err = mEncoderSensor.setVelocityConversionFactor(kRPMtoRPS); // Set conversion factor.
                mEncoder = new SpartronicsMaxPWMEncoder();
                mPIDController.setFeedbackDevice(mEncoderSensor);
                break;
            case kAnalogRelative:
                mAnalogMode = AnalogMode.kRelative;
                mAnalogSensor = mSparkMax.getAnalog(mAnalogMode);
                err = mAnalogSensor.setVelocityConversionFactor(kRPMtoRPS);
                mEncoder = new SpartronicsMaxAnalogEncoder();
                mPIDController.setFeedbackDevice(mAnalogSensor);
                break;
            case kAnalogAbsolute:
                mAnalogMode = AnalogMode.kAbsolute;
                mAnalogSensor = mSparkMax.getAnalog(mAnalogMode);
                err = mAnalogSensor.setVelocityConversionFactor(kRPMtoRPS);
                mEncoder = new SpartronicsMaxAnalogEncoder();
                mPIDController.setFeedbackDevice(mAnalogSensor);
                break;
            default:
                mEncoder = new SpartronicsMaxPWMEncoder();
                err = CANError.kError; // stops errors. Should never happen
        }
        if (err != CANError.kOk)
        {
            Logger.error("SparkMax on with ID " + mSparkMax.getDeviceId()
                    + " returned a non-OK error code on sensor configuration... Is the motor controller plugged in?");
            mHadStartupError = true;
        }
        else
        {
            mHadStartupError = false;
        }
        

        mSparkMax.enableVoltageCompensation(mVoltageCompSaturation);
    }

    @Override
    public SpartronicsEncoder getEncoder()
    {
        return mEncoder;
    }

    @Override
    public boolean hadStartupError()
    {
        return mHadStartupError;
    }

    @Override
    public double getVoltageOutput()
    {
        return mSparkMax.getBusVoltage() * mSparkMax.getAppliedOutput();
    }

    @Override
    public boolean getOutputInverted()
    {
        return mSparkMax.getInverted();
    }

    @Override
    public void setOutputInverted(boolean inverted)
    {
        mSparkMax.setInverted(inverted);
    }

    @Override
    public boolean getBrakeMode()
    {
        return mBrakeMode;
    }

    @Override
    public void setBrakeMode(boolean mode)
    {
        mBrakeMode = mode;
        mSparkMax.setIdleMode(mode ? IdleMode.kBrake : IdleMode.kCoast);
    }

    @Override
    public double getVoltageCompSaturation()
    {
        return mVoltageCompSaturation;
    }

    @Override
    public void setVoltageCompSaturation(double voltage)
    {
        mVoltageCompSaturation = voltage;
        mSparkMax.enableVoltageCompensation(mVoltageCompSaturation);
    }

    @Override
    public double getMotionProfileCruiseVelocity()
    {
        return mSensorModel.toCustomUnits(mMotionProfileCruiseVelocity);
    }

    @Override
    public void setMotionProfileCruiseVelocity(double velocityMetersPerSecond)
    { // Set to slot
        mMotionProfileCruiseVelocity = mSensorModel.toNativeUnits(velocityMetersPerSecond);
        mPIDController.setSmartMotionMaxVelocity((int) mMotionProfileCruiseVelocity,
                kVelocitySlotIdx);
    }

    @Override
    public double getMotionProfileMaxAcceleration()
    {
        return mSensorModel.toCustomUnits(mMotionProfileAcceleration);
    }

    @Override
    public void setMotionProfileMaxAcceleration(double accelerationMetersPerSecondSq)
    {
        mMotionProfileAcceleration = mSensorModel.toNativeUnits(accelerationMetersPerSecondSq);
        mPIDController.setSmartMotionMaxAccel((int) mMotionProfileAcceleration,
                kVelocitySlotIdx);
    }

    @Override
    public void setUseMotionProfileForPosition(boolean useMotionProfile)
    {
        mUseMotionProfileForPosition = useMotionProfile;
    }

    @Override
    public void setDutyCycle(double dutyCycle, double arbitraryFeedForwardVolts)
    {
        mPIDController.setReference(dutyCycle, ControlType.kDutyCycle, 0,
                arbitraryFeedForwardVolts);
    }

    @Override
    public void setDutyCycle(double dutyCycle)
    {
        setDutyCycle(dutyCycle, 0.0);
    }

    @Override
    public void setVelocity(double velocityMetersPerSecond, double arbitraryFeedForwardVolts)
    {
        double velocityNative = mSensorModel.toNativeUnits(velocityMetersPerSecond);
        mPIDController.setReference(velocityNative, ControlType.kVelocity,
                kVelocitySlotIdx, arbitraryFeedForwardVolts);
    }

    @Override
    public void setVelocityGains(double kP, double kD)
    {
        setVelocityGains(kP, 0, kD, 0);
    }

    @Override
    public void setVelocityGains(double kP, double kI, double kD, double kF)
    {
        mPIDController.setP(kP, kVelocitySlotIdx);
        mPIDController.setI(kI, kVelocitySlotIdx);
        mPIDController.setD(kD, kVelocitySlotIdx);
        mPIDController.setFF(kF, kVelocitySlotIdx);
    }

    @Override
    public void setPosition(double positionMeters)
    {
        double positionNativeUnits = mSensorModel.toNativeUnits(positionMeters);
        mPIDController.setReference(positionNativeUnits,
                mUseMotionProfileForPosition ? ControlType.kSmartMotion : ControlType.kPosition,
                kPositionSlotIdx);
    }

    @Override
    public void setPositionGains(double kP, double kD)
    {
        setPositionGains(kP, 0, kD, 0);
    }

    @Override
    public void setPositionGains(double kP, double kI, double kD, double kF)
    {
        mPIDController.setP(kP, kPositionSlotIdx);
        mPIDController.setI(kI, kPositionSlotIdx);
        mPIDController.setD(kD, kPositionSlotIdx);
        mPIDController.setFF(kF, kPositionSlotIdx);
    }

    @Override
    public SensorModel getSensorModel()
    {
        return mSensorModel;
    }

    @Override
    public void setVelocity(double velocityMetersPerSecond)
    {
        setVelocity(velocityMetersPerSecond, 0.0);
    }

    @Override
    public void setNeutral()
    {
        mPIDController.setReference(0.0, ControlType.kDutyCycle, 0);
    }

    public FeedbackSensorType getFeedbackSensor()
    {
        return mFeedbackSensor;
    }

    @Override
    public double getOutputCurrent()
    {
        return mSparkMax.getOutputCurrent();
    }

}