package io.github.wysohn.realeconomy.manager;

import copy.com.google.gson.*;
import io.github.wysohn.rapidframework3.interfaces.serialize.CustomAdapter;
import io.github.wysohn.rapidframework3.utils.Pair;
import io.github.wysohn.realeconomy.interfaces.banking.IAccount;
import io.github.wysohn.realeconomy.interfaces.banking.IBankingType;
import io.github.wysohn.realeconomy.manager.banking.BankingTypeRegistry;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.UUID;

public class CustomTypeAdapters {
    private static final String PACKAGE_NAME = "io.github.wysohn.realeconomy.manager.banking.account";
    private static final String KEY_CLASS = "type";
    private static final String KEY_VALUE = "value";

    public static final Pair<Class<?>, CustomAdapter<?>> ACCOUNT = Pair.of(IAccount.class, new CustomAdapter<IAccount>() {
        @Override
        public IAccount deserialize(
                JsonElement jsonElement,
                Type type,
                JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = jsonElement.getAsJsonObject();
            String className = obj.get(KEY_CLASS).getAsString();
            try {
                Class<?> clazz = Class.forName(PACKAGE_NAME + "." + className);
                return context.deserialize(obj.get(KEY_VALUE), clazz);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public JsonElement serialize(
                IAccount iAccount, Type type, JsonSerializationContext context) {
            String className = iAccount.getClass().getSimpleName();
            JsonElement serializedValue = context.serialize(iAccount);

            JsonObject obj = new JsonObject();
            obj.addProperty(KEY_CLASS, className);
            obj.add(KEY_VALUE, serializedValue);
            return obj;
        }
    });

    public static final Pair<Class<?>, CustomAdapter<?>> BANKING_TYPE = Pair.of(IBankingType.class, new CustomAdapter<IBankingType>() {
        @Override
        public IBankingType deserialize(
                JsonElement jsonElement,
                Type type,
                JsonDeserializationContext context) throws JsonParseException {
            return Objects.requireNonNull(BankingTypeRegistry.fromUuid(context.deserialize(jsonElement, UUID.class)));
        }

        @Override
        public JsonElement serialize(
                IBankingType iBankingType, Type type, JsonSerializationContext context) {
            return context.serialize(iBankingType.getUuid());
        }
    });
}
