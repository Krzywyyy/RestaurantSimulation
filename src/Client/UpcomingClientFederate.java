package Client;

import hla.rti1516e.*;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.encoding.HLAinteger64BE;
import hla.rti1516e.exceptions.*;
import hla.rti1516e.time.HLAfloat64Interval;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;


public class UpcomingClientFederate {

    public static final String READY_TO_RUN = "ReadyToRun";
    public static final String federateName = "UpcomingClientFederate";

    private RTIambassador rtiamb;
    private UpcomingClientFederateAmbassador fedamb;
    private HLAfloat64TimeFactory timeFactory;
    protected EncoderFactory encoderFactory;

    protected InteractionClassHandle clientComingHandle;

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
        fedamb = new UpcomingClientFederateAmbassador(this);
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
        rtiamb.joinFederationExecution(federateName, "UpcomingClient", "RestaurantFederation");

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
            generateNewClient();

            advanceTime(UpcomingClient.timeToNextClient());
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

    private void generateNewClient() throws FederateNotExecutionMember, NotConnected, NameNotFound, InvalidInteractionClassHandle, RTIinternalError, InteractionClassNotPublished, InteractionParameterNotDefined, InteractionClassNotDefined, SaveInProgress, RestoreInProgress {
        UpcomingClient client = UpcomingClient.getNewClient();
        ParameterHandleValueMap parameterHandleValueMap = rtiamb.getParameterHandleValueMapFactory().create(2);
        ParameterHandle currentClientIdHandle = rtiamb.getParameterHandle(clientComingHandle, "currentClientId");
        ParameterHandle patienceHandle = rtiamb.getParameterHandle(clientComingHandle, "patience");
        HLAinteger64BE currentClientId = encoderFactory.createHLAinteger64BE(client.getCurrentClientId());
        HLAinteger32BE patience = encoderFactory.createHLAinteger32BE(client.getClientPatience());
        parameterHandleValueMap.put(currentClientIdHandle, currentClientId.toByteArray());
        parameterHandleValueMap.put(patienceHandle, patience.toByteArray());

        log("New client with id = " + currentClientId.getValue() + " and patience = " +
                patience.getValue() + " came to queue. Time = " + fedamb.federateTime);
        rtiamb.sendInteraction(clientComingHandle, parameterHandleValueMap, generateTag());
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
        String iname = "HLAinteractionRoot.Client.ClientComing";
        clientComingHandle = rtiamb.getInteractionClassHandle(iname);
        rtiamb.publishInteractionClass(clientComingHandle);
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
            new UpcomingClientFederate().runFederate(federateName);
        } catch (Exception rtie) {
            rtie.printStackTrace();
        }
    }
}