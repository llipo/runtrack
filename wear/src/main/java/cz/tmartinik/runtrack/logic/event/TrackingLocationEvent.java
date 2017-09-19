package cz.tmartinik.runtrack.logic.event;

import android.location.Location;

/**
 * Created by tmartinik on 19.9.2017.
 */

public class TrackingLocationEvent implements TrackingEvent {
    private Location location;

    public TrackingLocationEvent(Location location) {
        this.location = location;
    }

    public Location getLocation() {

        return location;
    }
}
