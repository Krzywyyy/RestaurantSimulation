package Statistics;

public class StatisticClient {
    private final long id;
    private final double appearanceTime;
    private double waitingTime;
    private boolean resigned;
    private boolean entered;

    public StatisticClient(long id, double appearanceTime) {
        this.id = id;
        this.appearanceTime = appearanceTime;
        this.waitingTime = 0d;
        this.resigned = false;
        this.entered = false;
    }

    public long getId() {
        return id;
    }

    void addEnteringTime(double enteringTime) {
        waitingTime = enteringTime - appearanceTime;
        entered = true;
    }

    void resignFromQueue() {
        resigned = true;
    }

    boolean enteredToRestaurant() {
        return entered;
    }

    boolean resigned() {
        return resigned;
    }

    double getWaitingTime() {
        return waitingTime;
    }
}
