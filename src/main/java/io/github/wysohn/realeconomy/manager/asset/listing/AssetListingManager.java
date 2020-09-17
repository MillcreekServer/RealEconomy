package io.github.wysohn.realeconomy.manager.asset.listing;

import io.github.wysohn.rapidframework3.core.inject.annotations.PluginDirectory;
import io.github.wysohn.rapidframework3.core.main.Manager;
import io.github.wysohn.rapidframework3.utils.sql.SQLSession;
import io.github.wysohn.rapidframework3.utils.sql.SQLiteSession;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.File;

@Singleton
public class AssetListingManager extends Manager {
    private final File pluginDir;

    private SQLSession sqlSession;

    @Inject
    public AssetListingManager(@PluginDirectory File pluginDir) {
        this.pluginDir = pluginDir;
    }

    @Override
    public void enable() throws Exception {
        pluginDir.mkdirs();
        File dbFile = new File(pluginDir, "assetListings.db");
        sqlSession = new SQLiteSession(dbFile, (connection -> {
            //TODO CREATE TABLE IF NOT EXISTS
        }));
    }

    @Override
    public void load() throws Exception {

    }

    @Override
    public void disable() throws Exception {

    }
}
