package com.raoulvdberge.refinedstorage.apiimpl.network.node.diskmanipulator;

import com.raoulvdberge.refinedstorage.RS;
import com.raoulvdberge.refinedstorage.api.storage.AccessType;
import com.raoulvdberge.refinedstorage.api.storage.disk.IStorageDisk;
import com.raoulvdberge.refinedstorage.api.storage.disk.IStorageDiskContainerContext;
import com.raoulvdberge.refinedstorage.api.util.Action;
import com.raoulvdberge.refinedstorage.api.util.IComparer;
import com.raoulvdberge.refinedstorage.api.util.StackListEntry;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNode;
import com.raoulvdberge.refinedstorage.inventory.fluid.FluidInventory;
import com.raoulvdberge.refinedstorage.inventory.item.BaseItemHandler;
import com.raoulvdberge.refinedstorage.inventory.item.ProxyItemHandler;
import com.raoulvdberge.refinedstorage.inventory.item.UpgradeItemHandler;
import com.raoulvdberge.refinedstorage.inventory.item.validator.StorageDiskItemValidator;
import com.raoulvdberge.refinedstorage.inventory.listener.NetworkNodeFluidInventoryListener;
import com.raoulvdberge.refinedstorage.inventory.listener.NetworkNodeInventoryListener;
import com.raoulvdberge.refinedstorage.item.UpgradeItem;
import com.raoulvdberge.refinedstorage.tile.TileDiskManipulator;
import com.raoulvdberge.refinedstorage.tile.config.IComparable;
import com.raoulvdberge.refinedstorage.tile.config.IType;
import com.raoulvdberge.refinedstorage.tile.config.IWhitelistBlacklist;
import com.raoulvdberge.refinedstorage.util.StackUtils;
import com.raoulvdberge.refinedstorage.util.WorldUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fluids.FluidAttributes;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NetworkNodeDiskManipulator extends NetworkNode implements IComparable, IWhitelistBlacklist, IType, IStorageDiskContainerContext {
    public static final ResourceLocation ID = new ResourceLocation(RS.ID, "disk_manipulator");

    public static final int IO_MODE_INSERT = 0;
    public static final int IO_MODE_EXTRACT = 1;

    private static final String NBT_COMPARE = "Compare";
    private static final String NBT_MODE = "Mode";
    private static final String NBT_TYPE = "Type";
    private static final String NBT_IO_MODE = "IOMode";
    private static final String NBT_FLUID_FILTERS = "FluidFilters";

    private int compare = IComparer.COMPARE_NBT;
    private int mode = IWhitelistBlacklist.BLACKLIST;
    private int type = IType.ITEMS;
    private int ioMode = IO_MODE_INSERT;

    private IStorageDisk<ItemStack>[] itemDisks = new IStorageDisk[6];
    private IStorageDisk<FluidStack>[] fluidDisks = new IStorageDisk[6];

    private UpgradeItemHandler upgrades = (UpgradeItemHandler) new UpgradeItemHandler(4, UpgradeItem.Type.SPEED, UpgradeItem.Type.STACK) {
        @Override
        public int getStackInteractCount() {
            int count = super.getStackInteractCount();

            if (type == IType.FLUIDS) {
                count *= FluidAttributes.BUCKET_VOLUME;
            }

            return count;
        }
    }.addListener(new NetworkNodeInventoryListener(this));

    private BaseItemHandler inputDisks = new BaseItemHandler(3)
        .addValidator(new StorageDiskItemValidator())
        .addListener(new NetworkNodeInventoryListener(this))
        .addListener((handler, slot, reading) -> {
            if (!world.isRemote) {
                StackUtils.createStorages(
                    (ServerWorld) world,
                    handler.getStackInSlot(slot),
                    slot,
                    itemDisks,
                    fluidDisks,
                    s -> new StorageDiskItemManipulatorWrapper(NetworkNodeDiskManipulator.this, s),
                    s -> new StorageDiskFluidManipulatorWrapper(NetworkNodeDiskManipulator.this, s)
                );

                WorldUtils.updateBlock(world, pos);
            }
        });

    private BaseItemHandler outputDisks = new BaseItemHandler(3)
        .addValidator(new StorageDiskItemValidator())
        .addListener(new NetworkNodeInventoryListener(this))
        .addListener(((handler, slot, reading) -> {
            if (!world.isRemote) {
                StackUtils.createStorages(
                    (ServerWorld) world,
                    handler.getStackInSlot(slot),
                    3 + slot,
                    itemDisks,
                    fluidDisks,
                    s -> new StorageDiskItemManipulatorWrapper(NetworkNodeDiskManipulator.this, s),
                    s -> new StorageDiskFluidManipulatorWrapper(NetworkNodeDiskManipulator.this, s)
                );

                WorldUtils.updateBlock(world, pos);
            }
        }));

    private ProxyItemHandler disks = new ProxyItemHandler(inputDisks, outputDisks);

    public NetworkNodeDiskManipulator(World world, BlockPos pos) {
        super(world, pos);
    }

    private BaseItemHandler itemFilters = new BaseItemHandler(9).addListener(new NetworkNodeInventoryListener(this));
    private FluidInventory fluidFilters = new FluidInventory(9).addListener(new NetworkNodeFluidInventoryListener(this));

    @Override
    public int getEnergyUsage() {
        return RS.INSTANCE.config.diskManipulatorUsage + upgrades.getEnergyUsage();
    }

    @Override
    public void update() {
        super.update();

        if (!canUpdate() || ticks % upgrades.getSpeed() != 0) {
            return;
        }

        int slot = 0;
        if (type == IType.ITEMS) {
            while (slot < 3 && (itemDisks[slot] == null || isItemDiskDone(itemDisks[slot], slot))) {
                slot++;
            }

            if (slot == 3) {
                return;
            }

            IStorageDisk<ItemStack> storage = itemDisks[slot];

            if (ioMode == IO_MODE_INSERT) {
                insertItemIntoNetwork(storage, slot);
            } else if (ioMode == IO_MODE_EXTRACT) {
                extractItemFromNetwork(storage, slot);
            }
        } else if (type == IType.FLUIDS) {
            while (slot < 3 && (fluidDisks[slot] == null || isFluidDiskDone(fluidDisks[slot], slot))) {
                slot++;
            }

            if (slot == 3) {
                return;
            }

            IStorageDisk<FluidStack> storage = fluidDisks[slot];

            if (ioMode == IO_MODE_INSERT) {
                insertFluidIntoNetwork(storage, slot);
            } else if (ioMode == IO_MODE_EXTRACT) {
                extractFluidFromNetwork(storage, slot);
            }
        }
    }

    private void insertItemIntoNetwork(IStorageDisk<ItemStack> storage, int slot) {
        List<ItemStack> stacks = new ArrayList<>(storage.getStacks());
        for (int i = 0; i < stacks.size(); ++i) {
            ItemStack stack = stacks.get(i);

            ItemStack extracted = storage.extract(stack, upgrades.getStackInteractCount(), compare, Action.PERFORM);
            if (extracted.isEmpty()) {
                continue;
            }

            ItemStack remainder = network.insertItem(extracted, extracted.getCount(), Action.PERFORM);
            if (remainder.isEmpty()) {
                break;
            }

            // We need to check if the stack was inserted
            storage.insert(((extracted == remainder) ? remainder.copy() : remainder), remainder.getCount(), Action.PERFORM);
        }
    }

    // Iterate through disk stacks, if none can be inserted, return that it is done processing and can be output.
    private boolean isItemDiskDone(IStorageDisk<ItemStack> storage, int slot) {
        if (ioMode == IO_MODE_INSERT && storage.getStored() == 0) {
            moveDriveToOutput(slot);
            return true;
        }

        // In Extract mode, we just need to check if the disk is full or not.
        if (ioMode == IO_MODE_EXTRACT)
            if (storage.getStored() == storage.getCapacity()) {
                moveDriveToOutput(slot);
                return true;
            } else {
                return false;
            }

        List<ItemStack> stacks = new ArrayList<>(storage.getStacks());
        for (int i = 0; i < stacks.size(); ++i) {
            ItemStack stack = stacks.get(i);

            ItemStack extracted = storage.extract(stack, upgrades.getStackInteractCount(), compare, Action.SIMULATE);
            if (extracted.isEmpty()) {
                continue;
            }

            ItemStack remainder = network.insertItem(extracted, extracted.getCount(), Action.SIMULATE);
            if (remainder.isEmpty()) { // An item could be inserted (no remainders when trying to). This disk isn't done.
                return false;
            }
        }
        return true;
    }

    private void extractItemFromNetwork(IStorageDisk<ItemStack> storage, int slot) {
        ItemStack extracted = ItemStack.EMPTY;
        int i = 0;

        if (itemFilters.isEmpty()) {
            ItemStack toExtract = null;
            List<ItemStack> networkItems = network.getItemStorageCache().getList().getStacks().stream().map(StackListEntry::getStack).collect(Collectors.toList());

            int j = 0;

            while ((toExtract == null || toExtract.isEmpty()) && j < networkItems.size()) {
                toExtract = networkItems.get(j++);
            }

            if (toExtract != null) {
                extracted = network.extractItem(toExtract, upgrades.getStackInteractCount(), compare, Action.PERFORM);
            }
        } else {
            while (itemFilters.getSlots() > i && extracted == null) {
                ItemStack filterStack = ItemStack.EMPTY;

                while (itemFilters.getSlots() > i && filterStack.isEmpty()) {
                    filterStack = itemFilters.getStackInSlot(i++);
                }

                if (!filterStack.isEmpty()) {
                    extracted = network.extractItem(filterStack, upgrades.getStackInteractCount(), compare, Action.PERFORM);
                }
            }
        }

        if (extracted.isEmpty()) {
            moveDriveToOutput(slot);
            return;
        }

        ItemStack remainder = storage.insert(extracted, extracted.getCount(), Action.PERFORM);

        network.insertItem(remainder, remainder.getCount(), Action.PERFORM);
    }

    private void insertFluidIntoNetwork(IStorageDisk<FluidStack> storage, int slot) {
        List<FluidStack> stacks = new ArrayList<>(storage.getStacks());

        FluidStack extracted = FluidStack.EMPTY;
        int i = 0;

        while (extracted.isEmpty() && stacks.size() > i) {
            FluidStack stack = stacks.get(i++);

            extracted = storage.extract(stack, upgrades.getStackInteractCount(), compare, Action.PERFORM);
        }

        if (extracted.isEmpty()) {
            moveDriveToOutput(slot);
            return;
        }

        FluidStack remainder = network.insertFluid(extracted, extracted.getAmount(), Action.PERFORM);

        storage.insert(remainder, remainder.getAmount(), Action.PERFORM);
    }

    private boolean isFluidDiskDone(IStorageDisk<FluidStack> storage, int slot) {
        if (ioMode == IO_MODE_INSERT && storage.getStored() == 0) {
            moveDriveToOutput(slot);
            return true;
        }

        //In Extract mode, we just need to check if the disk is full or not.
        if (ioMode == IO_MODE_EXTRACT)
            if (storage.getStored() == storage.getCapacity()) {
                moveDriveToOutput(slot);
                return true;
            } else {
                return false;
            }

        List<FluidStack> stacks = new ArrayList<>(storage.getStacks());
        for (int i = 0; i < stacks.size(); ++i) {
            FluidStack stack = stacks.get(i);

            FluidStack extracted = storage.extract(stack, upgrades.getStackInteractCount(), compare, Action.SIMULATE);
            if (extracted.isEmpty()) {
                continue;
            }

            FluidStack remainder = network.insertFluid(extracted, extracted.getAmount(), Action.SIMULATE);
            if (remainder.isEmpty()) { // A fluid could be inserted (no remainders when trying to). This disk isn't done.
                return false;
            }
        }
        return true;
    }

    private void extractFluidFromNetwork(IStorageDisk<FluidStack> storage, int slot) {
        FluidStack extracted = FluidStack.EMPTY;
        int i = 0;

        if (fluidFilters.isEmpty()) {
            FluidStack toExtract = null;
            List<FluidStack> networkFluids = network.getFluidStorageCache().getList().getStacks().stream().map(StackListEntry::getStack).collect(Collectors.toList());

            int j = 0;

            while ((toExtract == null || toExtract.getAmount() == 0) && j < networkFluids.size()) {
                toExtract = networkFluids.get(j++);
            }

            if (toExtract != null) {
                extracted = network.extractFluid(toExtract, upgrades.getStackInteractCount(), compare, Action.PERFORM);
            }
        } else {
            while (fluidFilters.getSlots() > i && extracted == null) {
                FluidStack filterStack = FluidStack.EMPTY;

                while (fluidFilters.getSlots() > i && filterStack.isEmpty()) {
                    filterStack = fluidFilters.getFluid(i++);
                }

                if (!filterStack.isEmpty()) {
                    extracted = network.extractFluid(filterStack, upgrades.getStackInteractCount(), compare, Action.PERFORM);
                }
            }
        }

        if (extracted.isEmpty()) {
            moveDriveToOutput(slot);
            return;
        }

        FluidStack remainder = storage.insert(extracted, extracted.getAmount(), Action.PERFORM);

        network.insertFluid(remainder, remainder.getAmount(), Action.PERFORM);
    }

    private void moveDriveToOutput(int slot) {
        ItemStack disk = inputDisks.getStackInSlot(slot);
        if (!disk.isEmpty()) {
            int i = 0;
            while (i < 3 && !outputDisks.getStackInSlot(i).isEmpty()) {
                i++;
            }

            if (i == 3) {
                return;
            }

            inputDisks.extractItem(slot, 1, false);
            outputDisks.insertItem(i, disk, false);
        }
    }

    @Override
    public int getCompare() {
        return compare;
    }

    @Override
    public void setCompare(int compare) {
        this.compare = compare;
    }

    @Override
    public int getType() {
        return world.isRemote ? TileDiskManipulator.TYPE.getValue() : type;
    }

    @Override
    public void setType(int type) {
        this.type = type;
    }

    @Override
    public IItemHandlerModifiable getItemFilters() {
        return itemFilters;
    }

    @Override
    public FluidInventory getFluidFilters() {
        return fluidFilters;
    }

    @Override
    public void setWhitelistBlacklistMode(int mode) {
        this.mode = mode;
    }

    @Override
    public int getWhitelistBlacklistMode() {
        return this.mode;
    }

    public int getIoMode() {
        return ioMode;
    }

    public void setIoMode(int ioMode) {
        this.ioMode = ioMode;
    }

    public IItemHandler getInputDisks() {
        return inputDisks;
    }

    public IItemHandler getOutputDisks() {
        return outputDisks;
    }

    public ProxyItemHandler getDisks() {
        return disks;
    }

    public IItemHandler getUpgrades() {
        return upgrades;
    }

    public IStorageDisk[] getItemDisks() {
        return itemDisks;
    }

    public IStorageDisk[] getFluidDisks() {
        return fluidDisks;
    }

    @Override
    public void read(CompoundNBT tag) {
        super.read(tag);

        StackUtils.readItems(upgrades, 3, tag);
        StackUtils.readItems(inputDisks, 4, tag);
        StackUtils.readItems(outputDisks, 5, tag);
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    @Override
    public CompoundNBT write(CompoundNBT tag) {
        super.write(tag);

        StackUtils.writeItems(upgrades, 3, tag);
        StackUtils.writeItems(inputDisks, 4, tag);
        StackUtils.writeItems(outputDisks, 5, tag);

        return tag;
    }

    @Override
    public CompoundNBT writeConfiguration(CompoundNBT tag) {
        super.writeConfiguration(tag);

        StackUtils.writeItems(itemFilters, 1, tag);

        tag.put(NBT_FLUID_FILTERS, fluidFilters.writeToNbt());
        tag.putInt(NBT_COMPARE, compare);
        tag.putInt(NBT_MODE, mode);
        tag.putInt(NBT_TYPE, type);
        tag.putInt(NBT_IO_MODE, ioMode);

        return tag;
    }

    @Override
    public void readConfiguration(CompoundNBT tag) {
        super.readConfiguration(tag);

        StackUtils.readItems(itemFilters, 1, tag);

        if (tag.contains(NBT_FLUID_FILTERS)) {
            fluidFilters.readFromNbt(tag.getCompound(NBT_FLUID_FILTERS));
        }

        if (tag.contains(NBT_COMPARE)) {
            compare = tag.getInt(NBT_COMPARE);
        }

        if (tag.contains(NBT_MODE)) {
            mode = tag.getInt(NBT_MODE);
        }

        if (tag.contains(NBT_TYPE)) {
            type = tag.getInt(NBT_TYPE);
        }

        if (tag.contains(NBT_IO_MODE)) {
            ioMode = tag.getInt(NBT_IO_MODE);
        }
    }

    @Override
    public IItemHandler getDrops() {
        return new CombinedInvWrapper(inputDisks, outputDisks, upgrades);
    }

    @Override
    public AccessType getAccessType() {
        return AccessType.INSERT_EXTRACT;
    }
}
