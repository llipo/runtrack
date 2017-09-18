package cz.tmartinik.runtrack.logic.sensor;

/**
 * Created by tmartinik on 18.9.2017.
 */

public class HrSensorEvent implements SensorEvent {
    private final int heartRate;
    private final Type type;

    public HrSensorEvent(int heartRate) {
        this.type = Type.DATA;
        this.heartRate = heartRate;
    }

    public int getHeartRate() {
        return heartRate;
    }

    public Type getType() {
        return type;
    }

    public static enum Type{
        DATA;
    }
}
