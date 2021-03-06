package com.raoulvdberge.refinedstorage;

import com.raoulvdberge.refinedstorage.api.network.NetworkType;
import com.raoulvdberge.refinedstorage.api.network.grid.GridType;
import com.raoulvdberge.refinedstorage.api.network.node.INetworkNode;
import com.raoulvdberge.refinedstorage.api.storage.StorageType;
import com.raoulvdberge.refinedstorage.apiimpl.API;
import com.raoulvdberge.refinedstorage.apiimpl.network.NetworkListener;
import com.raoulvdberge.refinedstorage.apiimpl.network.NetworkNodeListener;
import com.raoulvdberge.refinedstorage.apiimpl.network.grid.factory.GridBlockGridFactory;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.*;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.diskdrive.DiskDriveNetworkNode;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.storage.FluidStorageNetworkNode;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.storage.StorageNetworkNode;
import com.raoulvdberge.refinedstorage.apiimpl.storage.FluidStorageType;
import com.raoulvdberge.refinedstorage.apiimpl.storage.ItemStorageType;
import com.raoulvdberge.refinedstorage.apiimpl.storage.disk.factory.FluidStorageDiskFactory;
import com.raoulvdberge.refinedstorage.apiimpl.storage.disk.factory.ItemStorageDiskFactory;
import com.raoulvdberge.refinedstorage.apiimpl.storage.externalstorage.FluidExternalStorageProvider;
import com.raoulvdberge.refinedstorage.apiimpl.storage.externalstorage.ItemExternalStorageProvider;
import com.raoulvdberge.refinedstorage.block.*;
import com.raoulvdberge.refinedstorage.capability.NetworkNodeProxyCapability;
import com.raoulvdberge.refinedstorage.config.ClientConfig;
import com.raoulvdberge.refinedstorage.config.ServerConfig;
import com.raoulvdberge.refinedstorage.container.*;
import com.raoulvdberge.refinedstorage.container.factory.GridContainerFactory;
import com.raoulvdberge.refinedstorage.container.factory.PositionalTileContainerFactory;
import com.raoulvdberge.refinedstorage.item.*;
import com.raoulvdberge.refinedstorage.item.blockitem.ControllerBlockItem;
import com.raoulvdberge.refinedstorage.item.blockitem.FluidStorageBlockItem;
import com.raoulvdberge.refinedstorage.item.blockitem.StorageBlockItem;
import com.raoulvdberge.refinedstorage.item.group.MainItemGroup;
import com.raoulvdberge.refinedstorage.loottable.StorageBlockLootFunctionSerializer;
import com.raoulvdberge.refinedstorage.network.NetworkHandler;
import com.raoulvdberge.refinedstorage.recipe.UpgradeWithEnchantedBookRecipeSerializer;
import com.raoulvdberge.refinedstorage.tile.*;
import com.raoulvdberge.refinedstorage.tile.data.TileDataManager;
import com.raoulvdberge.refinedstorage.tile.grid.GridTile;
import com.raoulvdberge.refinedstorage.util.BlockUtils;
import net.minecraft.block.Block;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.world.storage.loot.functions.LootFunctionManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.extensions.IForgeContainerType;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(RS.ID)
public final class RS {
    public static final String ID = "refinedstorage";

    public static RS INSTANCE;
    public RSOldConfig config = new RSOldConfig();

    public static final NetworkHandler NETWORK_HANDLER = new NetworkHandler();
    public static final ItemGroup MAIN_GROUP = new MainItemGroup();
    public static final ServerConfig SERVER_CONFIG = new ServerConfig();
    public static final ClientConfig CLIENT_CONFIG = new ClientConfig();

    public RS() {
        DistExecutor.runWhenOn(Dist.CLIENT, () -> ClientSetup::new);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SERVER_CONFIG.getSpec());
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_CONFIG.getSpec());

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onCommonSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addGenericListener(Block.class, this::onRegisterBlocks);
        FMLJavaModLoadingContext.get().getModEventBus().addGenericListener(TileEntityType.class, this::onRegisterTiles);
        FMLJavaModLoadingContext.get().getModEventBus().addGenericListener(Item.class, this::onRegisterItems);
        FMLJavaModLoadingContext.get().getModEventBus().addGenericListener(IRecipeSerializer.class, this::onRegisterRecipeSerializers);
        FMLJavaModLoadingContext.get().getModEventBus().addGenericListener(ContainerType.class, this::onRegisterContainers);
    }

    @SubscribeEvent
    public void onCommonSetup(FMLCommonSetupEvent e) {
        NETWORK_HANDLER.register();

        NetworkNodeProxyCapability.register();

        MinecraftForge.EVENT_BUS.register(new NetworkNodeListener());
        MinecraftForge.EVENT_BUS.register(new NetworkListener());

        API.instance().getStorageDiskRegistry().add(ItemStorageDiskFactory.ID, new ItemStorageDiskFactory());
        API.instance().getStorageDiskRegistry().add(FluidStorageDiskFactory.ID, new FluidStorageDiskFactory());

        API.instance().getNetworkNodeRegistry().add(DiskDriveNetworkNode.ID, (tag, world, pos) -> readAndReturn(tag, new DiskDriveNetworkNode(world, pos)));
        API.instance().getNetworkNodeRegistry().add(CableNetworkNode.ID, (tag, world, pos) -> readAndReturn(tag, new CableNetworkNode(world, pos)));
        API.instance().getNetworkNodeRegistry().add(GridNetworkNode.ID, (tag, world, pos) -> readAndReturn(tag, new GridNetworkNode(world, pos, GridType.NORMAL)));
        API.instance().getNetworkNodeRegistry().add(GridNetworkNode.CRAFTING_ID, (tag, world, pos) -> readAndReturn(tag, new GridNetworkNode(world, pos, GridType.CRAFTING)));
        API.instance().getNetworkNodeRegistry().add(GridNetworkNode.PATTERN_ID, (tag, world, pos) -> readAndReturn(tag, new GridNetworkNode(world, pos, GridType.PATTERN)));
        API.instance().getNetworkNodeRegistry().add(GridNetworkNode.FLUID_ID, (tag, world, pos) -> readAndReturn(tag, new GridNetworkNode(world, pos, GridType.FLUID)));

        API.instance().getNetworkNodeRegistry().add(StorageNetworkNode.ONE_K_STORAGE_BLOCK_ID, (tag, world, pos) -> readAndReturn(tag, new StorageNetworkNode(world, pos, ItemStorageType.ONE_K)));
        API.instance().getNetworkNodeRegistry().add(StorageNetworkNode.FOUR_K_STORAGE_BLOCK_ID, (tag, world, pos) -> readAndReturn(tag, new StorageNetworkNode(world, pos, ItemStorageType.FOUR_K)));
        API.instance().getNetworkNodeRegistry().add(StorageNetworkNode.SIXTEEN_K_STORAGE_BLOCK_ID, (tag, world, pos) -> readAndReturn(tag, new StorageNetworkNode(world, pos, ItemStorageType.SIXTEEN_K)));
        API.instance().getNetworkNodeRegistry().add(StorageNetworkNode.SIXTY_FOUR_K_STORAGE_BLOCK_ID, (tag, world, pos) -> readAndReturn(tag, new StorageNetworkNode(world, pos, ItemStorageType.SIXTY_FOUR_K)));
        API.instance().getNetworkNodeRegistry().add(StorageNetworkNode.CREATIVE_STORAGE_BLOCK_ID, (tag, world, pos) -> readAndReturn(tag, new StorageNetworkNode(world, pos, ItemStorageType.CREATIVE)));

        API.instance().getNetworkNodeRegistry().add(FluidStorageNetworkNode.SIXTY_FOUR_K_FLUID_STORAGE_BLOCK_ID, (tag, world, pos) -> readAndReturn(tag, new FluidStorageNetworkNode(world, pos, FluidStorageType.SIXTY_FOUR_K)));
        API.instance().getNetworkNodeRegistry().add(FluidStorageNetworkNode.TWO_HUNDRED_FIFTY_SIX_K_FLUID_STORAGE_BLOCK_ID, (tag, world, pos) -> readAndReturn(tag, new FluidStorageNetworkNode(world, pos, FluidStorageType.TWO_HUNDRED_FIFTY_SIX_K)));
        API.instance().getNetworkNodeRegistry().add(FluidStorageNetworkNode.THOUSAND_TWENTY_FOUR_K_FLUID_STORAGE_BLOCK_ID, (tag, world, pos) -> readAndReturn(tag, new FluidStorageNetworkNode(world, pos, FluidStorageType.THOUSAND_TWENTY_FOUR_K)));
        API.instance().getNetworkNodeRegistry().add(FluidStorageNetworkNode.FOUR_THOUSAND_NINETY_SIX_K_FLUID_STORAGE_BLOCK_ID, (tag, world, pos) -> readAndReturn(tag, new FluidStorageNetworkNode(world, pos, FluidStorageType.FOUR_THOUSAND_NINETY_SIX_K)));
        API.instance().getNetworkNodeRegistry().add(FluidStorageNetworkNode.CREATIVE_FLUID_STORAGE_BLOCK_ID, (tag, world, pos) -> readAndReturn(tag, new FluidStorageNetworkNode(world, pos, FluidStorageType.CREATIVE)));

        API.instance().getNetworkNodeRegistry().add(ExternalStorageNetworkNode.ID, (tag, world, pos) -> readAndReturn(tag, new ExternalStorageNetworkNode(world, pos)));
        API.instance().getNetworkNodeRegistry().add(ImporterNetworkNode.ID, (tag, world, pos) -> readAndReturn(tag, new ImporterNetworkNode(world, pos)));
        API.instance().getNetworkNodeRegistry().add(ExporterNetworkNode.ID, (tag, world, pos) -> readAndReturn(tag, new ExporterNetworkNode(world, pos)));
        API.instance().getNetworkNodeRegistry().add(NetworkReceiverNetworkNode.ID, (tag, world, pos) -> readAndReturn(tag, new NetworkReceiverNetworkNode(world, pos)));
        API.instance().getNetworkNodeRegistry().add(NetworkTransmitterNetworkNode.ID, (tag, world, pos) -> readAndReturn(tag, new NetworkTransmitterNetworkNode(world, pos)));
        API.instance().getNetworkNodeRegistry().add(RelayNetworkNode.ID, (tag, world, pos) -> readAndReturn(tag, new RelayNetworkNode(world, pos)));
        API.instance().getNetworkNodeRegistry().add(DetectorNetworkNode.ID, (tag, world, pos) -> readAndReturn(tag, new DetectorNetworkNode(world, pos)));
        API.instance().getNetworkNodeRegistry().add(SecurityManagerNetworkNode.ID, (tag, world, pos) -> readAndReturn(tag, new SecurityManagerNetworkNode(world, pos)));

        API.instance().getGridManager().add(GridBlockGridFactory.ID, new GridBlockGridFactory());

        API.instance().addExternalStorageProvider(StorageType.ITEM, new ItemExternalStorageProvider());
        API.instance().addExternalStorageProvider(StorageType.FLUID, new FluidExternalStorageProvider());

        LootFunctionManager.registerFunction(new StorageBlockLootFunctionSerializer());
    }

    private INetworkNode readAndReturn(CompoundNBT tag, NetworkNode node) {
        node.read(tag);

        return node;
    }

    @SubscribeEvent
    public void onRegisterRecipeSerializers(RegistryEvent.Register<IRecipeSerializer<?>> e) {
        e.getRegistry().register(new UpgradeWithEnchantedBookRecipeSerializer().setRegistryName(RS.ID, "upgrade_with_enchanted_book"));
    }

    @SubscribeEvent
    public void onRegisterBlocks(RegistryEvent.Register<Block> e) {
        e.getRegistry().register(new QuartzEnrichedIronBlock());
        e.getRegistry().register(new ControllerBlock(NetworkType.NORMAL));
        e.getRegistry().register(new ControllerBlock(NetworkType.CREATIVE));
        e.getRegistry().register(new MachineCasingBlock());
        e.getRegistry().register(new CableBlock());
        e.getRegistry().register(new DiskDriveBlock());
        e.getRegistry().register(new GridBlock(GridType.NORMAL));
        e.getRegistry().register(new GridBlock(GridType.CRAFTING));
        e.getRegistry().register(new GridBlock(GridType.PATTERN));
        e.getRegistry().register(new GridBlock(GridType.FLUID));

        for (ItemStorageType type : ItemStorageType.values()) {
            e.getRegistry().register(new StorageBlock(type));
        }

        for (FluidStorageType type : FluidStorageType.values()) {
            e.getRegistry().register(new FluidStorageBlock(type));
        }

        e.getRegistry().register(new ExternalStorageBlock());
        e.getRegistry().register(new ImporterBlock());
        e.getRegistry().register(new ExporterBlock());
        e.getRegistry().register(new NetworkReceiverBlock());
        e.getRegistry().register(new NetworkTransmitterBlock());
        e.getRegistry().register(new RelayBlock());
        e.getRegistry().register(new DetectorBlock());
        e.getRegistry().register(new SecurityManagerBlock());
    }

    @SubscribeEvent
    public void onRegisterTiles(RegistryEvent.Register<TileEntityType<?>> e) {
        e.getRegistry().register(registerTileDataParameters(TileEntityType.Builder.create(() -> new ControllerTile(NetworkType.NORMAL), RSBlocks.CONTROLLER).build(null).setRegistryName(RS.ID, "controller")));
        e.getRegistry().register(registerTileDataParameters(TileEntityType.Builder.create(() -> new ControllerTile(NetworkType.CREATIVE), RSBlocks.CREATIVE_CONTROLLER).build(null).setRegistryName(RS.ID, "creative_controller")));
        e.getRegistry().register(TileEntityType.Builder.create(CableTile::new, RSBlocks.CABLE).build(null).setRegistryName(RS.ID, "cable"));
        e.getRegistry().register(registerTileDataParameters(TileEntityType.Builder.create(DiskDriveTile::new, RSBlocks.DISK_DRIVE).build(null).setRegistryName(RS.ID, "disk_drive")));
        e.getRegistry().register(registerTileDataParameters(TileEntityType.Builder.create(() -> new GridTile(GridType.NORMAL), RSBlocks.GRID).build(null).setRegistryName(RS.ID, "grid")));
        e.getRegistry().register(registerTileDataParameters(TileEntityType.Builder.create(() -> new GridTile(GridType.CRAFTING), RSBlocks.CRAFTING_GRID).build(null).setRegistryName(RS.ID, "crafting_grid")));
        e.getRegistry().register(registerTileDataParameters(TileEntityType.Builder.create(() -> new GridTile(GridType.PATTERN), RSBlocks.PATTERN_GRID).build(null).setRegistryName(RS.ID, "pattern_grid")));
        e.getRegistry().register(registerTileDataParameters(TileEntityType.Builder.create(() -> new GridTile(GridType.FLUID), RSBlocks.FLUID_GRID).build(null).setRegistryName(RS.ID, "fluid_grid")));

        e.getRegistry().register(registerTileDataParameters(TileEntityType.Builder.create(() -> new StorageTile(ItemStorageType.ONE_K), RSBlocks.ONE_K_STORAGE_BLOCK).build(null).setRegistryName(RS.ID, "1k_storage_block")));
        e.getRegistry().register(registerTileDataParameters(TileEntityType.Builder.create(() -> new StorageTile(ItemStorageType.FOUR_K), RSBlocks.FOUR_K_STORAGE_BLOCK).build(null).setRegistryName(RS.ID, "4k_storage_block")));
        e.getRegistry().register(registerTileDataParameters(TileEntityType.Builder.create(() -> new StorageTile(ItemStorageType.SIXTEEN_K), RSBlocks.SIXTEEN_K_STORAGE_BLOCK).build(null).setRegistryName(RS.ID, "16k_storage_block")));
        e.getRegistry().register(registerTileDataParameters(TileEntityType.Builder.create(() -> new StorageTile(ItemStorageType.SIXTY_FOUR_K), RSBlocks.SIXTY_FOUR_K_STORAGE_BLOCK).build(null).setRegistryName(RS.ID, "64k_storage_block")));
        e.getRegistry().register(registerTileDataParameters(TileEntityType.Builder.create(() -> new StorageTile(ItemStorageType.CREATIVE), RSBlocks.CREATIVE_STORAGE_BLOCK).build(null).setRegistryName(RS.ID, "creative_storage_block")));

        e.getRegistry().register(registerTileDataParameters(TileEntityType.Builder.create(() -> new FluidStorageTile(FluidStorageType.SIXTY_FOUR_K), RSBlocks.SIXTY_FOUR_K_FLUID_STORAGE_BLOCK).build(null).setRegistryName(RS.ID, "64k_fluid_storage_block")));
        e.getRegistry().register(registerTileDataParameters(TileEntityType.Builder.create(() -> new FluidStorageTile(FluidStorageType.TWO_HUNDRED_FIFTY_SIX_K), RSBlocks.TWO_HUNDRED_FIFTY_SIX_K_FLUID_STORAGE_BLOCK).build(null).setRegistryName(RS.ID, "256k_fluid_storage_block")));
        e.getRegistry().register(registerTileDataParameters(TileEntityType.Builder.create(() -> new FluidStorageTile(FluidStorageType.THOUSAND_TWENTY_FOUR_K), RSBlocks.THOUSAND_TWENTY_FOUR_K_FLUID_STORAGE_BLOCK).build(null).setRegistryName(RS.ID, "1024k_fluid_storage_block")));
        e.getRegistry().register(registerTileDataParameters(TileEntityType.Builder.create(() -> new FluidStorageTile(FluidStorageType.FOUR_THOUSAND_NINETY_SIX_K), RSBlocks.FOUR_THOUSAND_NINETY_SIX_K_FLUID_STORAGE_BLOCK).build(null).setRegistryName(RS.ID, "4096k_fluid_storage_block")));
        e.getRegistry().register(registerTileDataParameters(TileEntityType.Builder.create(() -> new FluidStorageTile(FluidStorageType.CREATIVE), RSBlocks.CREATIVE_FLUID_STORAGE_BLOCK).build(null).setRegistryName(RS.ID, "creative_fluid_storage_block")));

        e.getRegistry().register(registerTileDataParameters(TileEntityType.Builder.create(ExternalStorageTile::new, RSBlocks.EXTERNAL_STORAGE).build(null).setRegistryName(RS.ID, "external_storage")));
        e.getRegistry().register(registerTileDataParameters(TileEntityType.Builder.create(ImporterTile::new, RSBlocks.IMPORTER).build(null).setRegistryName(RS.ID, "importer")));
        e.getRegistry().register(registerTileDataParameters(TileEntityType.Builder.create(ExporterTile::new, RSBlocks.EXPORTER).build(null).setRegistryName(RS.ID, "exporter")));
        e.getRegistry().register(registerTileDataParameters(TileEntityType.Builder.create(NetworkReceiverTile::new, RSBlocks.NETWORK_RECEIVER).build(null).setRegistryName(RS.ID, "network_receiver")));
        e.getRegistry().register(registerTileDataParameters(TileEntityType.Builder.create(NetworkTransmitterTile::new, RSBlocks.NETWORK_TRANSMITTER).build(null).setRegistryName(RS.ID, "network_transmitter")));
        e.getRegistry().register(registerTileDataParameters(TileEntityType.Builder.create(RelayTile::new, RSBlocks.RELAY).build(null).setRegistryName(RS.ID, "relay")));
        e.getRegistry().register(registerTileDataParameters(TileEntityType.Builder.create(DetectorTile::new, RSBlocks.DETECTOR).build(null).setRegistryName(RS.ID, "detector")));
        e.getRegistry().register(registerTileDataParameters(TileEntityType.Builder.create(SecurityManagerTile::new, RSBlocks.SECURITY_MANAGER).build(null).setRegistryName(RS.ID, "security_manager")));
    }

    private <T extends TileEntity> TileEntityType<T> registerTileDataParameters(TileEntityType<T> t) {
        BaseTile tile = (BaseTile) t.create();

        tile.getDataManager().getParameters().forEach(TileDataManager::registerParameter);

        return t;
    }

    @SubscribeEvent
    public void onRegisterContainers(RegistryEvent.Register<ContainerType<?>> e) {
        e.getRegistry().register(IForgeContainerType.create((windowId, inv, data) -> new FilterContainer(inv.player, inv.getCurrentItem(), windowId)).setRegistryName(RS.ID, "filter"));
        e.getRegistry().register(IForgeContainerType.create(((windowId, inv, data) -> new ControllerContainer(null, inv.player, windowId))).setRegistryName(RS.ID, "controller"));
        e.getRegistry().register(IForgeContainerType.create(new PositionalTileContainerFactory<DiskDriveContainer, DiskDriveTile>((windowId, inv, tile) -> new DiskDriveContainer(tile, inv.player, windowId))).setRegistryName(RS.ID, "disk_drive"));
        e.getRegistry().register(IForgeContainerType.create(new GridContainerFactory()).setRegistryName(RS.ID, "grid"));
        e.getRegistry().register(IForgeContainerType.create(new PositionalTileContainerFactory<StorageContainer, StorageTile>((windowId, inv, tile) -> new StorageContainer(tile, inv.player, windowId))).setRegistryName(RS.ID, "storage_block"));
        e.getRegistry().register(IForgeContainerType.create(new PositionalTileContainerFactory<FluidStorageContainer, FluidStorageTile>((windowId, inv, tile) -> new FluidStorageContainer(tile, inv.player, windowId))).setRegistryName(RS.ID, "fluid_storage_block"));
        e.getRegistry().register(IForgeContainerType.create(new PositionalTileContainerFactory<ExternalStorageContainer, ExternalStorageTile>((windowId, inv, tile) -> new ExternalStorageContainer(tile, inv.player, windowId))).setRegistryName(RS.ID, "external_storage"));
        e.getRegistry().register(IForgeContainerType.create(new PositionalTileContainerFactory<ImporterContainer, ImporterTile>((windowId, inv, tile) -> new ImporterContainer(tile, inv.player, windowId))).setRegistryName(RS.ID, "importer"));
        e.getRegistry().register(IForgeContainerType.create(new PositionalTileContainerFactory<ExporterContainer, ExporterTile>((windowId, inv, tile) -> new ExporterContainer(tile, inv.player, windowId))).setRegistryName(RS.ID, "exporter"));
        e.getRegistry().register(IForgeContainerType.create(new PositionalTileContainerFactory<NetworkTransmitterContainer, NetworkTransmitterTile>((windowId, inv, tile) -> new NetworkTransmitterContainer(tile, inv.player, windowId))).setRegistryName(RS.ID, "network_transmitter"));
        e.getRegistry().register(IForgeContainerType.create(new PositionalTileContainerFactory<RelayContainer, RelayTile>((windowId, inv, tile) -> new RelayContainer(tile, inv.player, windowId))).setRegistryName(RS.ID, "relay"));
        e.getRegistry().register(IForgeContainerType.create(new PositionalTileContainerFactory<DetectorContainer, DetectorTile>((windowId, inv, tile) -> new DetectorContainer(tile, inv.player, windowId))).setRegistryName(RS.ID, "detector"));
        e.getRegistry().register(IForgeContainerType.create(new PositionalTileContainerFactory<SecurityManagerContainer, SecurityManagerTile>((windowId, inv, tile) -> new SecurityManagerContainer(tile, inv.player, windowId))).setRegistryName(RS.ID, "security_manager"));
    }

    @SubscribeEvent
    public void onRegisterItems(RegistryEvent.Register<Item> e) {
        e.getRegistry().register(new CoreItem(CoreItem.Type.CONSTRUCTION));
        e.getRegistry().register(new CoreItem(CoreItem.Type.DESTRUCTION));
        e.getRegistry().register(new QuartzEnrichedIronItem());
        e.getRegistry().register(new ProcessorBindingItem());

        for (ProcessorItem.Type type : ProcessorItem.Type.values()) {
            e.getRegistry().register(new ProcessorItem(type));
        }

        e.getRegistry().register(new SiliconItem());

        e.getRegistry().register(new SecurityCardItem());
        e.getRegistry().register(new NetworkCardItem());

        for (ItemStorageType type : ItemStorageType.values()) {
            if (type != ItemStorageType.CREATIVE) {
                e.getRegistry().register(new StoragePartItem(type));
            }

            e.getRegistry().register(new StorageDiskItem(type));
        }

        for (FluidStorageType type : FluidStorageType.values()) {
            if (type != FluidStorageType.CREATIVE) {
                e.getRegistry().register(new FluidStoragePartItem(type));
            }

            e.getRegistry().register(new FluidStorageDiskItem(type));
        }

        e.getRegistry().register(new StorageHousingItem());

        for (UpgradeItem.Type type : UpgradeItem.Type.values()) {
            e.getRegistry().register(new UpgradeItem(type));
        }

        e.getRegistry().register(new WrenchItem());
        e.getRegistry().register(new PatternItem());
        e.getRegistry().register(new FilterItem());

        e.getRegistry().register(BlockUtils.createBlockItemFor(RSBlocks.QUARTZ_ENRICHED_IRON));
        e.getRegistry().register(new ControllerBlockItem(RSBlocks.CONTROLLER));
        e.getRegistry().register(new ControllerBlockItem(RSBlocks.CREATIVE_CONTROLLER));
        e.getRegistry().register(BlockUtils.createBlockItemFor(RSBlocks.MACHINE_CASING));
        e.getRegistry().register(BlockUtils.createBlockItemFor(RSBlocks.CABLE));
        e.getRegistry().register(BlockUtils.createBlockItemFor(RSBlocks.DISK_DRIVE));
        e.getRegistry().register(BlockUtils.createBlockItemFor(RSBlocks.GRID));
        e.getRegistry().register(BlockUtils.createBlockItemFor(RSBlocks.CRAFTING_GRID));
        e.getRegistry().register(BlockUtils.createBlockItemFor(RSBlocks.PATTERN_GRID));
        e.getRegistry().register(BlockUtils.createBlockItemFor(RSBlocks.FLUID_GRID));

        e.getRegistry().register(new StorageBlockItem(RSBlocks.ONE_K_STORAGE_BLOCK));
        e.getRegistry().register(new StorageBlockItem(RSBlocks.FOUR_K_STORAGE_BLOCK));
        e.getRegistry().register(new StorageBlockItem(RSBlocks.SIXTEEN_K_STORAGE_BLOCK));
        e.getRegistry().register(new StorageBlockItem(RSBlocks.SIXTY_FOUR_K_STORAGE_BLOCK));
        e.getRegistry().register(new StorageBlockItem(RSBlocks.CREATIVE_STORAGE_BLOCK));

        e.getRegistry().register(new FluidStorageBlockItem(RSBlocks.SIXTY_FOUR_K_FLUID_STORAGE_BLOCK));
        e.getRegistry().register(new FluidStorageBlockItem(RSBlocks.TWO_HUNDRED_FIFTY_SIX_K_FLUID_STORAGE_BLOCK));
        e.getRegistry().register(new FluidStorageBlockItem(RSBlocks.THOUSAND_TWENTY_FOUR_K_FLUID_STORAGE_BLOCK));
        e.getRegistry().register(new FluidStorageBlockItem(RSBlocks.FOUR_THOUSAND_NINETY_SIX_K_FLUID_STORAGE_BLOCK));
        e.getRegistry().register(new FluidStorageBlockItem(RSBlocks.CREATIVE_FLUID_STORAGE_BLOCK));

        e.getRegistry().register(BlockUtils.createBlockItemFor(RSBlocks.EXTERNAL_STORAGE));
        e.getRegistry().register(BlockUtils.createBlockItemFor(RSBlocks.IMPORTER));
        e.getRegistry().register(BlockUtils.createBlockItemFor(RSBlocks.EXPORTER));
        e.getRegistry().register(BlockUtils.createBlockItemFor(RSBlocks.NETWORK_RECEIVER));
        e.getRegistry().register(BlockUtils.createBlockItemFor(RSBlocks.NETWORK_TRANSMITTER));
        e.getRegistry().register(BlockUtils.createBlockItemFor(RSBlocks.RELAY));
        e.getRegistry().register(BlockUtils.createBlockItemFor(RSBlocks.DETECTOR));
        e.getRegistry().register(BlockUtils.createBlockItemFor(RSBlocks.SECURITY_MANAGER));
    }

    /* TODO
    @EventHandler
    public void onServerStarting(FMLServerStartingEvent e) {
        e.registerServerCommand(new CommandCreateDisk());
    }

    @EventHandler
    public void onFingerprintViolation(FMLFingerprintViolationEvent e) {
        FMLLog.bigWarning("Invalid fingerprint detected for the Refined Storage jar file! The file " + e.getSource().getName() + " may have been tampered with. This version will NOT be supported!");
    }*/
}
