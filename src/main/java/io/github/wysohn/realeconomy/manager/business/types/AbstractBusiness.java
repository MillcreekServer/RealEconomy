package io.github.wysohn.realeconomy.manager.business.types;

import com.google.inject.Inject;
import io.github.wysohn.rapidframework3.core.caching.CachedElement;
import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.rapidframework3.utils.Pair;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.realeconomy.interfaces.business.IBusiness;
import io.github.wysohn.realeconomy.interfaces.business.tiers.ITier;
import io.github.wysohn.realeconomy.interfaces.business.upgrades.IUpgrade;
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

    @Inject
    private transient AssetListingManager assetListingManager;

    private transient Map<AssetSignature, Double> requirements;
    private transient Map<AssetSignature, Double> fulfillment;
    private transient Map<AssetSignature, Double> inputs;
    private transient Map<AssetSignature, Double> outputs;
    private final List<Asset> ownedAssets = new ArrayList<>();
    private final Map<AssetSignature, Double> currentProgress = new HashMap<>();
    private final Map<AssetSignature, Double> productionStorage = new HashMap<>();
    private final Map<UUID, Integer> upgrades = new ConcurrentHashMap<>();

    private UUID ownerUuid;
    private ITier tier;
    private String subType = AbstractBusinessManager.DEFAULT_SUB_TYPE;
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
    public Collection<Asset> removeAsset(AssetSignature signature, int amount) {
        Collection<Asset> assets = AssetUtil.removeAsset(ownedAssets, signature, amount);
        if (assets.size() > 0)
            notifyObservers();
        return assets;
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
    }

    @Override
    public void init() {
        requirements = tier.requirement(subType).getAll(assetListingManager);
        fulfillment = tier.fulfillment(subType).getAll(assetListingManager);
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
                return;
            }

            if (!handleDuration(inputs, productionStorage))
                return;

            // adjust amount in production storage
            inputs.forEach((sign, required) -> {
                if (required <= 0.0)
                    return;

                double current = productionStorage.getOrDefault(sign, 0.0);
                productionStorage.put(sign, current - required);
            });

            // produce outputs
            outputs.forEach((sign, amount) -> {
                if (amount <= 0.0)
                    return;

                Asset asset = sign.create(new HashMap<String, Object>() {{
                    put(AssetSignature.KEY_NUMERIC_MEASURE, amount);
                }});
                AssetUtil.addAsset(ownedAssets, asset);
            });
        }
    }

    private boolean allFilled(Map<AssetSignature, Double> required, Map<AssetSignature, Double> destination) {
        for (Map.Entry<AssetSignature, Double> entry : required.entrySet()) {
            AssetSignature sign = entry.getKey();
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
        // remove from asset store and fill progress
        required.forEach((sign, requiredAmount) -> {
            double current = destination.getOrDefault(sign, 0.0);
            AssetUtil.removeAsset(ownedAssets,
                    sign,
                    requiredAmount - current).forEach(removed -> {
                double updated = destination.getOrDefault(sign, 0.0) + removed.getNumericalMeasure();
                destination.put(sign, updated);
            });
        });
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

    @Override
    public void stop() {

    }
}
