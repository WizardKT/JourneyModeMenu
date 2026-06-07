package wizicl.mre.capabilities;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.ItemStackHandler;

import java.util.HashMap;
import java.util.Map;

public class ResearchSerializer {
    public static NBTTagCompound serialize(Research instance) {
        NBTTagCompound nbt = new NBTTagCompound();

        // Сохраняем тумблер в начале NBT
        nbt.setBoolean("AutoResearchState", instance.getAutoResearchState());

        // Упаковываем инвентарь через Pattern Matching (избавились от ручного каста)
        if (instance.getResearchInventory() instanceof ItemStackHandler handler) {
            nbt.setTag("ResearchSlot", handler.serializeNBT());
        }

        // Упаковываем мапу исследований
        var list = new NBTTagList();
        for (var entry : instance.getReadOnlyMap().entrySet()) {
            var entryTag = new NBTTagCompound();

            // Вызываем упаковщик ключа
            entryTag.setTag("KeyData", serializeKey(entry.getKey()));
            entryTag.setInteger("ProgressValue", entry.getValue());

            list.appendTag(entryTag);
        }
        nbt.setTag("ProgressMap", list);

        return nbt;
    }

    public static void deserialize(Research instance, NBTTagCompound nbt) {
        // Проверяем наличие ключа
        if (nbt.hasKey("AutoResearchState")) {
            instance.setAutoResearchState(nbt.getBoolean("AutoResearchState"));
        }

        // Распаковываем инвентарь через Pattern Matching
        if (nbt.hasKey("ResearchSlot") && instance.getResearchInventory() instanceof ItemStackHandler handler) {
            handler.deserializeNBT(nbt.getCompoundTag("ResearchSlot"));
        }

        // Распаковываем мапу исследований
        if (nbt.hasKey("ProgressMap", Constants.NBT.TAG_LIST)) {
            var list = nbt.getTagList("ProgressMap", Constants.NBT.TAG_COMPOUND);
            Map<ResearchKey, Integer> loadedMap = new HashMap<>();

            for (int i = 0; i < list.tagCount(); i++) {
                var entryTag = list.getCompoundTagAt(i);
                var key = deserializeKey(entryTag.getCompoundTag("KeyData"));

                // Вызываем распаковщик ключа
                if (key != null) {
                    loadedMap.put(key, entryTag.getInteger("ProgressValue"));
                }
            }

            instance.refreshFromServer(loadedMap);
        }
    }

    public static NBTTagCompound serializeKey(ResearchKey key) {
        var tag = new NBTTagCompound();

        // ResourceLocation легко переводится в строку
        tag.setString("RegistryName", key.getRegistryName().toString());
        tag.setInteger("Meta", key.getMeta());

        // Если у предмета есть NBT после твоей зачистки - сохраняем его
        if (key.getCleanedNbt() != null && !key.getCleanedNbt().isEmpty()) {
            tag.setTag("CleanedNBT", key.getCleanedNbt());
        }

        return tag;
    }

    public static ResearchKey deserializeKey(NBTTagCompound tag) {
        if (!tag.hasKey("RegistryName")) return null;

        // Восстанавливаем ResourceLocation из строки
        var registryName = new ResourceLocation(tag.getString("RegistryName"));
        int meta = tag.getInteger("Meta");

        // Элегантное чтение NBT предмета в одну строчку
        var cleanedNbt = tag.hasKey("CleanedNBT", Constants.NBT.TAG_COMPOUND) ? tag.getCompoundTag("CleanedNBT") : null;

        // Наш новенький рекорд ResearchKey из прошлого шага!
        return new ResearchKey(registryName, meta, cleanedNbt);
    }
}
