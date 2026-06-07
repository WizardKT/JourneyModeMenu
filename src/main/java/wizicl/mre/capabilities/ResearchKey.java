package wizicl.mre.capabilities;

import wizicl.mre.config.ConfigNBT;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public record ResearchKey(ResourceLocation registryName, int meta, NBTTagCompound cleanedNbt) {

    // Главный (компактный) конструктор. Он автоматически вызывается в конце.
    // Здесь перехватывается rawNbt и очищается прямо перед записью в поле
    public ResearchKey(ResourceLocation registryName, int meta, NBTTagCompound cleanedNbt) {
        this.registryName = registryName;
        this.meta = meta;
        this.cleanedNbt = isolateAndCleanNBT(cleanedNbt);
    }

    // Дополнительный конструктор из ItemStack (вызывает главный через this(...))
    public ResearchKey(ItemStack stack) {
        this(
                (stack == null || stack.isEmpty()) ? new ResourceLocation("minecraft", "air") : stack.getItem().getRegistryName(),
                (stack == null || stack.isEmpty()) ? 0 : stack.getMetadata(),
                (stack != null && stack.hasTagCompound()) ? stack.getTagCompound() : null
        );
    }

    // Очистка NBT
    private static NBTTagCompound isolateAndCleanNBT(NBTTagCompound sourceNbt) {
        if (sourceNbt == null || sourceNbt.isEmpty()) {
            return null;
        }

        var isolatedCopy = sourceNbt.copy();

        // Чистим мусор
        isolatedCopy.removeTag("ForgeCaps");
        isolatedCopy.removeTag("charge");

        if (ConfigNBT.IGNORED_TAGS != null) {
            for (var tagToRemove : ConfigNBT.IGNORED_TAGS) {
                if (isolatedCopy.hasKey(tagToRemove)) {
                    isolatedCopy.removeTag(tagToRemove);
                }
            }
        }

        return isolatedCopy.isEmpty() ? null : isolatedCopy;
    }

    // Создание стака из ключа
    public ItemStack createItemStack() {
        var item = ForgeRegistries.ITEMS.getValue(this.registryName);
        if (item == null) {
            return ItemStack.EMPTY;
        }

        var stack = new ItemStack(item, 1, this.meta);
        if (this.cleanedNbt != null) {
            stack.setTagCompound(this.cleanedNbt.copy());
        }
        return stack;
    }

    // --- СОВМЕСТИМОСТЬ СО СТАРЫМ КОДОМ ---
    // Record автоматически создал методы registryName(), meta() и cleanedNbt().
    // Но чтобы не переписывать весь остальной мод, делаем мосты для старых геттеров:

    public ResourceLocation getRegistryName() {
        return registryName(); // Вызывает нативный метод рекорда
    }

    public int getMeta() {
        return meta();
    }

    public NBTTagCompound getCleanedNbt() {
        return this.cleanedNbt == null ? null : this.cleanedNbt.copy(); // Сохраняем защиту копированием
    }
}
