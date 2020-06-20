package Queue;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

public class RestaurantQueue {
    private final Queue<QueueClient> restaurantQueue = new LinkedList<QueueClient>();

    public boolean isEmpty() {
        return restaurantQueue.isEmpty();
    }

    public void addClient(QueueClient queueClient) {
        this.restaurantQueue.add(queueClient);
    }

    public QueueClient getFirstClient() {
        return restaurantQueue.isEmpty() ? null :
                restaurantQueue.remove();
    }

    public List<QueueClient> getImpatientClients(double simulationTime) {
        return restaurantQueue.stream()
                .filter(queueClient -> queueClient.getImpatienceTime() <= simulationTime)
                .collect(Collectors.toList());
    }

    public void removeImpatientClient(QueueClient impatientClient) {
        restaurantQueue.remove(impatientClient);
    }
}
