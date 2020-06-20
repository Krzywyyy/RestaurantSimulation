package Restaurant;

import java.util.ArrayList;
import java.util.List;

public class Restaurant {
    private static final int tablesCount = 10;

    private final List<Long> currentClientsIds = new ArrayList<>();
    public long currentClientsCount = 0;

    public boolean hasFreeTable() {
        return currentClientsCount < tablesCount;
    }
//
//    public int freeTablesCount() {
//        return 10 - currentClientsIds.size();
//    }

    public void letClientIn(long clientId) {
        currentClientsIds.add(clientId);
        currentClientsCount++;
    }

    public void letClientOut(long clientId) {
        currentClientsIds.remove(clientId);
        currentClientsCount--;
    }
}
