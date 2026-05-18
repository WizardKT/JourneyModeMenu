package me.wizicl.journeymode.capabilities;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;

import java.util.HashMap;
import java.util.Map;

public class ResearchSerializer {
    public static NBTTagCompound serialize(Research instance) {
        NBTTagCompound nbt = new NBTTagCompound();

        // Упаковываем инвентарь исследовательского слота
        if (instance.getResearchInventory() instanceof net.minecraftforge.items.ItemStackHandler) {
            NBTTagCompound inventoryNBT = ((net.minecraftforge.items.ItemStackHandler) instance.getResearchInventory()).serializeNBT();
            nbt.setTag("ResearchSlot", inventoryNBT);
        }

        // Упаковываем мапу исследований
        NBTTagList list = new NBTTagList();
        for (Map.Entry<ResearchKey, Integer> entry : instance.getReadOnlyMap().entrySet()) {
            NBTTagCompound entryTag = new NBTTagCompound();

            // Вызываем упаковщик ключа
            entryTag.setTag("KeyData", serializeKey(entry.getKey()));
            entryTag.setInteger("ProgressValue", entry.getValue());

            list.appendTag(entryTag);
        }
        nbt.setTag("ProgressMap", list);

        return nbt;
    }

    public static void deserialize(Research instance, NBTTagCompound nbt) {

        // Распаковываем инвентарь исследовательского слота
        if (nbt.hasKey("ResearchSlot") && instance.getResearchInventory() instanceof net.minecraftforge.items.ItemStackHandler) {
            ((net.minecraftforge.items.ItemStackHandler) instance.getResearchInventory()).deserializeNBT(nbt.getCompoundTag("ResearchSlot"));
        }

        // Распаковываем мапу исследований
        if (nbt.hasKey("ProgressMap", Constants.NBT.TAG_LIST)) {
            NBTTagList list = nbt.getTagList("ProgressMap", Constants.NBT.TAG_COMPOUND);
            Map<ResearchKey, Integer> loadedMap = new HashMap<>();

            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound entryTag = list.getCompoundTagAt(i);

                // Вызываем распаковщик ключа
                ResearchKey key = deserializeKey(entryTag.getCompoundTag("KeyData"));
                if (key != null) {
                    loadedMap.put(key, entryTag.getInteger("ProgressValue"));
                }
            }

            instance.refreshFromServer(loadedMap);
        }
    }

    public static NBTTagCompound serializeKey(ResearchKey key) {
        NBTTagCompound tag = new NBTTagCompound();

        // ResourceLocation легко переводится в строку (например: "minecraft:stone")
        tag.setString("RegistryName", key.getRegistryName().toString());
        tag.setInteger("Meta", key.getMeta());

        // Если у предмета есть NBT после твоей зачистки - сохраняем его
        if (key.getCleanedNbt() != null) {
            tag.setTag("CleanedNBT", key.getCleanedNbt());
        }

        return tag;
    }

    public static ResearchKey deserializeKey(NBTTagCompound tag) {

        // Если нет имени, ключ недействителен
        if (!tag.hasKey("RegistryName")) return null;

        // Восстанавливаем ResourceLocation из строки
        ResourceLocation registryName = new ResourceLocation(tag.getString("RegistryName"));
        int meta = tag.getInteger("Meta");

        // Восстанавливаем NBT предмета, если оно было
        NBTTagCompound cleanedNbt = tag.getCompoundTag("CleanedNbt");
        if (tag.hasKey("CleanedNbt", Constants.NBT.TAG_COMPOUND)) {
            cleanedNbt = tag.getCompoundTag("CleanedNBT");
        }

        // Вызываем конструктор
        return new ResearchKey(registryName, meta, cleanedNbt);
    }
}
