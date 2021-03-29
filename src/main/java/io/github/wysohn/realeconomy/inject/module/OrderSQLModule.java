package io.github.wysohn.realeconomy.inject.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginDirectory;
import io.github.wysohn.rapidframework3.core.main.ManagerConfig;
import io.github.wysohn.rapidframework3.interfaces.io.IPluginResourceProvider;
import io.github.wysohn.rapidframework3.interfaces.plugin.IShutdownHandle;
import io.github.wysohn.rapidframework3.utils.sql.SQLSession;
import io.github.wysohn.realeconomy.inject.annotation.OrderSQL;

import java.io.File;
import java.sql.SQLException;

public class OrderSQLModule extends AbstractModule {
    public static final String ORDER_ID = "order_id";
    public static final String CATEGORY_ID = "category_id";
    public static final String LISTING_UUID = "listing_uuid";

    @Provides
    @Singleton
    @OrderSQL
    SQLSession sqlSession(@PluginDirectory File pluginDir,
                          IPluginResourceProvider resourceProvider,
                          IShutdownHandle shutdownHandle,
                          ManagerConfig config) throws SQLException {
        SQLSession.Builder builder = null;
        try {
            if (config.get("database.type")
                    .filter(String.class::isInstance)
                    .map(String.class::cast)
                    .map("mysql"::equals)
                    .orElse(false)) {
                builder = SQLSession.Builder.mysql(config.get("database.host")
                                .filter(String.class::isInstance)
                                .map(String.class::cast)
                                .orElseThrow(RuntimeException::new),
                        config.get("database.name")
                                .filter(String.class::isInstance)
                                .map(String.class::cast)
                                .orElseThrow(RuntimeException::new),
                        config.get("database.user")
                                .filter(String.class::isInstance)
                                .map(String.class::cast)
                                .orElseThrow(RuntimeException::new),
                        config.get("database.password")
                                .filter(String.class::isInstance)
                                .map(String.class::cast)
                                .orElseThrow(RuntimeException::new));
            } else {
                config.put("database.type", "sqlite");
                config.put("database.host", "127.0.0.1");
                config.put("database.name", "realeconomy");
                config.put("database.user", "re");
                config.put("database.password", "re");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (builder == null)
                builder = SQLSession.Builder.sqlite(new File(pluginDir, "orders.db"));
        }

        return builder
                .createTable("buy_orders", tableInitializer -> tableInitializer.ifNotExist()
                        .field(ORDER_ID, "integer",
                                SQLSession.Attribute.PRIMARY_KEY, SQLSession.Attribute.AUTO_INCREMENT)
                        .field(LISTING_UUID, "char(36)", SQLSession.Attribute.NOT_NULL)
                        .field(CATEGORY_ID, "integer")
                        .field("timestamp", "datetime", SQLSession.Attribute.NOT_NULL)
                        .field("issuer", "char(36)", SQLSession.Attribute.NOT_NULL)
                        .field("price", "double precision", SQLSession.Attribute.NOT_NULL)
                        .field("currency_uuid", "char(36)", SQLSession.Attribute.NOT_NULL)
                        .field("amount", "integer", SQLSession.Attribute.NOT_NULL)
                        .field("maximum", "integer", SQLSession.Attribute.NOT_NULL)
                        .field("temp", "boolean"))
                .createTable("sell_orders", tableInitializer -> tableInitializer.ifNotExist()
                        .field(ORDER_ID, "integer",
                                SQLSession.Attribute.PRIMARY_KEY, SQLSession.Attribute.AUTO_INCREMENT)
                        .field(LISTING_UUID, "char(36)", SQLSession.Attribute.NOT_NULL)
                        .field(CATEGORY_ID, "integer")
                        .field("timestamp", "datetime", SQLSession.Attribute.NOT_NULL)
                        .field("issuer", "char(36)", SQLSession.Attribute.NOT_NULL)
                        .field("price", "double precision", SQLSession.Attribute.NOT_NULL)
                        .field("currency_uuid", "char(36)", SQLSession.Attribute.NOT_NULL)
                        .field("amount", "integer", SQLSession.Attribute.NOT_NULL)
                        .field("maximum", "integer", SQLSession.Attribute.NOT_NULL)
                        .field("temp", "boolean"))
                .createTable("category", tableInitializer -> tableInitializer.ifNotExist()
                        .field(CATEGORY_ID, "integer",
                                SQLSession.Attribute.PRIMARY_KEY, SQLSession.Attribute.AUTO_INCREMENT)
                        .field("category_value varchar(256) UNIQUE"))
                .createTable("trade_logs", tableInitializer -> tableInitializer.ifNotExist()
                        .field(ORDER_ID, "integer",
                                SQLSession.Attribute.PRIMARY_KEY, SQLSession.Attribute.AUTO_INCREMENT)
                        .field(LISTING_UUID, "char(36)", SQLSession.Attribute.NOT_NULL)
                        .field("category_id", "integer", SQLSession.Attribute.NOT_NULL)
                        .field("timestamp", "datetime", SQLSession.Attribute.NOT_NULL)
                        .field("seller", "char(36)", SQLSession.Attribute.NOT_NULL)
                        .field("buyer", "char(36)", SQLSession.Attribute.NOT_NULL)
                        .field("price", "double precision", SQLSession.Attribute.NOT_NULL)
                        .field("currency_uuid", "char(36)", SQLSession.Attribute.NOT_NULL)
                        .field("amount", "integer", SQLSession.Attribute.NOT_NULL))
                .createTable("listing_names", tableInitializer -> tableInitializer.ifNotExist()
                        .field("id", "integer",
                                SQLSession.Attribute.PRIMARY_KEY, SQLSession.Attribute.AUTO_INCREMENT)
                        .field(LISTING_UUID, "char(36)", SQLSession.Attribute.NOT_NULL, SQLSession.Attribute.UNIQUE)
                        .field("name", "text", SQLSession.Attribute.NOT_NULL))
                .createTable("currency_names", tableInitializer -> tableInitializer.ifNotExist()
                        .field("id", "integer",
                                SQLSession.Attribute.PRIMARY_KEY, SQLSession.Attribute.AUTO_INCREMENT)
                        .field("currency_uuid", "char(36)", SQLSession.Attribute.NOT_NULL, SQLSession.Attribute.UNIQUE)
                        .field("full", "text", SQLSession.Attribute.NOT_NULL)
                        .field("short", "text", SQLSession.Attribute.NOT_NULL))
                .build();
    }
}
