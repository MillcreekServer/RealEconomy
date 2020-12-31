package io.github.wysohn.realeconomy.manager.asset.signature;

import io.github.wysohn.rapidframework3.core.language.ManagerLanguage;
import io.github.wysohn.rapidframework3.core.message.Message;
import io.github.wysohn.rapidframework3.core.message.MessageBuilder;
import io.github.wysohn.rapidframework3.interfaces.ICommandSender;
import io.github.wysohn.realeconomy.main.RealEconomyLangs;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.Duration;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class DurationSignature extends AssetSignature {
    /**
     * Duration in seconds (long)
     */
    public static final String KEY_DURATION_MILLIS = "key_duration";

    @Override
    public boolean isPhysical() {
        return false;
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
    public Asset create(Map<String, Object> metaData) {
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
}
