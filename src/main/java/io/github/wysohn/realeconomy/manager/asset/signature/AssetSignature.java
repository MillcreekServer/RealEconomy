package io.github.wysohn.realeconomy.manager.asset.signature;

import io.github.wysohn.rapidframework3.core.language.ManagerLanguage;
import io.github.wysohn.rapidframework3.core.message.Message;
import io.github.wysohn.rapidframework3.interfaces.ICommandSender;
import io.github.wysohn.realeconomy.manager.asset.Asset;

import java.util.HashMap;
import java.util.Map;

/**
 * A class that represent the detailed information about an Asset.
 * Children class must be immutable in order to provide concurrent access from multiple threads.
 */
public abstract class AssetSignature {
    public static final String KEY_NUMERIC_MEASURE = "measurement";

    public abstract String category();

    public abstract AssetSignature clone();

    /**
     * Create a new Asset based on the signature. Uses {@link #KEY_NUMERIC_MEASURE} as the
     * key
     *
     * @param numericMeasure Arbitrary numeric value. For item, this is integer, but it can be other
     *                       measurement depending on the signature.
     * @return the newly created asset
     */
    public Asset asset(Double numericMeasure) {
        return asset(new HashMap<String, Object>() {{
            put(KEY_NUMERIC_MEASURE, numericMeasure);
        }});
    }

    /**
     * Create a new Asset based on the signature. In here, you can freely define the metaData.
     *
     * @param metaData the metaData; the usage of it differ depending on the signature
     * @return the newly created asset
     */
    public abstract Asset asset(Map<String, Object> metaData);

    public abstract Message[] toMessage(ManagerLanguage lang, ICommandSender sender);

    @Override
    public int hashCode() {
        throw new RuntimeException();
    }

    @Override
    public boolean equals(Object obj) {
        throw new RuntimeException();
    }

    @Override
    public String toString() {
        throw new RuntimeException();
    }
}
