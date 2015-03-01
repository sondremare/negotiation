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
                    }
                    else {
                        proposalUtility = Utility.getSellersUtility(proposedPrice);
                        newProposalPrice = Utility.getSellersNextBid(wantedItem, timeSpent, NegotiatingAgent.totalTimeAllowed);
                        if (sellerWantedItem != null) {
                            newProposalPrice -= Utility.getBuyersNextBid(((NegotiatingAgent) myAgent).getWishlist(), ((NegotiatingAgent) myAgent).getMoney(), sellerWantedItem, timeSpent, NegotiatingAgent.totalTimeAllowed);
                        }
                        newProposalUtility = Utility.getSellersUtility(newProposalPrice);
                    }

                    if (newProposalUtility < proposalUtility) {
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
        myAgent.send(message);

    }

    private void performTransaction(Item item, int price, boolean isBuyer, Item sellersWantedItem) {
        if (isBuyer) {
            ((NegotiatingAgent) myAgent).subtractMoney(price);
            item.setValue(price);
            ((NegotiatingAgent) myAgent).getInventory().add(item);
            if (sellersWantedItem != null) {
                ((NegotiatingAgent) myAgent).getInventory().remove(sellersWantedItem);
            }
        } else {
            ((NegotiatingAgent) myAgent).addMoney(price);
            ((NegotiatingAgent) myAgent).getInventory().remove(item);
            if (sellersWantedItem != null) {
                ((NegotiatingAgent) myAgent).getInventory().add(sellersWantedItem);
            }
        }
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
