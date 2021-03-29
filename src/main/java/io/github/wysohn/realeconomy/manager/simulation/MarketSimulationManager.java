package io.github.wysohn.realeconomy.manager.simulation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginLogger;
import io.github.wysohn.rapidframework3.core.main.Manager;
import io.github.wysohn.rapidframework3.core.main.ManagerConfig;
import io.github.wysohn.rapidframework3.utils.Pair;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.realeconomy.interfaces.banking.IBankUserProvider;
import io.github.wysohn.realeconomy.interfaces.listing.IListingInfoProvider;
import io.github.wysohn.realeconomy.interfaces.simulation.IAgentReloadObserver;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.asset.signature.ItemStackSignature;
import io.github.wysohn.realeconomy.manager.listing.AssetListingManager;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;

import java.util.*;
import java.util.logging.Logger;

@Singleton
public class MarketSimulationManager extends Manager {
    public static final String SIMULATOR = "simulator";
    private static final Random RAND = new Random();
    private static final Set<Material> WOODS = new HashSet<>();
    private static final Set<Material> LOGS = new HashSet<>();

    private final Map<UUID, Agent> agentList = new HashMap<>();

    private final ManagerConfig config;
    private final Logger logger;
    private final IListingInfoProvider assetInfoProvider;
    private final IAgentReloadObserver agentReloadObserver;

    private final IBankUserProvider provider = agentList::get;

    @Inject
    public MarketSimulationManager(ManagerConfig config,
                                   @PluginLogger Logger logger,
                                   IListingInfoProvider assetInfoProvider,
                                   IAgentReloadObserver agentReloadObserver) {
        this.config = config;
        this.logger = logger;
        this.assetInfoProvider = assetInfoProvider;
        this.agentReloadObserver = agentReloadObserver;

        dependsOn(AssetListingManager.class);

        for (Material value : Material.values()) {
            if (value.name().startsWith("LEGACY"))
                continue;

            if (value.name().endsWith("_WOOD"))
                WOODS.add(value);
        }
        for (Material value : Material.values()) {
            if (value.name().startsWith("LEGACY"))
                continue;

            if (value.name().endsWith("_LOG"))
                LOGS.add(value);
        }
    }

    @Override
    public void enable() throws Exception {

    }

    private void addAgent(Agent agent) {
        agentList.put(agent.getUuid(), agent);
    }

    @Override
    public void load() throws Exception {
        agentReloadObserver.beforeAgentReload(getAgents());

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

        agentList.clear();
        if (!config.get(SIMULATOR).isPresent()) {
//            addAgent(new AgentConfigBuilder("")
//                    .build(logger, config, assetInfoProvider));

            addAgent(new AgentConfigBuilder("Pastry_1")
                    .addNeededResource(Material.WHEAT, 300)
                    .addOutput(Material.BREAD, 100)
                    .build(logger, config, assetInfoProvider));
            addAgent(new AgentConfigBuilder("Pastry_2")
                    .addNeededResource(Material.WHEAT, 200)
                    .addNeededResource(Material.COCOA_BEANS, 100)
                    .addOutput(Material.COOKIE, 800)
                    .build(logger, config, assetInfoProvider));
            addAgent(new AgentConfigBuilder("Pastry_3")
                    .addNeededResource(Material.PUMPKIN, 100)
                    .addNeededResource(Material.SUGAR, 100)
                    .addNeededResource(Material.EGG, 100)
                    .addOutput(Material.PUMPKIN_PIE, 100)
                    .build(logger, config, assetInfoProvider));
            addAgent(new AgentConfigBuilder("Pastry_4")
                    .addNeededResource(Material.SUGAR, 200)
                    .addNeededResource(Material.EGG, 100)
                    .addNeededResource(Material.WHEAT, 300)
                    .addNeededResource(Material.IRON_INGOT, 9) // represents 3 buckets
                    .addOutput(Material.CAKE, 100)
                    .build(logger, config, assetInfoProvider));
            addAgent(new AgentConfigBuilder("Pastry_5")
                    .addNeededResource(Material.GOLD_NUGGET, 800)
                    .addNeededResource(Material.CARROT, 100)
                    .addOutput(Material.GOLDEN_CARROT, 100)
                    .build(logger, config, assetInfoProvider));
            addAgent(new AgentConfigBuilder("Pastry_6")
                    .addNeededResource(Material.GOLD_NUGGET, 800)
                    .addNeededResource(Material.MELON_SLICE, 100)
                    .addOutput(Material.GLISTERING_MELON_SLICE, 100)
                    .build(logger, config, assetInfoProvider));
            addAgent(new AgentConfigBuilder("Pastry_7")
                    .addNeededResource(Material.POTATO, 100)
                    .addOutput(Material.BAKED_POTATO, 100)
                    .build(logger, config, assetInfoProvider));
            addAgent(new AgentConfigBuilder("Pastry_8")
                    .addNeededResource(Material.BEETROOT, 600)
                    .addNeededResource(Material.BOWL, 100) // this is made of easy to get material
                    .addOutput(Material.BEETROOT_SOUP, 100)
                    .build(logger, config, assetInfoProvider));

            addAgent(new AgentConfigBuilder("Farmer_1")
                    .addNeededResource(Material.WHEAT_SEEDS, 100)
                    .addOutput(Material.WHEAT, 100)
                    .build(logger, config, assetInfoProvider));
            addAgent(new AgentConfigBuilder("Farmer_1_2")
                    .addNeededResource(Material.WHEAT_SEEDS, 100)
                    .addOutput(Material.WHEAT_SEEDS, 150)
                    .build(logger, config, assetInfoProvider));
            addAgent(new AgentConfigBuilder("Farmer_2")
                    .addNeededResource(Material.CARROT, 100)
                    .addOutput(Material.CARROT, 150)
                    .build(logger, config, assetInfoProvider));
            addAgent(new AgentConfigBuilder("Farmer_3")
                    .addNeededResource(Material.POTATO, 100)
                    .addOutput(Material.POTATO, 150)
                    .build(logger, config, assetInfoProvider));
            addAgent(new AgentConfigBuilder("Farmer_4")
                    .addNeededResource(Material.BEETROOT_SEEDS, 100)
                    .addOutput(Material.BEETROOT, 100)
                    .build(logger, config, assetInfoProvider));
            addAgent(new AgentConfigBuilder("Farmer_4_2")
                    .addNeededResource(Material.BEETROOT_SEEDS, 100)
                    .addOutput(Material.BEETROOT_SEEDS, 150)
                    .build(logger, config, assetInfoProvider));
            addAgent(new AgentConfigBuilder("Farmer_5")
                    .addNeededResource(Material.SWEET_BERRIES, 100)
                    .addOutput(Material.SWEET_BERRIES, 125)
                    .build(logger, config, assetInfoProvider));
            addAgent(new AgentConfigBuilder("Farmer_6")
                    .addNeededResource(Material.MELON_SEEDS, 100)
                    .addOutput(Material.MELON_SLICE, 500)
                    .build(logger, config, assetInfoProvider));
            addAgent(new AgentConfigBuilder("Farmer_7")
                    .addNeededResource(Material.PUMPKIN_SEEDS, 100)
                    .addOutput(Material.PUMPKIN, 500)
                    .build(logger, config, assetInfoProvider));

            addAgent(new AgentConfigBuilder("Librarian_1")
                    .addNeededResource(Material.PAPER, 300)
                    .addNeededResource(Material.LEATHER, 100)
                    .addOutput(Material.BOOK, 100)
                    .build(logger, config, assetInfoProvider));
            addAgent(new AgentConfigBuilder("Librarian_2")
                    .addNeededResource(Material.PAPER, 100)
                    .addNeededResource(Material.DIAMOND, 200)
                    .addNeededResource(Material.OBSIDIAN, 400)
                    .addOutput(Material.ENCHANTING_TABLE, 100)
                    .build(logger, config, assetInfoProvider));
            addAgent(new AgentConfigBuilder("Librarian_3")
                    .addNeededResource(Material.PAPER, 300)
                    .addNeededResource(Material.LEATHER, 100)
                    .addOutput(Material.BOOK, 100)
                    .build(logger, config, assetInfoProvider));
            addAgent(new AgentConfigBuilder("Librarian_4")
                    .addNeededResource(Material.IRON_INGOT, 40)
                    .addNeededResource(Material.REDSTONE, 10)
                    .addOutput(Material.COMPASS, 10)
                    .build(logger, config, assetInfoProvider));
            addAgent(new AgentConfigBuilder("Librarian_5")
                    .addNeededResource(Material.GOLD_INGOT, 40)
                    .addNeededResource(Material.REDSTONE, 10)
                    .addOutput(Material.CLOCK, 10)
                    .build(logger, config, assetInfoProvider));
            for (Material wood : WOODS) {
                addAgent(new AgentConfigBuilder("Librarian_BookShelf_" + wood)
                        .addNeededResource(wood, 60)
                        .addNeededResource(Material.BOOK, 30)
                        .addOutput(Material.BOOKSHELF, 10)
                        .build(logger, config, assetInfoProvider));
                addAgent(new AgentConfigBuilder("Librarian_NameTag_" + wood)
                        .addNeededResource(wood, 20)
                        .addNeededResource(Material.STRING, 10)
                        .addNeededResource(Material.DIAMOND, 1)
                        .addOutput(Material.NAME_TAG, 10)
                        .build(logger, config, assetInfoProvider));
            }
            for (Enchantment ench : Enchantment.values()) {
                ItemStack enchBook = new ItemStack(Material.ENCHANTED_BOOK);
                EnchantmentStorageMeta esm = (EnchantmentStorageMeta) enchBook.getItemMeta();
                esm.addStoredEnchant(ench, ench.getStartLevel()
                                + RAND.nextInt(ench.getMaxLevel() - ench.getStartLevel() + 1),
                        false);
                enchBook.setItemMeta(esm);

                addAgent(new AgentConfigBuilder("Librarian_E_" + ench)
                        .addNeededResource(Material.BOOK, 10)
                        .addNeededResource(Material.EMERALD, 5)
                        .addNeededResource(Material.DIAMOND, 5)
                        .addNeededResource(Material.GOLD_INGOT, 5)
                        .addNeededResource(Material.LAPIS_LAZULI, 10)
                        .addOutput(enchBook, 10)
                        .build(logger, config, assetInfoProvider));
            }

            addAgent(new AgentConfigBuilder("Toolsmith_1")
                    .addNeededResource(Material.GOLD_INGOT, 25)
                    .addNeededResource(Material.STICK, 10)
                    .addNeededResource(Material.DIAMOND, 5)
                    .addOutput(Material.BELL, 5)
                    .build(logger, config, assetInfoProvider));

        } else {
            config.get(SIMULATOR)
                    .filter(config::isSection)
                    .map(section -> Agent.readAll(config,
                            logger,
                            assetInfoProvider,
                            section))
                    .ifPresent(agentCollection -> agentCollection.forEach(this::addAgent));
        }

        logger.info(agentList.size() + " Market Simulation Agents are active");
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
            Validation.assertNotNull(agentName);
            Validation.validate(agentName, name -> name.length() > 0, "name cannot be empty.");

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

        public AgentConfigBuilder addOutput(ItemStack itemStack, int amount) {
            return addOutput(new ItemStackSignature(itemStack), amount);
        }

        public AgentConfigBuilder addOutput(Material material, int amount) {
            return addOutput(new ItemStackSignature(material), amount);
        }

        public Agent build(Logger logger,
                           ManagerConfig config,
                           IListingInfoProvider assetInfoProvider) {
            Validation.validate(needed, val -> val.size() > 0, "empty needed resources.");
            Validation.validate(production, val -> val.size() > 0, "empty production outputs.");

            Agent agent = new Agent(logger,
                    uuid,
                    agentName,
                    needed,
                    production);

            ConfigurationSection section = config.get(SIMULATOR)
                    .map(ConfigurationSection.class::cast)
                    .orElseGet(YamlConfiguration::new);
            agent.write(section, assetInfoProvider);
            config.put(SIMULATOR, section);

            return agent;
        }
    }
}
