package negotiation.agent.behaviour.negotiating;

import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import negotiation.agent.NegotiatingAgent;
import negotiation.util.Item;

import java.util.ArrayList;

public class StartNegotiationBehaviour extends CyclicBehaviour {

    ArrayList<AID> agentList = new ArrayList<AID>();

    @Override
    public void action() {
        MessageTemplate messageTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId("StartNegotiation"),
                MessageTemplate.MatchPerformative(ACLMessage.INFORM));
        ACLMessage incomingMessage = myAgent.receive(messageTemplate);
        if (incomingMessage != null) {
            System.out.println(myAgent.getLocalName() + " is starting negotiations");
            ((NegotiatingAgent) myAgent).setAdministrator(incomingMessage.getSender());
            Item wantedItem = ((NegotiatingAgent) myAgent).getRandomWantedItem();
            if (wantedItem == null) {
                ((NegotiatingAgent) myAgent).sendNegotiationsEndedMessage(myAgent);
            } else {
                ((NegotiatingAgent) myAgent).setBuyer(true);
                sendProposalToAll(wantedItem);
            }
        }
        else {
            block();
        }
    }

    private void sendProposalToAll(Item wantedItem) {
        findAllOtherNegotiators();
        ACLMessage proposalMessage = new ACLMessage(ACLMessage.PROPOSE);
        for (AID agentID : agentList) {
            if (!agentID.equals(myAgent.getAID())) {
                proposalMessage.addReceiver(agentID);
            }
        }
        proposalMessage.setConversationId("proposal on item");
        proposalMessage.setContent(wantedItem.getName() + ":" + 0 + ":" + getInventoryAsString());
        System.out.println(myAgent.getLocalName() + " sending proposal to all for item: " + wantedItem.getName());
        myAgent.send(proposalMessage);
    }
    private String getInventoryAsString() {
        String returnString = "";
        for (Item inventoryItem : ((NegotiatingAgent)myAgent).getInventory()) {
            returnString += inventoryItem.getName() + ",";
        }
        return returnString;
    }

    //TODO This method is duplicated for two different behaviours
    private void findAllOtherNegotiators() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("negotiator");
        try {
            DFAgentDescription[] result = DFService.search(myAgent, template);
            for (DFAgentDescription aResult : result) {
                if (aResult.getName() != myAgent.getAID()) {
                    agentList.add(aResult.getName());
                }
            }
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
}
