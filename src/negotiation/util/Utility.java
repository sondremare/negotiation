package negotiation.util;

import java.util.ArrayList;

public class Utility {

    /** The buyers utility is multiplied with 2, because the most desired state is to own all wanted items */
    public static int getBuyersUtility(Item item, int bid) {
        return 2 * item.getValue() - bid;
    }

    public static int getSellersUtility(Item item) {
        return item.getValue();
    }

    public static int convertPriceToBuyersUtility(int price) {
        return price * 2;
    }

    public static int convertPriceToSellersUtility(int price) {
        return price;
    }

    /** Gets the buyers next bid based on a conceeder strategy **/
    public static int getBuyersNextBid(ArrayList<Item> wishList, int remainingMoney, Item item, int time, int totalTime) {
        int recommendedPriceForAllWishedItems = 0;
        for (Item wishedItem : wishList) {
            recommendedPriceForAllWishedItems += wishedItem.getValue();
        }
        double maxBidFactor = ((double)recommendedPriceForAllWishedItems / remainingMoney);
        double buyersMinimumBid = 0;
        double buyersMaximumBid = maxBidFactor * item.getValue();
        return (int)(buyersMinimumBid + (buyersMaximumBid - buyersMinimumBid) * Math.pow((double)(time/totalTime), 1/Math.E));
    }

    /** Gets the sellers next bid based on a boulware strategy **/
    public static int getSellersNextBid(Item item, int time, int totalTime) {
        int buyersMinimumBid = (int)(0.5 * item.getValue());
        int buyersMaximumBid = 2 * item.getValue();
        return (int)(buyersMinimumBid + (buyersMaximumBid - buyersMinimumBid) * Math.pow(1 - (double)(time/totalTime), 1/Math.E));
    }
}
