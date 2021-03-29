package io.github.wysohn.realeconomy.interfaces.simulation;

import io.github.wysohn.realeconomy.manager.simulation.Agent;

import java.util.Collection;

public interface IAgentReloadObserver {

    void beforeAgentReload(Collection<Agent> agents);
}
