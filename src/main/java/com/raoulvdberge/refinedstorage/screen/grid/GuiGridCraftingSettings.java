package com.raoulvdberge.refinedstorage.screen.grid;

import com.google.common.primitives.Ints;
import com.raoulvdberge.refinedstorage.container.CraftingSettingsContainer;
import com.raoulvdberge.refinedstorage.screen.AmountSpecifyingScreen;
import com.raoulvdberge.refinedstorage.screen.BaseScreen;
import com.raoulvdberge.refinedstorage.screen.grid.stack.FluidGridStack;
import com.raoulvdberge.refinedstorage.screen.grid.stack.IGridStack;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fluids.FluidAttributes;

public class GuiGridCraftingSettings extends AmountSpecifyingScreen<CraftingSettingsContainer> {
    private IGridStack stack;

    public GuiGridCraftingSettings(BaseScreen parent, PlayerEntity player, IGridStack stack) {
        super(parent, new CraftingSettingsContainer(player, stack), 172, 99, player.inventory, new TranslationTextComponent("container.crafting"));

        this.stack = stack;
    }

    @Override
    protected String getOkButtonText() {
        return I18n.format("misc.refinedstorage:start");
    }

    @Override
    protected String getTexture() {
        return "gui/crafting_settings.png";
    }

    @Override
    protected int[] getIncrements() {
        if (stack instanceof FluidGridStack) {
            return new int[]{
                100, 500, 1000,
                -100, -500, -1000
            };
        } else {
            return new int[]{
                1, 10, 64,
                -1, -10, -64
            };
        }
    }

    @Override
    protected int getDefaultAmount() {
        return stack instanceof FluidGridStack ? FluidAttributes.BUCKET_VOLUME : 1;
    }

    @Override
    protected boolean canAmountGoNegative() {
        return false;
    }

    @Override
    protected int getMaxAmount() {
        return Integer.MAX_VALUE;
    }

    protected void onOkButtonPressed(boolean shiftDown) {
        Integer quantity = Ints.tryParse(amountField.getText());

        if (quantity != null && quantity > 0) {
            // TODO RS.INSTANCE.network.sendToServer(new MessageGridCraftingPreview(stack.getHash(), quantity, shiftDown, stack instanceof GridStackFluid));

            okButton.active = false;
        }
    }
}
