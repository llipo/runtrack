package cz.tmartinik.runtrack.logic.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventCallback;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * Created by tmartinik on 18.9.2017.
 */

public class InternalHrSensorProvider implements HrSensorProvider {

    private static final String TAG = InternalHrSensorProvider.class.getSimpleName();
    private Sensor mHeartRateSensor;
    private SensorManager mSensorManager;
    private SensorEventCallback mSensorListener;

    @Override
    public void register(Context context, SensorListener<HrSensorEvent> listener) {
        mSensorManager = ((SensorManager) context.getSystemService(Context.SENSOR_SERVICE));
        mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

        mSensorListener = new SensorEventCallback() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
                    int heartRate = (int) event.values[0];
                    Log.d(TAG, "HR:" + heartRate);
                    //Notify listener
                    listener.onSensorEvent(new HrSensorEvent(heartRate));
                } else
                    Log.d(TAG, "Unknown sensor type");
            }
        };
        mSensorManager.registerListener(mSensorListener, mHeartRateSensor, mSensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    public void unregister(){
        mSensorManager.unregisterListener(mSensorListener);
    }
}
