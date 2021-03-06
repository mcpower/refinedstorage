package com.raoulvdberge.refinedstorage.screen.grid.stack;

import com.raoulvdberge.refinedstorage.api.storage.tracker.StorageTrackerEntry;
import com.raoulvdberge.refinedstorage.apiimpl.API;
import com.raoulvdberge.refinedstorage.screen.BaseScreen;
import com.raoulvdberge.refinedstorage.util.RenderUtils;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.stream.Collectors;

public class ItemGridStack implements IGridStack {
    private UUID id;
    private ItemStack stack;
    private String cachedName;
    private boolean craftable;
    private boolean displayCraftText;
    private String[] oreIds = null;
    @Nullable
    private StorageTrackerEntry entry;
    private String modId;
    private String modName;
    private String tooltip;

    public ItemGridStack(ItemStack stack) {
        this.stack = stack;
    }

    public ItemGridStack(UUID id, ItemStack stack, boolean craftable, boolean displayCraftText, StorageTrackerEntry entry) {
        this.id = id;
        this.stack = stack;
        this.craftable = craftable;
        this.displayCraftText = displayCraftText;
        this.entry = entry;
    }

    @Nullable
    static String getModNameByModId(String modId) {
        /*ModContainer container = Loader.instance().getActiveModList().stream()
            .filter(m -> m.getModId().toLowerCase().equals(modId))
            .findFirst()
            .orElse(null);

        return container == null ? null : container.getName();*/
        return "dinosaur"; // TODO
    }

    public ItemStack getStack() {
        return stack;
    }

    @Override
    public boolean isCraftable() {
        return craftable;
    }

    @Override
    public boolean doesDisplayCraftText() {
        return displayCraftText;
    }

    @Override
    public void setDisplayCraftText(boolean displayCraftText) {
        this.displayCraftText = displayCraftText;

        if (displayCraftText) {
            this.stack.setCount(1);
        }
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public String getName() {
        try {
            if (cachedName == null) {
                cachedName = stack.getDisplayName().getFormattedText();
            }

            return cachedName;
        } catch (Throwable t) {
            return "";
        }
    }

    @Override
    public String getModId() {
        if (modId == null) {
            modId = stack.getItem().getCreatorModId(stack);

            if (modId == null) {
                modId = "???";
            }
        }

        return modId;
    }

    @Override
    public String getModName() {
        if (modName == null) {
            modName = getModNameByModId(getModId());

            if (modName == null) {
                modName = "???";
            }
        }

        return modName;
    }

    @Override
    public String[] getOreIds() {
        if (oreIds == null) {
            if (stack.isEmpty()) {
                oreIds = new String[]{};
            } else {
                oreIds = new String[]{};//TODO OreDict
                //oreIds = Arrays.stream(OreDictionary.getOreIDs(stack)).mapToObj(OreDictionary::getOreName).toArray(String[]::new);
            }
        }

        return oreIds;
    }

    @Override
    public String getTooltip() {
        if (tooltip == null) {
            try {
                tooltip = RenderUtils.getTooltipFromItem(stack).stream().collect(Collectors.joining("\n"));
            } catch (Throwable t) {
                tooltip = "";
            }
        }

        return tooltip;
    }

    @Override
    public int getQuantity() {
        return doesDisplayCraftText() ? 0 : stack.getCount();
    }

    @Override
    public String getFormattedFullQuantity() {
        return API.instance().getQuantityFormatter().format(getQuantity());
    }

    @Override
    public void draw(BaseScreen gui, int x, int y) {
        String text = null;

        if (displayCraftText) {
            text = I18n.format("gui.refinedstorage.grid.craft");
        } else if (stack.getCount() > 1) {
            text = API.instance().getQuantityFormatter().formatWithUnits(getQuantity());
        }

        gui.renderItem(x, y, stack, true, text);
    }

    @Override
    public Object getIngredient() {
        return stack;
    }

    @Nullable
    @Override
    public StorageTrackerEntry getTrackerEntry() {
        return entry;
    }

    @Override
    public void setTrackerEntry(@Nullable StorageTrackerEntry entry) {
        this.entry = entry;
    }
}
