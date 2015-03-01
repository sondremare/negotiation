package negotiation.agent.behaviour.administering;

import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import negotiation.agent.ItemAdministratorAgent;

import java.util.ArrayList;

public class FindNegotiatingAgents extends OneShotBehaviour {

    @Override
    public void action() {

        try{
            Thread.sleep(1000); //Give the Negotiation agents som inital time to register
        } catch (InterruptedException e){
            e.printStackTrace();
        }

        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription serviceDescription = new ServiceDescription();
        serviceDescription.setType("NegotiatingAgent");
        template.addServices(serviceDescription);
        try {
            DFAgentDescription[] result = DFService.search(myAgent, template);
            ArrayList<AID> negotiatingAgents = new ArrayList<AID>();
            for (int i = 0; i < result.length; i++) {
                negotiatingAgents.add(result[i].getName());
            }
            System.out.println(negotiatingAgents.size());
            ((ItemAdministratorAgent)myAgent).setNegotiatingAgents(negotiatingAgents);
            myAgent.addBehaviour(new SendItemListsToAgents());

        } catch (FIPAException e) {
            e.printStackTrace();
        }
    }
}