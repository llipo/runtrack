package cz.tmartinik.runtrack;

import android.Manifest;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.widget.TextView;

import com.tbruyelle.rxpermissions.RxPermissions;

import butterknife.BindView;
import butterknife.ButterKnife;
import cz.tmartinik.runtrack.logic.sensor.HrSensorEvent;
import cz.tmartinik.runtrack.logic.sensor.HrSensorProvider;
import cz.tmartinik.runtrack.logic.sensor.InternalHrSensorProvider;
import cz.tmartinik.runtrack.logic.sensor.SensorListener;
import cz.tmartinik.runtrack.ui.HeartRateView;
import rx.functions.Action1;

public class HeartRateActivity extends WearableActivity {

    private static final String TAG = HeartRateActivity.class.getSimpleName();
    private HrSensorProvider mHrProvider;

    @BindView(R.id.hr_hr)
    TextView hrView;

    @BindView(R.id.hrCircle)
    HeartRateView hrCircleView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hr);
        setAmbientEnabled();

        ButterKnife.bind(this);
        requestPermissions();


    }

    private void requestPermissions() {
        RxPermissions rxPermissions = new RxPermissions(HeartRateActivity.this);
        rxPermissions
                .request(Manifest.permission.BODY_SENSORS)
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean granted) {
                        // Bind to the service. If the service is in foreground mode, this signals to the service
                        // that since this activity is in the foreground, the service can exit foreground mode.
                        mHrProvider = new InternalHrSensorProvider();
                        mHrProvider.register(HeartRateActivity.this, new SensorListener<HrSensorEvent>() {
                            @Override
                            public void onSensorEvent(HrSensorEvent event) {
                                int heartRate = event.getHeartRate();
                                Log.d("HR", ""+ heartRate);
                                hrView.setText(Integer.toString(heartRate));
                                int hrRes = (int)(((float)(heartRate - 46) / (193f - 46f)) *100);
                                hrCircleView.setHrRes(hrRes);
                                if(hrRes < 60) {
                                    hrCircleView.setColorPrimary(getColor(R.color.green));
                                }else if(hrRes < 70){
                                        hrCircleView.setColorPrimary(getColor(R.color.green));
                                }else if(hrRes < 80){
                                    hrCircleView.setColorPrimary(getColor(R.color.yellow));
                                }else if(hrRes < 90){
                                    hrCircleView.setColorPrimary(getColor(R.color.orange));
                                }else {
                                    hrCircleView.setColorPrimary(getColor(R.color.red));
                                }
                            }
                        });
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        requestPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHrProvider.unregister();
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {

    }

}
