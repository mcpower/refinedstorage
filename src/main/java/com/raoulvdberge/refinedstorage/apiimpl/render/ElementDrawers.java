package com.raoulvdberge.refinedstorage.apiimpl.render;

import com.raoulvdberge.refinedstorage.api.render.IElementDrawer;
import com.raoulvdberge.refinedstorage.api.render.IElementDrawers;
import com.raoulvdberge.refinedstorage.render.FluidRenderer;
import com.raoulvdberge.refinedstorage.screen.BaseScreen;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

public class ElementDrawers implements IElementDrawers {
    protected BaseScreen gui;
    private FontRenderer fontRenderer;

    public ElementDrawers(BaseScreen gui, FontRenderer fontRenderer) {
        this.gui = gui;
        this.fontRenderer = fontRenderer;
    }

    @Override
    public IElementDrawer<ItemStack> getItemDrawer() {
        return gui::renderItem;
    }

    @Override
    public IElementDrawer<FluidStack> getFluidDrawer() {
        return FluidRenderer.INSTANCE::render;
    }

    @Override
    public IElementDrawer<String> getStringDrawer() {
        return gui::renderString;
    }

    @Override
    public FontRenderer getFontRenderer() {
        return fontRenderer;
    }
}
