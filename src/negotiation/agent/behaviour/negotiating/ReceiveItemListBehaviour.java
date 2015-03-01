package negotiation.agent.behaviour.negotiating;

import com.sun.org.apache.xpath.internal.operations.Neg;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import negotiation.agent.NegotiatingAgent;
import negotiation.util.Item;
import negotiation.util.ItemFactory;

import java.util.ArrayList;

public class ReceiveItemListBehaviour extends CyclicBehaviour {

    @Override
    public void action() {
        MessageTemplate messageTemplate = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                MessageTemplate.MatchConversationId("ItemList"));
        ACLMessage message = myAgent.receive(messageTemplate);
        if (message != null) {
            String[] itemLists = message.getContent().split(";");
            ArrayList<Item> inventory = ItemFactory.parseItemList(itemLists[0]);
            ArrayList<Item> wishlist = ItemFactory.parseItemList(itemLists[1]);
            ((NegotiatingAgent) myAgent).setInventory(inventory);
            ((NegotiatingAgent) myAgent).setWishlist(wishlist);
            System.out.println(myAgent.getLocalName() + " has items: " + inventory + "  and wants: " + wishlist);
        }
        else {
            block();
        }
    }
}
