package cz.tmartinik.runtrack.model;

import android.location.Location;
import android.util.Log;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tmartinik on 20.9.2017.
 */

public class TrackSegment {
    private DateTime start;
    private DateTime end;
    private List<TrackEntry> data = new ArrayList<>();

    public DateTime start(){
        this.start = new DateTime();
        return this.start;
    }

    public DateTime stop(){
        this.end = new DateTime();
        return this.end;
    }

    public Double getDistance(){
        Double distance = 0d;
        TrackEntry previousLocation = null;
        for(TrackEntry entry : data){
            if(previousLocation != null){
                distance += previousLocation.distance(entry);
            }
            previousLocation = entry;
        }
        return distance;
    }

    public TrackEntry add(Location location, Integer hr) {
        TrackEntry e = new TrackEntry(location, hr);
        this.data.add(e);
        return e;
    }

    public Duration getDuration() {
        Duration result = null;
        if(end != null) {
            result = new Duration(start, end);
        }else{
            result = new Duration(start, new DateTime());
        }
        Log.d("TrackSegment", ""+result.getMillis());
        return result;
    }
}
