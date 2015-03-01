package negotiation.agent.behaviour.negotiating;

import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import negotiation.agent.NegotiatingAgent;
import negotiation.util.Item;
import negotiation.util.Utility;

import java.util.ArrayList;
import java.util.Arrays;

public class RespondToProposalBehaviour extends CyclicBehaviour {
    private int timeSpent = 0;

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
            Item wantedItem = ((NegotiatingAgent) myAgent).isBuyer() ? getItemFromWishList(proposalContent[0]) : getItemFromInventory(proposalContent[0]);
            String exchangeItemName = proposalContent[2];
            ArrayList<String> otherAgentInventory = new ArrayList<String>(Arrays.asList(exchangeItemName.split(",")));
            Item sellerWantedItem = findWantedItem(otherAgentInventory);
            if (incomingMessage.getPerformative() == ACLMessage.REFUSE) {
                System.out.println(myAgent.getLocalName() + " got a refusal from " + incomingMessage.getSender().getLocalName());
                endNegotiations();
            } else if (incomingMessage.getPerformative() == ACLMessage.ACCEPT_PROPOSAL) {
                int proposedPrice = Integer.parseInt(proposalContent[1]);
                performTransaction(wantedItem, proposedPrice, ((NegotiatingAgent) myAgent).isBuyer(), sellerWantedItem);
                endNegotiations();
            } else {
                int proposedPrice = Integer.parseInt(proposalContent[1]);
                if (wantedItem != null) {
                    ACLMessage returnMessage;
                    int proposalUtility;
                    int newProposalUtility;
                    int newProposalPrice;
                    if (((NegotiatingAgent) myAgent).isBuyer()) {
                        proposalUtility = Utility.getBuyersUtility(wantedItem, proposedPrice);
                        newProposalPrice = Utility.getBuyersNextBid(((NegotiatingAgent) myAgent).getWishlist(), ((NegotiatingAgent) myAgent).getMoney(), wantedItem, timeSpent, NegotiatingAgent.totalTimeAllowed);
                        if (sellerWantedItem != null) {
                            newProposalPrice -= Utility.getSellersNextBid(sellerWantedItem, timeSpent, NegotiatingAgent.totalTimeAllowed);
                        }
                        newProposalUtility = Utility.getBuyersUtility(wantedItem, newProposalPrice);
                        System.out.println("--------------BUYER: "+myAgent.getLocalName()+"------------------");
                        System.out.println("Item name: "+wantedItem.getName());
                        System.out.println("Retail price: "+wantedItem.getValue());
                        System.out.println("Sellers proposed price: "+proposedPrice);
                        System.out.println("Buyers new proposal price: "+newProposalPrice);
                        System.out.println("Time spent: "+ timeSpent+", of total: "+ NegotiatingAgent.totalTimeAllowed);
                    }
                    else {
                        proposalUtility = Utility.getSellersUtility(proposedPrice);
                        newProposalPrice = Utility.getSellersNextBid(wantedItem, timeSpent, NegotiatingAgent.totalTimeAllowed);
                        if (sellerWantedItem != null) {
                            newProposalPrice -= Utility.getBuyersNextBid(((NegotiatingAgent) myAgent).getWishlist(), ((NegotiatingAgent) myAgent).getMoney(), sellerWantedItem, timeSpent, NegotiatingAgent.totalTimeAllowed);
                        }
                        newProposalUtility = Utility.getSellersUtility(newProposalPrice);
                        System.out.println("--------------SELLER: "+myAgent.getLocalName()+"------------------");
                        System.out.println("Item name: "+wantedItem.getName());
                        System.out.println("Retail price: "+wantedItem.getValue());
                        System.out.println("Buyers proposed price: "+proposedPrice);
                        System.out.println("Sellers new proposal price: "+newProposalPrice);
                        System.out.println("Time spent: "+ timeSpent+", of total: "+ NegotiatingAgent.totalTimeAllowed);
                    }

                    if (newProposalUtility < proposalUtility) {
                        System.out.println("This agent was about to propose: " + newProposalPrice + " with utility: " + newProposalUtility + " Both agree to the price: " + proposedPrice + " with utility: " + proposalUtility);
                        returnMessage = createAcceptProposal(incomingMessage, proposedPrice, sellerWantedItem);
                        performTransaction(wantedItem, proposedPrice, ((NegotiatingAgent) myAgent).isBuyer(), sellerWantedItem);
                        endNegotiations();                        }
                    else if (timeSpent >= NegotiatingAgent.totalTimeAllowed) {
                        returnMessage = createRefuseMessage(incomingMessage);
                        endNegotiations();
                    }
                    else {
                        returnMessage = createPropositionMessage(incomingMessage, newProposalPrice, sellerWantedItem);
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

    private Item findWantedItem(ArrayList<String> otherAgentInventory) {
        for (String itemName : otherAgentInventory) {
            for (Item wantedItem : ((NegotiatingAgent) myAgent).getWishlist()) {
                if (itemName.equals(wantedItem.getName())){
                    return wantedItem;
                }
            }
            for (Item inventoryItem : ((NegotiatingAgent) myAgent).getInventory()) {
                if (itemName.equals(inventoryItem.getName())){
                    return inventoryItem;
                    }
                }
            }
        return null;
    }

    private void sendFinishedMessage() {
        ACLMessage message = new ACLMessage(ACLMessage.INFORM);
        message.setConversationId("Finished");
        message.setContent(String.valueOf(((NegotiatingAgent) myAgent).getMoney()));
        message.addReceiver(((NegotiatingAgent) myAgent).getAdministrator());
        System.out.println("#######################################################################");
        System.out.println("# Name: "+myAgent.getLocalName());
        System.out.println("# Inventory: "+ ((NegotiatingAgent) myAgent).getInventory());
        System.out.println("# WishList: "+ ((NegotiatingAgent) myAgent).getWishlist());
        System.out.println("#######################################################################");
        myAgent.send(message);

    }

    private void performTransaction(Item item, int price, boolean isBuyer, Item sellersWantedItem) {
        if (isBuyer) {
            ((NegotiatingAgent) myAgent).subtractMoney(price);
            item.setValue(price);
            ((NegotiatingAgent) myAgent).getInventory().add(item);
            if (sellersWantedItem != null) {
                ((NegotiatingAgent) myAgent).getInventory().remove(sellersWantedItem);
                System.out.println(" A TRADE WAS COMPLETED!! " + item.getName() + " was traded for " + sellersWantedItem.getName() + " for an added " + price + " money");
            }
        } else {
            ((NegotiatingAgent) myAgent).addMoney(price);
            ((NegotiatingAgent) myAgent).getInventory().remove(item);
            if (sellersWantedItem != null) {
                ((NegotiatingAgent) myAgent).getInventory().add(sellersWantedItem);
            }
        }
        System.out.println(myAgent.getLocalName() + " has items: "+ ((NegotiatingAgent) myAgent).getInventory()+", and money: "+ ((NegotiatingAgent) myAgent).getMoney() + " after transaction.");
    }

    private Item getItemFromInventory(String wantedItemName) {
        for (Item item : ((NegotiatingAgent) myAgent).getInventory()) {
            if (item.getName().equals(wantedItemName)) {
                return item;
            }
        }
        return null;
    }

    private Item getItemFromWishList(String wantedItemName) {
        for (Item item : ((NegotiatingAgent) myAgent).getWishlist()) {
            if (item.getName().equals(wantedItemName)) {
                return item;
            }
        }
        return null;
    }

    private ACLMessage createPropositionMessage(ACLMessage incomingMessage, int newProposalPrice, Item sellerWantedItem)
    {
        ACLMessage responseMessage = incomingMessage.createReply();
        String nameOfItem = incomingMessage.getContent().split(":")[0];
        responseMessage.setContent(nameOfItem + ":" + newProposalPrice + ":" + sellerWantedItem);
        responseMessage.setPerformative(ACLMessage.PROPOSE);
        responseMessage.setConversationId("proposal on item");
        return responseMessage;
    }

    private ACLMessage createAcceptProposal(ACLMessage incomingMessage, int acceptablePrice, Item sellerWantedItem) {
        ACLMessage responseMessage = incomingMessage.createReply();
        responseMessage.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
        String nameOfItem = incomingMessage.getContent().split(":")[0];
        responseMessage.setContent(nameOfItem + ":" + acceptablePrice + ":" + sellerWantedItem);
        return responseMessage;
    }

    private ACLMessage createRefuseMessage(ACLMessage incomingMessage) {
        ACLMessage responseMessage = incomingMessage.createReply();
        responseMessage.setPerformative(ACLMessage.REFUSE);
        String nameOfItem = incomingMessage.getContent().split(":")[0];
        responseMessage.setContent(nameOfItem + ":-:-");
        return responseMessage;
    }

    private void endNegotiations() {
        Item nextWantedItem = ((NegotiatingAgent) myAgent).getRandomWantedItem();
        if (nextWantedItem == null) { //agent has completed its wishlist
            sendFinishedMessage();
        }
        if (((NegotiatingAgent) myAgent).isBuyer()) {
            ((NegotiatingAgent) myAgent).sendNegotiationsEndedMessage(myAgent);
            ((NegotiatingAgent) myAgent).setBuyer(false);
        }
        timeSpent = 0;
    }
}
