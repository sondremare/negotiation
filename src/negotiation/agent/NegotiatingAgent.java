package negotiation.agent;

import jade.core.AID;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import negotiation.agent.behaviour.negotiating.*;
import negotiation.util.Item;

import java.util.ArrayList;

public class NegotiatingAgent extends Agent {
    private ArrayList<Item> inventory;
    private ArrayList<Item> wishlist;
    private int money = 2800;
    private boolean isBuyer = false;
    private AID administrator;

    public static int totalTimeAllowed = 10;

    public ArrayList<Item> getInventory() {
        return inventory;
    }

    public void setInventory(ArrayList<Item> inventory) {
        this.inventory = inventory;
    }

    public ArrayList<Item> getWishlist() {
        return wishlist;
    }

    public void setWishlist(ArrayList<Item> wishlist) {
        this.wishlist = wishlist;
    }

    public AID getAdministrator() {
        return administrator;
    }

    public void setAdministrator(AID administrator) {
        this.administrator = administrator;
    }

    public boolean isBuyer() {
        return isBuyer;
    }

    public void setBuyer(boolean isBuyer) {
        this.isBuyer = isBuyer;
    }

    public int getMoney() {
        return money;
    }

    public void subtractMoney(int money) {
        this.money -= money;
    }

    public void addMoney(int money) {
        this.money += money;
    }


    public Item getNextWantedItem() {
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

    public void sendNegotiationsEndedMessage(Agent agent) {
        ACLMessage message = new ACLMessage(ACLMessage.INFORM);
        message.setConversationId("NegotiationsEnded");
        message.addReceiver(administrator);
        System.out.println(agent.getLocalName()+ " sending negotiations ended to: "+administrator);
        agent.send(message);
    }

    protected void setup() {
        addBehaviour(new RegisterBehaviour(getAID()));
        addBehaviour(new ReceiveItemListBehaviour());
        addBehaviour(new GetInventoryInfoBehaviour());
        addBehaviour(new RespondToInventoryRequestBehaviour());

        addBehaviour(new StartNegotiationBehaviour());
        addBehaviour(new RespondToProposalBehaviour());

    }

}
