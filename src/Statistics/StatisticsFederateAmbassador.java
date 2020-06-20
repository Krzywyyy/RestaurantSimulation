package Statistics;

import hla.rti1516e.*;
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.HLAinteger64BE;
import hla.rti1516e.exceptions.FederateInternalError;
import hla.rti1516e.time.HLAfloat64Time;
import org.portico.impl.hla1516e.types.encoding.HLA1516eInteger64BE;

public class StatisticsFederateAmbassador extends NullFederateAmbassador {
    private StatisticsFederate federate;

    protected double federateTime = 0.0;
    protected double federateLookahead = 1.0;

    protected boolean isRegulating = false;
    protected boolean isConstrained = false;
    protected boolean isAdvancing = false;

    protected boolean isAnnounced = false;
    protected boolean isReadyToRun = false;


    protected boolean isRunning = true;

    public StatisticsFederateAmbassador(StatisticsFederate federate) {
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
        if (label.equals(StatisticsFederate.READY_TO_RUN))
            this.isAnnounced = true;
    }

    @Override
    public void federationSynchronized(String label, FederateHandleSet failed) {
        log("Federation Synchronized: " + label);
        if (label.equals(StatisticsFederate.READY_TO_RUN))
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
        if (interactionClass.equals(federate.clientComingHandle)) {
            StatisticClient newStatisticClient = getNewStatisticClient(theParameters);
            federate.statistics.addNewStatisticClient(newStatisticClient);
            info = String.format("Interaction received: ClientComing: clientId = %d. Time = %s", newStatisticClient.getId(), federateTime);
        } else if (interactionClass.equals(federate.clientResignHandle)) {
            long resignedClientId = getResignedClientId(theParameters);
            federate.statistics.addClientResignation(resignedClientId);
            info = String.format("Interaction received: ClientImpatience: clientId = %d. Time = %s", resignedClientId, federateTime);
        } else if(interactionClass.equals(federate.lettingClientInHandle)){
            long enteringRestaurantClientId = getEnteringRestaurantClientId(theParameters);
            federate.statistics.addEnteringTime(enteringRestaurantClientId, federateTime);
            info = String.format("Interaction received: LetClientIn: clientId = %d. Time = %s", enteringRestaurantClientId, federateTime);
        }
        log(info);
    }

    private long getEnteringRestaurantClientId(ParameterHandleValueMap theParameters) {
        HLAinteger64BE enteringRestaurantClientId = new HLA1516eInteger64BE();
        try {
            enteringRestaurantClientId.decode(theParameters.get(federate.enteringRestaurantClientIdHandle));
        } catch (DecoderException e) {
            e.printStackTrace();
        }
        return enteringRestaurantClientId.getValue();
    }

    private long getResignedClientId(ParameterHandleValueMap theParameters) {
        HLAinteger64BE resignedClientId = new HLA1516eInteger64BE();
        try {
            resignedClientId.decode(theParameters.get(federate.currentClientIdHandle));
        } catch (DecoderException e) {
            e.printStackTrace();
        }
        return resignedClientId.getValue();
    }

    private StatisticClient getNewStatisticClient(ParameterHandleValueMap theParameters) {
        HLAinteger64BE comingClientId = new HLA1516eInteger64BE();
        try {
            comingClientId.decode(theParameters.get(federate.currentClientIdHandle));
        } catch (DecoderException e) {
            e.printStackTrace();
        }
        return new StatisticClient(comingClientId.getValue(), federateTime);
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
