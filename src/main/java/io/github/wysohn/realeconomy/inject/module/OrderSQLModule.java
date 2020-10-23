package io.github.wysohn.realeconomy.inject.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.github.wysohn.rapidframework3.core.inject.annotations.PluginDirectory;
import io.github.wysohn.rapidframework3.interfaces.io.IPluginResourceProvider;
import io.github.wysohn.rapidframework3.interfaces.plugin.IShutdownHandle;
import io.github.wysohn.rapidframework3.utils.sql.SQLSession;
import io.github.wysohn.realeconomy.inject.annotation.OrderSQL;

import java.io.File;
import java.sql.SQLException;

public class OrderSQLModule extends AbstractModule {
    public static final String ORDER_ID = "order_id";
    public static final String CATEGORY_ID = "category_id";

    @Provides
    @Singleton
    @OrderSQL
    SQLSession sqlSession(@PluginDirectory File pluginDir,
                          IPluginResourceProvider resourceProvider,
                          IShutdownHandle shutdownHandle) throws SQLException {
        return SQLSession.Builder.sqlite(new File(pluginDir, "orders.db"))
                .createTable("buy_orders", tableInitializer -> tableInitializer.ifNotExist()
                        .field(ORDER_ID, "integer",
                                SQLSession.Attribute.PRIMARY_KEY, SQLSession.Attribute.AUTO_INCREMENT)
                        .field("listing_uuid", "char(36)", SQLSession.Attribute.NOT_NULL)
                        .field("timestamp", "datetime", SQLSession.Attribute.NOT_NULL)
                        .field("issuer", "char(36)", SQLSession.Attribute.NOT_NULL)
                        .field("price", "double precision",
                                SQLSession.Attribute.KEY, SQLSession.Attribute.NOT_NULL)
                        .field("currency_uuid", "char(36)", SQLSession.Attribute.NOT_NULL)
                        .field("amount", "integer", SQLSession.Attribute.NOT_NULL)
                        .field("maximum", "integer", SQLSession.Attribute.NOT_NULL))
                .createTable("sell_orders", tableInitializer -> tableInitializer.ifNotExist()
                        .field(ORDER_ID, "integer",
                                SQLSession.Attribute.PRIMARY_KEY, SQLSession.Attribute.AUTO_INCREMENT)
                        .field("listing_uuid", "char(36)", SQLSession.Attribute.NOT_NULL)
                        .field(CATEGORY_ID, "integer")
                        .field("timestamp", "datetime", SQLSession.Attribute.NOT_NULL)
                        .field("issuer", "char(36)", SQLSession.Attribute.NOT_NULL)
                        .field("price", "double precision",
                                SQLSession.Attribute.KEY, SQLSession.Attribute.NOT_NULL)
                        .field("currency_uuid", "char(36)", SQLSession.Attribute.NOT_NULL)
                        .field("amount", "integer", SQLSession.Attribute.NOT_NULL)
                        .field("maximum", "integer", SQLSession.Attribute.NOT_NULL))
                .createTable("category", tableInitializer -> tableInitializer.ifNotExist()
                        .field(CATEGORY_ID, "integer",
                                SQLSession.Attribute.PRIMARY_KEY, SQLSession.Attribute.AUTO_INCREMENT)
                        .field("category_value varchar(256) UNIQUE"))
                .createTable("trade_logs", tableInitializer -> tableInitializer.ifNotExist()
                        .field(ORDER_ID, "integer",
                                SQLSession.Attribute.PRIMARY_KEY, SQLSession.Attribute.AUTO_INCREMENT)
                        .field("listing_uuid", "char(36)", SQLSession.Attribute.NOT_NULL)
                        .field("timestamp", "datetime", SQLSession.Attribute.NOT_NULL)
                        .field("seller", "char(36)", SQLSession.Attribute.NOT_NULL)
                        .field("buyer", "char(36)", SQLSession.Attribute.NOT_NULL)
                        .field("price", "double precision", SQLSession.Attribute.NOT_NULL)
                        .field("currency_uuid", "char(36)", SQLSession.Attribute.NOT_NULL)
                        .field("amount", "integer", SQLSession.Attribute.NOT_NULL))
                .build();
    }
}
