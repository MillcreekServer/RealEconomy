package io.github.wysohn.realeconomy.manager.asset.signature;

import io.github.wysohn.rapidframework3.core.language.ManagerLanguage;
import io.github.wysohn.rapidframework3.core.message.Message;
import io.github.wysohn.rapidframework3.core.message.MessageBuilder;
import io.github.wysohn.rapidframework3.interfaces.ICommandSender;
import io.github.wysohn.realeconomy.main.RealEconomyLangs;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.Electricity;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class ElectricitySignature extends UtilityAssetSignature {
    public static final String UTILITY_TYPE = "electricity";

    public ElectricitySignature() {
        super(UTILITY_TYPE);
    }

    @Override
    public AssetSignature clone() {
        return new ElectricitySignature();
    }

    @Override
    public Asset create(Map<String, Object> metaData) {
        Electricity electricity = new Electricity(UUID.randomUUID(), this);
        electricity.setNumericalMeasure((double) Objects.requireNonNull(metaData.get(KEY_NUMERIC_MEASURE)));
        return electricity;
    }

    @Override
    public Message[] toMessage(ManagerLanguage lang, ICommandSender sender) {
        return MessageBuilder.forMessage(lang.parseFirst(sender, RealEconomyLangs.Electricity))
                .build();
    }
}
