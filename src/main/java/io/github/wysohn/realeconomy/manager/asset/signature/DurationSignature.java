package io.github.wysohn.realeconomy.manager.asset.signature;

import io.github.wysohn.rapidframework3.core.language.ManagerLanguage;
import io.github.wysohn.rapidframework3.core.message.Message;
import io.github.wysohn.rapidframework3.core.message.MessageBuilder;
import io.github.wysohn.rapidframework3.interfaces.ICommandSender;
import io.github.wysohn.realeconomy.main.RealEconomyLangs;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class DurationSignature extends AssetSignature {
    /**
     * Duration in seconds (long)
     */
    public static final String KEY_DURATION_MILLIS = "key_duration";

    @Override
    public Asset asset(Double numericMeasure) {
        return asset(new HashMap<String, Object>() {{
            put(KEY_DURATION_MILLIS, numericMeasure);
        }});
    }

    @Override
    public String category() {
        return "duration";
    }

    @Override
    public AssetSignature clone() {
        return new DurationSignature();
    }

    @Override
    public Asset asset(Map<String, Object> metaData) {
        Duration duration = new Duration(UUID.randomUUID(), this);
        duration.setNumericalMeasure((double) Objects.requireNonNull(metaData.get(KEY_DURATION_MILLIS)));
        return duration;
    }

    @Override
    public Message[] toMessage(ManagerLanguage lang, ICommandSender sender) {
        String parsed = lang.parseFirst(sender, RealEconomyLangs.Duration);
        return MessageBuilder.forMessage(parsed)
                .build();
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;

        return getClass() == obj.getClass();
    }

    @Override
    public String toString() {
        return "Duration";
    }
}
