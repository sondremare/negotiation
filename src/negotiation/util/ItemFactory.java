package negotiation.util;

import jade.core.AID;
import negotiation.agent.NegotiatingAgent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

public class ItemFactory {
    private static int MAX_PER_AGENT = 10;
    private static int MIN_PER_AGENT = 1;

    public static ArrayList<Item> getItemList(ArrayList<AID> agents) {
        ArrayList<Item> itemList = new ArrayList<Item>();
        Random random = new Random();
        int numberOfItemsToGenerate = random.nextInt((MAX_PER_AGENT - MIN_PER_AGENT) + 1 + MIN_PER_AGENT) * agents.size();
        for (int i = 0; i < numberOfItemsToGenerate; i++) {
            Item item = new Item("Item"+i);
            itemList.add(item);
        }
        return itemList;
    }

    public static String stringifyItemList(ArrayList<Item> itemList) {
        String stringifiedList = "";
        for (int i = 0; i < itemList.size(); i++) {
            stringifiedList += stringifyItem(itemList.get(i));
            boolean lastItem = i == itemList.size() - 1;
            if (!lastItem) {
                stringifiedList += ",";
            }
        }
        return stringifiedList;
    }

    public static String parseItemList(String stringifiedList) {
        ArrayList<Item> itemList = new ArrayList<Item>();

    }

    public static String stringifyItem(Item item) {
        return item.getName();
    }

    public static Item parseItem(String item) {
        return new Item(item);
    }
}
