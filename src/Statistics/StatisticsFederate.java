package Statistics;

import hla.rti1516e.*;
import hla.rti1516e.encoding.EncoderFactory;
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


public class StatisticsFederate {

    public static final String READY_TO_RUN = "ReadyToRun";
    public static final String federateName = "StatisticsFederate";

    private RTIambassador rtiamb;
    private StatisticsFederateAmbassador fedamb;
    private HLAfloat64TimeFactory timeFactory;
    protected EncoderFactory encoderFactory;

    protected InteractionClassHandle clientComingHandle;
    protected InteractionClassHandle clientResignHandle;
    protected ParameterHandle currentClientIdHandle;

    protected InteractionClassHandle lettingClientInHandle;
    protected ParameterHandle enteringRestaurantClientIdHandle;

    protected Statistics statistics = new Statistics();

    private void log(String message) {
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
        fedamb = new StatisticsFederateAmbassador(this);
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
        rtiamb.joinFederationExecution(federateName, "Statistics", "RestaurantFederation");

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

        int i = 1;

        while (fedamb.isRunning) {
            advanceTime(1d);
            i++;
            if (i >= 100) {
                printStatistics();
                i = 1;
            }
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

    private void printStatistics() {
        String stats = statistics.getStatistics();
        log(stats);
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
        String baseIName = "HLAinteractionRoot.Client";
        currentClientIdHandle = rtiamb.getParameterHandle(
                rtiamb.getInteractionClassHandle(baseIName), "currentClientId");

        String iname = "HLAinteractionRoot.Client.ClientComing";
        clientComingHandle = rtiamb.getInteractionClassHandle(iname);
        rtiamb.subscribeInteractionClass(clientComingHandle);

        iname = baseIName + ".ClientImpatience";
        clientResignHandle = rtiamb.getInteractionClassHandle(iname);
        rtiamb.subscribeInteractionClass(clientResignHandle);

        iname = "HLAinteractionRoot.LetClientIn";
        lettingClientInHandle = rtiamb.getInteractionClassHandle(iname);
        enteringRestaurantClientIdHandle = rtiamb.getParameterHandle(lettingClientInHandle, "firstClientId");
        rtiamb.subscribeInteractionClass(lettingClientInHandle);
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
            new StatisticsFederate().runFederate(federateName);
        } catch (Exception rtie) {
            rtie.printStackTrace();
        }
    }
}