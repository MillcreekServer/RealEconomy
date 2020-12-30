package io.github.wysohn.realeconomy.manager.business.types;

import com.google.inject.Inject;
import io.github.wysohn.rapidframework3.core.caching.CachedElement;
import io.github.wysohn.rapidframework3.interfaces.paging.DataProvider;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.realeconomy.interfaces.business.IBusiness;
import io.github.wysohn.realeconomy.interfaces.business.ITier;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.banking.AssetUtil;
import io.github.wysohn.realeconomy.manager.listing.AssetListingManager;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractBusiness extends CachedElement<UUID> implements IBusiness {
    @Inject
    private transient AssetListingManager assetListingManager;

    private transient Map<AssetSignature, Double> requirements;
    private transient Map<AssetSignature, Double> fulfillment;
    private transient Map<AssetSignature, Double> inputs;
    private transient Map<AssetSignature, Double> outputs;

    private final List<Asset> ownedAssets = new ArrayList<>();
    private final Map<AssetSignature, Double> currentProgress = new ConcurrentHashMap<>();
    private final Map<AssetSignature, Double> productionStorage = new ConcurrentHashMap<>();

    private UUID ownerUuid;
    private ITier tier;
    private boolean established = false;

    public AbstractBusiness(UUID key, UUID ownerUuid, ITier tier) {
        super(key);
        Validation.assertNotNull(ownerUuid);
        Validation.assertNotNull(tier);

        this.ownerUuid = ownerUuid;
        this.tier = tier;
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

    @Override
    public boolean isEstablished() {
        return established;
    }

    @Override
    public boolean endOfLife() {
        return tier.timeToLive() >= 0 && System.currentTimeMillis() > tier.timeToLive();
    }

    @Override
    public void addAsset(Asset asset) {
        AssetUtil.addAsset(ownedAssets, asset);
    }

    @Override
    public Collection<Asset> removeAsset(AssetSignature signature, int amount) {
        return AssetUtil.removeAsset(ownedAssets, signature, amount);
    }

    @Override
    public DataProvider<Asset> assetDataProvider() {
        return AssetUtil.assetDataProvider(ownedAssets);
    }

    @Override
    public Map<AssetSignature, Double> getCurrentProgress() {
        return currentProgress;
    }

    @Override
    public Map<AssetSignature, Double> getProductionStorage() {
        return productionStorage;
    }

    @Override
    public void init() {
        requirements = tier.requirement().getAll(assetListingManager);
        fulfillment = tier.fulfillment().getAll(assetListingManager);
        inputs = tier.inputs().getAll(assetListingManager);
        outputs = tier.outputs().getAll(assetListingManager);
    }

    @Override
    public void update() {
        // end of life
        if (endOfLife())
            return;

        // establishment process
        if (requirements.size() > 1 && !established) {
            established = true;
            requirements.forEach((sign, required) -> {
                double current = currentProgress.getOrDefault(sign, 0.0);
                if (current >= required)
                    return;

                double increment = fulfillment.getOrDefault(sign, 0.0);
                currentProgress.put(sign, current + increment);
                established = false;
            });
            return;
        }

        // or production
        // remove item from asset store and fill production store
        inputs.forEach((sign, required) -> AssetUtil.removeAsset(ownedAssets,
                sign,
                required).forEach(removed -> {
            double updated = productionStorage.getOrDefault(sign, 0.0) + removed.getNumericalMeasure();
            productionStorage.put(sign, updated);
        }));

        // produce only if queue has enough assets
        for (Map.Entry<AssetSignature, Double> entry : inputs.entrySet()) {
            AssetSignature sign = entry.getKey();
            double required = entry.getValue();
            double current = productionStorage.getOrDefault(sign, 0.0);

            // if at least one fail, entire production is skipped
            if (current < required) {
                return;
            }
        }

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

    @Override
    public void stop() {

    }
}
