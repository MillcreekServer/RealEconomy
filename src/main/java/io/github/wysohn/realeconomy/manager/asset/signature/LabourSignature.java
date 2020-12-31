package io.github.wysohn.realeconomy.manager.asset.signature;

import io.github.wysohn.rapidframework3.core.language.ManagerLanguage;
import io.github.wysohn.rapidframework3.core.message.Message;
import io.github.wysohn.rapidframework3.interfaces.ICommandSender;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.Labour;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class LabourSignature extends AssetSignature {
    public static final String KEY_LABOUR_POINT = "key_labour";

    @Override
    public boolean isPhysical() {
        return false;
    }

    @Override
    public String category() {
        return "labour";
    }

    @Override
    public AssetSignature clone() {
        return new LabourSignature();
    }

    @Override
    public Asset create(Map<String, Object> metaData) {
        Labour labour = new Labour(UUID.randomUUID(), this);
        labour.setNumericalMeasure((double) Objects.requireNonNull(metaData.get(KEY_LABOUR_POINT)));
        return labour;
    }

    @Override
    public Message[] toMessage(ManagerLanguage lang, ICommandSender sender) {
        return new Message[0];
    }
}
