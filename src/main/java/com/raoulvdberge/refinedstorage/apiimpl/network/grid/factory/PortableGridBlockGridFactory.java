package com.raoulvdberge.refinedstorage.apiimpl.network.grid.factory;

import com.raoulvdberge.refinedstorage.api.network.grid.GridFactoryType;
import com.raoulvdberge.refinedstorage.api.network.grid.IGrid;
import com.raoulvdberge.refinedstorage.api.network.grid.IGridFactory;
import com.raoulvdberge.refinedstorage.tile.grid.portable.TilePortableGrid;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class PortableGridBlockGridFactory implements IGridFactory {
    @Override
    @Nullable
    public IGrid createFromStack(PlayerEntity player, ItemStack stack) {
        return null;
    }

    @Override
    @Nullable
    public IGrid createFromBlock(PlayerEntity player, BlockPos pos) {
        TileEntity tile = getRelevantTile(player.world, pos);

        if (tile instanceof TilePortableGrid) {
            return (TilePortableGrid) tile;
        }

        return null;
    }

    @Nullable
    @Override
    public TileEntity getRelevantTile(World world, BlockPos pos) {
        return world.getTileEntity(pos);
    }

    @Override
    public GridFactoryType getType() {
        return GridFactoryType.BLOCK;
    }
}
