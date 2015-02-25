package negotiation.agent;

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

import java.util.ArrayList;

public class NegotiatingAgent extends Agent {
    private ArrayList<Item> inventory;
    private ArrayList<Item> wishlist;
    private double money = 2000;

    protected void setup() {
        addBehaviour(new RegisterBehaviour());
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
            addBehaviour(new ReceiveItemListBehaviour());
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
            }
        }
    }
}
