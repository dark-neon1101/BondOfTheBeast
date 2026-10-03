package com.bondofthebeast;

import com.bondofthebeast.component.ModComponents;
import com.bondofthebeast.component.PlayerBondComponent;
import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketEnums;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketItem;
import dev.emi.trinkets.api.TrinketsApi;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import net.onixary.shapeShifterCurseFabric.items.accessory.AccessoryItem;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class CollarItem extends AccessoryItem{
    public CollarItem(Settings settings) {
        super(settings);
    }

    // НОВОЕ: Ошейник больше не выпадает при смерти!
    @Override
    public DropRule getDropRule(ItemStack stack, LivingEntity entity, SlotData slot) {
        return DropRule.KEEP;
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (!(entity instanceof PlayerEntity targetPet)) return ActionResult.PASS;

        World world = user.getWorld();
        if (world.isClient) return ActionResult.SUCCESS;

        PlayerBondComponent bond = ModComponents.PLAYER_BOND.get(targetPet);

        if (bond.hasOwner() && bond.getOwnerUUID().equals(user.getUuidAsString())) {
            if (equipToPet(targetPet, stack, user.getName().getString())) {
                if (stack.hasCustomName()) {
                    bond.setPetNickname(stack.getName().getString());
                }
                user.sendMessage(Text.translatable("text.bondofthebeast.collar_put_on", targetPet.getName().getString()).formatted(Formatting.GREEN), true);
                targetPet.sendMessage(Text.translatable("text.bondofthebeast.collar_received", user.getName().getString()).formatted(Formatting.RED), true);

                if (!user.getAbilities().creativeMode) {
                    stack.decrement(1);
                }
                return ActionResult.SUCCESS;
            } else {
                user.sendMessage(Text.translatable("text.bondofthebeast.collar_no_slot").formatted(Formatting.RED), true);
                return ActionResult.CONSUME;
            }
        } else {
            user.sendMessage(Text.translatable("text.bondofthebeast.not_your_pet").formatted(Formatting.RED), true);
            return ActionResult.CONSUME;
        }
    }

    private boolean equipToPet(PlayerEntity pet, ItemStack collar, String ownerName) {
        var optionalComponent = TrinketsApi.getTrinketComponent(pet);
        if (optionalComponent.isPresent()) {
            var component = optionalComponent.get();
            for (var group : component.getInventory().values()) {
                for (Map.Entry<String, TrinketInventory> entry : group.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase("necklace")) {
                        TrinketInventory inventory = entry.getValue();
                        for (int i = 0; i < inventory.size(); i++) {
                            if (inventory.getStack(i).isEmpty()) {
                                ItemStack personalizedCollar = collar.copy();
                                personalizedCollar.setCount(1);
                                personalizedCollar.getOrCreateNbt().putString("OwnerName", ownerName);
                                inventory.setStack(i, personalizedCollar);
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void onEquip(ItemStack stack, LivingEntity entity, SlotData slot) {
        super.onEquip(stack, entity, slot);
        if (!entity.getWorld().isClient() && entity instanceof PlayerEntity player) {
            if (stack.hasCustomName()) {
                PlayerBondComponent bond = ModComponents.PLAYER_BOND.get(player);
                bond.setPetNickname(stack.getName().getString());
            }
        }
    }

    @Override
    public void onUnequip(ItemStack stack, LivingEntity entity, SlotData slot) {
        super.onUnequip(stack, entity, slot);
        if (!entity.getWorld().isClient() && entity instanceof PlayerEntity player) {
            PlayerBondComponent bond = ModComponents.PLAYER_BOND.get(player);
            bond.setPetNickname(null);
        }
    }

    @Override
    public boolean canUnequip(ItemStack stack, LivingEntity entity, SlotData slot) {
        if (entity instanceof PlayerEntity player) {
            PlayerBondComponent bond = ModComponents.PLAYER_BOND.get(player);
            return !bond.hasOwner();
        }
        return true;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains("OwnerName")) {
            tooltip.add(Text.translatable("tooltip.bondofthebeast.collar_owner")
                    .append(Text.literal(nbt.getString("OwnerName")).formatted(Formatting.GOLD))
                    .formatted(Formatting.GRAY));
            tooltip.add(Text.translatable("tooltip.bondofthebeast.collar_sealed").formatted(Formatting.DARK_RED, Formatting.ITALIC));
        }
    }
}