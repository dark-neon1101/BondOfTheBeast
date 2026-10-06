package com.bondofthebeast;

import com.bondofthebeast.block.PetBedBlock;
import com.bondofthebeast.block.PetBedBlockEntity;
import com.bondofthebeast.component.ModComponents;
import com.bondofthebeast.component.PlayerBondComponent;
import dev.emi.trinkets.api.TrinketsApi;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.minecraft.block.BedBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameMode;

import java.util.ArrayList;
import java.util.UUID;

public class ModEvents {
    private static int tickCounter = 0;
    private static int expTickCounter = 0;
    private static int chainTickCounter = 0;
    private static int tamingTickCounter = 0; // Новый счетчик для системы подавления воли

    public static boolean canPetObey(PlayerEntity pet) {
        try {
            var sscComp = net.onixary.shapeShifterCurseFabric.player_form.ability.RegPlayerFormComponent.PLAYER_FORM.get(pet);
            if (sscComp != null && sscComp.getCurrentForm() != null) {
                if (sscComp.getCurrentForm().FormID != null && sscComp.getCurrentForm().FormID.getPath().toLowerCase().contains("allay")) return false;
                int index = sscComp.getCurrentForm().getIndex();
                boolean isFeral = sscComp.getCurrentForm().getBodyType() == net.onixary.shapeShifterCurseFabric.player_form.PlayerFormBodyType.FERAL;
                return index >= 3 || isFeral;
            }
        } catch (Exception ignored) {}
        return false;
    }

    public static void registerEvents() {
        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (!(entity instanceof PlayerEntity pet)) return ActionResult.PASS;
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;

            var bond = ModComponents.PLAYER_BOND.get(pet);

            if (bond.hasOwner() && bond.getOwnerUUID().equals(player.getUuidAsString())) {
                ItemStack foodStack = player.getStackInHand(hand);

                if (player.isSneaking() && foodStack.isEmpty()) {
                    if (world.isClient) return ActionResult.SUCCESS;
                    return TrinketsApi.getTrinketComponent(pet).map(c -> {
                        boolean rem = false;
                        for (var g : c.getInventory().values()) for (var inv : g.values()) for (int i = 0; i < inv.size(); i++) {
                            if (inv.getStack(i).getItem() instanceof CollarItem) {
                                ItemStack droppedCollar = inv.getStack(i).copy();
                                if (droppedCollar.hasNbt()) droppedCollar.getNbt().remove("OwnerName");
                                player.getInventory().offerOrDrop(droppedCollar);
                                inv.setStack(i, ItemStack.EMPTY);
                                rem = true;
                            }
                        }
                        if (rem) {
                            if (bond.isAbsorbed() && pet instanceof ServerPlayerEntity sp) {
                                sp.changeGameMode(GameMode.SURVIVAL);
                                bond.setAbsorbed(false);
                            }
                            bond.setBedPos(null);
                            bond.setPetNickname(null);
                            player.sendMessage(Text.translatable("text.bondofthebeast.collar_removed_owner", pet.getName().getString()).formatted(Formatting.AQUA), true);
                            pet.sendMessage(Text.translatable("text.bondofthebeast.collar_removed_pet", player.getName().getString()).formatted(Formatting.AQUA), true);
                            return ActionResult.SUCCESS;
                        }
                        return ActionResult.PASS;
                    }).orElse(ActionResult.PASS);
                }

                if (!foodStack.isEmpty() && foodStack.isOf(ModItems.PET_TREAT)) {
                    if (world.isClient) return ActionResult.SUCCESS;
                    if (!player.getAbilities().creativeMode) foodStack.decrement(1);
                    pet.getHungerManager().add(6, 0.6f);
                    bond.addBondExperience(50);
                    ModComponents.PLAYER_BOND.sync(pet);
                    ModComponents.PLAYER_BOND.sync(player);
                    world.playSound(null, pet.getX(), pet.getY(), pet.getZ(), SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.PLAYERS, 1.0f, 1.1f);
                    if (pet instanceof ServerPlayerEntity sp) sp.getServerWorld().spawnParticles(ParticleTypes.HEART, pet.getX(), pet.getY() + 1.2, pet.getZ(), 12, 0.4, 0.4, 0.4, 0.15);
                    String petName = bond.getPetNickname() != null ? bond.getPetNickname() : pet.getGameProfile().getName();
                    player.sendMessage(Text.translatable("text.bondofthebeast.event.feed_treat_owner", petName).formatted(Formatting.AQUA), true);
                    pet.sendMessage(Text.translatable("text.bondofthebeast.event.feed_treat_pet", player.getGameProfile().getName()).formatted(Formatting.AQUA), true);
                    if (player instanceof ServerPlayerEntity sp) BondOfTheBeast.grantAdvancement(sp, "owner_story/treat");
                    if (pet instanceof ServerPlayerEntity sp) BondOfTheBeast.grantAdvancement(sp, "pet_story/treat");
                    return ActionResult.SUCCESS;
                }
                if (!foodStack.isEmpty() && foodStack.isOf(ModItems.CATALYST_TREAT)) {
                    if (world.isClient) return ActionResult.SUCCESS;
                    if (!player.getAbilities().creativeMode) foodStack.decrement(1);
                    pet.getHungerManager().add(6, 0.6f);
                    var sscComp = net.onixary.shapeShifterCurseFabric.player_form.ability.RegPlayerFormComponent.PLAYER_FORM.get(pet);
                    if (sscComp.getCurrentForm().FormIndex < 2){
                        net.onixary.shapeShifterCurseFabric.player_form.instinct.InstinctManager.applySustainedEffect(pet,"bondofthebeast:catalyst_treat",1,40);
                    }
                    bond.addBondExperience(50);
                    ModComponents.PLAYER_BOND.sync(pet);
                    ModComponents.PLAYER_BOND.sync(player);
                    world.playSound(null, pet.getX(), pet.getY(), pet.getZ(), SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.PLAYERS, 1.0f, 1.1f);
                    if (pet instanceof ServerPlayerEntity sp) sp.getServerWorld().spawnParticles(ParticleTypes.HEART, pet.getX(), pet.getY() + 1.2, pet.getZ(), 12, 0.4, 0.4, 0.4, 0.15);
                    String petName = bond.getPetNickname() != null ? bond.getPetNickname() : pet.getGameProfile().getName();
                    player.sendMessage(Text.translatable("text.bondofthebeast.event.feed_treat_owner", petName).formatted(Formatting.AQUA), true);
                    pet.sendMessage(Text.translatable("text.bondofthebeast.event.feed_treat_pet", player.getGameProfile().getName()).formatted(Formatting.AQUA), true);
                    if (player instanceof ServerPlayerEntity sp) BondOfTheBeast.grantAdvancement(sp, "owner_story/treat");
                    if (pet instanceof ServerPlayerEntity sp) BondOfTheBeast.grantAdvancement(sp, "pet_story/treat");
                    return ActionResult.SUCCESS;
                }
                if (!foodStack.isEmpty() && foodStack.isOf(ModItems.ECHO_CATALYST_TREAT)) {
                    if (world.isClient) return ActionResult.SUCCESS;
                    if (!player.getAbilities().creativeMode) foodStack.decrement(1);
                    pet.getHungerManager().add(6, 0.6f);
                    var sscComp = net.onixary.shapeShifterCurseFabric.player_form.ability.RegPlayerFormComponent.PLAYER_FORM.get(pet);
                    if (sscComp.getCurrentForm().FormIndex == 2){
                        var form = sscComp.getCurrentForm().getIDString();
                        var newForm = form.replace("2","3");
                        var transformTo = net.onixary.shapeShifterCurseFabric.player_form.RegPlayerForms.getPlayerForm(newForm);
                        net.onixary.shapeShifterCurseFabric.player_form.transform.TransformManager.handleDirectTransform(pet, transformTo, false);
                    }
                    bond.addBondExperience(50);
                    ModComponents.PLAYER_BOND.sync(pet);
                    ModComponents.PLAYER_BOND.sync(player);
                    world.playSound(null, pet.getX(), pet.getY(), pet.getZ(), SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.PLAYERS, 1.0f, 1.1f);
                    if (pet instanceof ServerPlayerEntity sp) sp.getServerWorld().spawnParticles(ParticleTypes.HEART, pet.getX(), pet.getY() + 1.2, pet.getZ(), 12, 0.4, 0.4, 0.4, 0.15);
                    String petName = bond.getPetNickname() != null ? bond.getPetNickname() : pet.getGameProfile().getName();
                    player.sendMessage(Text.translatable("text.bondofthebeast.event.feed_treat_owner", petName).formatted(Formatting.AQUA), true);
                    pet.sendMessage(Text.translatable("text.bondofthebeast.event.feed_treat_pet", player.getGameProfile().getName()).formatted(Formatting.AQUA), true);
                    if (player instanceof ServerPlayerEntity sp) BondOfTheBeast.grantAdvancement(sp, "owner_story/treat");
                    if (pet instanceof ServerPlayerEntity sp) BondOfTheBeast.grantAdvancement(sp, "pet_story/treat");
                    return ActionResult.SUCCESS;
                }
                if (!foodStack.isEmpty() && foodStack.getItem().isFood()) {
                    if (!pet.getHungerManager().isNotFull()) {
                        if (!world.isClient) player.sendMessage(Text.translatable("text.bondofthebeast.event.pet_not_hungry").formatted(Formatting.YELLOW), true);
                        return ActionResult.SUCCESS;
                    }
                    if (world.isClient) return ActionResult.SUCCESS;
                    var foodComponent = foodStack.getItem().getFoodComponent();
                    if (foodComponent != null) {
                        int hungerValue = foodComponent.getHunger();
                        int xpGained = Math.max(5, hungerValue * 4);
                        if (!player.getAbilities().creativeMode) foodStack.decrement(1);
                        pet.getHungerManager().add(hungerValue, foodComponent.getSaturationModifier());
                        bond.addBondExperience(xpGained);
                        ModComponents.PLAYER_BOND.sync(pet);
                        ModComponents.PLAYER_BOND.sync(player);
                        world.playSound(null, pet.getX(), pet.getY(), pet.getZ(), SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.PLAYERS, 1.0f, 1.0f);
                        world.playSound(null, pet.getX(), pet.getY(), pet.getZ(), SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 0.5f, 1.0f);
                        if (pet instanceof ServerPlayerEntity sp) sp.getServerWorld().spawnParticles(ParticleTypes.HEART, pet.getX(), pet.getY() + 1.2, pet.getZ(), 6, 0.3, 0.3, 0.3, 0.1);
                        String petName = bond.getPetNickname() != null ? bond.getPetNickname() : pet.getGameProfile().getName();
                        player.sendMessage(Text.translatable("text.bondofthebeast.event.feed_food_owner", petName, xpGained).formatted(Formatting.GREEN), true);
                        pet.sendMessage(Text.translatable("text.bondofthebeast.event.feed_food_pet", player.getGameProfile().getName(), xpGained).formatted(Formatting.GOLD), true);
                        if (player instanceof ServerPlayerEntity sp) BondOfTheBeast.grantAdvancement(sp, "owner_story/treat");
                        if (pet instanceof ServerPlayerEntity sp) BondOfTheBeast.grantAdvancement(sp, "pet_story/treat");
                        return ActionResult.SUCCESS;
                    }
                }
            }
            return ActionResult.PASS;
        });

        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient || !(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
            var bond = ModComponents.PLAYER_BOND.get(sp);
            if (entity instanceof PlayerEntity targetPlayer) {
                if (bond.hasOwner() && bond.getOwnerUUID().equals(targetPlayer.getUuidAsString())) {
                    sp.sendMessage(Text.translatable("text.bondofthebeast.damage_blocked_pet").formatted(Formatting.RED), true);
                    return ActionResult.FAIL;
                }
                var targetBond = ModComponents.PLAYER_BOND.get(targetPlayer);
                if (targetBond.hasOwner() && targetBond.getOwnerUUID().equals(sp.getUuidAsString())) {
                    sp.sendMessage(Text.translatable("text.bondofthebeast.damage_blocked_owner").formatted(Formatting.RED), true);
                    return ActionResult.FAIL;
                }
            }
            if (bond.hasOwner() && bond.isPacifistMode() && canPetObey(sp)) {
                sp.sendMessage(Text.translatable("text.bondofthebeast.command.pacifist_warning").formatted(Formatting.RED), true);
                return ActionResult.FAIL;
            }
            if (!(entity instanceof LivingEntity targetLiving)) return ActionResult.PASS;
            for (ServerPlayerEntity p : sp.getServer().getPlayerManager().getPlayerList()) {
                PlayerBondComponent pBond = ModComponents.PLAYER_BOND.get(p);
                if (pBond.hasOwner() && pBond.getOwnerUUID().equals(sp.getUuidAsString()) && pBond.isProtectionMode() && pBond.getBondLevel() >= 3 && canPetObey(p)) {
                    targetLiving.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 100, 0, false, false));
                }
            }
            return ActionResult.PASS;
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
            var bond = ModComponents.PLAYER_BOND.get(player);
            if (!bond.hasOwner() || !canPetObey(player)) return ActionResult.PASS;
            net.minecraft.block.BlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof PetBedBlock) {
                BlockPos headPos = state.get(PetBedBlock.PART) == net.minecraft.block.enums.BedPart.HEAD ? pos : pos.offset(state.get(PetBedBlock.FACING));
                if (world.getBlockEntity(headPos) instanceof PetBedBlockEntity bed) {
                    if (player.getUuidAsString().equals(bed.getBoundPetUUID())) {
                        if (!world.isClient) player.sendMessage(Text.translatable("text.bondofthebeast.cannot_break_own_bed").formatted(Formatting.RED), true);
                        return ActionResult.FAIL;
                    }
                }
            }
            String blockId = Registries.BLOCK.getId(state.getBlock()).toString();
            if (bond.getBlacklistedBlocks().contains(blockId)) {
                if (!world.isClient) player.sendMessage(Text.translatable("text.bondofthebeast.blacklisted_warning").formatted(Formatting.DARK_RED), true);
                return ActionResult.FAIL;
            }
            if (bond.isNoBreakMode() && !bond.getWhitelistedBlocks().contains(blockId)) {
                if (!world.isClient) player.sendMessage(Text.translatable("text.bondofthebeast.soft_paws_warning").formatted(Formatting.RED), true);
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) -> {
            var bond = ModComponents.PLAYER_BOND.get(player);
            if (!bond.hasOwner() || !canPetObey(player)) return true;
            if (state.getBlock() instanceof PetBedBlock) {
                BlockPos headPos = state.get(PetBedBlock.PART) == net.minecraft.block.enums.BedPart.HEAD ? pos : pos.offset(state.get(PetBedBlock.FACING));
                if (world.getBlockEntity(headPos) instanceof PetBedBlockEntity bed) {
                    if (player.getUuidAsString().equals(bed.getBoundPetUUID())) {
                        if (!world.isClient) player.sendMessage(Text.translatable("text.bondofthebeast.cannot_break_own_bed").formatted(Formatting.RED), true);
                        return false;
                    }
                }
            }
            String blockId = Registries.BLOCK.getId(state.getBlock()).toString();
            if (bond.getBlacklistedBlocks().contains(blockId)) return false;
            if (bond.isNoBreakMode() && !bond.getWhitelistedBlocks().contains(blockId)) return false;
            return true;
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            var bond = ModComponents.PLAYER_BOND.get(player);
            if (bond.hasOwner() && world.getBlockState(hitResult.getBlockPos()).getBlock() instanceof BedBlock) {
                if (!world.isClient) player.sendMessage(Text.translatable("text.bondofthebeast.cannot_sleep_here_human").formatted(Formatting.YELLOW), true);
                return ActionResult.FAIL;
            }
            if (!bond.hasOwner() || !canPetObey(player)) return ActionResult.PASS;
            String blockId = Registries.BLOCK.getId(world.getBlockState(hitResult.getBlockPos()).getBlock()).toString();
            if (bond.getBlacklistedBlocks().contains(blockId)) {
                if (!world.isClient) player.sendMessage(Text.translatable("text.bondofthebeast.blacklisted_interact_warning").formatted(Formatting.DARK_RED), true);
                return ActionResult.FAIL;
            }
            if (bond.isNoInteractMode()) {
                if (!world.isClient) player.sendMessage(Text.translatable("text.bondofthebeast.event.interact_blocked").formatted(Formatting.RED), true);
                return ActionResult.FAIL;
            }
            return ActionResult.PASS;
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {

            // --- 1. НОВАЯ ЛОГИКА: СИСТЕМА ПОДАВЛЕНИЯ ВОЛИ (Срабатывает 2 раза в секунду) ---

            if (++tamingTickCounter % 10 == 0) {
                for (ServerPlayerEntity victim : server.getPlayerManager().getPlayerList()) {
                    var bond = ModComponents.PLAYER_BOND.get(victim);

                    if (bond.getTamingState() == 1) { // 1 = Воля ломается
                        ServerPlayerEntity master = server.getPlayerManager().getPlayer(UUID.fromString(bond.getOwnerUUID()));
                        // А) Пассивная накачка инстинктов (каждые 10 секунд)
                        // Лучше суда поставить само завершение подчинения воли так как накачку можно сделать через apoli.
                        if (tamingTickCounter % 200 == 0) {
                            try {
                                var sscComp = net.onixary.shapeShifterCurseFabric.player_form.ability.RegPlayerFormComponent.PLAYER_FORM.get(victim);
                                boolean isFeral = sscComp.getCurrentForm().getBodyType() == net.onixary.shapeShifterCurseFabric.player_form.PlayerFormBodyType.FERAL;
                                if (sscComp.getCurrentForm().getIndex() >= 2 || isFeral){
                                    bond.setTamingProgress(bond.getTamingProgress() + 1);
                                } else  {
                                    if (bond.getTamingProgress() > 0) {
                                        bond.setTamingProgress(bond.getTamingProgress() - 1);
                                    }

                                }
                                if (master != null){
                                    if (bond.getTamingProgress() > 10){
                                        bond.setOwner(bond.getOwnerUUID(),bond.getOwnerName());
                                        ModComponents.PLAYER_BOND.get(master).addPetToRegistry(victim.getUuidAsString(), victim.getName().getString());
                                        victim.sendMessage(Text.translatable("text.bondofthebeast.taming_finished_pet").formatted(Formatting.DARK_RED), false);
                                        master.sendMessage(Text.translatable("text.bondofthebeast.taming_finished_owner").formatted(Formatting.GREEN), false);
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                        // Б) Невидимая цепь (Поводок воли)
                        double maxDistance = 25.0; // Дальше 25 блоков не уйти

                        if (master != null && master.getWorld() == victim.getWorld()) {
                            double distance = victim.distanceTo(master);

                            if (distance > maxDistance) {
                                Vec3d mPos = master.getPos();
                                Vec3d vPos = victim.getPos();
                                Vec3d dir = mPos.subtract(vPos).normalize();

                                // Мрачные партиклы вокруг жертвы
                                if (tamingTickCounter % 10 == 0) {
                                    ((ServerWorld) victim.getWorld()).spawnParticles(ParticleTypes.SQUID_INK, victim.getX(), victim.getY() + 1, victim.getZ(), 10, 0.3, 0.5, 0.3, 0.01);
                                }

                                if (distance > maxDistance + 15.0) {
                                    // Телепорт, если как-то умудрился убежать слишком далеко
                                    victim.teleport((ServerWorld) master.getWorld(), master.getX() - dir.x * 2, master.getY(), master.getZ() - dir.z * 2, victim.getYaw(), victim.getPitch());
                                    victim.sendMessage(Text.translatable("text.bondofthebeast.taming_leash_snap").formatted(Formatting.DARK_RED), true);
                                    victim.getWorld().playSound(null, victim.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0f, 0.5f);
                                } else {
                                    // Векторная тяга назад (как у лежанок)
                                    double pull = (distance - maxDistance) * 0.15;
                                    Vec3d vel = victim.getVelocity();
                                    victim.setVelocity(vel.x + dir.x * pull, vel.y, vel.z + dir.z * pull);
                                    victim.velocityModified = true;

                                    if (tamingTickCounter % 40 == 0) {
                                        victim.sendMessage(Text.translatable("text.bondofthebeast.taming_leash_pull").formatted(Formatting.RED), true);
                                        victim.getWorld().playSound(null, victim.getBlockPos(), SoundEvents.ITEM_ARMOR_EQUIP_CHAIN, SoundCategory.PLAYERS, 0.5f, 0.8f);
                                    }
                                }
                            }
                        } else {
                            // В) Наказание, если попытался сбежать, пока хозяин оффлайн или в другом измерении
                            if (tamingTickCounter % 100 == 0) {
                                victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 140, 2, false, false));
                                victim.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 140, 0, false, false));
                                victim.sendMessage(Text.translatable("text.bondofthebeast.taming_lost_without_master").formatted(Formatting.DARK_GRAY), true);
                            }
                        }
                        if (tamingTickCounter >= 200){
                            tamingTickCounter = 0;
                        }
                    }
                }
            }
            // -------------------------------------------------------------------------------

            if (++chainTickCounter >= 1) {
                chainTickCounter = 0;
                for (ServerPlayerEntity pet : server.getPlayerManager().getPlayerList()) {
                    var bond = ModComponents.PLAYER_BOND.get(pet);
                    if (!bond.hasOwner() || !canPetObey(pet)) continue;

                    BlockPos bedPos = bond.getBedPos();
                    if (bedPos != null) {
                        ServerWorld bedWorld = server.getWorld(pet.getSpawnPointDimension());
                        if (bedWorld != null) {
                            if (bedWorld.getBlockState(bedPos).getBlock() instanceof PetBedBlock) {
                                if (pet.getWorld() == bedWorld) {
                                    var be = bedWorld.getBlockEntity(bedPos);
                                    if (be instanceof PetBedBlockEntity bed) {
                                        int radius = bed.getChainRadius();
                                        if (radius > 0) {
                                            Vec3d bedVec = bedPos.toCenterPos();
                                            double dx = pet.getX() - bedVec.x;
                                            double dz = pet.getZ() - bedVec.z;
                                            double dist = Math.sqrt(dx * dx + dz * dz);

                                            if (dist > radius) {
                                                if (pet.age % 2 == 0) {
                                                    Vec3d petChainAnchor = pet.getPos().add(0, 0.7, 0);
                                                    Vec3d bedChainAnchor = bedVec.add(0, -0.3, 0);
                                                    double distance = petChainAnchor.distanceTo(bedChainAnchor);
                                                    int pCount = (int) (distance * 2.5);
                                                    if (pCount > 0) {
                                                        for (int i = 0; i <= pCount; i++) {
                                                            double t = (double) i / pCount;
                                                            double px = bedChainAnchor.x + (petChainAnchor.x - bedChainAnchor.x) * t;
                                                            double py = bedChainAnchor.y + (petChainAnchor.y - bedChainAnchor.y) * t;
                                                            double pz = bedChainAnchor.z + (petChainAnchor.z - bedChainAnchor.z) * t;
                                                            pet.getServerWorld().spawnParticles(ParticleTypes.CRIT, px, py, pz, 1, 0, 0, 0, 0);
                                                        }
                                                    }
                                                }
                                                Vec3d dir = new Vec3d(dx, 0, dz).normalize();
                                                if (dist <= radius + 5.0) {
                                                    double pullFactor = (dist - radius) * 0.15;
                                                    Vec3d currentVel = pet.getVelocity();
                                                    pet.setVelocity(currentVel.x - dir.x * pullFactor, currentVel.y, currentVel.z - dir.z * pullFactor);
                                                    pet.velocityModified = true;
                                                } else {
                                                    double targetX = bedVec.x + dir.x * radius;
                                                    double targetZ = bedVec.z + dir.z * radius;
                                                    pet.teleport(pet.getServerWorld(), targetX, pet.getY(), targetZ, pet.getYaw(), pet.getPitch());
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                bond.setBedPos(null);
                                BlockPos spawnPos = pet.getSpawnPointPosition();
                                if (spawnPos != null && spawnPos.equals(bedPos.up())) {
                                    pet.setSpawnPoint(null, null, 0.0f, false, false);
                                }
                            }
                        }
                    }
                }
            }

            if (++tickCounter >= 20) {
                tickCounter = 0;
                for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                    var ownerBond = ModComponents.PLAYER_BOND.get(player);
                    if (!ownerBond.hasOwner() || !canPetObey(player)) continue;

                    ServerPlayerEntity owner = server.getPlayerManager().getPlayer(UUID.fromString(ownerBond.getOwnerUUID()));
                    if (owner == null || owner.getWorld() != player.getWorld()) continue;

                    if (ownerBond.isAbsorbed()) {
                        if (player.interactionManager.getGameMode() != GameMode.SPECTATOR) player.changeGameMode(GameMode.SPECTATOR);
                        if (player.getCameraEntity() != owner) player.setCameraEntity(owner);
                        continue;
                    }

                    double d = player.squaredDistanceTo(owner);
                    if (!ownerBond.isSitting() && ownerBond.isTeleportEnabled() && ownerBond.getBondLevel() >= 2 && d > 400.0 && d <= 10000.0) {
                        tryTeleportPet(player, owner);
                    }

                    if (d <= 144.0 && ownerBond.getBondLevel() >= 4 && ownerBond.isAuraEnabled()) {
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 60, 0, true, false, true));
                        owner.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 60, 0, true, false, true));
                    }

                    if (ownerBond.getBondLevel() >= 3 && ownerBond.isProtectionMode()) {
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0, true, false, true));
                    }
                }
            }

            if (++expTickCounter >= 200) {
                expTickCounter = 0;
                for (ServerPlayerEntity pet : server.getPlayerManager().getPlayerList()) {
                    var bond = ModComponents.PLAYER_BOND.get(pet);
                    if (!bond.hasOwner()) continue;
                    ServerPlayerEntity owner = server.getPlayerManager().getPlayer(UUID.fromString(bond.getOwnerUUID()));
                    if (owner != null && owner.getWorld() == pet.getWorld() && pet.squaredDistanceTo(owner) <= 256.0) bond.addBondExperience(5);
                }
            }
        });
    }

    private static void tryTeleportPet(ServerPlayerEntity pet, ServerPlayerEntity owner) {
        ServerWorld world = owner.getServerWorld();
        BlockPos ownerPos = owner.getBlockPos();
        Random random = pet.getRandom();
        for (int i = 0; i < 10; i++) {
            int dx = random.nextBetween(-2, 2);
            int dy = random.nextBetween(-1, 1);
            int dz = random.nextBetween(-2, 2);
            BlockPos targetPos = ownerPos.add(dx, dy, dz);
            if (canTeleportTo(targetPos, world)) {
                pet.teleport(world, targetPos.getX() + 0.5, targetPos.getY(), targetPos.getZ() + 0.5, pet.getYaw(), pet.getPitch());
                pet.setVelocity(Vec3d.ZERO);
                pet.fallDistance = 0.0f;
                world.spawnParticles(ParticleTypes.PORTAL, targetPos.getX() + 0.5, targetPos.getY() + 1, targetPos.getZ() + 0.5, 10, 0.2, 0.5, 0.2, 0.1);
                return;
            }
        }
    }

    private static boolean canTeleportTo(BlockPos pos, ServerWorld world) {
        net.minecraft.block.BlockState floor = world.getBlockState(pos.down());
        net.minecraft.block.BlockState feet = world.getBlockState(pos);
        net.minecraft.block.BlockState head = world.getBlockState(pos.up());
        if (floor.getCollisionShape(world, pos.down()).isEmpty()) return false;
        if (!feet.getCollisionShape(world, pos).isEmpty() || !head.getCollisionShape(world, pos.up()).isEmpty()) return false;
        if (!feet.getFluidState().isEmpty() || !head.getFluidState().isEmpty()) return false;
        if (floor.getBlock() == net.minecraft.block.Blocks.MAGMA_BLOCK || floor.getBlock() == net.minecraft.block.Blocks.CAMPFIRE) return false;
        return true;
    }
}