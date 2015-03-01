package negotiation.agent.behaviour.negotiating;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import negotiation.agent.NegotiatingAgent;
import negotiation.util.Item;

public class RespondToInventoryRequestBehaviour extends CyclicBehaviour {

    @Override
    public void action() {
        MessageTemplate messageTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId("requesting inventory"),
                MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
        ACLMessage incomingMessage = myAgent.receive(messageTemplate);

        if (incomingMessage != null) {
            ACLMessage replyMessage = incomingMessage.createReply();
            sendInventory(replyMessage);
        }
        else {
            block();
        }
    }

    private void sendInventory(ACLMessage replyMessage) {
        replyMessage.setPerformative(ACLMessage.INFORM);
        String messageContent = createMessageString();
        replyMessage.setContent(messageContent);
        myAgent.send(replyMessage);
    }

    private String createMessageString() {
        String messageContent = "";

        for (Item item : ((NegotiatingAgent) myAgent).getInventory()) {
            messageContent += item.getName() + ":" + item.getValue() + ",";
        }
        return messageContent;
    }
}
