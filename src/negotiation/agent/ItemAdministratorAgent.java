package negotiation.agent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import negotiation.util.ItemFactory;

import java.util.ArrayList;

public class ItemAdministratorAgent extends Agent {

    private ArrayList<AID> negotiatingAgents;
    private int counter = 0;

    protected void setup() {
        addBehaviour(new FindNegotiatingAgents());
    }

    private class FindNegotiatingAgents extends OneShotBehaviour {

        @Override
        public void action() {

            try{
                Thread.sleep(1000);
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
                myAgent.addBehaviour(new SendItemListsToAgents());

            } catch (FIPAException e) {
                e.printStackTrace();
            }
        }
    }

    private class SendItemListsToAgents extends OneShotBehaviour {

        @Override
        public void action() {
            String[] agentItems = ItemFactory.getItemsForAgents(negotiatingAgents);
            for (int i = 0; i < negotiatingAgents.size(); i++) {
                ACLMessage message = new ACLMessage(ACLMessage.INFORM);
                message.addReceiver(negotiatingAgents.get(i));
                message.setContent(agentItems[i]);
                message.setConversationId("ItemList");
                myAgent.send(message);
            }
            addBehaviour(new ControlNegotiationsBehaviour());
        }
    }

    private class ControlNegotiationsBehaviour extends CyclicBehaviour {

        public void sendStartMessageToAgent(AID agent) {
            ACLMessage message = new ACLMessage(ACLMessage.INFORM);
            message.addReceiver(agent);
            message.setConversationId("Negotiation");
            myAgent.send(message);
        }

        @Override
        public void action() {
            switch (counter) {
                case 0:
                    sendStartMessageToAgent(negotiatingAgents.get(counter % negotiatingAgents.size()));
                    counter++;
                    break;
                default:
                    MessageTemplate messageTemplate = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                            MessageTemplate.MatchConversationId("NegotiationsEnded"));
                    if (myAgent.receive(messageTemplate) != null) {
                        sendStartMessageToAgent(negotiatingAgents.get(counter % negotiatingAgents.size()));
                        counter++;
                        break;
                    }
            }
        }
    }
}
