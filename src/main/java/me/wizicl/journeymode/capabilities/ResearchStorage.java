package me.wizicl.journeymode.capabilities;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.Map;

public class ResearchStorage implements Capability.IStorage<IResearch> {
    @Nullable
    @Override
    public NBTBase writeNBT(Capability<IResearch> capability, IResearch instance, EnumFacing side) {
        NBTTagList researchedItems = new NBTTagList();

        for (Map.Entry<ResearchKey, Integer> entry : instance.getReadOnlyMap().entrySet()) {
            NBTTagCompound tag = new NBTTagCompound();
            ResearchKey key = entry.getKey();

            tag.setString("id", key.getRegistryName().toString());
            tag.setInteger("meta", key.getMeta());
            if (key.getCleanedNbt()  != null) {
                tag.setTag("item_nbt", key.getCleanedNbt());
            }

            tag.setInteger("amount", entry.getValue());

            researchedItems.appendTag(tag);
        }
        return researchedItems;
    }

    @Override
    public void readNBT(Capability<IResearch> capability, IResearch instance, EnumFacing side, NBTBase nbt) {
        if (!(nbt instanceof NBTTagList)) return ;

        NBTTagList researchedItems = (NBTTagList) nbt;
        instance.clear();

        for (int i = 0; i < researchedItems.tagCount(); i++) {
            NBTTagCompound tag = researchedItems.getCompoundTagAt(i);

            String id = tag.getString("id");
            int meta = tag.getInteger("meta");
            int amount = tag.getInteger("amount");

            Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(id));

            if (item != null) {
                ItemStack stack = new ItemStack(item, 1, meta);

                if (tag.hasKey("item_nbt", Constants.NBT.TAG_COMPOUND)) {
                    stack.setTagCompound(tag.getCompoundTag("item_nbt"));
                }

                instance.setResearch(stack, amount);
            }
        }

    }
}
