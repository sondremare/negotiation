package negotiation;

import jade.core.AID;
import negotiation.util.ItemFactory;

import java.util.ArrayList;

public class Main {
    public static void main(String[] args) {
        ArrayList<AID> agents = new ArrayList<AID>();
        agents.add(new AID());
        agents.add(new AID());
        agents.add(new AID());
        String[] items = ItemFactory.getItemsForAgents(agents);
        for (int i = 0; i < items.length; i++) {
            System.out.println(items[i]);
        }
    }

}
