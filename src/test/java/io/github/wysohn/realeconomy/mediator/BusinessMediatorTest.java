package io.github.wysohn.realeconomy.mediator;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import io.github.wysohn.rapidframework3.bukkit.data.BukkitPlayer;
import io.github.wysohn.rapidframework3.bukkit.manager.location.ManagerPlayerLocation;
import io.github.wysohn.rapidframework3.bukkit.testutils.manager.AbstractBukkitManagerTest;
import io.github.wysohn.rapidframework3.core.inject.module.PluginInfoModule;
import io.github.wysohn.rapidframework3.core.inject.module.TaskSupervisorModule;
import io.github.wysohn.rapidframework3.core.inject.module.TypeAsserterModule;
import io.github.wysohn.rapidframework3.core.main.PluginMain;
import io.github.wysohn.rapidframework3.data.SimpleLocation;
import io.github.wysohn.rapidframework3.interfaces.plugin.ITaskSupervisor;
import io.github.wysohn.rapidframework3.interfaces.serialize.ISerializer;
import io.github.wysohn.rapidframework3.interfaces.store.IKeyValueStorage;
import io.github.wysohn.rapidframework3.testmodules.*;
import io.github.wysohn.rapidframework3.utils.Pair;
import io.github.wysohn.realeconomy.inject.module.BusinessConstantsModule;
import io.github.wysohn.realeconomy.interfaces.business.IBusinessContextHandler;
import io.github.wysohn.realeconomy.interfaces.business.IBusinessProvider;
import io.github.wysohn.realeconomy.interfaces.business.tiers.ITier;
import io.github.wysohn.realeconomy.interfaces.business.types.mining.IBlockGenerator;
import io.github.wysohn.realeconomy.manager.business.tiers.TierAdapter;
import io.github.wysohn.realeconomy.manager.business.tiers.TierRegistry;
import io.github.wysohn.realeconomy.manager.business.types.AbstractBusiness;
import io.github.wysohn.realeconomy.manager.business.types.mining.MiningBusinessManager;
import io.github.wysohn.realeconomy.manager.claim.ChunkClaimManager;
import io.github.wysohn.realeconomy.manager.listing.AssetListingManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.PluginManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.ref.Reference;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class BusinessMediatorTest extends AbstractBukkitManagerTest {
    private final ExecutorService exec = Executors.newSingleThreadExecutor();

    private final List<AbstractModule> moduleList = new LinkedList<>();
    private IKeyValueStorage tierStorage;
    private IBusinessContextHandler visitStateProvider;
    private AssetListingManager assetListingManager;
    private ISerializer serializer;
    private PluginMain main;

    @Before
    public void init() {
        tierStorage = mock(IKeyValueStorage.class);
        visitStateProvider = mock(IBusinessContextHandler.class);
        assetListingManager = mock(AssetListingManager.class);
        serializer = mock(ISerializer.class);
        main = mock(PluginMain.class);

        moduleList.add(new PluginInfoModule("test", "test", "test"));
        moduleList.add(new MockConfigModule(
                Pair.of(ChunkClaimManager.KEY_ENABLE, true)
        ));
        moduleList.add(new MockLoggerModule());
        moduleList.add(new MockPluginDirectoryModule());
        moduleList.add(new MockStorageFactoryModule(tierStorage));
        moduleList.add(new AbstractModule() {
            @Provides
            IBusinessContextHandler visitStateProvider() {
                return visitStateProvider;
            }

            @Provides
            AssetListingManager assetListingManager() {
                return assetListingManager;
            }

            @Provides
            IBlockGenerator blockGenerator() {
                return mock(IBlockGenerator.class);
            }

            @Provides
            PluginMain pluginMain() {
                return main;
            }
        });
        moduleList.add(new TaskSupervisorModule(new ITaskSupervisor() {
            @Override
            public <V> Future<V> sync(Callable<V> callable) {
                return exec.submit(callable);
            }

            @Override
            public void sync(Runnable runnable) {
                try {
                    exec.submit(runnable).get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public <V> Future<V> async(Callable<V> callable) {
                return exec.submit(callable);
            }

            @Override
            public void async(Runnable runnable) {
                exec.submit(runnable);
            }
        }));
        moduleList.add(new MockShutdownModule(() -> {
        }));
        moduleList.add(new MockSerializerModule(serializer));
        moduleList.add(new TypeAsserterModule());
        moduleList.add(new BusinessConstantsModule());
    }

    @Test
    public void enable() throws Exception {
        BusinessMediator mediator = Guice.createInjector(moduleList).getInstance(BusinessMediator.class);
        Object tierSection = mock(Object.class);
        Object subTypeSection = mock(Object.class);

        when(tierStorage.getKeys(anyBoolean())).thenReturn(new HashSet<String>() {{
            add("mining");
        }});
        when(tierStorage.get(eq("mining"))).thenReturn(Optional.of(tierSection));
        when(tierStorage.get(eq(tierSection), eq(ITier.DEFAULT_SUB_TYPE))).thenReturn(Optional.of(subTypeSection));

        mediator.enable();
    }

    @Test
    public void openNewBusiness() throws Exception {
        Injector injector = Guice.createInjector(moduleList);
        MiningBusinessManager mining = injector.getInstance(MiningBusinessManager.class);
        BusinessMediator mediator = injector.getInstance(BusinessMediator.class);
        Object tierSection = mock(Object.class);
        Object subTypeSection = mock(Object.class);

        when(tierStorage.getKeys(anyBoolean())).thenReturn(new HashSet<String>() {{
            add("mining");
        }});
        when(tierStorage.get(eq("mining"))).thenReturn(Optional.of(tierSection));
        when(tierStorage.get(eq(tierSection), eq(ITier.DEFAULT_SUB_TYPE))).thenReturn(Optional.of(subTypeSection));

        mediator.preload();
        mediator.enable();

        AbstractBusiness business = mediator.openNewBusiness("mining", UUID.randomUUID(), ITier.DEFAULT_SUB_TYPE);
        assertNotNull(mediator.getBusiness(business.getUuid()));

        mediator.deleteBusiness(business);
    }

    @Test
    public void getUsingBusiness() throws Exception {
        List<ManagerPlayerLocation.PlayerTracker> listeners = new ArrayList<>();
        PluginManager manager = mock(PluginManager.class);
        when(Bukkit.getPluginManager()).thenReturn(manager);
        doAnswer(invocation -> {
            listeners.add((ManagerPlayerLocation.PlayerTracker) invocation.getArguments()[0]);
            return null;
        }).when(manager).registerEvents(any(), any());

        Injector injector = Guice.createInjector(moduleList);
        MiningBusinessManager mining = injector.getInstance(MiningBusinessManager.class);
        ChunkClaimManager chunkClaimManager = injector.getInstance(ChunkClaimManager.class);
        ManagerPlayerLocation managerPlayerLocation = injector.getInstance(ManagerPlayerLocation.class);
        BusinessMediator mediator = injector.getInstance(BusinessMediator.class);
        Object tierSection = mock(Object.class);
        Object subTypeSection = mock(Object.class);
        Map<String, Object> requirementSection = mock(Map.class);
        BukkitPlayer player = mock(BukkitPlayer.class);
        UUID uuid = UUID.randomUUID();
        SimpleLocation location = new SimpleLocation("world", 1, 2, 3);

        managerPlayerLocation.enable();

        when(tierStorage.getKeys(anyBoolean())).thenReturn(new HashSet<String>() {{
            add("mining");
        }});
        when(tierStorage.get(eq("mining"))).thenReturn(Optional.of(tierSection));
        when(tierStorage.get(eq(tierSection), eq(ITier.DEFAULT_SUB_TYPE))).thenReturn(Optional.of(subTypeSection));
        when(tierStorage.isSection(eq(requirementSection))).thenReturn(true);
        when(tierStorage.get(eq(subTypeSection), eq(TierAdapter.REQUIREMENT))).thenReturn(Optional.of(requirementSection));
        when(tierStorage.get(eq(subTypeSection), eq(TierAdapter.INPUT))).thenReturn(Optional.of(requirementSection));
        when(tierStorage.get(eq(subTypeSection), eq(TierAdapter.OUTPUT))).thenReturn(Optional.of(requirementSection));
        when(tierStorage.get(eq(subTypeSection), eq(TierAdapter.TIME_TO_LIVE_MIN))).thenReturn(Optional.empty());
        when(tierStorage.get(eq(subTypeSection), eq(TierAdapter.TIME_TO_LIVE_MAX))).thenReturn(Optional.empty());
        when(player.getUuid()).thenReturn(uuid);
        when(player.getSloc()).thenReturn(location);

        mediator.preload();
        mediator.enable();

        Player p = mock(Player.class);
        Location loc = mock(Location.class, RETURNS_DEEP_STUBS);
        when(p.getUniqueId()).thenReturn(uuid);
        when(p.getLocation()).thenReturn(loc);
        when(loc.getWorld().getName()).thenReturn("world");
        when(loc.getX()).thenReturn(1.0);
        when(loc.getY()).thenReturn(2.0);
        when(loc.getZ()).thenReturn(3.0);
        PlayerJoinEvent event = new PlayerJoinEvent(p, "join");
        listeners.forEach(playerTracker -> playerTracker.onJoin(event));

        assertEquals(BusinessMediator.Result.OK, mediator.openNewBusinessLocation("mining", ITier.DEFAULT_SUB_TYPE, player));
        assertTrue(mediator.getUsingBusiness(uuid).size() > 0);
    }

    @Test
    public void isMember() throws Exception {
        Injector injector = Guice.createInjector(moduleList);
        MiningBusinessManager mining = injector.getInstance(MiningBusinessManager.class);
        ChunkClaimManager chunkClaimManager = injector.getInstance(ChunkClaimManager.class);
        ManagerPlayerLocation managerPlayerLocation = injector.getInstance(ManagerPlayerLocation.class);
        BusinessMediator mediator = injector.getInstance(BusinessMediator.class);
        Object tierSection = mock(Object.class);
        Object subTypeSection = mock(Object.class);
        Map<String, Object> requirementSection = mock(Map.class);
        BukkitPlayer player = mock(BukkitPlayer.class);
        UUID uuid = UUID.randomUUID();
        SimpleLocation location = new SimpleLocation("world", 1, 2, 3);

        managerPlayerLocation.enable();

        when(tierStorage.getKeys(anyBoolean())).thenReturn(new HashSet<String>() {{
            add("mining");
        }});
        when(tierStorage.get(eq("mining"))).thenReturn(Optional.of(tierSection));
        when(tierStorage.get(eq(tierSection), eq(ITier.DEFAULT_SUB_TYPE))).thenReturn(Optional.of(subTypeSection));
        when(tierStorage.isSection(eq(requirementSection))).thenReturn(true);
        when(tierStorage.get(eq(subTypeSection), eq(TierAdapter.REQUIREMENT))).thenReturn(Optional.of(requirementSection));
        when(tierStorage.get(eq(subTypeSection), eq(TierAdapter.INPUT))).thenReturn(Optional.of(requirementSection));
        when(tierStorage.get(eq(subTypeSection), eq(TierAdapter.OUTPUT))).thenReturn(Optional.of(requirementSection));
        when(tierStorage.get(eq(subTypeSection), eq(TierAdapter.TIME_TO_LIVE_MIN))).thenReturn(Optional.empty());
        when(tierStorage.get(eq(subTypeSection), eq(TierAdapter.TIME_TO_LIVE_MAX))).thenReturn(Optional.empty());
        when(player.getUuid()).thenReturn(uuid);
        when(player.getSloc()).thenReturn(location);

        mediator.preload();
        mediator.enable();

        assertEquals(BusinessMediator.Result.OK, mediator.openNewBusinessLocation("mining", ITier.DEFAULT_SUB_TYPE, player));
        assertTrue(mediator.isMember(mining.get(chunkClaimManager.queryBusiness(location))
                .map(Reference::get).orElseThrow(RuntimeException::new), uuid));
    }

    @Test
    public void addMember() throws Exception {
        Injector injector = Guice.createInjector(moduleList);
        MiningBusinessManager mining = injector.getInstance(MiningBusinessManager.class);
        ChunkClaimManager chunkClaimManager = injector.getInstance(ChunkClaimManager.class);
        ManagerPlayerLocation managerPlayerLocation = injector.getInstance(ManagerPlayerLocation.class);
        BusinessMediator mediator = injector.getInstance(BusinessMediator.class);
        Object tierSection = mock(Object.class);
        Object subTypeSection = mock(Object.class);
        Map<String, Object> requirementSection = mock(Map.class);
        BukkitPlayer player = mock(BukkitPlayer.class);
        UUID uuid = UUID.randomUUID();
        SimpleLocation location = new SimpleLocation("world", 1, 2, 3);

        managerPlayerLocation.enable();

        when(tierStorage.getKeys(anyBoolean())).thenReturn(new HashSet<String>() {{
            add("mining");
        }});
        when(tierStorage.get(eq("mining"))).thenReturn(Optional.of(tierSection));
        when(tierStorage.get(eq(tierSection), eq(ITier.DEFAULT_SUB_TYPE))).thenReturn(Optional.of(subTypeSection));
        when(tierStorage.isSection(eq(requirementSection))).thenReturn(true);
        when(tierStorage.get(eq(subTypeSection), eq(TierAdapter.REQUIREMENT))).thenReturn(Optional.of(requirementSection));
        when(tierStorage.get(eq(subTypeSection), eq(TierAdapter.INPUT))).thenReturn(Optional.of(requirementSection));
        when(tierStorage.get(eq(subTypeSection), eq(TierAdapter.OUTPUT))).thenReturn(Optional.of(requirementSection));
        when(tierStorage.get(eq(subTypeSection), eq(TierAdapter.TIME_TO_LIVE_MIN))).thenReturn(Optional.empty());
        when(tierStorage.get(eq(subTypeSection), eq(TierAdapter.TIME_TO_LIVE_MAX))).thenReturn(Optional.empty());
        when(player.getUuid()).thenReturn(uuid);
        when(player.getSloc()).thenReturn(location);

        mediator.preload();
        mediator.enable();

        assertEquals(BusinessMediator.Result.OK, mediator.openNewBusinessLocation("mining", ITier.DEFAULT_SUB_TYPE, player));
        assertTrue(mediator.isMember(mining.get(chunkClaimManager.queryBusiness(location))
                .map(Reference::get).orElseThrow(RuntimeException::new), uuid));

        UUID uuid2 = UUID.randomUUID();
        assertTrue(mediator.addMember(mining.get(chunkClaimManager.queryBusiness(location))
                .map(Reference::get).orElseThrow(RuntimeException::new), uuid2));
        assertTrue(mediator.isMember(mining.get(chunkClaimManager.queryBusiness(location))
                .map(Reference::get).orElseThrow(RuntimeException::new), uuid2));

        assertTrue(mediator.removeMember(mining.get(chunkClaimManager.queryBusiness(location))
                .map(Reference::get).orElseThrow(RuntimeException::new), uuid2));
        assertFalse(mediator.isMember(mining.get(chunkClaimManager.queryBusiness(location))
                .map(Reference::get).orElseThrow(RuntimeException::new), uuid2));
    }

    @Test
    public void openNewBusinessInChunk() throws Exception {
        Injector injector = Guice.createInjector(moduleList);
        MiningBusinessManager mining = injector.getInstance(MiningBusinessManager.class);
        ChunkClaimManager chunkClaimManager = injector.getInstance(ChunkClaimManager.class);
        BusinessMediator mediator = injector.getInstance(BusinessMediator.class);
        Object tierSection = mock(Object.class);
        Object subTypeSection = mock(Object.class);
        Map<String, Object> requirementSection = mock(Map.class);
        BukkitPlayer player = mock(BukkitPlayer.class);
        UUID uuid = UUID.randomUUID();
        SimpleLocation location = new SimpleLocation("world", 1, 2, 3);

        when(tierStorage.getKeys(anyBoolean())).thenReturn(new HashSet<String>() {{
            add("mining");
        }});
        when(tierStorage.get(eq("mining"))).thenReturn(Optional.of(tierSection));
        when(tierStorage.get(eq(tierSection), eq(ITier.DEFAULT_SUB_TYPE))).thenReturn(Optional.of(subTypeSection));
        when(tierStorage.isSection(eq(requirementSection))).thenReturn(true);
        when(tierStorage.get(eq(subTypeSection), eq(TierAdapter.REQUIREMENT))).thenReturn(Optional.of(requirementSection));
        when(tierStorage.get(eq(subTypeSection), eq(TierAdapter.INPUT))).thenReturn(Optional.of(requirementSection));
        when(tierStorage.get(eq(subTypeSection), eq(TierAdapter.OUTPUT))).thenReturn(Optional.of(requirementSection));
        when(tierStorage.get(eq(subTypeSection), eq(TierAdapter.TIME_TO_LIVE_MIN))).thenReturn(Optional.empty());
        when(tierStorage.get(eq(subTypeSection), eq(TierAdapter.TIME_TO_LIVE_MAX))).thenReturn(Optional.empty());
        when(player.getUuid()).thenReturn(uuid);
        when(player.getSloc()).thenReturn(location);

        mediator.preload();
        mediator.enable();

        BusinessMediator.Result result = mediator.openNewBusinessLocation("mining", ITier.DEFAULT_SUB_TYPE, player);
        assertEquals(BusinessMediator.Result.OK, result);
    }

    @After
    public void end() throws Exception {
        Field field = BusinessMediator.class.getDeclaredField("enabled");
        field.setAccessible(true);
        field.set(null, false);

        Field field2 = BusinessMediator.class.getDeclaredField("BUSINESSES_PROVIDERS");
        field2.setAccessible(true);
        Map<String, IBusinessProvider> map = (Map<String, IBusinessProvider>) field2.get(null);
        map.clear();

        Field field3 = BusinessMediator.class.getDeclaredField("BUSINESS_CONTEXT_HANDLERS");
        field3.setAccessible(true);
        List<IBusinessContextHandler> list = (List<IBusinessContextHandler>) field3.get(null);
        list.clear();

        Field field4 = TierRegistry.class.getDeclaredField("REGISTERED_TIERS");
        field4.setAccessible(true);
        Map<String, ITier> map2 = (Map<String, ITier>) field4.get(null);
        map2.clear();

        Field field5 = ManagerPlayerLocation.class.getDeclaredField("TRACKER");
        field5.setAccessible(true);
        field5.set(null, null);
    }
}