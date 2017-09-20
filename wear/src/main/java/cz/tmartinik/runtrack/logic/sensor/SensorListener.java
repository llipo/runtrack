package cz.tmartinik.runtrack.logic.sensor;

/**
 * Created by tmartinik on 18.9.2017.
 */

public interface SensorListener<T extends SensorEvent> {

    public void onSensorEvent(T event);
}
