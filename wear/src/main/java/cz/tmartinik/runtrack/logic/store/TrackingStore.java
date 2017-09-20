package cz.tmartinik.runtrack.logic.store;

import org.joda.time.Duration;

import cz.tmartinik.runtrack.model.Track;

/**
 * Created by tmartinik on 20.9.2017.
 */

public interface TrackingStore {
    Track track();

    Track finish();

    Duration getElapsedTime();
}
