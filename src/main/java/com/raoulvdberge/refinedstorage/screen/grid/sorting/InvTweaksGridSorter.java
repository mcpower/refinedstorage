package com.raoulvdberge.refinedstorage.screen.grid.sorting;

import com.raoulvdberge.refinedstorage.api.network.grid.IGrid;
import com.raoulvdberge.refinedstorage.screen.grid.stack.IGridStack;

// TODO
public class InvTweaksGridSorter implements IGridSorter {
    public static final String MOD_ID = "inventorytweaks";

    @Override
    public boolean isApplicable(IGrid grid) {
        return false;
    }

    @Override
    public int compare(IGridStack left, IGridStack right, SortingDirection direction) {
        return 0;
    }

/*    private InvTweaksAPI api = null;

    public GridSorterInventoryTweaks() {
        try {
            api = (InvTweaksAPI) Class.forName("invtweaks.forge.InvTweaksMod", true, Loader.instance().getModClassLoader()).getField("instance").get(null);
        } catch (Exception e) {
            // NO OP
        }
    }

    @Override
    public boolean isApplicable(IGrid grid) {
        return grid.getSortingType() == IGrid.SORTING_TYPE_INVENTORYTWEAKS;
    }

    @Override
    public int compare(IGridStack left, IGridStack right, GridSorterDirection sortingDirection) {
        if (api != null && left instanceof GridStackItem && right instanceof GridStackItem) {
            if (sortingDirection == GridSorterDirection.DESCENDING) {
                return api.compareItems(((GridStackItem) left).getStack(), ((GridStackItem) right).getStack());
            } else if (sortingDirection == GridSorterDirection.ASCENDING) {
                return api.compareItems(((GridStackItem) right).getStack(), ((GridStackItem) left).getStack());
            }
        }

        return 0;
    }*/

}