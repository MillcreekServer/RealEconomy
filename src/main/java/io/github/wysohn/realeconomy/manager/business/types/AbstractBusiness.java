package io.github.wysohn.realeconomy.manager.business.types;

import com.google.inject.Inject;
import io.github.wysohn.rapidframework3.core.caching.CachedElement;
import io.github.wysohn.rapidframework3.utils.Validation;
import io.github.wysohn.realeconomy.interfaces.business.IBusiness;
import io.github.wysohn.realeconomy.interfaces.business.ITier;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.listing.AssetListingManager;

import java.util.*;

public abstract class AbstractBusiness extends CachedElement<UUID> implements IBusiness {
    @Inject
    private transient AssetListingManager assetListingManager;

    private final List<Asset> ownedAssets = new ArrayList<>();
    private final Map<AssetSignature, Double> currentProgress = new HashMap<>();
    private final Map<AssetSignature, Double> productionStorage = new HashMap<>();

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

    private transient Map<AssetSignature, Double> requirements;
    private transient Map<AssetSignature, Double> fulfillment;
    private transient Map<AssetSignature, Double> inputs;
    private transient Map<AssetSignature, Double> outputs;

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
        if (tier.endOfLife() >= 0 && System.currentTimeMillis() > tier.endOfLife())
            return;

        // establishment process
        if (!established) {
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
        // 1. remove item from asset store
        // 2. fill the production store
        // 3. produce only if queue has enough assets
        // 4. store outputs in asset store
    }

    @Override
    public void stop() {

    }
}
