package Statistics;

import java.util.HashMap;
import java.util.Map;

public class Statistics {
    protected Map<Long, StatisticClient> statisticClients = new HashMap<>();

    public void addNewStatisticClient(StatisticClient statisticClient) {
        statisticClients.put(statisticClient.getId(), statisticClient);
    }

    public void addEnteringTime(long clientId, double enteringTime) {
        StatisticClient statisticClient = statisticClients.get(clientId);
        statisticClient.addEnteringTime(enteringTime);
        statisticClients.put(clientId, statisticClient);
    }

    public void addClientResignation(long clientId) {
        StatisticClient statisticClient = statisticClients.get(clientId);
        statisticClient.resignFromQueue();
        statisticClients.put(clientId, statisticClient);
    }

    public long getNumberOfPeopleWhoResign() {
        return statisticClients.values().stream()
                .filter(statisticClient -> statisticClient.resigned())
                .count();
    }

    public double getAverageWaitingTime() {
        return statisticClients.values().stream()
                .filter(statisticClient -> statisticClient.enteredToRestaurant())
                .mapToDouble(statisticClient -> statisticClient.getWaitingTime())
                .average().orElse(0d);
    }

    public String getStatistics() {
        long resignedCount = getNumberOfPeopleWhoResign();
        double avgWaitingTime = getAverageWaitingTime();

        return "\n============================================================\n" +
                "==================== CURRENT STATISTICS ====================\n" +
                "============================================================\n" +
                "Number of people who resigned from waiting in queue = " + resignedCount + "\n" +
                "Average waiting time for free table in restaurant = " + avgWaitingTime + "\n" +
                "============================================================\n";
    }
}
