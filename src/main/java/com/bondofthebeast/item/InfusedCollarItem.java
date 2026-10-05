package com.bondofthebeast.item;

import com.bondofthebeast.component.ModComponents;
import com.bondofthebeast.component.PlayerBondComponent;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketItem;
import dev.emi.trinkets.api.TrinketsApi;
import dev.emi.trinkets.api.TrinketComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import com.bondofthebeast.BondOfTheBeast;
import net.onixary.shapeShifterCurseFabric.items.accessory.AccessoryItem;

import java.util.Optional;

public class InfusedCollarItem extends AccessoryItem {



    public InfusedCollarItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (!user.getWorld().isClient() && entity instanceof ServerPlayerEntity target) {

            // 1. Проверяем, парализована ли жертва нашей пылью (Замедление 99 уровня)
            boolean isStunned = target.hasStatusEffect(StatusEffects.SLOWNESS) &&
                    target.getStatusEffect(StatusEffects.SLOWNESS).getAmplifier() == 99;

            if (!isStunned) {
                user.sendMessage(Text.translatable("text.bondofthebeast.target_too_strong").formatted(Formatting.RED), true);
                return ActionResult.FAIL;
            }

            PlayerBondComponent bond = ModComponents.PLAYER_BOND.get(target);

            // 2. Проверяем, свободна ли жертва
            if (bond.getTamingState() != 0) {
                user.sendMessage(Text.translatable("text.bondofthebeast.already_tamed").formatted(Formatting.RED), true);
                return ActionResult.FAIL;
            }

            // 3. Надеваем ошейник через Trinkets API
            Optional<TrinketComponent> trinketComponent = TrinketsApi.getTrinketComponent(target);
            if (trinketComponent.isPresent()) {
                TrinketComponent trinkets = trinketComponent.get();

                try {
                    // Ищем инвентарь ожерелья. Если на жертве уже что-то надето — срываем это и кидаем на пол
                    var necklaceInventory = trinkets.getInventory().get("chest").get("necklace");
                    if (necklaceInventory != null) {
                        ItemStack oldStack = necklaceInventory.getStack(0);
                        if (!oldStack.isEmpty()) {
                            target.dropStack(oldStack);
                        }

                        // Надеваем 1 проклятый ошейник
                        necklaceInventory.setStack(0, stack.copy().split(1));

                        // 4. Ломаем волю и привязываем к хозяину
                        bond.setTamingState(1);
                        bond.setOwnerForced(user.getUuidAsString(), user.getName().getString());

                        // 5. Читаем FormID зелья из классического NBT (база 1.20.1)
                        NbtCompound nbt = stack.getNbt();

                        if (nbt != null && nbt.contains("CursedFormID")) {
                            String formId = nbt.getString("CursedFormID");

                            // Заражаем проклятием Shape Shifter Curse
                            try {
                                var sscComp = net.onixary.shapeShifterCurseFabric.player_form.ability.RegPlayerFormComponent.PLAYER_FORM.get(target);
                                if (sscComp != null) {
                                    if (sscComp.getCurrentForm().FormID.getPath().equalsIgnoreCase("original_shifter")) {
                                        var form = net.onixary.shapeShifterCurseFabric.player_form.RegPlayerForms.getPlayerForm(formId);
                                        net.onixary.shapeShifterCurseFabric.player_form.transform.TransformManager.handleDirectTransform(target, form, false);
                                    }
                                }
                            } catch (Exception ignored) {}
                        }

                        if (!user.isCreative()) {
                            stack.decrement(1);
                        }

                        // 6. Звуки ужаса и заковывания в цепи
                        target.getWorld().playSound(null, target.getBlockPos(), SoundEvents.ITEM_ARMOR_EQUIP_CHAIN, SoundCategory.PLAYERS, 1.0f, 0.5f);
                        target.getWorld().playSound(null, target.getBlockPos(), SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.PLAYERS, 0.5f, 0.8f);

                        user.sendMessage(Text.translatable("text.bondofthebeast.taming_started").formatted(Formatting.GREEN), true);
                        target.sendMessage(Text.translatable("text.bondofthebeast.will_breaking").formatted(Formatting.DARK_RED), true);

                        return ActionResult.SUCCESS;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return ActionResult.PASS;
    }

    // 7. Блокировка снятия
    @Override
    public boolean canUnequip(ItemStack stack, LivingEntity entity, SlotData slot) {
        if (entity instanceof PlayerEntity player) {
            PlayerBondComponent bond = ModComponents.PLAYER_BOND.get(player);
            // Если воля ломается, жертва не может снять ошейник из инвентаря Trinkets
            if (bond.getTamingState() == 1) {
                return false;
            }
        }
        return super.canUnequip(stack, entity, slot);
    }
}