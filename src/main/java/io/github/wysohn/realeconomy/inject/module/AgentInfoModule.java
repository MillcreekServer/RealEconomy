package io.github.wysohn.realeconomy.inject.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.github.wysohn.realeconomy.interfaces.simulation.IAgentReloadObserver;
import io.github.wysohn.realeconomy.mediator.SimulationMediator;

public class AgentInfoModule extends AbstractModule {
    @Provides
    @Singleton
    IAgentReloadObserver agentReloadObserver(SimulationMediator simulationMediator) {
        return simulationMediator::cancelAgentOrders;
    }
}
