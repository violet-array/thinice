package violet.thinmyice;

import net.minecraft.world.item.*;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.WaterloggedTransparentBlock;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import violet.thinmyice.block.ThinIceBlock;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(ThinMyIce.MODID)
public class ThinMyIce
{
    // Define mod id in a common place for everything to reference
    public static final String MODID = "thinmyice";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);


    // Thin Ice Block and Item
    public static final String THIN_ICE_BLOCK_ID = "thin_ice";
    public static final DeferredBlock<Block> THIN_ICE_BLOCK = BLOCKS.register(THIN_ICE_BLOCK_ID, () -> new ThinIceBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.ICE)));
    public static final DeferredItem<BlockItem> THIN_ICE_ITEM = ITEMS.registerSimpleBlockItem(THIN_ICE_BLOCK_ID, THIN_ICE_BLOCK);




    // Creates a creative tab after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> THIN_MY_ICE_TAB = CREATIVE_MODE_TABS.register("thin_my_ice_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.thinmyice"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> THIN_ICE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(THIN_ICE_ITEM.get());
            }).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public ThinMyIce(IEventBus modEventBus, ModContainer modContainer)
    {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (ExampleMod) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
//        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
    }

    // Add the example block item to the building blocks tab
    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {

            event.accept(THIN_ICE_ITEM.toStack());

        }
    }

//    // You can use SubscribeEvent and let the Event Bus discover methods to call
//    @SubscribeEvent
//    public void onServerStarting(ServerStartingEvent event)
//    {
//    }
//
//    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
//    @EventBusSubscriber(modid = MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
//    public static class ClientModEvents
//    {
//        @SubscribeEvent
//        public static void onClientSetup(FMLClientSetupEvent event)
//        {
//
//
//        }
//    }
}
