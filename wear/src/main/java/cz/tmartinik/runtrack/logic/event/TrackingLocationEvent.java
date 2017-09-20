package cz.tmartinik.runtrack.logic.event;

import cz.tmartinik.runtrack.model.Tempo;

/**
 * Created by tmartinik on 19.9.2017.
 */

public class TrackingLocationEvent implements TrackingEvent {

    private Double distance;
    private Tempo tempo;

    public TrackingLocationEvent(Double distance, Tempo tempo) {
        this.distance = distance;
        this.tempo = tempo;
    }

    public Double getDistance() {
        return distance;
    }

    public Tempo getTempo() {
        return tempo;
    }
}
