package com.raoulvdberge.refinedstorage.item;

import com.raoulvdberge.refinedstorage.api.network.item.INetworkItem;
import com.raoulvdberge.refinedstorage.api.network.item.INetworkItemHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class ItemWirelessCraftingMonitor extends NetworkItem {
    public static final String NBT_TAB_SELECTED = "TabSelected";
    public static final String NBT_TAB_PAGE = "TabPage";

    public ItemWirelessCraftingMonitor(Properties item, boolean creative, int energyCapacity) {
        super(item, creative, energyCapacity);
    }

    @Nonnull
    @Override
    public INetworkItem provide(INetworkItemHandler handler, PlayerEntity player, ItemStack stack) {
        return null;
    }

/* TODO
    @Override
    @SideOnly(Side.CLIENT)
    public void registerModels(IModelRegistration modelRegistration) {
        modelRegistration.setModel(this, 0, new ModelResourceLocation(info.getId(), "inventory"));
    }

    @Override
    @Nonnull
    public INetworkItem provide(INetworkItemHandler handler, PlayerEntity player, ItemStack stack) {
        int invIndex = 0;
        for (int i = 0; i < player.inventory.getSizeInventory(); ++i) {
            if (player.inventory.getStackInSlot(i) == stack) {
                invIndex = i;
                break;
            }
        }

        return new NetworkItemWirelessCraftingMonitor(handler, player, stack, invIndex);
    }

    public static Optional<UUID> getTabSelected(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().hasUniqueId(NBT_TAB_SELECTED)) {
            return Optional.of(stack.getTag().getUniqueId(NBT_TAB_SELECTED));
        }

        return Optional.absent();
    }

    public static void setTabSelected(ItemStack stack, Optional<UUID> tabSelected) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new CompoundNBT());
        }

        if (tabSelected.isPresent()) {
            stack.getTagCompound().setUniqueId(NBT_TAB_SELECTED, tabSelected.get());
        } else {
            stack.getTagCompound().removeTag(NBT_TAB_SELECTED + "Least");
            stack.getTagCompound().removeTag(NBT_TAB_SELECTED + "Most");
        }
    }

    public static int getTabPage(ItemStack stack) {
        if (stack.hasTagCompound() && stack.getTagCompound().hasKey(NBT_TAB_PAGE)) {
            return stack.getTagCompound().getInteger(NBT_TAB_PAGE);
        }

        return 0;
    }

    public static void setTabPage(ItemStack stack, int tabPage) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new CompoundNBT());
        }

        stack.getTagCompound().putInt(NBT_TAB_PAGE, tabPage);
    }*/
}
