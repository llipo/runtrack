package cz.tmartinik.runtrack.logic.event;

/**
 * Created by tmartinik on 19.9.2017.
 */

public class TrackingHrEvent implements TrackingEvent {
    private int hr;

    public TrackingHrEvent(int hr) {
        this.hr = hr;
    }

    public int getHr() {

        return hr;
    }
}
