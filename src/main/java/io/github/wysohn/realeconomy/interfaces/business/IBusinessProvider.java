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
    IBusiness get(UUID uuid);

    /**
     * Get UUID key set of all the businesses that this provider can provide.
     *
     * @return return the 'snapshot' of the UUID keys. Do not directly return the
     * Set that is backed by whatever data structure this provider internally uses
     * to store the businesses.
     */
    Set<UUID> keys();
}
