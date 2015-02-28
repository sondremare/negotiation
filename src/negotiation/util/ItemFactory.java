package negotiation.util;

import jade.core.AID;

import java.util.*;

public class ItemFactory {
    private static int MIN_PER_AGENT = 10;
    private static int MAX_PER_AGENT = 20;
    private static int MIN_ITEM_VALUE = 200;
    private static int MAX_ITEM_VALUE = 1000;

    public static ArrayList<Item> createItems(ArrayList<AID> agents) {
        ArrayList<Item> itemList = new ArrayList<Item>();
        Random random = new Random();
        int numberOfItemsToGenerate = (random.nextInt((MAX_PER_AGENT - MIN_PER_AGENT) + 1) + MIN_PER_AGENT) * agents.size();
        for (int i = 0; i < numberOfItemsToGenerate; i++) {
            int value = random.nextInt((MAX_ITEM_VALUE - MIN_ITEM_VALUE) + 1) + MIN_ITEM_VALUE;
            Item item = new Item("Item"+i, value);
            itemList.add(item);
        }
        return itemList;
    }

    public static String[] getItemsForAgents(ArrayList<AID> agents) {
        /** Assigning items at random to agents */
        ArrayList<Item> itemList = createItems(agents);
        HashMap<Integer, ArrayList<Item>> assignedItems = new HashMap<Integer, ArrayList<Item>>();
        Collections.shuffle(itemList);
        for (int i = 0; i < itemList.size(); i++) {
            for (int j = 0; j < agents.size(); j++) {
                if (i % agents.size() == j) {
                    if (assignedItems.size() == 0 || assignedItems.get(j) == null) {
                        ArrayList<Item> items = new ArrayList<Item>();
                        items.add(itemList.get(i));
                        assignedItems.put(j, items);
                    } else {
                        assignedItems.get(j).add(itemList.get(i));
                    }
                }
            }
        }

        /** Randomize the itemList, and assign wishes to agents */
        HashMap<Integer, ArrayList<Item>> wishedItems = new HashMap<Integer, ArrayList<Item>>();
        Collections.shuffle(itemList);
        for (int k = 0; k < itemList.size(); k++) {
            for (int l = 0; l < agents.size(); l++) {
                if (k % agents.size() == l) {
                    if (wishedItems.size() == 0 || wishedItems.get(l) == null) {
                        ArrayList<Item> items = new ArrayList<Item>();
                        items.add(itemList.get(k));
                        wishedItems.put(l, items);
                    } else {
                        wishedItems.get(l).add(itemList.get(k));
                    }
                }
            }
        }

        /** Iterate the assignedItems and wishedItems, and stringify them */
        String[] itemStrings = new String[agents.size()];
        for (int m = 0; m < agents.size(); m++) {
            itemStrings[m] = stringifyItemList(assignedItems.get(m)) + ";"+stringifyItemList(wishedItems.get(m));
        }
        return itemStrings;
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

    public static ArrayList<Item> parseItemList(String stringifiedList) {
        ArrayList<Item> itemList = new ArrayList<Item>();
        String[] items = stringifiedList.split(",");
        for (int i = 0; i < items.length; i++) {
            itemList.add(parseItem(items[i]));
        }
        return itemList;
    }

    public static String stringifyItem(Item item) {
        return item.getName() + ":" +item.getValue();
    }

    public static Item parseItem(String item) {
        String[] itemValues = item.split(":");
        return new Item(itemValues[0], Integer.parseInt(itemValues[1]));
    }
}
