package cz.tmartinik.runtrack.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.TextView;

import java.util.Optional;

import butterknife.BindView;
import butterknife.ButterKnife;
import cz.tmartinik.runtrack.R;
import cz.tmartinik.runtrack.TrackingService;
import cz.tmartinik.runtrack.logic.event.TrackingHrEvent;
import cz.tmartinik.runtrack.logic.event.TrackingLocationEvent;

import static cz.tmartinik.runtrack.ui.Format.decimal;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link TrackingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TrackingFragment extends TrackingServiceFragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    @BindView(R.id.hr)
    TextView mHrView;
    @BindView(R.id.clock)
    Chronometer mClockView;
    @BindView(R.id.distance)
    TextView mDistanceView;
    @BindView(R.id.tempo)
    TextView mTempoView;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public TrackingFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment TrackingFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static TrackingFragment newInstance(String param1, String param2) {
        TrackingFragment fragment = new TrackingFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_tracking, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        register(TrackingHrEvent.class, event -> {
            mHrView.setText(event.getHr() + " Bmp");
        });
        register(TrackingLocationEvent.class, event -> {
            mDistanceView.setText(decimal(event.getDistance() / 1000));
            Optional.ofNullable(event.getTempo()).
                    ifPresent(t -> {
                                mTempoView.setText(decimal(t.getMinKm()));
                            }
                    );
        });
    }

    @Override
    protected void onServiceConnected(TrackingService service) {
        mClockView.setBase(SystemClock.elapsedRealtime() - service.getElapsedTime().getMillis());
        mClockView.start();
    }

    @Override
    public int getMenuResource() {
        return R.menu.tracking;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_tracking_pause:
                getService().stopTracking();
                break;
        }
        return true;
    }
}
