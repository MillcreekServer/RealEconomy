package io.github.wysohn.realeconomy.manager.simulation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginLogger;
import io.github.wysohn.rapidframework3.core.main.Manager;
import io.github.wysohn.rapidframework3.core.main.ManagerConfig;
import io.github.wysohn.rapidframework3.utils.Pair;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUserProvider;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.asset.signature.ItemStackSignature;
import io.github.wysohn.realeconomy.manager.listing.AssetListing;
import io.github.wysohn.realeconomy.manager.listing.AssetListingManager;
import org.bukkit.Material;

import java.lang.ref.Reference;
import java.util.*;
import java.util.logging.Logger;

@Singleton
public class MarketSimulationManager extends Manager {
    public static final String SIMULATOR = "simulator";
    public static final String AGENT_UUID = "uuid";
    public static final String RESOURCES_NEEDED = "resourcesNeeded";
    public static final String PRODUCTION = "production";

    private final Map<UUID, Agent> agentList = new HashMap<>();

    private final ManagerConfig config;
    private final Logger logger;
    private final AssetListingManager assetListingManager;

    private final IBankUserProvider provider = agentList::get;

    @Inject
    public MarketSimulationManager(ManagerConfig config,
                                   @PluginLogger Logger logger,
                                   AssetListingManager assetListingManager){
        this.config = config;
        this.logger = logger;
        this.assetListingManager = assetListingManager;

        dependsOn(AssetListingManager.class);
    }

    private void addAgentConfig(String agentName,
                                List<Pair<AssetSignature, Double>> needed,
                                List<Pair<AssetSignature, Double>> production) {
        config.put(SIMULATOR + "." + agentName + "." + AGENT_UUID, UUID.randomUUID());

        needed.forEach(pair -> {
            AssetSignature sign = pair.key;
            double amount = pair.value;

            assetListingManager.newListing(sign);
            UUID uuid = assetListingManager.signatureToUuid(sign);
            config.put(SIMULATOR + "." + agentName + "." + RESOURCES_NEEDED
                    + "." + uuid, amount);
        });

        production.forEach(pair -> {
            AssetSignature sign = pair.key;
            double amount = pair.value;

            assetListingManager.newListing(sign);
            UUID uuid = assetListingManager.signatureToUuid(sign);
            config.put(SIMULATOR + "." + agentName + "." + PRODUCTION
                    + "." + uuid, amount);
        });
    }

    @Override
    public void enable() throws Exception {

    }

    @Override
    public void load() throws Exception {
        agentList.clear();

                        /*
        simulator:
          <agentName>:
            uuid: <UUID>
            resourcesNeeded:
              <UUID>: 22
              <UUID>: 5
            production:
              <UUID>: 2
         */
        if (!config.get(SIMULATOR).isPresent()) {
            addAgentConfig("Pastry_1",
                    new LinkedList<Pair<AssetSignature, Double>>() {{
                        add(Pair.of(new ItemStackSignature(Material.WHEAT), 300.0));
                    }},
                    new LinkedList<Pair<AssetSignature, Double>>() {{
                        add(Pair.of(new ItemStackSignature(Material.BREAD), 100.0));
                    }});
            addAgentConfig("Pastry_2",
                    new LinkedList<Pair<AssetSignature, Double>>() {{
                        add(Pair.of(new ItemStackSignature(Material.WHEAT), 200.0));
                        add(Pair.of(new ItemStackSignature(Material.COCOA_BEANS), 100.0));
                    }},
                    new LinkedList<Pair<AssetSignature, Double>>() {{
                        add(Pair.of(new ItemStackSignature(Material.COOKIE), 800.0));
                    }});
        }
        config.get(SIMULATOR)
                .filter(config::isSection)
                .map(obj -> config.getKeys(obj, false))
                .ifPresent(agentNames -> agentNames.forEach(agentName -> {
                    config.get(SIMULATOR +"."+agentName).ifPresent(agentSection -> {
                        List<Pair<AssetSignature, Double>> resourcedNeeded = new LinkedList<>();
                        List<Pair<AssetSignature, Double>> production = new LinkedList<>();

                        UUID uuid = config.get(agentSection, AGENT_UUID)
                                .map(UUID.class::cast)
                                .orElseThrow(RuntimeException::new);

                        config.get(agentSection, RESOURCES_NEEDED).ifPresent(resourcesSection -> {
                            config.getKeys(resourcesSection, false).forEach(uuidKey -> {
                                UUID listingUuid = UUID.fromString(uuidKey);
                                double amount = config.get(resourcesSection, uuidKey)
                                        .map(Number.class::cast)
                                        .map(Number::doubleValue)
                                        .orElse(0.0);

                                if (amount <= 0.0)
                                    return;

                                Optional.of(listingUuid)
                                        .flatMap(assetListingManager::get)
                                        .map(Reference::get)
                                        .map(AssetListing::getSignature)
                                        .ifPresent(signature -> resourcedNeeded.add(Pair.of(signature, amount)));
                            });
                        });

                        config.get(agentSection, PRODUCTION).ifPresent(productionSection -> {
                            config.getKeys(productionSection, false).forEach(uuidKey -> {
                                UUID listingUuid = UUID.fromString(uuidKey);
                                double amount = config.get(productionSection, uuidKey)
                                        .map(Number.class::cast)
                                        .map(Number::doubleValue)
                                        .orElse(0.0);

                                if (amount <= 0.0)
                                    return;

                                Optional.of(listingUuid)
                                        .flatMap(assetListingManager::get)
                                        .map(Reference::get)
                                        .map(AssetListing::getSignature)
                                        .ifPresent(signature -> production.add(Pair.of(signature, amount)));
                            });
                        });

                        agentList.put(uuid, new Agent(logger,
                                uuid,
                                agentName,
                                resourcedNeeded,
                                production
                        ));
                    });
                }));

        logger.info(agentList.size()+" Market Simulation Agents are active");
    }

    @Override
    public void disable() throws Exception {

    }

    /**
     * Get read-only collection of Agents.
     * @return
     */
    public Collection<Agent> getAgents(){
        return Collections.unmodifiableCollection(agentList.values());
    }

    public IBankUserProvider getAgentProvider() {
        return provider;
    }


}
