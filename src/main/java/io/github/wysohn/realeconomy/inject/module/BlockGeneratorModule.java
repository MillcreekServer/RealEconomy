package io.github.wysohn.realeconomy.inject.module;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.github.wysohn.rapidframework3.interfaces.plugin.ITaskSupervisor;
import io.github.wysohn.realeconomy.interfaces.business.types.mining.IBlockGenerator;
import org.bukkit.Bukkit;

import java.util.Optional;

public class BlockGeneratorModule extends AbstractModule {
    @Provides
    @Singleton
    public IBlockGenerator blockGenerator(ITaskSupervisor task) {
        return (sloc, material) -> Optional.of(sloc.getWorld())
                .map(Bukkit::getWorld)
                .ifPresent(world -> {
                    try {
                        task.sync(() -> {
                            world.getBlockAt(sloc.getX(), sloc.getY(), sloc.getZ()).setType(material);
                            return null;
                        }).get();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }
}
