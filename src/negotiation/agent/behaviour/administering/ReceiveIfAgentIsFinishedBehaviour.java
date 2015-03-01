package negotiation.agent.behaviour.administering;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import negotiation.agent.ItemAdministratorAgent;

public class ReceiveIfAgentIsFinishedBehaviour extends CyclicBehaviour {

    @Override
    public void action() {
        MessageTemplate messageTemplate = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchConversationId("Finished"));
        ACLMessage message = myAgent.receive(messageTemplate);
        if (message != null) {
            System.out.println("received finished message");
            ((ItemAdministratorAgent) myAgent).setOneAgentFinished(true);
            int agentsMoney = Integer.parseInt(message.getContent());
            if (agentsMoney > ((ItemAdministratorAgent) myAgent).getMaxMoney()) {
                ((ItemAdministratorAgent) myAgent).setMaxMoney(agentsMoney);
                ((ItemAdministratorAgent) myAgent).setWinningAgent(message.getSender().getLocalName());
            }
        }
        else {
            block();
        }
    }
}
