package CustomerService;

import java.util.concurrent.ThreadLocalRandom;

import static CustomerService.ServiceType.*;

public class SupportedClient {
    private final static int minEatTime = 30;
    private final static int maxEatTime = 60;

    private final long id;

    private boolean ateSecondDish;
    private double exitingTime;

    public SupportedClient(long id, double federateTime) {
        this.id = id;
        this.ateSecondDish = false;
        this.exitingTime = ThreadLocalRandom.current().nextInt(minEatTime, maxEatTime) + federateTime;
    }

    public long getId() {
        return id;
    }

    public ServiceType receiveOrderOrPayment(double federateTime) {
        if (ateSecondDish && shouldPayOrOrderSecondDish(federateTime)) {
            return PAY;
        }
        if (!ateSecondDish && shouldPayOrOrderSecondDish(federateTime)) {
            boolean wantSecondDish = ThreadLocalRandom.current().nextBoolean();
            return wantSecondDish ? orderSecondDish() : PAY;
        }
        return NOTHING;
    }

    public boolean shouldPayOrOrderSecondDish(double federateTime) {
        return exitingTime <= federateTime;
    }

    private ServiceType orderSecondDish() {
        ateSecondDish = true;
        exitingTime += ThreadLocalRandom.current().nextInt(minEatTime, maxEatTime);
        return ORDER_SECOND_DISH;
    }
}
