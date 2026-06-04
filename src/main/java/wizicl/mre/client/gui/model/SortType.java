package wizicl.mre.client.gui.model;

import net.minecraft.item.ItemStack;

import java.util.Comparator;

public enum SortType {
    ALPHABETICAL(Comparator.comparing(ItemStack::getDisplayName)),
    ID(Comparator.comparing(stack -> stack.getItem().getRegistryName().toString())),
    SUPER_SORT(Comparator.comparing((ItemStack stack) -> stack.getItem().getRegistryName().getNamespace())
            .thenComparing(stack -> stack.getItem().getRegistryName().getPath())
            .thenComparingInt(ItemStack::getMetadata));
    private final Comparator<ItemStack> comparator;

    SortType(Comparator<ItemStack> comparator) {
        this.comparator = comparator;
    }

    public Comparator<ItemStack> getComparator() {
        return comparator;
    }
}
