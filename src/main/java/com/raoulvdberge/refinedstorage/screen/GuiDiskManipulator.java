package com.raoulvdberge.refinedstorage.screen;

import com.raoulvdberge.refinedstorage.RS;
import com.raoulvdberge.refinedstorage.container.DiskManipulatorContainer;
import com.raoulvdberge.refinedstorage.screen.widget.sidebutton.*;
import com.raoulvdberge.refinedstorage.tile.TileDiskManipulator;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerInventory;

public class GuiDiskManipulator extends BaseScreen<DiskManipulatorContainer> {
    public GuiDiskManipulator(DiskManipulatorContainer container, PlayerInventory playerInventory) {
        super(container, 211, 211, playerInventory, null);
    }

    @Override
    public void onPostInit(int x, int y) {
        addSideButton(new RedstoneModeSideButton(this, TileDiskManipulator.REDSTONE_MODE));
        addSideButton(new IoModeSideButton(this));
        addSideButton(new TypeSideButton(this, TileDiskManipulator.TYPE));
        addSideButton(new WhitelistBlacklistSideButton(this, TileDiskManipulator.WHITELIST_BLACKLIST));
        addSideButton(new ExactModeSideButton(this, TileDiskManipulator.COMPARE));
    }

    @Override
    public void tick(int x, int y) {

    }

    @Override
    public void renderBackground(int x, int y, int mouseX, int mouseY) {
        bindTexture(RS.ID, "gui/disk_manipulator.png");

        blit(x, y, 0, 0, xSize, ySize);
    }

    @Override
    public void renderForeground(int mouseX, int mouseY) {
        renderString(7, 7, I18n.format("block.refinedstorage:disk_manipulator.name"));
        renderString(7, 117, I18n.format("container.inventory"));
        renderString(43, 45, I18n.format("gui.refinedstorage:fluid_interface.in"));
        renderString(115, 45, I18n.format("gui.refinedstorage:fluid_interface.out"));
    }
}
