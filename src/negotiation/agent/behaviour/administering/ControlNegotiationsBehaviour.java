package negotiation.agent.behaviour.administering;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import negotiation.agent.ItemAdministratorAgent;

import java.util.ArrayList;

public class ControlNegotiationsBehaviour extends CyclicBehaviour {
    private final ArrayList<AID> negotiatingAgents;
    private int counter = 0;

    public ControlNegotiationsBehaviour(ArrayList<AID> negotiatingAgents) {
        this.negotiatingAgents = negotiatingAgents;
    }

    public void sendStartMessageToAgent(AID agent) {
        System.out.println("Admin says START NEGOTIATION");
        ACLMessage message = new ACLMessage(ACLMessage.INFORM);
        message.addReceiver(agent);
        message.setConversationId("StartNegotiation");
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
                ACLMessage message = myAgent.receive(messageTemplate);
                if (message != null) {
                    if (counter % negotiatingAgents.size() == 0 && ((ItemAdministratorAgent) myAgent).isOneAgentFinished()) { //new round starting, and we check if someone is done
                        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                        System.out.println(((ItemAdministratorAgent) myAgent).getWinningAgent()+ " won the negotiations with "+ ((ItemAdministratorAgent) myAgent).getMaxMoney()+" money.");
                        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                        this.done();
                    } else {
                        sendStartMessageToAgent(negotiatingAgents.get(counter % negotiatingAgents.size()));
                        counter++;
                    }
                }
                break;
        }
    }
}
