package wizicl.mre.capabilities;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.INBTSerializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ResearchProvider implements ICapabilityProvider, INBTSerializable<NBTTagCompound> {

    @CapabilityInject(IResearch.class)
    public static Capability<IResearch> RESEARCH = null;

    // Инициализируем инстанс по умолчанию
    private final IResearch instance = RESEARCH.getDefaultInstance();

    // Проверяем наличие интерфейса
    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == RESEARCH;
    }

    // Берём данные интерфейса
    @Override
    @Nullable
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        // Тернарный оператор в Java 25 работает молниеносно благодаря улучшенному выводу типов дженериков
        return capability == RESEARCH ? RESEARCH.cast(this.instance) : null;
    }

    // Задаём данные интерфейса
    @Override
    public NBTTagCompound serializeNBT() {
        // Pattern Matching: проверяем тип и сразу создаем переменную 'research'
        if (this.instance instanceof Research research) {
            return research.serializeNBT();
        }

        return new NBTTagCompound();
    }

    // Удаляем данные интерфейса
    @Override
    public void deserializeNBT(NBTTagCompound nbt) {
        if (this.instance instanceof Research research) {
            research.deserializeNBT(nbt);
        }
    }
}
