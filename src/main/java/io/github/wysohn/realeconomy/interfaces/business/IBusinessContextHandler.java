package io.github.wysohn.realeconomy.interfaces.business;

import io.github.wysohn.rapidframework3.data.SimpleLocation;

import java.util.Set;
import java.util.UUID;

public interface IBusinessContextHandler {
    /**
     * define priority of the handler. Higher the value, later it will handle.
     *
     * @return priority
     */
    default int priority() {
        return 100;
    }

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
     * @param memberUuid target member
     * @return set of businesses. This usually has to be only one, but who knows? Can be empty
     * but never null.
     */
    Set<UUID> getUsingBusiness(UUID memberUuid);

    /**
     * Add member to the business. This is not stored directly into the business itself,
     * but it will be handled by whatever plugin that is responsible for it.
     * <p>
     * Worldguard, for example, has concept of Owner group and Member group, so it is straight
     * forward to implement. But different plugins may have different rule on how to handle
     * members.
     *
     * @param business   target business to add member
     * @param memberUuid uuid of member
     * @return true if added; false otherwise
     */
    boolean addMember(IBusiness business, UUID memberUuid);

    /**
     * Remove member from the business. Refere to {@link #removeMember(IBusiness, UUID)}
     * for more details.
     *
     * @param business   target business to remove member from
     * @param memberUuid uuid of member
     * @return true if removed; false otherwise
     */
    boolean removeMember(IBusiness business, UUID memberUuid);

    /**
     * Check if the user is considered as 'member' of the business.
     *
     * @param business   the business where the user is accessing
     * @param memberUuid the user to check
     * @return true if the user is member; false otherwise.
     */
    boolean isMember(IBusiness business, UUID memberUuid);

    /**
     * Check if the user is physically residing in the business.
     *
     * @param business   the business where the user is accessing
     * @param memberUuid the user to check
     * @return true if physically present; false otherwise.
     */
    boolean isInBusiness(IBusiness business, UUID memberUuid);

    /**
     * Query the location to get what business is opened at the location
     *
     * @param location location
     * @return UUID of business; null if not occupied.
     */
    UUID queryBusiness(SimpleLocation location);

    /**
     * Get the location of the business. Note that the return type is SimpleLocation, yet
     * it doesn't make sense for most of the land claiming plugin. For consistency, use
     * the minimum coordinate out of the region. For example, if the claim is 2D, use the
     * smaller of X and Z coordinates from two points. Similar goes for 3D.
     * <p>
     * However, since SimpleLocation only supports 3D coordinates, if the points are in 2D,
     * use 0 for the non-existing axis. For example, use 0 as Y for 2D coordinates.
     *
     * @param business the business to query
     * @return the smallest coordinate of the claim; null if not exist (but it shouldn't unless that was
     * your intent)
     */
    SimpleLocation getLocationOfBusiness(IBusiness business);

    /**
     * Called upon new business opened. Use this to update the mapping.
     * <p>
     * If the claim is 1 to 1 mapping to the business, as this method is invoked after a new
     * business is created, you may want to create a new claim information at this point.
     * <p>
     * Note that this is also called when plugin first start, iterating all the existing businesses.
     *
     * @param location the location where it has to be mapped with the business.
     * @param business the business to map the location with
     * @return true is handled; if return false, it will be handled by the handler with the lower priority.
     */
    boolean updateMapping(SimpleLocation location, IBusiness business);

    /**
     * Called upon deleting a business. Use this to remove the mapping.
     * <p>
     * If the claim is 1 to 1 mapping to the business, as the business is already deleted when this
     * method is invoked, this is where you may delete the claim.
     *
     * @param location the location where the business was associated with
     * @param business the business to remove
     * @return true is handled; if return false, it will be handled by the handler with the lower priority.
     */
    boolean removeMapping(SimpleLocation location, IBusiness business);
}
