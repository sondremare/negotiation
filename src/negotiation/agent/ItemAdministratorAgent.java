package negotiation.agent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import negotiation.util.Item;
import negotiation.util.ItemFactory;

import java.util.ArrayList;
import java.util.Collections;

public class ItemAdministratorAgent extends Agent {

    protected void setup() {
        addBehaviour(new FindNegotiatingAgents());
    }

    private class FindNegotiatingAgents extends OneShotBehaviour {

        @Override
        public void action() {
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
                myAgent.addBehaviour(new SendItemListsToAgents(negotiatingAgents));

            } catch (FIPAException e) {
                e.printStackTrace();
            }
        }
    }

    private class SendItemListsToAgents extends OneShotBehaviour {

        private ArrayList<AID> negotiatingAgents;

        public SendItemListsToAgents(ArrayList<AID> negotiatingAgents) {
            this.negotiatingAgents = negotiatingAgents;
        }

        @Override
        public void action() {
            String[] agentItems = ItemFactory.getItemsForAgents(negotiatingAgents);
            for (int i = 0; i < agentItems.length; i++) {
                System.out.println(agentItems);
            }
        }
    }
}
