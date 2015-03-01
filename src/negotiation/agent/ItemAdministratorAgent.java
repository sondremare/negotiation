package negotiation.agent;

import jade.core.AID;
import jade.core.Agent;
import negotiation.agent.behaviour.administering.FindNegotiatingAgents;

import java.util.ArrayList;

public class ItemAdministratorAgent extends Agent {

    private ArrayList<AID> negotiatingAgents;
    private boolean oneAgentFinished = false;
    private int maxMoney = Integer.MIN_VALUE;
    private String winningAgent;

    public ArrayList<AID> getNegotiatingAgents() {
        return negotiatingAgents;
    }

    public void setNegotiatingAgents(ArrayList<AID> negotiatingAgents) {
        this.negotiatingAgents = negotiatingAgents;
    }

    public boolean isOneAgentFinished() {
        return oneAgentFinished;
    }

    public void setOneAgentFinished(boolean oneAgentFinished) {
        this.oneAgentFinished = oneAgentFinished;
    }

    public int getMaxMoney() {
        return maxMoney;
    }

    public void setMaxMoney(int maxMoney) {
        this.maxMoney = maxMoney;
    }

    public String getWinningAgent() {
        return winningAgent;
    }

    public void setWinningAgent(String winningAgent) {
        this.winningAgent = winningAgent;
    }

    @Override
    protected void setup() {
        addBehaviour(new FindNegotiatingAgents());
    }

}
