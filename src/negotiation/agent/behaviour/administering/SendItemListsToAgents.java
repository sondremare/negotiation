package negotiation.agent.behaviour.administering;

import jade.core.AID;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import negotiation.agent.ItemAdministratorAgent;
import negotiation.util.ItemFactory;

import java.util.ArrayList;

public class SendItemListsToAgents extends OneShotBehaviour {

    @Override
    public void action() {
        ArrayList<AID> negotiatingAgents = ((ItemAdministratorAgent) myAgent).getNegotiatingAgents();
        String[] agentItems = ItemFactory.getItemsForAgents(negotiatingAgents);
        for (int i = 0; i < negotiatingAgents.size(); i++) {
            ACLMessage message = new ACLMessage(ACLMessage.INFORM);
            message.addReceiver(negotiatingAgents.get(i));
            message.setContent(agentItems[i]);
            message.setConversationId("ItemList");
            myAgent.send(message);
        }
        myAgent.addBehaviour(new ControlNegotiationsBehaviour(negotiatingAgents));
        myAgent.addBehaviour(new ReceiveIfAgentIsFinishedBehaviour());
    }
}