package me.wizicl.journeymode.items;

import me.wizicl.journeymode.capabilities.IResearch;
import me.wizicl.journeymode.capabilities.ResearchProvider;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;

public class ItemTestItem extends Item {

    public ItemTestItem(String name) {
        setUnlocalizedName(name); // Имя для локализации
        setRegistryName(name); // Айди предмета
        setCreativeTab(CreativeTabs.MISC); // Вкладка в креативе
        setMaxStackSize(1);
    }

    /**
     * Этот метод вызывается, когда игрок нажимает ПКМ с предметом в руке.
     *
     * @param world  Мир, в котором находится игрок.
     * @param player Игрок, использующий предмет.
     * @param hand   Рука (основная или вторая).
     * @return ActionResult, описывающий результат действия.
     */

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos block, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        block = block.offset(facing); // Блок на который смотрим игрок
        ItemStack stack = player.getHeldItem(hand); // Данные предмета

        // Создаем NBTTagCompound если такового нет
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }

        // Задаём переменные и NBT
        NBTTagCompound tag = stack.getTagCompound();
        int power = tag.getInteger("clicks");
        int maxpower = tag.getInteger("maxclicks");
        boolean charged = true;

        // Логика переменных
        if (power >= 10) charged = false;
        tag.setInteger("maxclicks", maxpower);
        tag.setInteger("maxclicks", maxpower = 10);
        tag.setInteger("clicks", power);

        // Выполняем заданные функции
        if (charged) {
            //Поджигание блока сверху и вывод количества кликов в чат
            if (player.canPlayerEdit(block, facing, stack) && world.isAirBlock(block)) {

                // Тратим заряды только если блок валидный
                tag.setInteger("clicks", power + 1);

                // Звук использования огнива на блоке
                world.playSound(player, block, SoundEvents.ITEM_FLINTANDSTEEL_USE, SoundCategory.BLOCKS, 1.0F, 1.0F);

                // Эффект огня
                world.spawnParticle(EnumParticleTypes.FLAME,
                        player.posX, player.posY + 1, player.posZ, 0, 0, 0, 5);

                if (!world.isRemote) {
                    IResearch cap = player.getCapability(ResearchProvider.RESEARCH, null);
                    if (cap != null) {
                        String currentItem = stack.getItem().getRegistryName().toString();
                        cap.addResearch(currentItem, 1);
                        int researchCount = cap.getResearchCount(currentItem);
                        player.sendMessage(new TextComponentTranslation("chat.journeymode.research", researchCount));
                    }

                    world.setBlockState(block, Blocks.FIRE.getDefaultState(), 11);
                    player.sendMessage(new TextComponentTranslation("chat.journeymode.click_count", power + 1, maxpower - power));
                    }
            }

            // Воспроизводим анимацию движения руки
            return EnumActionResult.SUCCESS;
        }
        else {

            // Меняем предмет на палку
            ItemStack vanillaStack = new ItemStack(Items.STICK);
            player.setHeldItem(hand, vanillaStack);

            // Воспроизводим анимацию движения руки
            return EnumActionResult.SUCCESS;
        }
    }
}

