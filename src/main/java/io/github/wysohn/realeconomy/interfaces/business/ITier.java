package io.github.wysohn.realeconomy.interfaces.business;

import io.github.wysohn.realeconomy.manager.business.TierInfoMap;

public interface ITier {
    /**
     * Map of required assets and its amount to establish the business.
     * Once every items are filled in the business' asset store by required amount,
     * the business is then established, and it will start producing outputs.
     *
     * @return
     */
    TierInfoMap requirement();

    /**
     * Map of amount of resources per assets to be filled.
     * Every process is per second, so you can use decimal numbers
     * to make the process slower. For example, if you set the value
     * of DIAMOND to 0.25, it will take 4 seconds to fill 1 DIAMOND requirement,
     * which is defined by {@link #requirement()}
     *
     * @return
     */
    TierInfoMap fulfillment();

    /**
     * Map of input assets to be used by this business.
     * Production of this process will halt if not enough assets exist in the
     * asset store as much as specified by this method.
     *
     * @return
     */
    TierInfoMap inputs();

    /**
     * Map of output assets to be produced by this business.
     *
     * @return
     */
    TierInfoMap outputs();

    long endOfLife();
}
