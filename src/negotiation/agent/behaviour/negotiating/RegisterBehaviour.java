package negotiation.agent.behaviour.negotiating;

import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

public class RegisterBehaviour extends OneShotBehaviour {

    private final AID aid;

    public RegisterBehaviour(AID aid) {
        this.aid = aid;
    }

    @Override
    public void action() {
        DFAgentDescription dfAgentDescription = new DFAgentDescription();
        dfAgentDescription.setName(aid);
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType("NegotiatingAgent");
        serviceDescription.setName(myAgent.getClass().getName());
        dfAgentDescription.addServices(serviceDescription);
        try {
            DFService.register(myAgent, dfAgentDescription);
        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
}
