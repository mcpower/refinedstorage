package com.raoulvdberge.refinedstorage.screen;

import com.raoulvdberge.refinedstorage.RS;
import com.raoulvdberge.refinedstorage.container.CrafterContainer;
import com.raoulvdberge.refinedstorage.tile.TileCrafter;
import com.raoulvdberge.refinedstorage.util.RenderUtils;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerInventory;

public class GuiCrafter extends BaseScreen<CrafterContainer> {
    public GuiCrafter(CrafterContainer container, PlayerInventory inventory) {
        super(container, 211, 137, inventory, null);
    }

    @Override
    public void onPostInit(int x, int y) {
    }

    @Override
    public void tick(int x, int y) {
    }

    @Override
    public void renderBackground(int x, int y, int mouseX, int mouseY) {
        bindTexture(RS.ID, "gui/crafter.png");

        blit(x, y, 0, 0, xSize, ySize);
    }

    @Override
    public void renderForeground(int mouseX, int mouseY) {
        renderString(7, 7, RenderUtils.shorten(I18n.format(TileCrafter.NAME.getValue()), 26));
        renderString(7, 43, I18n.format("container.inventory"));
    }
}
