package io.github.wysohn.realeconomy.interfaces.business;

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

    /**
     * Get the business that the member is currently involved in.
     * The context of 'involved' may change depending on the implementation, yet
     * once the method return the IBusiness instance to the BusinessManager,
     * it will consider that the member is currently actively involved in the business.
     * <p>
     * For example, mining business may keep track of the member to know whether the member
     * is within the designated mine area, so that the 'mining in the area' will be considered
     * as 'actively involved.' In this way, the activity, mining in the area, will be
     * customized depending on the setup of the business.
     *
     * @param member target member
     * @return set of businesses. This usually has to be only one, but who knows? Can be empty
     * but never null.
     */
    Set<IBusiness> getUsingBusiness(IBusinessMember member);
}
