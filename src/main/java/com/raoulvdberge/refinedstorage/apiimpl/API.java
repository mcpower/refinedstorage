package com.raoulvdberge.refinedstorage.apiimpl;

import com.raoulvdberge.refinedstorage.api.IRSAPI;
import com.raoulvdberge.refinedstorage.api.autocrafting.ICraftingPatternRenderHandler;
import com.raoulvdberge.refinedstorage.api.autocrafting.craftingmonitor.ICraftingMonitorElementList;
import com.raoulvdberge.refinedstorage.api.autocrafting.craftingmonitor.ICraftingMonitorElementRegistry;
import com.raoulvdberge.refinedstorage.api.autocrafting.preview.ICraftingPreviewElementRegistry;
import com.raoulvdberge.refinedstorage.api.autocrafting.registry.ICraftingTaskRegistry;
import com.raoulvdberge.refinedstorage.api.autocrafting.task.CraftingTaskReadException;
import com.raoulvdberge.refinedstorage.api.autocrafting.task.ICraftingRequestInfo;
import com.raoulvdberge.refinedstorage.api.network.INetworkManager;
import com.raoulvdberge.refinedstorage.api.network.grid.ICraftingGridBehavior;
import com.raoulvdberge.refinedstorage.api.network.grid.IGridManager;
import com.raoulvdberge.refinedstorage.api.network.node.INetworkNode;
import com.raoulvdberge.refinedstorage.api.network.node.INetworkNodeManager;
import com.raoulvdberge.refinedstorage.api.network.node.INetworkNodeRegistry;
import com.raoulvdberge.refinedstorage.api.storage.StorageType;
import com.raoulvdberge.refinedstorage.api.storage.disk.IStorageDisk;
import com.raoulvdberge.refinedstorage.api.storage.disk.IStorageDiskManager;
import com.raoulvdberge.refinedstorage.api.storage.disk.IStorageDiskRegistry;
import com.raoulvdberge.refinedstorage.api.storage.disk.IStorageDiskSync;
import com.raoulvdberge.refinedstorage.api.storage.externalstorage.IExternalStorageProvider;
import com.raoulvdberge.refinedstorage.api.util.IComparer;
import com.raoulvdberge.refinedstorage.api.util.IQuantityFormatter;
import com.raoulvdberge.refinedstorage.api.util.IStackList;
import com.raoulvdberge.refinedstorage.apiimpl.autocrafting.CraftingRequestInfo;
import com.raoulvdberge.refinedstorage.apiimpl.autocrafting.craftingmonitor.CraftingMonitorElementList;
import com.raoulvdberge.refinedstorage.apiimpl.autocrafting.craftingmonitor.CraftingMonitorElementRegistry;
import com.raoulvdberge.refinedstorage.apiimpl.autocrafting.preview.CraftingPreviewElementRegistry;
import com.raoulvdberge.refinedstorage.apiimpl.autocrafting.registry.CraftingTaskRegistry;
import com.raoulvdberge.refinedstorage.apiimpl.network.NetworkManager;
import com.raoulvdberge.refinedstorage.apiimpl.network.NetworkNodeManager;
import com.raoulvdberge.refinedstorage.apiimpl.network.NetworkNodeRegistry;
import com.raoulvdberge.refinedstorage.apiimpl.network.grid.CraftingGridBehavior;
import com.raoulvdberge.refinedstorage.apiimpl.network.grid.GridManager;
import com.raoulvdberge.refinedstorage.apiimpl.storage.disk.*;
import com.raoulvdberge.refinedstorage.apiimpl.util.Comparer;
import com.raoulvdberge.refinedstorage.apiimpl.util.FluidStackList;
import com.raoulvdberge.refinedstorage.apiimpl.util.ItemStackList;
import com.raoulvdberge.refinedstorage.apiimpl.util.QuantityFormatter;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fluids.FluidStack;

import javax.annotation.Nonnull;
import java.util.*;

public class API implements IRSAPI {
    private static final IRSAPI INSTANCE = new API();

    private final IComparer comparer = new Comparer();
    private final IQuantityFormatter quantityFormatter = new QuantityFormatter();
    private final INetworkNodeRegistry networkNodeRegistry = new NetworkNodeRegistry();
    private final ICraftingTaskRegistry craftingTaskRegistry = new CraftingTaskRegistry();
    private final ICraftingMonitorElementRegistry craftingMonitorElementRegistry = new CraftingMonitorElementRegistry();
    private final ICraftingPreviewElementRegistry craftingPreviewElementRegistry = new CraftingPreviewElementRegistry();
    private final IGridManager gridManager = new GridManager();
    private final ICraftingGridBehavior craftingGridBehavior = new CraftingGridBehavior();
    private final IStorageDiskRegistry storageDiskRegistry = new StorageDiskRegistry();
    private final IStorageDiskSync storageDiskSync = new StorageDiskSync();
    private final Map<StorageType, TreeSet<IExternalStorageProvider>> externalStorageProviders = new HashMap<>();
    private final List<ICraftingPatternRenderHandler> patternRenderHandlers = new LinkedList<>();

    public static IRSAPI instance() {
        return INSTANCE;
    }

    /* TODO API DELIVERY public static void deliver(ASMDataTable asmDataTable) {
        String annotationClassName = RSAPIInject.class.getCanonicalName();

        Set<ASMDataTable.ASMData> asmDataSet = asmDataTable.getAll(annotationClassName);

        for (ASMDataTable.ASMData asmData : asmDataSet) {
            try {
                Class clazz = Class.forName(asmData.getClassName());
                Field field = clazz.getField(asmData.getObjectName());

                if (field.getType() == IRSAPI.class) {
                    field.set(null, INSTANCE);
                }
            } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
                throw new RuntimeException("Failed to set: {}" + asmData.getClassName() + "." + asmData.getObjectName(), e);
            }
        }
    }*/

    @Nonnull
    @Override
    public IComparer getComparer() {
        return comparer;
    }

    @Override
    @Nonnull
    public IQuantityFormatter getQuantityFormatter() {
        return quantityFormatter;
    }

    @Override
    @Nonnull
    public INetworkNodeRegistry getNetworkNodeRegistry() {
        return networkNodeRegistry;
    }

    @Override
    public INetworkNodeManager getNetworkNodeManager(ServerWorld world) {
        String name = world.getDimension().getType().getRegistryName().getNamespace() + "_" + world.getDimension().getType().getRegistryName().getPath() + "_" + NetworkNodeManager.NAME;

        return world.getSavedData().getOrCreate(() -> new NetworkNodeManager(name, world), name);
    }

    @Override
    public INetworkManager getNetworkManager(ServerWorld world) {
        String name = world.getDimension().getType().getRegistryName().getNamespace() + "_" + world.getDimension().getType().getRegistryName().getPath() + "_" + NetworkManager.NAME;

        return world.getSavedData().getOrCreate(() -> new NetworkManager(name, world), name);
    }

    @Override
    @Nonnull
    public ICraftingTaskRegistry getCraftingTaskRegistry() {
        return craftingTaskRegistry;
    }

    @Override
    @Nonnull
    public ICraftingMonitorElementRegistry getCraftingMonitorElementRegistry() {
        return craftingMonitorElementRegistry;
    }

    @Override
    @Nonnull
    public ICraftingPreviewElementRegistry getCraftingPreviewElementRegistry() {
        return craftingPreviewElementRegistry;
    }

    @Nonnull
    @Override
    public IStackList<ItemStack> createItemStackList() {
        return new ItemStackList();
    }

    @Override
    @Nonnull
    public IStackList<FluidStack> createFluidStackList() {
        return new FluidStackList();
    }

    @Override
    @Nonnull
    public ICraftingMonitorElementList createCraftingMonitorElementList() {
        return new CraftingMonitorElementList();
    }

    @Nonnull
    @Override
    public IGridManager getGridManager() {
        return gridManager;
    }

    @Nonnull
    @Override
    public ICraftingGridBehavior getCraftingGridBehavior() {
        return craftingGridBehavior;
    }

    @Nonnull
    @Override
    public IStorageDiskRegistry getStorageDiskRegistry() {
        return storageDiskRegistry;
    }

    @Nonnull
    @Override
    public IStorageDiskManager getStorageDiskManager(ServerWorld anyWorld) {
        ServerWorld world = anyWorld.getServer().getWorld(DimensionType.OVERWORLD);

        return world.getSavedData().getOrCreate(() -> new StorageDiskManager(StorageDiskManager.NAME, world), StorageDiskManager.NAME);
    }

    @Nonnull
    @Override
    public IStorageDiskSync getStorageDiskSync() {
        return storageDiskSync;
    }

    @Override
    public void addExternalStorageProvider(StorageType type, IExternalStorageProvider provider) {
        externalStorageProviders.computeIfAbsent(type, k -> new TreeSet<>((a, b) -> Integer.compare(b.getPriority(), a.getPriority()))).add(provider);
    }

    @Override
    public Set<IExternalStorageProvider> getExternalStorageProviders(StorageType type) {
        TreeSet<IExternalStorageProvider> providers = externalStorageProviders.get(type);

        return providers == null ? Collections.emptySet() : providers;
    }

    @Override
    @Nonnull
    public IStorageDisk<ItemStack> createDefaultItemDisk(ServerWorld world, int capacity) {
        return new ItemStorageDisk(world, capacity);
    }

    @Override
    @Nonnull
    public IStorageDisk<FluidStack> createDefaultFluidDisk(ServerWorld world, int capacity) {
        return new FluidStorageDisk(world, capacity);
    }

    @Override
    public ICraftingRequestInfo createCraftingRequestInfo(ItemStack stack) {
        return new CraftingRequestInfo(stack);
    }

    @Override
    public ICraftingRequestInfo createCraftingRequestInfo(FluidStack stack) {
        return new CraftingRequestInfo(stack);
    }

    @Override
    public ICraftingRequestInfo createCraftingRequestInfo(CompoundNBT tag) throws CraftingTaskReadException {
        return new CraftingRequestInfo(tag);
    }

    @Override
    public void addPatternRenderHandler(ICraftingPatternRenderHandler renderHandler) {
        patternRenderHandlers.add(renderHandler);
    }

    @Override
    public List<ICraftingPatternRenderHandler> getPatternRenderHandlers() {
        return patternRenderHandlers;
    }

    // TODO: Remove eventually
    @Override
    public int getItemStackHashCode(ItemStack stack) {
        int result = stack.getItem().hashCode();

        if (stack.hasTag()) {
            result = getHashCode(stack.getTag(), result);
        }

        return result;
    }

    private int getHashCode(INBT tag, int result) {
        if (tag instanceof CompoundNBT) {
            result = getHashCode((CompoundNBT) tag, result);
        } else if (tag instanceof ListNBT) {
            result = getHashCode((ListNBT) tag, result);
        } else {
            result = 31 * result + tag.hashCode();
        }

        return result;
    }

    private int getHashCode(CompoundNBT tag, int result) {
        for (String key : tag.keySet()) {
            result = 31 * result + key.hashCode();
            result = getHashCode(tag.get(key), result);
        }

        return result;
    }

    private int getHashCode(ListNBT tag, int result) {
        for (int i = 0; i < tag.size(); ++i) {
            result = getHashCode(tag.get(i), result);
        }

        return result;
    }

    // TODO: Remove eventually
    @Override
    public int getFluidStackHashCode(FluidStack stack) {
        int result = stack.getFluid().hashCode();

        if (stack.getTag() != null) {
            result = getHashCode(stack.getTag(), result);
        }

        return result;
    }

    @Override
    public int getNetworkNodeHashCode(INetworkNode node) {
        int result = node.getPos().hashCode();
        result = 31 * result + node.getWorld().getDimension().getType().getId();

        return result;
    }

    @Override
    public boolean isNetworkNodeEqual(INetworkNode left, Object right) {
        if (!(right instanceof INetworkNode)) {
            return false;
        }

        if (left == right) {
            return true;
        }

        INetworkNode rightNode = (INetworkNode) right;

        if (left.getWorld().getDimension().getType().getId() != rightNode.getWorld().getDimension().getType().getId()) {
            return false;
        }

        return left.getPos().equals(rightNode.getPos());
    }
}
