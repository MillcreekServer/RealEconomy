package io.github.wysohn.realeconomy.manager.business.tiers;

import io.github.wysohn.rapidframework3.interfaces.ICommandSender;
import io.github.wysohn.rapidframework3.interfaces.store.IKeyValueStorage;
import io.github.wysohn.realeconomy.interfaces.business.tiers.ITier;

import java.util.List;
import java.util.Set;

/**
 * "mining": # business type
 * - "displayName"
 * - - "default": "&6Mining Business"
 * - - "ko": "&6광산업"
 * - "description"
 * - - "default": []
 * - - "ko": []
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
    public static final String DISPLAY_NAME = "displayName";
    public static final String DESCRIPTION = "description";

    private final String name;
    private final IKeyValueStorage keyValueStorage;

    public TierAdapter(String name, IKeyValueStorage keyValueStorage) {
        this.name = name;
        this.keyValueStorage = keyValueStorage;

        getSection();
    }

    private Object getSection() {
        return keyValueStorage.get(name).orElseThrow(() ->
                new RuntimeException("config section for " + name + " does not exist in " + keyValueStorage));
    }

    public String name() {
        return name;
    }

    @Override
    public String displayName(ICommandSender sender) {
        String defaultKey = DISPLAY_NAME + ".default";

        if (!keyValueStorage.get(getSection(), defaultKey).isPresent())
            keyValueStorage.put(getSection(), defaultKey, "&6" + name);

        return keyValueStorage.get(DISPLAY_NAME + "." + sender.getLocale().getLanguage())
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .orElseGet(() -> keyValueStorage.get(getSection(), defaultKey)
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .orElseThrow(RuntimeException::new));
    }

    @Override
    public String[] description(ICommandSender sender) {
        String defaultKey = DESCRIPTION + ".default";
        if (!keyValueStorage.get(getSection(), defaultKey).isPresent()) {
            return null;
        } else {
            return keyValueStorage.get(DESCRIPTION + "." + sender.getLocale().getLanguage())
                    .filter(List.class::isInstance)
                    .map(List.class::cast)
                    .map(list -> ((List<String>) list).toArray(new String[0]))
                    .orElseGet(() -> keyValueStorage.get(getSection(), defaultKey)
                            .filter(List.class::isInstance)
                            .map(List.class::cast)
                            .map(list -> ((List<String>) list).toArray(new String[0]))
                            .orElse(null));
        }
    }

    @Override
    public void reload() throws Exception {
        keyValueStorage.reload();
    }

    @Override
    public boolean verifySubType(String subType) {
        return keyValueStorage.get(getSection(), subType).isPresent();
    }

    @Override
    public Set<String> listSubTypes() {
        return keyValueStorage.getKeys(getSection(), false);
    }

    @Override
    public TierInfoMap requirement(String subType) {
        Object subSection = keyValueStorage.get(getSection(), subType).orElseThrow(() ->
                new RuntimeException(subType + " section is missing."));
        Object requirementSection = keyValueStorage.get(subSection, REQUIREMENT).orElseThrow(() ->
                new RuntimeException(REQUIREMENT + " section is missing in " + subSection));
        return new TierInfoMap(keyValueStorage, requirementSection);
    }

    @Override
    public TierInfoMap inputs(String subType) {
        Object subSection = keyValueStorage.get(getSection(), subType).orElseThrow(() ->
                new RuntimeException(subType + " section is missing."));
        Object inputSection = keyValueStorage.get(subSection, INPUT).orElseThrow(() ->
                new RuntimeException(INPUT + " section is missing in " + subSection));
        return new TierInfoMap(keyValueStorage, inputSection);
    }

    @Override
    public TierInfoMap outputs(String subType) {
        Object subSection = keyValueStorage.get(getSection(), subType).orElseThrow(() ->
                new RuntimeException(subType + " section is missing."));
        Object outputSection = keyValueStorage.get(subSection, OUTPUT).orElseThrow(() ->
                new RuntimeException(OUTPUT + " section is missing in " + subSection));
        return new TierInfoMap(keyValueStorage, outputSection);
    }

    @Override
    public long timeToLiveMin(String subType) {
        Object subSection = keyValueStorage.get(getSection(), subType).orElseThrow(() ->
                new RuntimeException(subType + " section is missing."));

        return keyValueStorage.get(subSection, TIME_TO_LIVE_MIN)
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .map(Number::longValue)
                .orElse(-1L);
    }

    @Override
    public long timeToLiveMax(String subType) {
        Object subSection = keyValueStorage.get(getSection(), subType).orElseThrow(() ->
                new RuntimeException(subType + " section is missing."));

        return keyValueStorage.get(subSection, TIME_TO_LIVE_MAX)
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .map(Number::longValue)
                .orElse(-1L);
    }
}
