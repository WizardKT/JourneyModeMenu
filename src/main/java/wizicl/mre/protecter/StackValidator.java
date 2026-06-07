package wizicl.mre.protecter;

import net.minecraft.item.ItemBlock;
import wizicl.mre.capabilities.IResearch;
import wizicl.mre.capabilities.ResearchProvider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.function.BiPredicate;

public class StackValidator {

    // Вместо создания объектов, объявляем готовые "правила" валидации.
    // Это ленивые функциональные предикаты, которые не занимают память при вызове
    private static final BiPredicate<EntityPlayer, ItemStack> BASE_CHECK = (player, stack) ->
            stack != null && !stack.isEmpty() && player != null && stack.getItem().getRegistryName() != null;

    private static final BiPredicate<EntityPlayer, ItemStack> IS_BLOCK = (player, stack) ->
            stack.getItem() instanceof ItemBlock;

    private static final BiPredicate<EntityPlayer, ItemStack> IS_DAMAGEABLE = (player, stack) ->
            stack.isItemStackDamageable();

    // НОВОЕ ПРАВИЛО: Проверяет, сдвинулась ли прочность от нуля (поломан ли предмет)
    private static final BiPredicate<EntityPlayer, ItemStack> IS_DAMAGED = (player, stack) ->
            stack.isItemDamaged();

    private static final BiPredicate<EntityPlayer, ItemStack> HAS_CAP = (player, stack) ->
            player.hasCapability(ResearchProvider.RESEARCH, null);

    private static final BiPredicate<EntityPlayer, ItemStack> IS_CREATIVE = (player, stack) ->
            player.capabilities.isCreativeMode;

    private final EntityPlayer player;
    private final ItemStack stack;
    private boolean isValid;

    // Приватный конструктор
    private StackValidator(EntityPlayer player, ItemStack stack) {
        this.player = player;
        this.stack = stack;
        this.isValid = BASE_CHECK.test(player, stack); // Сразу базовая проверка
    }

    // Точка входа
    public static StackValidator check(EntityPlayer player, ItemStack stack) {
        return new StackValidator(player, stack);
    }

    public StackValidator isBlock() {
        if (isValid) isValid = IS_BLOCK.test(player, stack);
        return this;
    }

    // Проверяет, может ли предмет вообще ломаться (инструменты, броня)
    public StackValidator isDamagable() {
        if (isValid) isValid = IS_DAMAGEABLE.test(player, stack);
        return this;
    }

    // Проверяет, что предмет ИМЕННО ПОЛОМАН прямо сейчас.
    // Если у кирки прочность 100%, этот метод вернёт false (валидация не пройдена)
    public StackValidator isDamaged() {
        if (isValid) isValid = IS_DAMAGED.test(player, stack);
        return this;
    }

    // Проверяет, что предмет ЦЕЛЫЙ.
    // Позволит отсечь поломанные инструменты, но пропустит цветную шерсть (у которой мета > 0)
    public StackValidator isNotDamaged() {
        if (isValid) {
            // Если предмет умеет ломаться, и он побит — значит он не валиден
            if (IS_DAMAGEABLE.test(player, stack) && IS_DAMAGED.test(player, stack)) {
                isValid = false;
            }
        }
        return this;
    }

    public StackValidator hasCapability() {
        if (isValid) isValid = HAS_CAP.test(player, stack);
        return this;
    }

    public StackValidator isCreative() {
        if (isValid) isValid = IS_CREATIVE.test(player, stack);
        return this;
    }

    public boolean get() {
        return this.isValid;
    }
}
