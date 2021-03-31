package io.github.wysohn.realeconomy.manager.business.types;

import com.google.inject.Inject;
import io.github.wysohn.rapidframework3.core.caching.CachedElement;
import io.github.wysohn.rapidframework3.core.language.ManagerLanguage;
import io.github.wysohn.rapidframework3.interfaces.ICommandSender;
import io.github.wysohn.rapidframework3.interfaces.IMemento;
import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.rapidframework3.utils.Pair;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.realeconomy.interfaces.business.IBusiness;
import io.github.wysohn.realeconomy.interfaces.business.tiers.ITier;
import io.github.wysohn.realeconomy.interfaces.business.upgrades.IUpgrade;
import io.github.wysohn.realeconomy.main.RealEconomyLangs;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.asset.signature.DurationSignature;
import io.github.wysohn.realeconomy.manager.banking.AssetUtil;
import io.github.wysohn.realeconomy.manager.business.upgrades.UpgradeRegistry;
import io.github.wysohn.realeconomy.manager.listing.AssetListingManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractBusiness extends CachedElement<UUID> implements IBusiness {
    private static final Random RANDOM = new Random();
    public static final DurationSignature DURATION_SIGNATURE = new DurationSignature();

    @Inject
    private transient AssetListingManager assetListingManager;

    private transient Map<AssetSignature, Double> requirements;
    private transient Map<AssetSignature, Double> inputs;
    private transient Map<AssetSignature, Double> outputs;

    private final List<Asset> ownedAssets = Collections.synchronizedList(new ArrayList<>());
    private final Map<AssetSignature, Double> currentProgress = new ConcurrentHashMap<>();
    private final Map<AssetSignature, Double> productionStorage = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> upgrades = new ConcurrentHashMap<>();

    private final Map<String, String> stringStore = new ConcurrentHashMap<>();
    private UUID ownerUuid;
    private ITier tier;
    private String subType = ITier.DEFAULT_SUB_TYPE;
    private long establishmentTime;
    private boolean established;
    private long timeToLive = 0;

    public AbstractBusiness(UUID key) {
        super(key);
        this.establishmentTime = -1L;
        this.established = false;
    }

    @Override
    public UUID getUuid() {
        return getKey();
    }

    @Override
    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    @Override
    public void setOwnerUuid(UUID ownerUuid) {
        Validation.assertNotNull(ownerUuid);
        this.ownerUuid = ownerUuid;

        notifyObservers();
    }

    @Override
    public ITier currentTier() {
        return tier;
    }

    @Override
    public void replaceTier(ITier tier) {
        Validation.assertNotNull(tier);
        this.tier = tier;

        notifyObservers();
    }

    public String getSubType() {
        return subType;
    }

    public void setSubType(String subType) {
        Validation.assertNotNull(subType);
        this.subType = subType;

        notifyObservers();
    }

    @Override
    public boolean isEstablished() {
        return established;
    }

    @Override
    public boolean endOfLife() {
        if (establishmentTime < 0 || timeToLive <= 0)
            return false;

        return System.currentTimeMillis() > establishmentTime + timeToLive;
    }

    @Override
    public void addAsset(Asset asset) {
        AssetUtil.addAsset(ownedAssets, asset);

        notifyObservers();
    }

    @Override
    public double countAsset(AssetSignature signature) {
        return AssetUtil.countAsset(ownedAssets, signature);
    }

    @Override
    public Collection<Asset> removeAsset(AssetSignature signature, double amount) {
        Collection<Asset> assets = AssetUtil.removeAsset(ownedAssets, signature, amount);
        if (assets.size() > 0)
            notifyObservers();
        return assets;
    }

    @Override
    public Asset removeAsset(int index) {
        Asset removeAsset = AssetUtil.removeAsset(ownedAssets, index);
        if (removeAsset != null)
            notifyObservers();
        return removeAsset;
    }

    @Override
    public DataProvider<Asset> assetDataProvider() {
        return AssetUtil.assetDataProvider(ownedAssets);
    }

    @Override
    public Map<AssetSignature, Double> getCurrentProgress() {
        Map<AssetSignature, Double> copy = new HashMap<>();
        synchronized (currentProgress) {
            currentProgress.forEach(copy::put);
        }
        return copy;
    }

    @Override
    public double getProgress(AssetSignature signature, boolean asRatio) {
        synchronized (currentProgress) {
            if (asRatio) {
                double outOf = requirements.getOrDefault(signature, 0.0);
                if (outOf <= 0.0)
                    return 0.0;

                return currentProgress.getOrDefault(signature, 0.0) / outOf;
            } else {
                return currentProgress.getOrDefault(signature, 0.0);
            }
        }
    }

    @Override
    public void setProgress(Pair<AssetSignature, Double>... progresses) {
        synchronized (currentProgress) {
            for (Pair<AssetSignature, Double> progress : progresses) {
                currentProgress.put(progress.key, progress.value);
            }
        }
        notifyObservers();
    }

    @Override
    public Map<AssetSignature, Double> getProductionMaterials() {
        Map<AssetSignature, Double> copy = new HashMap<>();
        synchronized (productionStorage) {
            productionStorage.forEach(copy::put);
        }
        return copy;
    }

    @Override
    public double getProductionMaterial(AssetSignature signature, boolean asRatio) {
        synchronized (productionStorage) {
            if (asRatio) {
                double outOf = inputs.getOrDefault(signature, 0.0);
                if (outOf <= 0.0)
                    return 0.0;

                return productionStorage.getOrDefault(signature, 0.0) / outOf;
            } else {
                return productionStorage.getOrDefault(signature, 0.0);
            }
        }
    }

    @Override
    public void setProductionMaterial(Pair<AssetSignature, Double>... values) {
        synchronized (productionStorage) {
            for (Pair<AssetSignature, Double> value : values) {
                productionStorage.put(value.key, value.value);
            }
        }
        notifyObservers();
    }

    @Override
    public Map<IUpgrade, Integer> getUpgrades() {
        Map<IUpgrade, Integer> copy = new HashMap<>();
        upgrades.forEach((uuid, level) -> Optional.of(uuid)
                .map(UpgradeRegistry::fromUuid)
                .ifPresent(upgrade -> copy.put(upgrade, level)));
        return copy;
    }

    @Override
    public int getUpgrade(IUpgrade upgrade) {
        Validation.assertNotNull(upgrade);
        return upgrades.getOrDefault(upgrade.getUuid(), 0);
    }

    @Override
    public void setUpgrade(IUpgrade upgrade, int level) {
        Validation.assertNotNull(upgrade);
        upgrades.put(upgrade.getUuid(), level);

        notifyObservers();
    }

    @Override
    public String getData(String key) {
        synchronized (stringStore) {
            return stringStore.get(key);
        }
    }

    @Override
    public boolean hasData(String key) {
        synchronized (stringStore) {
            return stringStore.containsKey(key);
        }
    }

    @Override
    public void putData(String key, String value) {
        synchronized (stringStore) {
            stringStore.put(key, value);
        }

        notifyObservers();
    }

    @Override
    public Map<Object, Object> properties(ManagerLanguage lang, ICommandSender sender) {
        /*
        tier: mining
        subType: default
        progress: --
          diamond [25.0 / 50.0] (50%)
          duration [1.0 / 100.0] (1%)
        inputs: --
          cobble 15.0/s
          labour 120.0/s
        outputs: --
          stone 20.0/s
         */
        Map<Object, Object> map = new LinkedHashMap<>();
        map.put(RealEconomyLangs.Business_Tier, tier.displayName(sender));
        synchronized (currentProgress) {
            if (!established) {
                Map<Object, Object> progressMap = new LinkedHashMap<>();
                map.put(RealEconomyLangs.Business_Progress, progressMap);
                requirements.forEach((sign, require) -> {
                    double current = currentProgress.getOrDefault(sign, 0.0);
                    double percentage = require <= 0.0 ? 0.0 : current / require;
                    progressMap.put(sign, String.format("&8[&b%.2f &8/ &3%.2f&8] &8(&e%.2f%%&8)",
                            current, require, percentage));
                });
            }
        }

        if (established) {
            Map<Object, Object> inputMap = new LinkedHashMap<>();
            map.put(RealEconomyLangs.Business_Input, inputMap);
            inputs.forEach((sign, inputVal) -> inputMap.put(sign, String.format("&c%.2f&8/&6s", inputVal)));

            Map<Object, Object> outputMap = new LinkedHashMap<>();
            map.put(RealEconomyLangs.Business_Output, outputMap);
            outputs.forEach((sign, outputVal) -> outputMap.put(sign, String.format("&a%.2f&8/&6s", outputVal)));
        }

        return map;
    }

    @Override
    public void init() {
        requirements = tier.requirement(subType).getAll(assetListingManager);
        inputs = tier.inputs(subType).getAll(assetListingManager);
        outputs = tier.outputs(subType).getAll(assetListingManager);
        timeToLive = getRandomTTL(tier.timeToLiveMin(subType), tier.timeToLiveMax(subType));
    }

    private long getRandomTTL(long timeToLiveMin, long timeToLiveMax) {
        if (timeToLiveMax < timeToLiveMin)
            throw new RuntimeException("Max is less than min");

        if (timeToLiveMax < 0)
            return -1;

        if (timeToLiveMin < 0)
            return RANDOM.nextLong() % timeToLiveMax;

        long diff = timeToLiveMax - timeToLiveMin;
        if (diff <= 0)
            return timeToLiveMin;

        return timeToLiveMin + (RANDOM.nextLong() % diff);
    }

    @Override
    public void update() {
        // end of life
        if (endOfLife())
            return;

        // establishment process
        if (requirements.size() > 1 && !established) {
            synchronized (currentProgress) {
                if (!allFilled(requirements, currentProgress)) {
                    transferAssets(requirements, currentProgress);
                    return;
                }

                // finally, duration requirement
                if (!handleDuration(requirements, currentProgress))
                    return;
            }

            establishmentTime = System.currentTimeMillis();
            established = true;
        }

        // or production
        // remove item from asset store and fill production store
        synchronized (productionStorage) {
            if (!allFilled(inputs, productionStorage)) {
                transferAssets(inputs, productionStorage);
            }

            if (!handleDuration(inputs, productionStorage))
                return;

            if (allFilled(inputs, productionStorage)) {
                produceOutput();
            }
        }
    }

    private boolean allFilled(Map<AssetSignature, Double> required, Map<AssetSignature, Double> destination) {
        for (Map.Entry<AssetSignature, Double> entry : required.entrySet()) {
            AssetSignature sign = entry.getKey();
            if (sign.equals(DURATION_SIGNATURE)) // duration is handled separately
                continue;

            double requiredAmount = entry.getValue();
            double currentAmount = destination.getOrDefault(sign, 0.0);

            // if at least one fail, entire production is skipped
            if (currentAmount < requiredAmount) {
                return false;
            }
        }

        return true;
    }

    private void transferAssets(Map<AssetSignature, Double> required, Map<AssetSignature, Double> destination) {
        boolean update = false;
        // remove from asset store and fill progress
        for (Map.Entry<AssetSignature, Double> entry : required.entrySet()) {
            AssetSignature sign = entry.getKey();
            Double requiredAmount = entry.getValue();
            double current = destination.getOrDefault(sign, 0.0);
            for (Asset removed : AssetUtil.removeAsset(ownedAssets, sign, requiredAmount - current)) {
                double updated = destination.getOrDefault(sign, 0.0) + removed.getNumericalMeasure();
                destination.put(sign, updated);

                if (current == updated) {
                    update = true;
                }
            }
        }

        if (update)
            notifyObservers();
    }

    private boolean handleDuration(Map<AssetSignature, Double> required, Map<AssetSignature, Double> destination) {
        // duration should be handled separately.
        double requiredDuration = required.getOrDefault(new DurationSignature(), 0.0);
        if (requiredDuration > 0.0) {
            double currentDuration = destination.getOrDefault(new DurationSignature(), 0.0);
            destination.put(new DurationSignature(), currentDuration + 1.0); // add 1.0 per second
        }

        return destination.getOrDefault(new DurationSignature(), 0.0) >= requiredDuration;
    }

    private void produceOutput() {
        boolean update = false;

        // adjust amount in production storage
        for (Map.Entry<AssetSignature, Double> e : inputs.entrySet()) {
            AssetSignature key = e.getKey();
            Double required = e.getValue();
            if (required <= 0.0)
                continue;

            double current = productionStorage.getOrDefault(key, 0.0);
            productionStorage.put(key, current - required);

            update = true;
        }

        // produce outputs
        for (Map.Entry<AssetSignature, Double> entry : outputs.entrySet()) {
            AssetSignature sign = entry.getKey();
            Double amount = entry.getValue();
            if (amount <= 0.0)
                continue;

            Asset asset = sign.asset(amount);
            AssetUtil.addAsset(ownedAssets, asset);

            update = true;
        }

        if (update)
            notifyObservers();
    }

    @Override
    public void stop() {

    }

    @Override
    public IMemento saveState() {
        return new ParentMemento(this);
    }

    @Override
    public void restoreState(IMemento iMemento) {
        ParentMemento memento = (ParentMemento) iMemento;

        ownedAssets.clear();
        ownedAssets.addAll(memento.ownedAssets);

        currentProgress.clear();
        currentProgress.putAll(memento.currentProgress);

        productionStorage.clear();
        productionStorage.putAll(memento.productionStorage);

        upgrades.clear();
        upgrades.putAll(memento.upgrades);

        ownerUuid = memento.ownerUuid;
        tier = memento.tier;
        subType = memento.subType;
        establishmentTime = memento.establishmentTime;
        established = memento.established;
        timeToLive = memento.timeToLive;
    }

    protected class ParentMemento implements IMemento {
        private final List<Asset> ownedAssets = new ArrayList<>();
        private final Map<AssetSignature, Double> currentProgress = new HashMap<>();
        private final Map<AssetSignature, Double> productionStorage = new HashMap<>();
        private final Map<UUID, Integer> upgrades = new ConcurrentHashMap<>();

        private final UUID ownerUuid;
        private final ITier tier;
        private final String subType;
        private final long establishmentTime;
        private final boolean established;
        private long timeToLive = 0;

        public ParentMemento(AbstractBusiness business) {
            business.ownedAssets.stream()
                    .map(Asset::clone)
                    .forEach(ownedAssets::add);

            currentProgress.putAll(business.currentProgress);
            productionStorage.putAll(business.productionStorage);
            upgrades.putAll(business.upgrades);

            ownerUuid = business.ownerUuid;
            tier = business.tier;
            subType = business.subType;
            establishmentTime = business.establishmentTime;
            established = business.established;
            timeToLive = business.timeToLive;
        }
    }
}
