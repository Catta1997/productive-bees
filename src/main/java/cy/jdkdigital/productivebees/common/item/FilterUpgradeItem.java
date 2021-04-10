package cy.jdkdigital.productivebees.common.item;

import cy.jdkdigital.productivebees.ProductiveBees;
import cy.jdkdigital.productivebees.common.entity.bee.ProductiveBeeEntity;
import cy.jdkdigital.productivebees.integrations.jei.ingredients.BeeIngredient;
import cy.jdkdigital.productivebees.integrations.jei.ingredients.BeeIngredientFactory;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class FilterUpgradeItem extends UpgradeItem
{
    private static final String KEY = "productivebees_allowed_bees";

    public FilterUpgradeItem(Properties properties) {
        super(properties);
    }

    public static void addAllowedBee(ItemStack stack, BeeEntity bee) {
        // Add bee type to filter
        String type = BeeIngredientFactory.getIngredientKey(bee);
        if (type != null) {
            CompoundNBT tag = stack.getOrCreateTag();

            INBT list = tag.get(KEY);
            if (!(list instanceof ListNBT)) {
                list = new ListNBT();
            }

            for (INBT inbtType: (ListNBT) list) {
                if (inbtType.getString().equals(type)) {
                    return;
                }
            }

            ((ListNBT) list).add(StringNBT.valueOf(type));

            tag.put(KEY, list);

            stack.setTag(tag);
        }
    }

    public static List<Supplier<BeeIngredient>> getAllowedBees(ItemStack stack) {
        CompoundNBT tag = stack.getTag();

        List<Supplier<BeeIngredient>> beeList = new ArrayList<>();
        if (tag != null && tag.contains(KEY)) {
            ListNBT list = (ListNBT) tag.get(KEY);

            if (list != null) {
                for (INBT type: list) {
                    beeList.add(BeeIngredientFactory.getIngredient(type.getString()));
                }
            }
        }
        return beeList;
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, world, tooltip, flagIn);
        List<Supplier<BeeIngredient>> beeList = getAllowedBees(stack);

        for (Supplier<BeeIngredient> allowedBee: beeList) {
            tooltip.add(new TranslationTextComponent("productivebees.information.upgrade.filter_entity", allowedBee.get().getBeeType()).mergeStyle(TextFormatting.GOLD));
        }
        
        if (!beeList.isEmpty()) {
            tooltip.add(new TranslationTextComponent("productivebees.information.upgrade.filter").mergeStyle(TextFormatting.WHITE));
        } else {
            tooltip.add(new TranslationTextComponent("productivebees.information.upgrade.filter_empty").mergeStyle(TextFormatting.WHITE));
        }
    }

    @Nonnull
    @Override
    public ActionResultType itemInteractionForEntity(ItemStack itemStack, PlayerEntity player, LivingEntity targetIn, Hand hand) {
        if (targetIn.getEntityWorld().isRemote() || !(targetIn instanceof BeeEntity)) {
            return ActionResultType.PASS;
        }

        addAllowedBee(itemStack, (BeeEntity) targetIn);

        if (player.isCreative()) {
            // Replace held item
            ItemStack newStack = new ItemStack(itemStack.getItem());
            newStack.setTag(itemStack.getTag());
            player.inventory.setPickedItemStack(newStack);
        }

        return ActionResultType.SUCCESS;
    }
}
