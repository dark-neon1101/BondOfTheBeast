package com.bondofthebeast;

import com.bondofthebeast.component.ModComponents;
import com.bondofthebeast.component.PlayerBondComponent;
import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import net.onixary.shapeShifterCurseFabric.player_form.ability.PlayerFormComponent;
import net.onixary.shapeShifterCurseFabric.player_form.ability.RegPlayerFormComponent;
import net.onixary.shapeShifterCurseFabric.player_form.PlayerFormBodyType;
import java.util.List;
import java.util.UUID;

public class ContractItem extends Item {
    public ContractItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (!(entity instanceof ServerPlayerEntity targetPlayer)) return ActionResult.PASS;
        if (user.getWorld().isClient) return ActionResult.SUCCESS;

        NbtCompound nbt = stack.getOrCreateNbt();
        ServerPlayerEntity serverUser = (ServerPlayerEntity) user;

        if (nbt.contains("OwnerUUID") && nbt.contains("PetUUID")) return ActionResult.PASS;

        if (nbt.contains("OwnerUUID") && nbt.getString("OwnerUUID").equals(user.getUuidAsString())) {
            user.sendMessage(Text.translatable("text.bondofthebeast.give_to_pet").formatted(Formatting.YELLOW), true);
            return ActionResult.FAIL;
        }

        if (nbt.contains("PetUUID") && nbt.getString("PetUUID").equals(user.getUuidAsString())) {
            user.sendMessage(Text.translatable("text.bondofthebeast.give_to_owner").formatted(Formatting.YELLOW), true);
            return ActionResult.FAIL;
        }

        int userIndex = getFormIndex(user);
        boolean ClearMindUser = TrinketsApi.getTrinketComponent(user).map(c -> c.isEquipped(st -> st.getItem() instanceof NecklaceOfClarity)).orElse(false);
        boolean isUserPet = isPlayerFeral(user) || (userIndex >= 2 && !ClearMindUser);
        boolean isUserOwner = !isUserPet;

        int targetIndex = getFormIndex(targetPlayer);
        boolean ClearMindTarget = TrinketsApi.getTrinketComponent(targetPlayer).map(c -> c.isEquipped(st -> st.getItem() instanceof NecklaceOfClarity)).orElse(false);
        boolean isTargetPet = isPlayerFeral(targetPlayer) || (targetIndex == 2 && !ClearMindTarget) || targetIndex > 2;
        boolean isTargetOwner = !isTargetPet;

        PlayerBondComponent userBond = ModComponents.PLAYER_BOND.get(user);
        PlayerBondComponent targetBond = ModComponents.PLAYER_BOND.get(targetPlayer);

        if (isUserPet && userBond.hasOwner()) {
            user.sendMessage(Text.translatable("text.bondofthebeast.already_owned_self").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }

        if (isTargetPet && targetBond.hasOwner()) {
            user.sendMessage(Text.translatable("text.bondofthebeast.already_owned_target").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }

        if (!nbt.contains("PetUUID") && !nbt.contains("OwnerUUID")) {
            if (isUserPet && isTargetOwner) {
                nbt.putString("PetUUID", user.getUuidAsString());
                nbt.putString("PetName", user.getName().getString());
                stack.setNbt(nbt);
                user.setStackInHand(hand, stack);
                if (user instanceof ServerPlayerEntity sp) sp.currentScreenHandler.sendContentUpdates();

                user.sendMessage(Text.translatable("text.bondofthebeast.pet_initiated").formatted(Formatting.RED), true);
                targetPlayer.sendMessage(Text.translatable("text.bondofthebeast.target_is_future_owner", user.getName().getString()).formatted(Formatting.GOLD), false);
                return ActionResult.SUCCESS;
            } else if (isUserOwner && isTargetPet) {
                nbt.putString("OwnerUUID", user.getUuidAsString());
                nbt.putString("OwnerName", user.getName().getString());
                stack.setNbt(nbt);
                user.setStackInHand(hand, stack);
                if (user instanceof ServerPlayerEntity sp) sp.currentScreenHandler.sendContentUpdates();

                user.sendMessage(Text.translatable("text.bondofthebeast.owner_initiated").formatted(Formatting.GOLD), true);
                targetPlayer.sendMessage(Text.translatable("text.bondofthebeast.target_is_future_pet", user.getName().getString()).formatted(Formatting.RED), false);
                return ActionResult.SUCCESS;
            } else {
                user.sendMessage(Text.translatable("text.bondofthebeast.invalid_interaction").formatted(Formatting.GRAY), true);
                return ActionResult.FAIL;
            }
        }
        else if (nbt.contains("PetUUID") && !nbt.contains("OwnerUUID")) {
            if (isUserOwner) {
                if (nbt.getString("PetUUID").equals(targetPlayer.getUuidAsString())) {
                    ServerPlayNetworking.send(serverUser, ModPackets.OPEN_OWNER_GUI, PacketByteBufs.create());
                    return ActionResult.SUCCESS;
                } else {
                    user.sendMessage(Text.translatable("text.bondofthebeast.wrong_pet").formatted(Formatting.GRAY), true);
                    return ActionResult.FAIL;
                }
            } else {
                user.sendMessage(Text.translatable("text.bondofthebeast.too_wild_owner").formatted(Formatting.RED), true);
                return ActionResult.FAIL;
            }
        }
        else if (nbt.contains("OwnerUUID") && !nbt.contains("PetUUID")) {
            if (isUserPet) {
                if (nbt.getString("OwnerUUID").equals(targetPlayer.getUuidAsString())) {
                    ServerPlayNetworking.send(serverUser, ModPackets.OPEN_PET_GUI, PacketByteBufs.create());
                    return ActionResult.SUCCESS;
                } else {
                    user.sendMessage(Text.translatable("text.bondofthebeast.wrong_owner").formatted(Formatting.GRAY), true);
                    return ActionResult.FAIL;
                }
            } else {
                user.sendMessage(Text.translatable("text.bondofthebeast.too_human_pet").formatted(Formatting.AQUA), true);
                return ActionResult.FAIL;
            }
        }

        return ActionResult.SUCCESS;
    }

    public void finalizeContract(ItemStack stack, PlayerEntity player) {
        NbtCompound nbt = stack.getOrCreateNbt();
        if (player.getWorld().isClient) return;

        if (nbt.contains("PetUUID") && !nbt.contains("OwnerUUID")) {
            nbt.putString("OwnerUUID", player.getUuidAsString());
            nbt.putString("OwnerName", player.getName().getString());
            nbt.putInt("CustomModelData", 1);
            player.sendMessage(Text.translatable("text.bondofthebeast.owner_accepted", nbt.getString("PetName")).formatted(Formatting.GOLD), false);

            if (player.getServer() != null) {
                ServerPlayerEntity pet = player.getServer().getPlayerManager().getPlayer(UUID.fromString(nbt.getString("PetUUID")));
                if (pet != null) {
                    dropOldCollars(pet);
                    pet.sendMessage(Text.translatable("text.bondofthebeast.pet_bound", player.getName().getString()).formatted(Formatting.GREEN), false);
                    ModComponents.PLAYER_BOND.get(pet).setOwner(player.getUuidAsString(), player.getName().getString());
                    ModComponents.PLAYER_BOND.get(player).addPetToRegistry(pet.getUuidAsString(), pet.getName().getString());

                    BondOfTheBeast.grantAdvancement((ServerPlayerEntity) player, "owner_story/tame_pet");
                    BondOfTheBeast.grantAdvancement(pet, "pet_story/root");
                }
            }
        }
        else if (nbt.contains("OwnerUUID") && !nbt.contains("PetUUID")) {
            nbt.putString("PetUUID", player.getUuidAsString());
            nbt.putString("PetName", player.getName().getString());
            nbt.putInt("CustomModelData", 1);
            player.sendMessage(Text.translatable("text.bondofthebeast.pet_accepted_owner", nbt.getString("OwnerName")).formatted(Formatting.RED), false);

            dropOldCollars(player);
            ModComponents.PLAYER_BOND.get(player).setOwner(nbt.getString("OwnerUUID"), nbt.getString("OwnerName"));

            if (player.getServer() != null) {
                ServerPlayerEntity owner = player.getServer().getPlayerManager().getPlayer(UUID.fromString(nbt.getString("OwnerUUID")));
                if (owner != null) {
                    owner.sendMessage(Text.translatable("text.bondofthebeast.owner_accepted", player.getName().getString()).formatted(Formatting.GOLD), false);
                    ModComponents.PLAYER_BOND.get(owner).addPetToRegistry(player.getUuidAsString(), player.getName().getString());

                    BondOfTheBeast.grantAdvancement(owner, "owner_story/tame_pet");
                    BondOfTheBeast.grantAdvancement((ServerPlayerEntity) player, "pet_story/root");
                }
            }
        }

        player.getInventory().markDirty();
    }

    private void dropOldCollars(PlayerEntity pet) {
        TrinketsApi.getTrinketComponent(pet).ifPresent(component -> {
            component.getInventory().values().forEach(group -> {
                group.values().forEach(inventory -> {
                    for (int i = 0; i < inventory.size(); i++) {
                        ItemStack item = inventory.getStack(i);
                        if (item.getItem() instanceof CollarItem) {
                            pet.dropItem(item.copy(), true);
                            inventory.setStack(i, ItemStack.EMPTY);
                        }
                    }
                });
            });
        });
    }

    private int getFormIndex(PlayerEntity player) {
        PlayerFormComponent component = (PlayerFormComponent) RegPlayerFormComponent.PLAYER_FORM.get(player);
        if (component != null && component.getCurrentForm() != null) {
            if (component.getCurrentForm().FormID != null && component.getCurrentForm().FormID.getPath().toLowerCase().contains("allay")) return 0;
            return component.getCurrentForm().getIndex();
        }
        return -2;
    }

    private boolean isPlayerFeral(PlayerEntity player) {
        PlayerFormComponent component = (PlayerFormComponent) RegPlayerFormComponent.PLAYER_FORM.get(player);
        if (component != null && component.getCurrentForm() != null) {
            if (component.getCurrentForm().FormID != null && component.getCurrentForm().FormID.getPath().toLowerCase().contains("allay")) return false;
            return component.getCurrentForm().getBodyType() == PlayerFormBodyType.FERAL;
        }
        return false;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null) {
            tooltip.add(Text.translatable("tooltip.bondofthebeast.empty_contract").formatted(Formatting.DARK_AQUA));
            return;
        }

        if (nbt.contains("OwnerName") && nbt.contains("PetName")) {
            tooltip.add(Text.translatable("tooltip.bondofthebeast.owner").formatted(Formatting.GRAY).append(Text.literal(nbt.getString("OwnerName")).formatted(Formatting.GOLD)));
            tooltip.add(Text.translatable("tooltip.bondofthebeast.pet").formatted(Formatting.GRAY).append(Text.literal(nbt.getString("PetName")).formatted(Formatting.RED)));
            tooltip.add(Text.translatable("tooltip.bondofthebeast.bound").formatted(Formatting.DARK_PURPLE, Formatting.ITALIC));
        } else if (nbt.contains("OwnerName")) {
            tooltip.add(Text.translatable("tooltip.bondofthebeast.owner").formatted(Formatting.GRAY).append(Text.literal(nbt.getString("OwnerName")).formatted(Formatting.GOLD)));
            tooltip.add(Text.translatable("tooltip.bondofthebeast.waiting_pet").formatted(Formatting.ITALIC, Formatting.DARK_GRAY));
        } else if (nbt.contains("PetName")) {
            tooltip.add(Text.translatable("tooltip.bondofthebeast.pet").formatted(Formatting.GRAY).append(Text.literal(nbt.getString("PetName")).formatted(Formatting.RED)));
            tooltip.add(Text.translatable("tooltip.bondofthebeast.waiting_owner").formatted(Formatting.ITALIC, Formatting.DARK_GRAY));
        }
    }
}