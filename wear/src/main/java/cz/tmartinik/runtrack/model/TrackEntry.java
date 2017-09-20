package cz.tmartinik.runtrack.model;

import android.location.Location;

/**
 * Created by tmartinik on 20.9.2017.
 */

class TrackEntry {
    private Location location;
    private Integer hr;

    public TrackEntry(Integer hr){
        this.hr = hr;
    }

    public TrackEntry(Location location) {
        this.location = location;
    }

    public TrackEntry(Location location, Integer hr) {
        this.location = location;
        this.hr = hr;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public double distance(TrackEntry entry) {
        if(this.location != null && entry.getLocation() != null){
            return (double)location.distanceTo(entry.getLocation());
        }
        return 0d;
    }
}
