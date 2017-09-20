package cz.tmartinik.runtrack.logic.store;

import org.joda.time.Duration;

import cz.tmartinik.runtrack.model.Track;

/**
 * Created by tmartinik on 20.9.2017.
 */

public class MemoryTrackingStore implements TrackingStore {

    private static MemoryTrackingStore instance;

    private Track track;

    public static TrackingStore getInstance() {
        if(instance == null){
            instance = new MemoryTrackingStore();
        }
        return instance;
    }

    @Override
    public Track track() {
        if(track == null){
            track = new Track();
        }
        return track;
    }

    @Override
    public Track finish() {
        Track result = track;
        track = null;
        return result;
    }

    @Override
    public Duration getElapsedTime() {
        return track.getElapsedTime();
    }
}
