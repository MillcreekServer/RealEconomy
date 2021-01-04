package io.github.wysohn.realeconomy.manager;

import copy.com.google.gson.*;
import io.github.wysohn.rapidframework3.interfaces.serialize.CustomAdapter;
import io.github.wysohn.rapidframework3.utils.Pair;
import io.github.wysohn.realeconomy.interfaces.banking.IAccount;
import io.github.wysohn.realeconomy.interfaces.banking.IBankingType;
import io.github.wysohn.realeconomy.interfaces.business.tiers.ITier;
import io.github.wysohn.realeconomy.manager.asset.Asset;
import io.github.wysohn.realeconomy.manager.asset.signature.AssetSignature;
import io.github.wysohn.realeconomy.manager.banking.BankingTypeRegistry;
import io.github.wysohn.realeconomy.manager.business.tiers.TierRegistry;
import io.github.wysohn.realeconomy.manager.business.types.mining.OreInfo;
import org.bukkit.Material;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.UUID;

public class CustomTypeAdapters {
    private static final String PACKAGE_NAME_ACCOUNT = "io.github.wysohn.realeconomy.manager.banking.account";
    private static final String PACKAGE_NAME_ASSET = "io.github.wysohn.realeconomy.manager.asset";
    private static final String PACKAGE_NAME_ASSET_SIGNATURE = "io.github.wysohn.realeconomy.manager.asset.signature";
    private static final String KEY_CLASS = "type";
    private static final String KEY_VALUE = "value";

    public static final Pair<Class<?>, CustomAdapter<?>> ACCOUNT = Pair.of(IAccount.class, new CustomAdapter<IAccount>() {
        @Override
        public JsonElement serialize(
                IAccount src, Type type, JsonSerializationContext context) {
            String className = src.getClass().getSimpleName();
            JsonElement serializedValue = context.serialize(src);

            JsonObject obj = new JsonObject();
            obj.addProperty(KEY_CLASS, className);
            obj.add(KEY_VALUE, serializedValue);
            return obj;
        }

        @Override
        public IAccount deserialize(
                JsonElement jsonElement,
                Type type,
                JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = jsonElement.getAsJsonObject();
            String className = obj.get(KEY_CLASS).getAsString();
            try {
                Class<?> clazz = Class.forName(PACKAGE_NAME_ACCOUNT + "." + className);
                return context.deserialize(obj.get(KEY_VALUE), clazz);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }
    });

    public static final Pair<Class<?>, CustomAdapter<?>> BANKING_TYPE = Pair.of(IBankingType.class, new CustomAdapter<IBankingType>() {
        @Override
        public JsonElement serialize(
                IBankingType iBankingType, Type type, JsonSerializationContext context) {
            return context.serialize(iBankingType.getUuid());
        }

        @Override
        public IBankingType deserialize(
                JsonElement jsonElement,
                Type type,
                JsonDeserializationContext context) throws JsonParseException {
            return Objects.requireNonNull(BankingTypeRegistry.fromUuid(context.deserialize(jsonElement, UUID.class)));
        }
    });

    public static final Pair<Class<?>, CustomAdapter<?>> ASSET = Pair.of(Asset.class, new CustomAdapter<Asset>() {
        @Override
        public Asset deserialize(
                JsonElement json,
                Type typeOfT,
                JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            String className = obj.get(KEY_CLASS).getAsString();
            try {
                Class<?> clazz = Class.forName(PACKAGE_NAME_ASSET + "." + className);
                return context.deserialize(obj.get(KEY_VALUE), clazz);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public JsonElement serialize(Asset src, Type typeOfSrc, JsonSerializationContext context) {
            String className = src.getClass().getSimpleName();
            JsonElement serializedValue = context.serialize(src);

            JsonObject obj = new JsonObject();
            obj.addProperty(KEY_CLASS, className);
            obj.add(KEY_VALUE, serializedValue);
            return obj;
        }
    });

    public static final Pair<Class<?>, CustomAdapter<?>> ASSET_SIGNATURE = Pair.of(AssetSignature.class, new CustomAdapter<AssetSignature>() {
        @Override
        public AssetSignature deserialize(
                JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = json.getAsJsonObject();
            String className = obj.get(KEY_CLASS).getAsString();
            try {
                Class<?> clazz = Class.forName(PACKAGE_NAME_ASSET_SIGNATURE + "." + className);
                return context.deserialize(obj.get(KEY_VALUE), clazz);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public JsonElement serialize(AssetSignature src, Type typeOfSrc, JsonSerializationContext context) {
            String className = src.getClass().getSimpleName();
            JsonElement serializedValue = context.serialize(src);

            JsonObject obj = new JsonObject();
            obj.addProperty(KEY_CLASS, className);
            obj.add(KEY_VALUE, serializedValue);
            return obj;
        }
    });

    public static final String KEY_BREAT_AT = "breakAt";
    public static final String KEY_MATERIAL = "material";
    public static final Pair<Class<?>, CustomAdapter<?>> ORE_INFO = Pair.of(OreInfo.class, new CustomAdapter<OreInfo>() {
        @Override
        public OreInfo deserialize(JsonElement jsonElement,
                                   Type type,
                                   JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {

            JsonObject obj = (JsonObject) jsonElement;

            long breakAt = obj.get(KEY_BREAT_AT).getAsLong();
            Material material = Material.valueOf(obj.get(KEY_MATERIAL).getAsString());

            return new OreInfo(material, breakAt);
        }

        @Override
        public JsonElement serialize(OreInfo oreInfo, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject obj = new JsonObject();

            obj.addProperty(KEY_BREAT_AT, oreInfo.breakAt);
            obj.addProperty(KEY_MATERIAL, oreInfo.material.name());

            return obj;
        }
    });

    public static final Pair<Class<?>, CustomAdapter<?>> I_TIER = Pair.of(ITier.class, new CustomAdapter<ITier>() {
        @Override
        public ITier deserialize(JsonElement json,
                                 Type typeOfT,
                                 JsonDeserializationContext context) throws JsonParseException {
            return TierRegistry.fromString(json.getAsString());
        }

        @Override
        public JsonElement serialize(ITier src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.name());
        }
    });
}
