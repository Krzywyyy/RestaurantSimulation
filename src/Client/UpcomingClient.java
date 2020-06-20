package Client;

import java.util.concurrent.ThreadLocalRandom;

public class UpcomingClient {
    private static int id = 1;
    private final int currentClientId;
    private final int clientPatience;

    private UpcomingClient() {
        this.currentClientId = id++;
        this.clientPatience = ThreadLocalRandom.current().nextInt(100, 200);
    }

    public int getCurrentClientId() {
        return currentClientId;
    }

    public int getClientPatience() {
        return clientPatience;
    }

    public static UpcomingClient getNewClient() {
        return new UpcomingClient();
    }

    public static int timeToNextClient() {
        return ThreadLocalRandom.current().nextInt(20, 50);
    }
}
