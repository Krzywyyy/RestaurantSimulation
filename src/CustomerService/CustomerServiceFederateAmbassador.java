package CustomerService;

import hla.rti1516e.*;
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.HLAinteger64BE;
import hla.rti1516e.exceptions.FederateInternalError;
import hla.rti1516e.time.HLAfloat64Time;
import org.portico.impl.hla1516e.types.encoding.HLA1516eInteger64BE;

public class CustomerServiceFederateAmbassador extends NullFederateAmbassador {

    private CustomerServiceFederate federate;

    protected double federateTime = 0.0;
    protected double federateLookahead = 1.0;

    protected boolean isRegulating = false;
    protected boolean isConstrained = false;
    protected boolean isAdvancing = false;

    protected boolean isAnnounced = false;
    protected boolean isReadyToRun = false;


    protected boolean isRunning = true;

    public CustomerServiceFederateAmbassador(CustomerServiceFederate federate) {
        this.federate = federate;
    }

    private void log(String message) {
        System.out.println("FederateAmbassador: " + message);
    }

    @Override
    public void synchronizationPointRegistrationFailed(String label,
                                                       SynchronizationPointFailureReason reason) {
        log("Failed to register sync point: " + label + ", reason=" + reason);
    }

    @Override
    public void synchronizationPointRegistrationSucceeded(String label) {
        log("Successfully registered sync point: " + label);
    }

    @Override
    public void announceSynchronizationPoint(String label, byte[] tag) {
        log("Synchronization point announced: " + label);
        if (label.equals(CustomerServiceFederate.READY_TO_RUN))
            this.isAnnounced = true;
    }

    @Override
    public void federationSynchronized(String label, FederateHandleSet failed) {
        log("Federation Synchronized: " + label);
        if (label.equals(CustomerServiceFederate.READY_TO_RUN))
            this.isReadyToRun = true;
    }

    @Override
    public void timeRegulationEnabled(LogicalTime time) {
        this.federateTime = ((HLAfloat64Time) time).getValue();
        this.isRegulating = true;
    }

    @Override
    public void timeConstrainedEnabled(LogicalTime time) {
        this.federateTime = ((HLAfloat64Time) time).getValue();
        this.isConstrained = true;
    }

    @Override
    public void timeAdvanceGrant(LogicalTime time) {
        this.federateTime = ((HLAfloat64Time) time).getValue();
        this.isAdvancing = false;
    }

    @Override
    public void receiveInteraction(InteractionClassHandle interactionClass,
                                   ParameterHandleValueMap theParameters,
                                   byte[] tag,
                                   OrderType sentOrdering,
                                   TransportationTypeHandle theTransport,
                                   SupplementalReceiveInfo receiveInfo) {

        this.receiveInteraction(interactionClass, theParameters, tag, sentOrdering, theTransport, null, sentOrdering, receiveInfo);
    }

    @Override
    public void receiveInteraction(InteractionClassHandle interactionClass,
                                   ParameterHandleValueMap theParameters,
                                   byte[] tag,
                                   OrderType sentOrdering,
                                   TransportationTypeHandle theTransport,
                                   LogicalTime time,
                                   OrderType receivedOrdering,
                                   SupplementalReceiveInfo receiveInfo) {

        String info = "";
        if (interactionClass.equals(federate.lettingClientIn)) {
            SupportedClient supportedClient = getSupportedClientWithGivenParameters(theParameters);
            info = String.format("Interaction received: LetClientIn: clientId = %d. Time = %s",
                    supportedClient.getId(), federateTime);
            federate.service.supportedClients.add(supportedClient);
            federate.log(String.format("CustomerService began order from client with id = %d. Time = %s", supportedClient.getId(), federateTime));
        }
        log(info);
    }

    private SupportedClient getSupportedClientWithGivenParameters(ParameterHandleValueMap theParameters) {
        HLAinteger64BE clientId = new HLA1516eInteger64BE();
        try {
            clientId.decode(theParameters.get(federate.comingClientIdHandle));
        } catch (DecoderException e) {
            e.printStackTrace();
        }
        return new SupportedClient(clientId.getValue(), federateTime);
    }

    @Override
    public void removeObjectInstance(ObjectInstanceHandle theObject,
                                     byte[] tag,
                                     OrderType sentOrdering,
                                     SupplementalRemoveInfo removeInfo)
            throws FederateInternalError {
        log("Object Removed: handle=" + theObject);
    }
}
