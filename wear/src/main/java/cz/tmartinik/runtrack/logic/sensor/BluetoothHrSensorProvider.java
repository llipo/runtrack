package cz.tmartinik.runtrack.logic.sensor;

import android.content.Context;
import android.util.Log;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.helpers.ValueInterpreter;

import java.util.UUID;

import cz.tmartinik.runtrack.SampleGattAttributes;
import rx.Subscription;

/**
 * Created by tmartinik on 18.9.2017.
 */

public class BluetoothHrSensorProvider implements HrSensorProvider {

    private static final String TAG = BluetoothHrSensorProvider.class.getSimpleName();
    private final String address;
    private boolean mConnected;
    private Subscription mSubscription;

    public BluetoothHrSensorProvider(final String address) {
        this.address = address;
    }

    @Override
    public void register(Context context, SensorListener<HrSensorEvent> listener) {
        if (!mConnected) {
            RxBleClient rxBleClient = RxBleClient.create(context);
            RxBleDevice mDevice = rxBleClient.getBleDevice(address);
            UUID characteristicUuid = UUID.fromString(SampleGattAttributes.HEART_RATE_MEASUREMENT);

            //                    .doOnCompleted(()-> mConnected = false;)
// Notification has been set up
// <-- Notification has been set up, now observe value changes.
// Given characteristic has been changes, here is the value.
// Handle an error here.
            mSubscription = mDevice.establishConnection(false)
//                    .doOnCompleted(()-> mConnected = false;)
                    .flatMap(
                            rxBleConnection -> {
                                if (rxBleConnection != null) {
                                    mConnected = true;
                                    return rxBleConnection.setupNotification(characteristicUuid);
                                }
                                return null;
                            })
                    .doOnNext(notificationObservable -> {
                        // Notification has been set up
                        Log.d("BLE", "Notification registered");
                    })
                    .flatMap(notificationObservable -> notificationObservable) // <-- Notification has been set up, now observe value changes.
                    .subscribe(
                            bytes -> {
                                // Given characteristic has been changes, here is the value.
                                int heartRate = readData(bytes);
                                Log.d(TAG, "Heart rate " + heartRate);
                                listener.onSensorEvent(new HrSensorEvent(heartRate));
                            },
                            throwable -> {
                                // Handle an error here.
                                Log.d("BLE", "Read error", throwable);
                            }
                    );
        }
    }


    private int readData(byte[] bytes) {
        int heartRate = 0;
        int flag = bytes[0];
        int format = -1;
        if ((flag & 0x01) != 0) {
            format = ValueInterpreter.FORMAT_UINT16;
        } else {
            format = ValueInterpreter.FORMAT_UINT8;
        }
        heartRate = ValueInterpreter.getIntValue(bytes, format, 1);
        return heartRate;
    }

    @Override
    public void unregister() {
        mSubscription.unsubscribe();
    }
}
