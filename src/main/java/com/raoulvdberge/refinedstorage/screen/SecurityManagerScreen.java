package com.raoulvdberge.refinedstorage.screen;

import com.raoulvdberge.refinedstorage.RS;
import com.raoulvdberge.refinedstorage.api.network.security.Permission;
import com.raoulvdberge.refinedstorage.container.SecurityManagerContainer;
import com.raoulvdberge.refinedstorage.item.SecurityCardItem;
import com.raoulvdberge.refinedstorage.network.SecurityManagerUpdateMessage;
import com.raoulvdberge.refinedstorage.screen.widget.sidebutton.RedstoneModeSideButton;
import com.raoulvdberge.refinedstorage.tile.SecurityManagerTile;
import com.raoulvdberge.refinedstorage.util.RenderUtils;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.client.config.GuiCheckBox;

public class SecurityManagerScreen extends BaseScreen<SecurityManagerContainer> {
    private final SecurityManagerTile securityManager;
    private GuiCheckBox[] permissions = new GuiCheckBox[Permission.values().length];

    public SecurityManagerScreen(SecurityManagerContainer container, PlayerInventory inventory, ITextComponent title) {
        super(container, 176, 234, inventory, title);

        this.securityManager = (SecurityManagerTile) container.getTile();
    }

    @Override
    public void onPostInit(int x, int y) {
        addSideButton(new RedstoneModeSideButton(this, SecurityManagerTile.REDSTONE_MODE));

        int padding = 15;

        permissions[0] = addCheckBox(x + 7, y + 93, I18n.format("gui.refinedstorage.security_manager.permission.0"), false, btn -> handle(0));
        permissions[1] = addCheckBox(permissions[0].x, permissions[0].y + padding, I18n.format("gui.refinedstorage.security_manager.permission.1"), false, btn -> handle(1));
        permissions[2] = addCheckBox(permissions[1].x, permissions[1].y + padding, I18n.format("gui.refinedstorage.security_manager.permission.2"), false, btn -> handle(2));
        permissions[3] = addCheckBox(permissions[0].x + 90, permissions[0].y, I18n.format("gui.refinedstorage.security_manager.permission.3"), false, btn -> handle(3));
        permissions[4] = addCheckBox(permissions[3].x, permissions[3].y + padding, I18n.format("gui.refinedstorage.security_manager.permission.4"), false, btn -> handle(4));
        permissions[5] = addCheckBox(permissions[4].x, permissions[4].y + padding, I18n.format("gui.refinedstorage.security_manager.permission.5"), false, btn -> handle(5));
    }

    private void handle(int i) {
        RS.NETWORK_HANDLER.sendToServer(new SecurityManagerUpdateMessage(securityManager.getPos(), Permission.values()[i], permissions[i].isChecked()));
    }

    @Override
    public void tick(int x, int y) {
        ItemStack card = securityManager.getNode().getEditCard().getStackInSlot(0);

        for (Permission permission : Permission.values()) {
            permissions[permission.getId()].setIsChecked(!card.isEmpty() && SecurityCardItem.hasPermission(card, permission));
        }
    }

    @Override
    public void renderBackground(int x, int y, int mouseX, int mouseY) {
        bindTexture(RS.ID, "gui/security_manager.png");

        blit(x, y, 0, 0, xSize, ySize);
    }

    @Override
    public void renderForeground(int mouseX, int mouseY) {
        renderString(7, 7, title.getFormattedText());
        renderString(7, 59, I18n.format("gui.refinedstorage.security_manager.configure"));
        renderString(7, 140, I18n.format("container.inventory"));

        for (int i = 0; i < permissions.length; ++i) {
            GuiCheckBox permission = permissions[i];

            if (RenderUtils.inBounds(permission.x - guiLeft, permission.y - guiTop, permission.getWidth(), permission.getHeight(), mouseX, mouseY)) {
                renderTooltip(mouseX, mouseY, I18n.format("gui.refinedstorage.security_manager.permission." + i + ".tooltip"));
            }
        }
    }
}
