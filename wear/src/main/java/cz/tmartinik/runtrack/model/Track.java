package cz.tmartinik.runtrack.model;

import android.util.Log;

import org.joda.time.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by tmartinik on 20.9.2017.
 */

public class Track {

    //TODO: Workout type
    List<TrackSegment> segments = new ArrayList<>();

    public List<TrackSegment> getSegments() {
        return segments;
    }

    public TrackSegment addSegment() {
        TrackSegment trackSegment = new TrackSegment();
        segments.add(trackSegment);
        return trackSegment;
    }

    public Duration getElapsedTime() {
        Duration result = new Duration(0);
        for(TrackSegment segment : segments){
            result = result.plus(segment.getDuration());
        }
        Log.d("Track", "" + result.getMillis());
        return result;
    }
}
