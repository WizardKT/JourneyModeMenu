package me.wizicl.journeymode.capabilities;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import java.util.Objects;

public class ResearchKey {
    private final ResourceLocation registryName;
    private final int meta;
    private final NBTTagCompound nbt;

    public ResearchKey(ItemStack stack) {
        this.registryName = stack.getItem().getRegistryName();
        this.meta = stack.getMetadata();
        this.nbt = stack.hasTagCompound() ? stack.getTagCompound().copy() : null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResearchKey that = (ResearchKey) o;

        if (meta != that.meta) return false;
        if (!registryName.equals(that.registryName)) return false;

        return Objects.equals(this.nbt, that.nbt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registryName, meta, nbt);
    }
}
