package negotiation.agent.behaviour.negotiating;

import jade.core.AID;
import jade.core.behaviours.Behaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import negotiation.agent.NegotiatingAgent;
import negotiation.util.Item;

import java.util.ArrayList;
import java.util.HashMap;

public class GetInventoryInfoBehaviour extends Behaviour {

    private int counter =  0;
    private boolean done = false;
    private HashMap<AID, ArrayList<Item>> agentInventoryMap = new HashMap<AID, ArrayList<Item>>();
    private int numberOfReceivedInventories = 0;
    private MessageTemplate messageTemplate;

    @Override
    public void action() {
        switch(counter) {
            case(0):
                if (properlyInitialized()) {
                    counter++;
                }
                break;
            case(1):
                requestAllInventories();
                counter++;
                break;
            case(2):
                receiveAllInventoryLists();
                if (numberOfReceivedInventories == agentInventoryMap.keySet().size()) {
                    counter++;
                }
                break;
            case (3):
                done = true;
                break;
        }
    }

    private boolean properlyInitialized() {
        return ((NegotiatingAgent) myAgent).getInventory() != null;
    }

    private void requestAllInventories() {
        findAllOtherNegotiators();
        sendRequestToAll();
    }

    private void findAllOtherNegotiators() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType("negotiator");
        try
        {
            DFAgentDescription[] searchResults = DFService.search(myAgent, template);
            for (DFAgentDescription searchResult : searchResults) {
                if (searchResult.getName() != myAgent.getAID()) {
                    agentInventoryMap.put(searchResult.getName(), null);
                }
            }
        }
        catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    private void sendRequestToAll() {
        ACLMessage requestMessage = new ACLMessage(ACLMessage.REQUEST);
        for (AID agentID : agentInventoryMap.keySet()) {
            if (agentID != myAgent.getAID()) {
                requestMessage.addReceiver(agentID);

            }
        }
        requestMessage.setConversationId("requesting inventory");
        String uniqueMessageID = "invReq" + System.nanoTime();
        requestMessage.setReplyWith(uniqueMessageID);
        myAgent.send(requestMessage);
        messageTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId("requesting inventory"),
                MessageTemplate.MatchInReplyTo(requestMessage.getReplyWith()));
    }

    private void receiveAllInventoryLists() {
        ACLMessage incomingMessage = myAgent.receive(messageTemplate);
        if (incomingMessage != null) {
            ArrayList<Item> itemList = createItemList(incomingMessage);
            AID senderID = incomingMessage.getSender();
            agentInventoryMap.put(senderID, itemList);
            numberOfReceivedInventories++;
        }
        else {
            block();
        }
    }

    private ArrayList<Item> createItemList(ACLMessage incomingMessage) {
        String messageContent = incomingMessage.getContent();
        ArrayList<Item> itemList = new ArrayList<Item>();
        for (String itemInfo : messageContent.split(",")) {
            String[] separatedItemInfo = itemInfo.split(":");
            Item itemCopy = new Item(separatedItemInfo[0], Integer.parseInt(separatedItemInfo[1]));
            itemList.add(itemCopy);
        }
        return itemList;
    }

    @Override
    public boolean done() {
        return done;
    }
}
