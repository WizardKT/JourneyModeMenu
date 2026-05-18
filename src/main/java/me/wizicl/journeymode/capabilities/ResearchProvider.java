package me.wizicl.journeymode.capabilities;

import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ResearchProvider implements ICapabilitySerializable<NBTTagCompound> {

    // Айди нашего интерфейса
    public static final ResourceLocation ID = new ResourceLocation("journeymode", "research");

    // Экземпляр нашего интерфейса
    @CapabilityInject(IResearch.class)
    public static Capability<IResearch> RESEARCH = null;

    private final IResearch instance = RESEARCH.getDefaultInstance();

    // Проверяем наличие интерфейса
    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == RESEARCH;
    }

    // Берём данные интерфейса
    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        return capability == RESEARCH ? RESEARCH.cast(this.instance) : null;
    }

    // Задаём данные интерфейса
    @Override
    public NBTTagCompound serializeNBT() {
        // Проверяем, что наш инстанс — это именно класс Research, у которого есть методы NBT
        if (this.instance instanceof Research) {
            return ((Research) this.instance).serializeNBT();
        }
        return new NBTTagCompound();
    }

    // Удаляем данные интерфейса
    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        if (this.instance instanceof Research) {
            ((Research) this.instance).deserializeNBT(nbt);
        }
    }
}
