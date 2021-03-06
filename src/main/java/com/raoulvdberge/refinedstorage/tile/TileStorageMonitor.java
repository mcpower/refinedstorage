package com.raoulvdberge.refinedstorage.tile;

import com.raoulvdberge.refinedstorage.RSTiles;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.NetworkNodeStorageMonitor;
import com.raoulvdberge.refinedstorage.tile.config.IComparable;
import com.raoulvdberge.refinedstorage.tile.data.TileDataParameter;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class TileStorageMonitor extends NetworkNodeTile<NetworkNodeStorageMonitor> {
    public static final TileDataParameter<Integer, TileStorageMonitor> COMPARE = IComparable.createParameter();

    private static final String NBT_STACK = "Stack";
    private static final String NBT_AMOUNT = "Amount";

    private int amount;
    @Nullable
    private ItemStack itemStack;

    public TileStorageMonitor() {
        super(RSTiles.STORAGE_MONITOR);

        dataManager.addWatchedParameter(COMPARE);
    }

    @Override
    public NetworkNodeStorageMonitor createNode(World world, BlockPos pos) {
        return new NetworkNodeStorageMonitor(world, pos);
    }

    @Override
    public CompoundNBT writeUpdate(CompoundNBT tag) {
        super.writeUpdate(tag);

        ItemStack stack = getNode().getItemFilters().getStackInSlot(0);

        if (!stack.isEmpty()) {
            tag.put(NBT_STACK, stack.write(new CompoundNBT()));
        }

        tag.putInt(NBT_AMOUNT, getNode().getAmount());

        return tag;
    }

    @Override
    public void readUpdate(CompoundNBT tag) {
        super.readUpdate(tag);

        itemStack = tag.contains(NBT_STACK) ? ItemStack.read(tag.getCompound(NBT_STACK)) : null;
        amount = tag.getInt(NBT_AMOUNT);
    }

    public int getAmount() {
        return amount;
    }

    @Nullable
    public ItemStack getItemStack() {
        return itemStack;
    }
}
