package com.raoulvdberge.refinedstorage.loottable;

import com.raoulvdberge.refinedstorage.apiimpl.network.node.storage.FluidStorageNetworkNode;
import com.raoulvdberge.refinedstorage.apiimpl.network.node.storage.StorageNetworkNode;
import com.raoulvdberge.refinedstorage.tile.FluidStorageTile;
import com.raoulvdberge.refinedstorage.tile.StorageTile;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootParameters;
import net.minecraft.world.storage.loot.functions.ILootFunction;

public class StorageBlockLootFunction implements ILootFunction {
    @Override
    public ItemStack apply(ItemStack stack, LootContext lootContext) {
        TileEntity tile = lootContext.get(LootParameters.BLOCK_ENTITY);

        if (tile instanceof StorageTile) {
            stack.setTag(new CompoundNBT());
            stack.getTag().putUniqueId(StorageNetworkNode.NBT_ID, ((StorageTile) tile).getRemovedNode().getStorageId());
        } else if (tile instanceof FluidStorageTile) {
            stack.setTag(new CompoundNBT());
            stack.getTag().putUniqueId(FluidStorageNetworkNode.NBT_ID, ((FluidStorageTile) tile).getRemovedNode().getStorageId());
        }

        return stack;
    }
}
