package Queue;

public class QueueClient {
    private final long clientId;
    private final double impatienceTime;

    public QueueClient(long clientId, double impatienceTime) {
        this.clientId = clientId;
        this.impatienceTime = impatienceTime;
    }

    public long getClientId() {
        return clientId;
    }

    public double getImpatienceTime() {
        return impatienceTime;
    }
}
