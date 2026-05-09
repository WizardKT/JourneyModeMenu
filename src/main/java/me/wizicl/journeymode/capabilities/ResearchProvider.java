package me.wizicl.journeymode.capabilities;

import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ResearchProvider implements ICapabilitySerializable<NBTBase> {

    // Айди нашего интерфейса
    public static final ResourceLocation ID = new ResourceLocation("journeymode", "IResearch");

    // Экземпляр нашего интерфейса
    @CapabilityInject(IResearch.class)
    public static Capability<IResearch> RESEARCH = null;

    // Задаём источник
    public IResearch instance = RESEARCH.getDefaultInstance();

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
    public NBTBase serializeNBT() {
        return RESEARCH.getStorage().writeNBT(RESEARCH, instance, null);
    }

    // Удаляем данные интерфейса
    @Override
    public void deserializeNBT(NBTBase nbt) {
        RESEARCH.getStorage().readNBT(RESEARCH, instance, null, nbt);
    }
}
