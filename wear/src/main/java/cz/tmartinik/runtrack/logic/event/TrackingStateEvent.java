package cz.tmartinik.runtrack.logic.event;

/**
 * Created by tmartinik on 19.9.2017.
 */

public class TrackingStateEvent {

    private Action action;

    public TrackingStateEvent(Action action) {
        this.action = action;
    }

    public Action getAction() {
        return action;
    }

    public static enum Action{
        START,
        STOP;
    }
}
