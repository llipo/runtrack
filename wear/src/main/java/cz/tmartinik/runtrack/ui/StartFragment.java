package cz.tmartinik.runtrack.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import butterknife.ButterKnife;
import butterknife.OnClick;
import cz.tmartinik.runtrack.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link StartFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StartFragment extends TrackingServiceFragment {

    public static final String TAG = StartFragment.class.getSimpleName();

    public StartFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment StartFragment.
     */
    public static StartFragment newInstance() {
        StartFragment fragment = new StartFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View inflate = inflater.inflate(R.layout.fragment_start, container, false);
        ButterKnife.bind(this, inflate);
        return inflate;
    }

    @OnClick(R.id.btn_start)
    public void onButtonPressed() {
        //TODO: Handle settings - HR monitor, activity type, etc
        getService().startTracking();
    }

    @Override
    public int getMenuResource() {
        return R.menu.start;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_start_run:
                //TODO: Set type to start
                break;
        }
        return true;
    }
}
