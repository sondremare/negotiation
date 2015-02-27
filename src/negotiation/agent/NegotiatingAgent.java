package negotiation.agent;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import negotiation.util.Item;
import negotiation.util.ItemFactory;
import negotiation.util.Utility;

import java.util.ArrayList;
import java.util.HashMap;

public class NegotiatingAgent extends Agent {
    private ArrayList<Item> inventory;
    private ArrayList<Item> wishlist;
    private int money = 2000;
    private boolean isBuyer = false;
    private int timeSpent = 0;

    public static int totalTimeAllowed = 10;

    protected void setup() {
        addBehaviour(new RegisterBehaviour());
        addBehaviour(new ReceiveItemListBehaviour());
        addBehaviour(new GetInventoryInfoBehaviour());
        addBehaviour(new RespondToInventoryRequestBehaviour());

        addBehaviour(new StartNegotiationBehaviour());
        addBehaviour(new RespondToProposalBehaviour());

    }

    private class RegisterBehaviour extends OneShotBehaviour {

        @Override
        public void action() {
            DFAgentDescription dfAgentDescription = new DFAgentDescription();
            dfAgentDescription.setName(getAID());
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

    private class ReceiveItemListBehaviour extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate messageTemplate = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchConversationId("ItemList"));
            ACLMessage message = myAgent.receive(messageTemplate);
            if (message != null) {
                String[] itemLists = message.getContent().split(";");
                inventory = ItemFactory.parseItemList(itemLists[0]);
                wishlist = ItemFactory.parseItemList(itemLists[1]);
                System.out.println(myAgent.getLocalName() + " has items: " + inventory + "  and wants: " + wishlist);
            }
            else {
                block();
            }
        }
    }

    private class StartNegotiationBehaviour extends CyclicBehaviour {

        ArrayList<AID> agentList = new ArrayList<AID>();

        @Override
        public void action() {
            MessageTemplate messageTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId("Negotiation"),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            ACLMessage incomingMessage = myAgent.receive(messageTemplate);

            if (incomingMessage != null) {
                isBuyer = true;
                Item wantedItem = getNextWantedItem();
                sendProposalToAll(wantedItem);
            }
            else {
                block();
            }
        }

        private Item getNextWantedItem() {
            for (Item wantedItem : wishlist) {
                boolean ownsItem = false;
                for (Item inventoryItem : inventory) {
                    ownsItem = ownsItem || (wantedItem.getName() == inventoryItem.getName());
                }
                if (!ownsItem) {
                    return wantedItem;
                }
            }
            return null;
        }


        private void sendProposalToAll(Item wantedItem) {
            findAllOtherNegotiators();
            ACLMessage proposalMessage = new ACLMessage(ACLMessage.PROPOSE);
            for (AID agentID : agentList) {
                if (agentID != myAgent.getAID()) {
                    proposalMessage.addReceiver(agentID);
                }
            }
            proposalMessage.setConversationId("proposal on item");
            proposalMessage.setContent(wantedItem.getName() + ":" + 0);
            myAgent.send(proposalMessage);
        }

        private void findAllOtherNegotiators() {
            DFAgentDescription template = new DFAgentDescription();
            ServiceDescription sd = new ServiceDescription();
            sd.setType("negotiator");
            try
            {
                DFAgentDescription[] result = DFService.search(myAgent, template);
                for (int i = 0; i < result.length; ++i)
                {
                    if (result[i].getName() != myAgent.getAID()) {
                        agentList.add(result[i].getName());
                    }
                }
            }
            catch (FIPAException fe) {
                fe.printStackTrace();
            }
        }
    }



    private class RespondToProposalBehaviour extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate messageTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId("proposal on item"),
                    MessageTemplate.MatchPerformative(ACLMessage.PROPOSE));
            ACLMessage incomingMessage = myAgent.receive(messageTemplate);

            if (incomingMessage != null) {
                timeSpent++;
                String[] proposalContent = incomingMessage.getContent().split(":");
                Item wantedItem = getItemFromInventory(proposalContent[0]);
                int proposedPrice = Integer.parseInt(proposalContent[1]);
                //TODO fix wantedItem for Buyers. Buyers need the original price. Not the proposed price
                if (wantedItem != null) {
                    ACLMessage returnMessage = null;
                    int proposalUtility;
                    int newProposalUtility;
                    int newProposalPrice;
                    if (isBuyer) {
                        proposalUtility = Utility.getBuyersUtility(wantedItem, proposedPrice);
                        newProposalPrice = Utility.getBuyersNextBid(wishlist, money, wantedItem, timeSpent, totalTimeAllowed);
                        System.out.println("buyer newProposalPrice: "+newProposalPrice);
                        newProposalUtility = Utility.getBuyersUtility(wantedItem, newProposalPrice);
                    }
                    else {
                        proposalUtility = Utility.getSellersUtility(wantedItem);
                        newProposalPrice = Utility.getSellersNextBid(wantedItem, timeSpent, totalTimeAllowed);
                        System.out.println("seller newProposalPrice: "+newProposalPrice);
                        newProposalUtility = Utility.convertPriceToSellersUtility(newProposalPrice);
                    }

                    if (newProposalUtility < proposalUtility) {
                        System.out.println("YAY");
                        returnMessage = createAcceptProposal(incomingMessage, proposedPrice);
                        //TODO start transaction
                    }
                    else {
                        System.out.println(myAgent.getAID() + ", price: "+newProposalPrice);
                        returnMessage = createPropositionMessage(incomingMessage, newProposalPrice);
                    }
                    myAgent.send(returnMessage);
                }
            }
            else {
                block();
            }
        }

        private Item getItemFromInventory(String wantedItemName) {
            for (Item item : inventory) {
                if (item.getName().equals(wantedItemName)) {
                    return item;
                }
            }
            return null;
        }

        private ACLMessage createPropositionMessage(ACLMessage incomingMessage, int newProposalPrice)
        {
            ACLMessage responseMessage = incomingMessage.createReply();
            String nameOfItem = incomingMessage.getContent().split(":")[0];
            responseMessage.setContent(nameOfItem + ":" + newProposalPrice);
            responseMessage.setPerformative(ACLMessage.PROPOSE);
            responseMessage.setConversationId("proposal on item");
            return responseMessage;
        }

        private ACLMessage createAcceptProposal(ACLMessage incomingMessage, int acceptablePrice) {
            ACLMessage responseMessage = incomingMessage.createReply();
            responseMessage.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
            String nameOfItem = incomingMessage.getContent().split(":")[0];
            responseMessage.setContent(nameOfItem + ":" + acceptablePrice);
            //TODO set isBuyer = false; somewhere in the code! AND set int timeSpent = 0;
            return responseMessage;
        }
    }


    private class RespondToInventoryRequestBehaviour extends CyclicBehaviour {

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

            for (Item item : inventory) {
                messageContent += item.getName() + ":" + item.getValue() + ",";
            }
            return messageContent;
        }
    }

    private class GetInventoryInfoBehaviour extends Behaviour {

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
            if (inventory == null) {
                return false;
            }
            return true;
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
                DFAgentDescription[] result = DFService.search(myAgent, template);
                for (int i = 0; i < result.length; ++i)
                {
                    if (result[i].getName() != myAgent.getAID()) {
                        agentInventoryMap.put(result[i].getName(), null);
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
}
