package me.wizicl.journeymode.proxy;

import me.wizicl.journeymode.init.ModItems;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(Side.CLIENT)
public class ClientProxy extends CommonProxy{

    @SubscribeEvent
    public static void onModelRegister(ModelRegistryEvent event) {
//        registerModel(ModItems.ITEM_TEST_ITEM);
    }

    private static void registerModel(Item item) {
//        ModelLoader.setCustomModelResourceLocation(item, 0,
//                new ModelResourceLocation(item.getRegistryName(), "inventory"));
    }


    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
    }

}
