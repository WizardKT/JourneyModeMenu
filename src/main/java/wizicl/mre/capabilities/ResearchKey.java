package wizicl.mre.capabilities;

import wizicl.mre.config.ConfigNBT;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Objects;

public final class ResearchKey {
    private final ResourceLocation registryName;
    private final int meta;
    private final NBTTagCompound cleanedNbt;

    public ResearchKey(ResourceLocation registryName, int meta, NBTTagCompound rawNbt) {
        this.registryName = registryName;
        this.meta = meta;
        this.cleanedNbt = isolateAndCleanNBT(rawNbt);
    }

    public ResearchKey(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            this.registryName = new ResourceLocation("minecraft", "air");
            this.meta = 0;
            this.cleanedNbt = null;
            return;
        }

        this.registryName = stack.getItem().getRegistryName();
        this.meta = stack.getMetadata();

        this.cleanedNbt = stack.hasTagCompound() ? isolateAndCleanNBT(stack.getTagCompound()) : null;
    }

    private static NBTTagCompound isolateAndCleanNBT(NBTTagCompound sourceNbt) {
        if (sourceNbt == null || sourceNbt.isEmpty()) {
            return null;
        }

        NBTTagCompound isolatedCopy = sourceNbt.copy();

        // Удаляем стандартный мусор Forge
        isolatedCopy.removeTag("ForgeCaps");

        // Удаляем мусор от HBM, который ломает проверки
        isolatedCopy.removeTag("charge");

        if (ConfigNBT.IGNORED_TAGS != null) {
            for (String tagToRemove : ConfigNBT.IGNORED_TAGS) {
                if (isolatedCopy.hasKey(tagToRemove)) {
                    isolatedCopy.removeTag(tagToRemove);
                }
            }
        }

        return isolatedCopy.isEmpty() ? null : isolatedCopy;
    }

    public NBTTagCompound getCleanedNbt() {
        return this.cleanedNbt == null ? null : this.cleanedNbt.copy();
    }

    public ResourceLocation getRegistryName() {
        return this.registryName;
    }

    public int getMeta() {
        return this.meta;
    }

    public ItemStack createItemStack() {
        Item item = ForgeRegistries.ITEMS.getValue(this.registryName);
        if (item == null) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(item, 1, this.meta);
        if (this.cleanedNbt != null) {
            stack.setTagCompound(this.cleanedNbt.copy());
        }
        return stack;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResearchKey that = (ResearchKey) o;

        return this.meta == that.meta &&
                Objects.equals(this.registryName, that.registryName) &&
                Objects.equals(this.cleanedNbt, that.cleanedNbt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.registryName, this.meta, this.cleanedNbt);
    }
}
