package io.github.wysohn.realeconomy.interfaces.business.tiers;

import io.github.wysohn.realeconomy.manager.business.tiers.TierInfoMap;

public interface ITier {
    /**
     * String version of tier
     *
     * @return
     */
    String name();

    /**
     * Map of required assets and its amount to establish the business.
     * Once every items are filled in the business' asset store by required amount,
     * the business is then established, and it will start producing outputs.
     *
     * @param subType subtype of this tier.
     * @return
     */
    TierInfoMap requirement(String subType);

    /**
     * Map of amount of resources per assets to be filled.
     * Every process is per second, so you can use decimal numbers
     * to make the process slower. For example, if you set the value
     * of DIAMOND to 0.25, it will take 4 seconds to fill 1 DIAMOND requirement,
     * and the total required amount is defined by {@link #requirement()}
     *
     * @param subType subtype of this tier.
     * @return
     */
    TierInfoMap fulfillment(String subType);

    /**
     * Map of input assets to be used by this business.
     * Production will halt if not enough assets exist in the
     * asset store as much as specified by this method.
     *
     * @param subType subtype of this tier.
     * @return
     */
    TierInfoMap inputs(String subType);

    /**
     * Map of output assets to be produced by this business.
     *
     * @param subType subtype of this tier.
     * @return
     */
    TierInfoMap outputs(String subType);

    /**
     * Decides how long this business will last.
     *
     * @param subType subtype of this tier.
     * @return -1L by default to indicate that the business will last forever; otherwise, time in
     * milliseoncds until how long this business can be in a service.
     */
    default long timeToLiveMin(String subType) {
        return -1L;
    }

    /**
     * Decides how long this business will last.
     *
     * @param subType subtype of this tier.
     * @return -1L by default to indicate that the business will last forever; otherwise, time in
     * milliseoncds until how long this business can be in a service.
     */
    default long timeToLiveMax(String subType) {
        return -1L;
    }
}
