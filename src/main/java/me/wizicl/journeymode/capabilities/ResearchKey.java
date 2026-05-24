package me.wizicl.journeymode.capabilities;

import me.wizicl.journeymode.config.ConfigNBT;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;

import java.util.Objects;

public class ResearchKey {
    private final ResourceLocation registryName;
    private final int meta;
    private final NBTTagCompound cleanedNbt;

    public ResearchKey(ResourceLocation registryName, int meta, NBTTagCompound cleanedNbt) {
        this.registryName = registryName;
        this.meta = meta;
        this.cleanedNbt = (cleanedNbt == null || cleanedNbt.hasNoTags()) ? null : cleanedNbt;
    }

    public ResearchKey(ItemStack stack) {
        this.registryName = stack.getItem().getRegistryName();
        this.meta = stack.getMetadata();

        if (stack.hasTagCompound()) {

            // Копия, чтобы не испортить оригинал
            NBTTagCompound nbtCopy = stack.getTagCompound().copy();

            // Удаляем NBT находящиеся в ЧС
            for (String tagToRemove : ConfigNBT.IGNORED_TAGS) {
                if (nbtCopy.hasKey(tagToRemove)) {
                    nbtCopy.removeTag(tagToRemove);
                }
            }

            // Если после проверки NBT остался пустым - заменяем null
            this.cleanedNbt = (nbtCopy == null || nbtCopy.hasNoTags()) ? null : nbtCopy;
        } else {
            this.cleanedNbt = null;
        }
    }

    public ItemStack createItemStack() {
        // 1. Ищем предмет в регистрах игры по его ResourceLocation
        net.minecraft.item.Item item = net.minecraftforge.fml.common.registry.ForgeRegistries.ITEMS.getValue(this.registryName);
        // Если по какой-то причине предмет не найден (например, мод удалили), возвращаем пустой стек
        if (item == null) {
            return ItemStack.EMPTY;
        }

        // 2. Создаем стек с количеством 1 и нашей метадатой
        ItemStack stack = new ItemStack(item, 1, this.meta);

        // 3. Если у нас были сохранены NBT-теги, возвращаем их предмету
        if (this.cleanedNbt != null) {
            stack.setTagCompound(this.cleanedNbt.copy()); // .copy() на всякий случай, чтобы не связать ссылки
        }

        return stack;
    }

    public ResourceLocation getRegistryName() { return registryName; }
    public int getMeta() { return meta; }
    public NBTTagCompound getCleanedNbt() { return cleanedNbt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResearchKey that = (ResearchKey) o;

        return meta == that.meta &&
                Objects.equals(registryName, that.registryName) &&
                Objects.equals(cleanedNbt, that.cleanedNbt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(registryName, meta, cleanedNbt);
    }
}
