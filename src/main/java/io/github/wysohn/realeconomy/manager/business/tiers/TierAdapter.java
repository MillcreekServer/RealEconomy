package io.github.wysohn.realeconomy.manager.business.tiers;

import io.github.wysohn.rapidframework3.interfaces.store.IKeyValueStorage;
import io.github.wysohn.realeconomy.interfaces.business.tiers.ITier;

import java.util.Set;

/**
 * "mining": # business type
 * - "default": # business sub type
 * - - "requirement" # configs
 * - - - "UUID":Double
 * - - "input"
 * - - - "UUID":Double
 * - - "output"
 * - - - "UUID":Double
 * - - "timeToLive": Long
 * <p>
 * - "something":
 * - - "requirement"
 * - - - "UUID":Double
 * ...
 */
public class TierAdapter implements ITier {
    public static final String REQUIREMENT = "requirement";
    public static final String INPUT = "input";
    public static final String OUTPUT = "output";
    public static final String TIME_TO_LIVE_MIN = "timeToLive.min";
    public static final String TIME_TO_LIVE_MAX = "timeToLive.man";

    private final String name;
    private final IKeyValueStorage keyValueStorage;
    private final Object section;

    public TierAdapter(String name, IKeyValueStorage keyValueStorage) {
        this.name = name;
        this.keyValueStorage = keyValueStorage;
        this.section = keyValueStorage.get(name).orElseThrow(() ->
                new RuntimeException("config section for " + name + " does not exist in " + keyValueStorage));
    }

    public String name() {
        return name;
    }

    @Override
    public boolean verifySubType(String subType) {
        return keyValueStorage.get(section, subType).isPresent();
    }

    @Override
    public Set<String> listSubTypes() {
        return keyValueStorage.getKeys(section, false);
    }

    @Override
    public TierInfoMap requirement(String subType) {
        Object subSection = keyValueStorage.get(section, subType).orElseThrow(() ->
                new RuntimeException(subType + " section is missing."));
        Object requirementSection = keyValueStorage.get(subSection, REQUIREMENT).orElseThrow(() ->
                new RuntimeException(REQUIREMENT + " section is missing in " + subSection));
        return new TierInfoMap(keyValueStorage, requirementSection);
    }

    @Override
    public TierInfoMap inputs(String subType) {
        Object subSection = keyValueStorage.get(section, subType).orElseThrow(() ->
                new RuntimeException(subType + " section is missing."));
        Object inputSection = keyValueStorage.get(subSection, INPUT).orElseThrow(() ->
                new RuntimeException(INPUT + " section is missing in " + subSection));
        return new TierInfoMap(keyValueStorage, inputSection);
    }

    @Override
    public TierInfoMap outputs(String subType) {
        Object subSection = keyValueStorage.get(section, subType).orElseThrow(() ->
                new RuntimeException(subType + " section is missing."));
        Object outputSection = keyValueStorage.get(subSection, OUTPUT).orElseThrow(() ->
                new RuntimeException(OUTPUT + " section is missing in " + subSection));
        return new TierInfoMap(keyValueStorage, outputSection);
    }

    @Override
    public long timeToLiveMin(String subType) {
        Object subSection = keyValueStorage.get(section, subType).orElseThrow(() ->
                new RuntimeException(subType + " section is missing."));

        return keyValueStorage.get(subSection, TIME_TO_LIVE_MIN)
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .map(Number::longValue)
                .orElse(-1L);
    }

    @Override
    public long timeToLiveMax(String subType) {
        Object subSection = keyValueStorage.get(section, subType).orElseThrow(() ->
                new RuntimeException(subType + " section is missing."));

        return keyValueStorage.get(subSection, TIME_TO_LIVE_MAX)
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .map(Number::longValue)
                .orElse(-1L);
    }
}
