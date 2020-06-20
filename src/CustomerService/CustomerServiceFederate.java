package CustomerService;

import hla.rti1516e.*;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAinteger64BE;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.FederationExecutionDoesNotExist;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Interval;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;


public class CustomerServiceFederate {

    public static final String READY_TO_RUN = "ReadyToRun";
    public static final String federateName = "CustomerServiceFederate";

    private RTIambassador rtiamb;
    private CustomerServiceFederateAmbassador fedamb;
    private HLAfloat64TimeFactory timeFactory;
    protected EncoderFactory encoderFactory;

    protected InteractionClassHandle lettingClientIn;
    protected ParameterHandle comingClientIdHandle;

    protected InteractionClassHandle paymentReceivedHandle;

    protected CustomerService service = new CustomerService();

    protected void log(String message) {
        System.out.println(federateName + " : " + message);
    }

    private void waitForUser() {
        log(" >>>>>>>>>> Press Enter to Continue <<<<<<<<<<");
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        try {
            reader.readLine();
        } catch (Exception e) {
            log("Error while waiting for user input: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void runFederate(String federateName) throws Exception {
        log("Creating RTIambassador");
        rtiamb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
        encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();
        log("Connecting...");
        fedamb = new CustomerServiceFederateAmbassador(this);
        rtiamb.connect(fedamb, CallbackModel.HLA_EVOKED);
        log("Creating Federation...");
        try {
            URL[] modules = new URL[]{
                    (new File("foms/Restaurant.xml")).toURI().toURL(),
            };

            rtiamb.createFederationExecution("RestaurantFederation", modules);
            log("Created Federation");
        } catch (FederationExecutionAlreadyExists exists) {
            log("Didn't create federation, it already existed");
        } catch (MalformedURLException urle) {
            log("Exception loading one of the FOM modules from disk: " + urle.getMessage());
            urle.printStackTrace();
            return;
        }
        rtiamb.joinFederationExecution(federateName, "CustomerService", "RestaurantFederation");

        log("Joined Federation as " + federateName);

        this.timeFactory = (HLAfloat64TimeFactory) rtiamb.getTimeFactory();

        rtiamb.registerFederationSynchronizationPoint(READY_TO_RUN, null);
        while (!fedamb.isAnnounced) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }

        waitForUser();

        rtiamb.synchronizationPointAchieved(READY_TO_RUN);
        log("Achieved sync point: " + READY_TO_RUN + ", waiting for federation...");
        while (!fedamb.isReadyToRun) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }

        enableTimePolicy();
        log("Time Policy Enabled");

        publishAndSubscribe();
        log("Published and Subscribed");

        while (fedamb.isRunning) {
            takeSecondDishesOrdersFromClients(service.getClientsWhoWantSecondDish(fedamb.federateTime));
            getPayments(service.getClientsWhoWantToPay(fedamb.federateTime));
            advanceTime(1.0);
        }

        rtiamb.resignFederationExecution(ResignAction.DELETE_OBJECTS);
        log("Resigned from Federation");

        try {
            rtiamb.destroyFederationExecution("RestaurantFederation");
            log("Destroyed Federation");
        } catch (FederationExecutionDoesNotExist dne) {
            log("No need to destroy federation, it doesn't exist");
        } catch (FederatesCurrentlyJoined fcj) {
            log("Didn't destroy federation, federates still joined");
        }
    }

    private void getPayments(List<SupportedClient> clientsWhoWantToPay) throws RTIexception {
        for (SupportedClient client : clientsWhoWantToPay) {
            ParameterHandleValueMap parameterHandleValueMap = rtiamb.getParameterHandleValueMapFactory().create(1);
            ParameterHandle payingClientIdHandle = rtiamb.getParameterHandle(paymentReceivedHandle, "clientId");
            HLAinteger64BE currentClientId = encoderFactory.createHLAinteger64BE(client.getId());
            parameterHandleValueMap.put(payingClientIdHandle, currentClientId.toByteArray());
            rtiamb.sendInteraction(paymentReceivedHandle, parameterHandleValueMap, generateTag());
            log(String.format("Client with id = %d paid for his order and left restaurant. Time = %s", client.getId(), fedamb.federateTime));
            service.removeClientWhoPaid(client);
        }
    }

    private void takeSecondDishesOrdersFromClients(List<SupportedClient> clientsWhoWantSecondDish) {
        clientsWhoWantSecondDish.forEach(supportedClient ->
                log(String.format("Client with id = %d ordered second dish. Time = %s", supportedClient.getId(), fedamb.federateTime)));
    }

    private void enableTimePolicy() throws Exception {
        HLAfloat64Interval lookahead = timeFactory.makeInterval(fedamb.federateLookahead);

        this.rtiamb.enableTimeRegulation(lookahead);

        while (!fedamb.isRegulating) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }

        this.rtiamb.enableTimeConstrained();

        while (!fedamb.isConstrained) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }
    }

    private void publishAndSubscribe() throws RTIexception {
        String iname = "HLAinteractionRoot.LetClientIn";
        lettingClientIn = rtiamb.getInteractionClassHandle(iname);
        comingClientIdHandle = rtiamb.getParameterHandle(lettingClientIn, "firstClientId");
        rtiamb.subscribeInteractionClass(lettingClientIn);

        iname = "HLAinteractionRoot.PaymentReceived";
        paymentReceivedHandle = rtiamb.getInteractionClassHandle(iname);
        rtiamb.publishInteractionClass(paymentReceivedHandle);
    }

    private void advanceTime(double timestep) throws RTIexception {
        fedamb.isAdvancing = true;
        HLAfloat64Time time = timeFactory.makeTime(fedamb.federateTime + timestep);
        rtiamb.timeAdvanceRequest(time);

        while (fedamb.isAdvancing) {
            rtiamb.evokeMultipleCallbacks(0.1, 0.2);
        }
    }

    private byte[] generateTag() {
        return ("(timestamp) " + System.currentTimeMillis()).getBytes();
    }

    public static void main(String[] args) {
        try {
            new CustomerServiceFederate().runFederate(federateName);
        } catch (Exception rtie) {
            rtie.printStackTrace();
        }
    }
}