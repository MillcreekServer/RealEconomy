package io.github.wysohn.realeconomy.interfaces.business;

import io.github.wysohn.realeconomy.interfaces.business.tiers.ITier;
import io.github.wysohn.realeconomy.manager.business.types.AbstractBusiness;

import java.util.Set;
import java.util.UUID;

public interface IBusinessProvider {
    /**
     * Get business associated with the uuid.
     * <p>
     * Make sure that this method is thread-safe.
     *
     * @param uuid uuid of the business
     * @return business instance
     */
    IBusiness getBusiness(UUID uuid);

    /**
     * Get UUID key set of all the businesses that this provider can provide.
     *
     * @return return the 'snapshot' of the UUID keys. Do not directly return the
     * Set that is backed by whatever data structure this provider internally uses
     * to store the businesses.
     */
    Set<UUID> keys();

    default AbstractBusiness openNewBusiness(UUID ownerUuid) {
        return openNewBusiness(ownerUuid, ITier.DEFAULT_SUB_TYPE);
    }

    /**
     * Open a new business from this specific business provider.
     *
     * @param ownerUuid UUID of owner
     * @param subType   the special tag to distinguish sub-types of a same business. For example, if the
     *                  business is about mining, it may specialize to coal business, then use this
     *                  parameter to distinguish them.
     * @return newly created business. Note that this operation always success and create a new business
     * with different UUID. The caller has to store the UUID to keep track of the business instance.
     */
    AbstractBusiness openNewBusiness(UUID ownerUuid, String subType);

    boolean deleteBusiness(IBusiness business);

    /**
     * Name of tier, or as known as the name of business.
     *
     * @return
     */
    String getTierName();
}
