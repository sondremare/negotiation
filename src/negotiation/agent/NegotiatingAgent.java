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
    private int money = 200;
    private boolean isBuyer = false;
    private int timeSpent = 0;
    private AID administrator;

    public static int totalTimeAllowed = 10;

    private void substractMoney(int money) {
        this.money -= money;
    }

    private void addMoney(int money) {
        this.money += money;
    }

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
            MessageTemplate messageTemplate = MessageTemplate.and(MessageTemplate.MatchConversationId("StartNegotiation"),
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM));
            ACLMessage incomingMessage = myAgent.receive(messageTemplate);
            if (incomingMessage != null) {
                administrator = incomingMessage.getSender();
                isBuyer = true;
                Item wantedItem = getNextWantedItem();
                if (wantedItem == null) {
                    //TODO MOVE THIS CHECK TO RIGHT AFTER A TRANSACTION IS COMPLETE
                    System.out.println("#######################################################################");
                    System.out.println("# Name: "+myAgent.getLocalName());
                    System.out.println("# Inventory: "+inventory);
                    System.out.println("# WishList: "+wishlist);
                    System.out.println("#######################################################################");
                    sendFinishedMessage();
                } else {
                    sendProposalToAll(wantedItem);
                }
            }
            else {
                block();
            }
        }

        private Item getNextWantedItem() {
            for (Item wantedItem : wishlist) {
                boolean ownsItem = false;
                for (Item inventoryItem : inventory) {
                    ownsItem = ownsItem || (wantedItem.getName().equals(inventoryItem.getName()));
                }
                if (!ownsItem) {
                    return wantedItem;
                }
            }
            return null;
        }

        private void sendFinishedMessage() {
            ACLMessage message = new ACLMessage(ACLMessage.INFORM);
            message.setConversationId("Finished");
            message.addReceiver(administrator);
            myAgent.send(message);
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
            proposalMessage.setContent(wantedItem.getName() + ":" + 0);
            System.out.println(myAgent.getLocalName() + " sending proposal to all for item: " + wantedItem.getName());
            myAgent.send(proposalMessage);
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



    private class RespondToProposalBehaviour extends CyclicBehaviour {

        @Override
        public void action() {
            try {
                Thread.sleep(30);
            } catch(Exception e) {

            }
            MessageTemplate conversationIDMatch = MessageTemplate.MatchConversationId("proposal on item");
            MessageTemplate proposeOrAccept = MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.PROPOSE),
                    MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL));
            MessageTemplate proposeOrAcceptOrCancel = MessageTemplate.or(proposeOrAccept, MessageTemplate.MatchPerformative(ACLMessage.REFUSE));

            MessageTemplate messageTemplate = MessageTemplate.and(conversationIDMatch, proposeOrAcceptOrCancel);
            ACLMessage incomingMessage = myAgent.receive(messageTemplate);

            if (incomingMessage != null) {
                timeSpent++;
                String[] proposalContent = incomingMessage.getContent().split(":");
                Item wantedItem = isBuyer ? getItemFromWishList(proposalContent[0]) : getItemFromInventory(proposalContent[0]);
                int proposedPrice = Integer.parseInt(proposalContent[1]);
                if (incomingMessage.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                    performTransaction(wantedItem, proposedPrice, isBuyer);
                    endNegotiations();
                } else {
                    if (wantedItem != null) {
                        ACLMessage returnMessage;
                        int proposalUtility;
                        int newProposalUtility;
                        int newProposalPrice;
                        if (isBuyer) {
                            proposalUtility = Utility.getBuyersUtility(wantedItem, proposedPrice);
                            newProposalPrice = Utility.getBuyersNextBid(wishlist, money, wantedItem, timeSpent, totalTimeAllowed);
                            newProposalUtility = Utility.getBuyersUtility(wantedItem, newProposalPrice);
                            System.out.println("--------------BUYER: "+myAgent.getLocalName()+"------------------");
                            System.out.println("Retail price: "+wantedItem.getValue());
                            System.out.println("Sellers proposed price: "+proposedPrice);
                            System.out.println("Buyers new proposal price: "+newProposalPrice);
                            System.out.println("Time spent: "+timeSpent+", of total: "+totalTimeAllowed);
                        }
                        else {
                            proposalUtility = Utility.getSellersUtility(wantedItem);
                            newProposalPrice = Utility.getSellersNextBid(wantedItem, timeSpent, totalTimeAllowed);
                            newProposalUtility = Utility.convertPriceToSellersUtility(newProposalPrice);
                            System.out.println("--------------SELLER: "+myAgent.getLocalName()+"------------------");
                            System.out.println("Retail price: "+wantedItem.getValue());
                            System.out.println("Buyers proposed price: "+proposedPrice);
                            System.out.println("Sellers new proposal price: "+newProposalPrice);
                            System.out.println("Time spent: "+timeSpent+", of total: "+totalTimeAllowed);
                        }

                        if (newProposalUtility < proposalUtility) {
                            System.out.println("This agent was about to propose: " + newProposalPrice + " with utility: " + newProposalUtility + " Both agree to the price: " + proposedPrice + " with utility: " + proposalUtility);
                            returnMessage = createAcceptProposal(incomingMessage, proposedPrice);
                            performTransaction(wantedItem, proposedPrice, isBuyer);
                            endNegotiations();
                        }
                        else if (timeSpent >= totalTimeAllowed) {
                            System.out.println("TIMEOUT");
                            returnMessage = createRefuseMessage(incomingMessage, proposedPrice);
                            endNegotiations();
                        }
                        else {
                            returnMessage = createPropositionMessage(incomingMessage, newProposalPrice);
                        }
                        myAgent.send(returnMessage);
                    }
                    else {
                        timeSpent = 0;
                    }
                }
            }
            else {
                block();
            }
        }

        private void sendNegotiationsEndedMessage() {
            ACLMessage message = new ACLMessage(ACLMessage.INFORM);
            message.setConversationId("NegotiationsEnded");
            message.addReceiver(administrator);
            System.out.println("Sending negotiations ended to: "+administrator.getLocalName());
            myAgent.send(message);
        }

        private void performTransaction(Item item, int price, boolean isBuyer) {
            if (isBuyer) {
                substractMoney(price);
                item.setValue(price);
                inventory.add(item);
            } else {
                addMoney(price);
                inventory.remove(item);
            }
            System.out.println(myAgent.getLocalName() + " has items: "+inventory+", and money: "+money);
        }

        private Item getItemFromInventory(String wantedItemName) {
            for (Item item : inventory) {
                if (item.getName().equals(wantedItemName)) {
                    return item;
                }
            }
            return null;
        }

        private Item getItemFromWishList(String wantedItemName) {
            for (Item item : wishlist) {
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
            return responseMessage;
        }

        private ACLMessage createRefuseMessage(ACLMessage incomingMessage, int oldProposalPrice) {
            ACLMessage responseMessage = incomingMessage.createReply();
            responseMessage.setPerformative(ACLMessage.REFUSE);
            String nameOfItem = incomingMessage.getContent().split(":")[0];
            responseMessage.setContent(nameOfItem + ":" + oldProposalPrice);
            return responseMessage;
        }

        private void endNegotiations() {
            if (isBuyer) {
                sendNegotiationsEndedMessage();
                isBuyer = false;
            }
            timeSpent = 0;
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
            return inventory != null;
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
}
