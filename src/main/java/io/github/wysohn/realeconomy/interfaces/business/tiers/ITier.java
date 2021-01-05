package io.github.wysohn.realeconomy.interfaces.business.tiers;

import io.github.wysohn.rapidframework3.interfaces.ICommandSender;
import io.github.wysohn.realeconomy.manager.business.tiers.TierInfoMap;

import java.util.Set;

public interface ITier {
    String DEFAULT_SUB_TYPE = "default";

    /**
     * String version of tier
     *
     * @return
     */
    String name();

    /**
     * Verify if the given 'subType' is valid to be used.
     *
     * @param subType
     * @return
     */
    boolean verifySubType(String subType);

    Set<String> listSubTypes();

    default TierInfoMap requirement() {
        return requirement(DEFAULT_SUB_TYPE);
    }

    /**
     * Map of required assets and its amount to establish the business.
     * Once every items are filled in the business' asset store by required amount,
     * the business is then established, and it will start producing outputs.
     *
     * @param subType subtype of this tier.
     * @return
     */
    TierInfoMap requirement(String subType);


    default TierInfoMap inputs() {
        return inputs(DEFAULT_SUB_TYPE);
    }

    /**
     * Map of input assets to be used by this business.
     * Production will halt if not enough assets exist in the
     * asset store as much as specified by this method.
     *
     * @param subType subtype of this tier.
     * @return
     */
    TierInfoMap inputs(String subType);


    default TierInfoMap outputs() {
        return outputs(DEFAULT_SUB_TYPE);
    }

    /**
     * Map of output assets to be produced by this business.
     *
     * @param subType subtype of this tier.
     * @return
     */
    TierInfoMap outputs(String subType);

    default long timeToLiveMin() {
        return timeToLiveMin(DEFAULT_SUB_TYPE);
    }

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

    default long timeToLiveMax() {
        return timeToLiveMax(DEFAULT_SUB_TYPE);
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

    /**
     * Display name to be shown to the user.
     *
     * @param sender target who will see the message
     * @return the message. Should not be null.
     */
    String displayName(ICommandSender sender);

    /**
     * Additional information to be shown
     *
     * @param sender target who will see the message
     * @return the message; null to disable output
     */
    String[] description(ICommandSender sender);

    void reload() throws Exception;
}
