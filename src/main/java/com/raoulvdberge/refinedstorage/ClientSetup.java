package com.raoulvdberge.refinedstorage;

import com.raoulvdberge.refinedstorage.apiimpl.API;
import com.raoulvdberge.refinedstorage.container.CrafterContainer;
import com.raoulvdberge.refinedstorage.container.CrafterManagerContainer;
import com.raoulvdberge.refinedstorage.container.slot.CrafterManagerSlot;
import com.raoulvdberge.refinedstorage.render.BakedModelOverrideRegistry;
import com.raoulvdberge.refinedstorage.render.color.PatternItemColor;
import com.raoulvdberge.refinedstorage.render.model.DiskDriveBakedModel;
import com.raoulvdberge.refinedstorage.render.model.FullbrightBakedModel;
import com.raoulvdberge.refinedstorage.render.model.PatternBakedModel;
import com.raoulvdberge.refinedstorage.screen.*;
import com.raoulvdberge.refinedstorage.screen.factory.GridScreenFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

public class ClientSetup {
    private BakedModelOverrideRegistry bakedModelOverrideRegistry = new BakedModelOverrideRegistry();

    public ClientSetup() {
        bakedModelOverrideRegistry.add(new ResourceLocation(RS.ID, "controller"), (base, registry) -> new FullbrightBakedModel(
            base,
            new ResourceLocation(RS.ID, "block/controller/cutouts/nearly_off"),
            new ResourceLocation(RS.ID, "block/controller/cutouts/nearly_on"),
            new ResourceLocation(RS.ID, "block/controller/cutouts/on")
        ));
        bakedModelOverrideRegistry.add(new ResourceLocation(RS.ID, "creative_controller"), (base, registry) -> new FullbrightBakedModel(
            base,
            new ResourceLocation(RS.ID, "block/controller/cutouts/nearly_off"),
            new ResourceLocation(RS.ID, "block/controller/cutouts/nearly_on"),
            new ResourceLocation(RS.ID, "block/controller/cutouts/on")
        ));
        bakedModelOverrideRegistry.add(new ResourceLocation(RS.ID, "grid"), (base, registry) -> new FullbrightBakedModel(base, new ResourceLocation(RS.ID, "block/grid/cutouts/front_connected")));
        bakedModelOverrideRegistry.add(new ResourceLocation(RS.ID, "crafting_grid"), (base, registry) -> new FullbrightBakedModel(base, new ResourceLocation(RS.ID, "block/grid/cutouts/crafting_front_connected")));
        bakedModelOverrideRegistry.add(new ResourceLocation(RS.ID, "pattern_grid"), (base, registry) -> new FullbrightBakedModel(base, new ResourceLocation(RS.ID, "block/grid/cutouts/pattern_front_connected")));
        bakedModelOverrideRegistry.add(new ResourceLocation(RS.ID, "fluid_grid"), (base, registry) -> new FullbrightBakedModel(base, new ResourceLocation(RS.ID, "block/grid/cutouts/fluid_front_connected")));
        bakedModelOverrideRegistry.add(new ResourceLocation(RS.ID, "network_receiver"), (base, registry) -> new FullbrightBakedModel(base, new ResourceLocation(RS.ID, "block/network_receiver/cutouts/connected")));
        bakedModelOverrideRegistry.add(new ResourceLocation(RS.ID, "network_transmitter"), (base, registry) -> new FullbrightBakedModel(base, new ResourceLocation(RS.ID, "block/network_transmitter/cutouts/connected")));
        bakedModelOverrideRegistry.add(new ResourceLocation(RS.ID, "relay"), (base, registry) -> new FullbrightBakedModel(base, new ResourceLocation(RS.ID, "block/relay/cutouts/connected")));
        bakedModelOverrideRegistry.add(new ResourceLocation(RS.ID, "detector"), (base, registry) -> new FullbrightBakedModel(base, new ResourceLocation(RS.ID, "block/detector/cutouts/on")));
        bakedModelOverrideRegistry.add(new ResourceLocation(RS.ID, "security_manager"), (base, registry) -> new FullbrightBakedModel(
            base,
            new ResourceLocation(RS.ID, "block/security_manager/cutouts/top_connected"),
            new ResourceLocation(RS.ID, "block/security_manager/cutouts/front_connected"),
            new ResourceLocation(RS.ID, "block/security_manager/cutouts/left_connected"),
            new ResourceLocation(RS.ID, "block/security_manager/cutouts/back_connected"),
            new ResourceLocation(RS.ID, "block/security_manager/cutouts/right_connected")
        ));

        bakedModelOverrideRegistry.add(new ResourceLocation(RS.ID, "disk_drive"), (base, registry) -> new FullbrightBakedModel(
            new DiskDriveBakedModel(
                base,
                registry.get(new ResourceLocation(RS.ID + ":block/disks/disk")),
                registry.get(new ResourceLocation(RS.ID + ":block/disks/disk_near_capacity")),
                registry.get(new ResourceLocation(RS.ID + ":block/disks/disk_full")),
                registry.get(new ResourceLocation(RS.ID + ":block/disks/disk_disconnected"))
            ),
            new ResourceLocation(RS.ID, "block/disks/leds")
        ).disableCache());

        bakedModelOverrideRegistry.add(new ResourceLocation(RS.ID, "pattern"), (base, registry) -> new PatternBakedModel(base));

        ModelLoader.addSpecialModel(new ResourceLocation(RS.ID + ":block/disks/disk"));
        ModelLoader.addSpecialModel(new ResourceLocation(RS.ID + ":block/disks/disk_near_capacity"));
        ModelLoader.addSpecialModel(new ResourceLocation(RS.ID + ":block/disks/disk_full"));
        ModelLoader.addSpecialModel(new ResourceLocation(RS.ID + ":block/disks/disk_disconnected"));

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onModelBake);

        API.instance().addPatternRenderHandler(pattern -> Screen.hasShiftDown());
        API.instance().addPatternRenderHandler(pattern -> {
            Container container = Minecraft.getInstance().player.openContainer;

            if (container instanceof CrafterManagerContainer) {
                for (Slot slot : container.inventorySlots) {
                    if (slot instanceof CrafterManagerSlot && slot.getStack() == pattern) {
                        return true;
                    }
                }
            }

            return false;
        });
        API.instance().addPatternRenderHandler(pattern -> {
            Container container = Minecraft.getInstance().player.openContainer;

            if (container instanceof CrafterContainer) {
                for (int i = 0; i < 9; ++i) {
                    if (container.getSlot(i).getStack() == pattern) {
                        return true;
                    }
                }
            }

            return false;
        });
    }

    @SubscribeEvent
    public void onClientSetup(FMLClientSetupEvent e) {
        ScreenManager.registerFactory(RSContainers.FILTER, FilterScreen::new);
        ScreenManager.registerFactory(RSContainers.CONTROLLER, ControllerScreen::new);
        ScreenManager.registerFactory(RSContainers.DISK_DRIVE, DiskDriveScreen::new);
        ScreenManager.registerFactory(RSContainers.GRID, new GridScreenFactory());
        ScreenManager.registerFactory(RSContainers.STORAGE_BLOCK, StorageBlockScreen::new);
        ScreenManager.registerFactory(RSContainers.FLUID_STORAGE_BLOCK, FluidStorageBlockScreen::new);
        ScreenManager.registerFactory(RSContainers.EXTERNAL_STORAGE, ExternalStorageScreen::new);
        ScreenManager.registerFactory(RSContainers.IMPORTER, ImporterScreen::new);
        ScreenManager.registerFactory(RSContainers.EXPORTER, ExporterScreen::new);
        ScreenManager.registerFactory(RSContainers.NETWORK_TRANSMITTER, NetworkTransmitterScreen::new);
        ScreenManager.registerFactory(RSContainers.RELAY, RelayScreen::new);
        ScreenManager.registerFactory(RSContainers.DETECTOR, DetectorScreen::new);
        ScreenManager.registerFactory(RSContainers.SECURITY_MANAGER, SecurityManagerScreen::new);

        ClientRegistry.registerKeyBinding(RSKeyBindings.FOCUS_SEARCH_BAR);
        ClientRegistry.registerKeyBinding(RSKeyBindings.CLEAR_GRID_CRAFTING_MATRIX);

        e.getMinecraftSupplier().get().getItemColors().register(new PatternItemColor(), RSItems.PATTERN);
    }

    @SubscribeEvent
    public void onModelBake(ModelBakeEvent e) {
        for (ResourceLocation id : e.getModelRegistry().keySet()) {
            BakedModelOverrideRegistry.BakedModelOverrideFactory factory = this.bakedModelOverrideRegistry.get(new ResourceLocation(id.getNamespace(), id.getPath()));

            if (factory != null) {
                e.getModelRegistry().put(id, factory.create(e.getModelRegistry().get(id), e.getModelRegistry()));
            }
        }
    }
}
