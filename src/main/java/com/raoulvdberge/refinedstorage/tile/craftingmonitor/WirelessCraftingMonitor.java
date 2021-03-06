package com.raoulvdberge.refinedstorage.tile.craftingmonitor;

import com.raoulvdberge.refinedstorage.api.autocrafting.ICraftingManager;
import com.raoulvdberge.refinedstorage.api.autocrafting.task.ICraftingTask;
import com.raoulvdberge.refinedstorage.api.network.INetwork;
import com.raoulvdberge.refinedstorage.tile.data.TileDataParameter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

public class WirelessCraftingMonitor implements ICraftingMonitor {
    private ItemStack stack;

    private int networkDimension;
    private BlockPos network;
    private int tabPage;
    private Optional<UUID> tabSelected;

    public WirelessCraftingMonitor(ItemStack stack) {
        this.stack = stack;
        /* TODO this.networkDimension = ItemWirelessCraftingMonitor.getDimensionId(stack);
        this.network = new BlockPos(ItemWirelessCraftingMonitor.getX(stack), ItemWirelessCraftingMonitor.getY(stack), ItemWirelessCraftingMonitor.getZ(stack));
        this.tabPage = ItemWirelessCraftingMonitor.getTabPage(stack);
        this.tabSelected = ItemWirelessCraftingMonitor.getTabSelected(stack);*/
    }

    @Override
    public String getGuiTitle() {
        return "gui.refinedstorage:wireless_crafting_monitor";
    }

    @Override
    public void onCancelled(ServerPlayerEntity player, @Nullable UUID id) {
        INetwork network = getNetwork();

        if (network != null) {
            network.getItemGridHandler().onCraftingCancelRequested(player, id);
        }
    }

    @Override
    public TileDataParameter<Integer, ?> getRedstoneModeParameter() {
        return null;
    }

    @Override
    public Collection<ICraftingTask> getTasks() {
        INetwork network = getNetwork();

        if (network != null) {
            return network.getCraftingManager().getTasks();
        }

        return Collections.emptyList();
    }

    @Nullable
    @Override
    public ICraftingManager getCraftingManager() {
        INetwork network = getNetwork();

        if (network != null) {
            return network.getCraftingManager();
        }

        return null;
    }

    private INetwork getNetwork() {
        // TODO World world = DimensionManager.getWorld(networkDimension);
        World world = null;

        if (world != null) {
            TileEntity tile = world.getTileEntity(network);

            return tile instanceof INetwork ? (INetwork) tile : null;
        }

        return null;
    }

    public ItemStack getStack() {
        return stack;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        INetwork network = getNetwork();

        if (network != null) {
            network.getNetworkItemHandler().close(player);
        }
    }

    @Override
    public Optional<UUID> getTabSelected() {
        return tabSelected;
    }

    @Override
    public int getTabPage() {
        return tabPage;
    }

    @Override
    public void onTabSelectionChanged(Optional<UUID> taskId) {
        if (taskId.isPresent() && tabSelected.isPresent() && taskId.get().equals(tabSelected.get())) {
            this.tabSelected = Optional.empty();
        } else {
            this.tabSelected = taskId;
        }

        // TODO RS.INSTANCE.network.sendToServer(new MessageWirelessCraftingMonitorSettings(tabSelected, tabPage));
    }

    @Override
    public void onTabPageChanged(int page) {
        if (page >= 0) {
            this.tabPage = page;

            // TODO RS.INSTANCE.network.sendToServer(new MessageWirelessCraftingMonitorSettings(tabSelected, tabPage));
        }
    }
}
