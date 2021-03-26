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
//            new AgentConfigBuilder()
//                    .addConfig(config, assetListingManager);

            new AgentConfigBuilder("Pastry_1")
                    .addNeededResource(Material.WHEAT, 300)
                    .addOutput(Material.BREAD, 100)
                    .addConfig(config, assetListingManager);
            new AgentConfigBuilder("Pastry_2")
                    .addNeededResource(Material.WHEAT, 200)
                    .addNeededResource(Material.COCOA_BEANS, 100)
                    .addOutput(Material.COOKIE, 800)
                    .addConfig(config, assetListingManager);
            new AgentConfigBuilder("Pastry_3")
                    .addNeededResource(Material.PUMPKIN, 100)
                    .addNeededResource(Material.SUGAR, 100)
                    .addNeededResource(Material.EGG, 100)
                    .addOutput(Material.PUMPKIN_PIE, 100)
                    .addConfig(config, assetListingManager);
            new AgentConfigBuilder("Pastry_4")
                    .addNeededResource(Material.SUGAR, 200)
                    .addNeededResource(Material.EGG, 100)
                    .addNeededResource(Material.WHEAT, 300)
                    .addNeededResource(Material.IRON_INGOT, 9) // represents 3 buckets
                    .addOutput(Material.CAKE, 100)
                    .addConfig(config, assetListingManager);
            new AgentConfigBuilder("Pastry_5")
                    .addNeededResource(Material.GOLD_NUGGET, 800)
                    .addNeededResource(Material.CARROT, 100)
                    .addOutput(Material.GOLDEN_CARROT, 100)
                    .addConfig(config, assetListingManager);
            new AgentConfigBuilder("Pastry_6")
                    .addNeededResource(Material.GOLD_NUGGET, 800)
                    .addNeededResource(Material.MELON_SLICE, 100)
                    .addOutput(Material.GLISTERING_MELON_SLICE, 100)
                    .addConfig(config, assetListingManager);
            new AgentConfigBuilder("Pastry_7")
                    .addNeededResource(Material.POTATO, 100)
                    .addOutput(Material.BAKED_POTATO, 100)
                    .addConfig(config, assetListingManager);
            new AgentConfigBuilder("Pastry_8")
                    .addNeededResource(Material.BEETROOT, 600)
                    .addNeededResource(Material.BOWL, 100) // this is made of easy to get material
                    .addOutput(Material.BEETROOT_SOUP, 100)
                    .addConfig(config, assetListingManager);

            new AgentConfigBuilder("Farmer_1")
                    .addNeededResource(Material.WHEAT_SEEDS, 100)
                    .addOutput(Material.WHEAT, 100)
                    .addConfig(config, assetListingManager);
            new AgentConfigBuilder("Farmer_1_2")
                    .addNeededResource(Material.WHEAT_SEEDS, 100)
                    .addOutput(Material.WHEAT_SEEDS, 150)
                    .addConfig(config, assetListingManager);
            new AgentConfigBuilder("Farmer_2")
                    .addNeededResource(Material.CARROT, 100)
                    .addOutput(Material.CARROT, 150)
                    .addConfig(config, assetListingManager);
            new AgentConfigBuilder("Farmer_3")
                    .addNeededResource(Material.POTATO, 100)
                    .addOutput(Material.POTATO, 150)
                    .addConfig(config, assetListingManager);
            new AgentConfigBuilder("Farmer_4")
                    .addNeededResource(Material.BEETROOT_SEEDS, 100)
                    .addOutput(Material.BEETROOT, 100)
                    .addConfig(config, assetListingManager);
            new AgentConfigBuilder("Farmer_4_2")
                    .addNeededResource(Material.BEETROOT_SEEDS, 100)
                    .addOutput(Material.BEETROOT_SEEDS, 150)
                    .addConfig(config, assetListingManager);
            new AgentConfigBuilder("Farmer_5")
                    .addNeededResource(Material.SWEET_BERRIES, 100)
                    .addOutput(Material.SWEET_BERRIES, 125)
                    .addConfig(config, assetListingManager);
            new AgentConfigBuilder("Farmer_6")
                    .addNeededResource(Material.MELON_SEEDS, 100)
                    .addOutput(Material.MELON_SLICE, 500)
                    .addConfig(config, assetListingManager);
            new AgentConfigBuilder("Farmer_7")
                    .addNeededResource(Material.PUMPKIN_SEEDS, 100)
                    .addOutput(Material.PUMPKIN, 500)
                    .addConfig(config, assetListingManager);
        }
        config.get(SIMULATOR)
                .filter(config::isSection)
                .map(obj -> config.getKeys(obj, false))
                .ifPresent(agentNames -> agentNames.forEach(agentName -> {
                    config.get(SIMULATOR +"."+agentName).ifPresent(agentSection -> {
                        List<Pair<AssetSignature, Double>> resourcedNeeded = new LinkedList<>();
                        List<Pair<AssetSignature, Double>> production = new LinkedList<>();

                        UUID uuid = config.get(agentSection, AGENT_UUID)
                                .map(String.class::cast)
                                .map(UUID::fromString)
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
    public Collection<Agent> getAgents() {
        return Collections.unmodifiableCollection(agentList.values());
    }

    public IBankUserProvider getAgentProvider() {
        return provider;
    }

    private static class AgentConfigBuilder {
        private final String agentName;
        private final UUID uuid;
        private final List<Pair<AssetSignature, Double>> needed = new LinkedList<>();
        private final List<Pair<AssetSignature, Double>> production = new LinkedList<>();

        private AgentConfigBuilder(String agentName) {
            this.agentName = agentName;
            this.uuid = UUID.randomUUID();
        }

        public static AgentConfigBuilder of(String agentName) {
            return new AgentConfigBuilder(agentName);
        }

        public AgentConfigBuilder addNeededResource(AssetSignature signature, double amount) {
            needed.add(Pair.of(signature, amount));
            return this;
        }

        public AgentConfigBuilder addNeededResource(Material material, int amount) {
            return addNeededResource(new ItemStackSignature(material), amount);
        }

        public AgentConfigBuilder addOutput(AssetSignature signature, double amount) {
            production.add(Pair.of(signature, amount));
            return this;
        }

        public AgentConfigBuilder addOutput(Material material, int amount) {
            return addOutput(new ItemStackSignature(material), amount);
        }

        private void addAgentConfig(
                ManagerConfig config,
                AssetListingManager assetListingManager,
                String agentName,
                List<Pair<AssetSignature, Double>> needed,
                List<Pair<AssetSignature, Double>> production) {
            config.put(SIMULATOR + "." + agentName + "." + AGENT_UUID, uuid.toString());

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

        public void addConfig(ManagerConfig config,
                              AssetListingManager assetListingManager) {
            addAgentConfig(config, assetListingManager, agentName, needed, production);
        }
    }
}
