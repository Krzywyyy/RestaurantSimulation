package Queue;

import hla.rti1516e.*;
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.HLAboolean;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.encoding.HLAinteger64BE;
import hla.rti1516e.exceptions.FederateInternalError;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Time;
import org.portico.impl.hla1516e.types.encoding.HLA1516eBoolean;
import org.portico.impl.hla1516e.types.encoding.HLA1516eInteger32BE;
import org.portico.impl.hla1516e.types.encoding.HLA1516eInteger64BE;

public class RestaurantQueueFederateAmbassador extends NullFederateAmbassador {

    private RestaurantQueueFederate federate;

    protected double federateTime = 0.0;
    protected double federateLookahead = 1.0;

    protected boolean isRegulating = false;
    protected boolean isConstrained = false;
    protected boolean isAdvancing = false;

    protected boolean isAnnounced = false;
    protected boolean isReadyToRun = false;


    protected boolean isRunning = true;

    public RestaurantQueueFederateAmbassador(RestaurantQueueFederate federate) {
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
        if (label.equals(RestaurantQueueFederate.READY_TO_RUN))
            this.isAnnounced = true;
    }

    @Override
    public void federationSynchronized(String label, FederateHandleSet failed) {
        log("Federation Synchronized: " + label);
        if (label.equals(RestaurantQueueFederate.READY_TO_RUN))
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
    public void reflectAttributeValues(ObjectInstanceHandle theObject,
                                       AttributeHandleValueMap theAttributes,
                                       byte[] tag,
                                       OrderType sentOrder,
                                       TransportationTypeHandle transport,
                                       SupplementalReflectInfo reflectInfo)
            throws FederateInternalError {
        reflectAttributeValues(theObject, theAttributes, tag, sentOrder, transport, null, sentOrder, reflectInfo);
    }

    @Override
    public void reflectAttributeValues(ObjectInstanceHandle theObject,
                                       AttributeHandleValueMap theAttributes,
                                       byte[] tag,
                                       OrderType sentOrdering,
                                       TransportationTypeHandle theTransport,
                                       LogicalTime time,
                                       OrderType receivedOrdering,
                                       SupplementalReflectInfo reflectInfo)
            throws FederateInternalError {
        HLAinteger64BE currentClientsCount = new HLA1516eInteger64BE();
        try {
            currentClientsCount.decode(theAttributes.get(federate.currentClientsCount));
        } catch (DecoderException e) {
            e.printStackTrace();
        }
        federate.restaurant.currentClientsCount = currentClientsCount.getValue();
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
        if (interactionClass.equals(federate.clientComingHandle)) {
            QueueClient client = getClientWithGivenParameters(theParameters);
            info = String.format("Interaction received: ClientComing: clientId = %d with patience = %s. Time = %s",
                    client.getClientId(), client.getImpatienceTime(), federateTime);
            federate.restaurantQueue.addClient(client);
        }
        log(info);
    }

    private QueueClient getClientWithGivenParameters(ParameterHandleValueMap theParameters) {
        HLAinteger64BE currentClientId = new HLA1516eInteger64BE();
        try {
            currentClientId.decode(theParameters.get(federate.currentClientIdHandle));
        } catch (DecoderException e) {
            e.printStackTrace();
        }
        HLAinteger32BE patience = new HLA1516eInteger32BE();
        try {
            patience.decode(theParameters.get(federate.patienceHandle));
        } catch (DecoderException e) {
            e.printStackTrace();
        }
        return new QueueClient(currentClientId.getValue(), patience.getValue() + federateTime);
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
