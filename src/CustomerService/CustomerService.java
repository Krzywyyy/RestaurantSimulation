package CustomerService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static CustomerService.ServiceType.ORDER_SECOND_DISH;
import static CustomerService.ServiceType.PAY;

public class CustomerService {
    protected List<SupportedClient> supportedClients = new ArrayList<>();

    public List<SupportedClient> getClientsWhoWantToPay(double federateTime) {
        return supportedClients.stream()
                .filter(supportedClient ->
                        PAY.equals(supportedClient.receiveOrderOrPayment(federateTime)))
                .collect(Collectors.toList());
    }

    public List<SupportedClient> getClientsWhoWantSecondDish(double federateTime) {
        return supportedClients.stream()
                .filter(supportedClient ->
                        ORDER_SECOND_DISH.equals(supportedClient.receiveOrderOrPayment(federateTime)))
                .collect(Collectors.toList());
    }

    public void removeClientWhoPaid(SupportedClient client) {
        supportedClients.remove(client);
    }
}

