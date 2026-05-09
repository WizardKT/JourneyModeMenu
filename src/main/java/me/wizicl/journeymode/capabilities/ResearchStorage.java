package me.wizicl.journeymode.capabilities;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;
import java.util.Map;

public class ResearchStorage implements Capability.IStorage<IResearch> {
    @Nullable
    @Override
    public NBTBase writeNBT(Capability<IResearch> capability, IResearch instance, EnumFacing side) {
        NBTTagList list = new NBTTagList();

        for (Map.Entry<String, Integer> entry : instance.getResearchMap().entrySet()) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("item", entry.getKey());
            tag.setInteger("amount", entry.getValue());
            list.appendTag(tag);
        }
        return list;
    }

    @Override
    public void readNBT(Capability<IResearch> capability, IResearch instance, EnumFacing side, NBTBase nbt) {
        NBTTagList tagList = (NBTTagList) nbt;

        for (int i = 0; i < tagList.tagCount(); i++) {
            NBTTagCompound tag = tagList.getCompoundTagAt(i);
            instance.addResearch(tag.getString("item"), tag.getInteger("amount"));
        }

    }
}
