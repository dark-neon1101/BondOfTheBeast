package com.bondofthebeast.component;

import com.bondofthebeast.BondOfTheBeast;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlayerBondComponentImpl implements PlayerBondComponent {
    private final PlayerEntity provider;
    private String ownerUUID = "";
    private String ownerName = "";
    private int tamingProgress = 0;
    private int tamingState = 0; // 0 = Свободен, 1 = Ломается воля, 2 = Полноценный питомец
    private String petNickname = null;
    private int bondLevel = 1;
    private int bondExperience = 0;
    private int skillPoints = 0;
    private final Set<String> unlockedSkills = new HashSet<>();
    private final Map<String, String> registeredPets = new HashMap<>();
    private BlockPos bedPos = null;

    private boolean sitting = false;
    private boolean teleportEnabled = false;
    private boolean protectionMode = false;
    private boolean auraEnabled = false;
    private boolean pacifistMode = false;
    private boolean vampiricMode = false;
    private boolean noBreakMode = false;
    private boolean absorbed = false;
    private boolean noInteractMode = false;

    private Set<String> blacklistedBlocks = new HashSet<>();
    private Set<String> whitelistedBlocks = new HashSet<>();

    public PlayerBondComponentImpl(PlayerEntity provider) {
        this.provider = provider;
        this.unlockedSkills.add("sit");
    }

    @Override
    public boolean shouldSyncWith(ServerPlayerEntity player) {
        return true;
    }

    @Override
    public void readFromNbt(NbtCompound tag) {
        this.ownerUUID = tag.getString("OwnerUUID");
        this.ownerName = tag.getString("OwnerName");

        if (tag.contains("TamingState")) {
            this.tamingState = tag.getInt("TamingState");
        }
        if (tag.contains("tamingProgress")) {
            this.tamingProgress = tag.getInt("TamingProgress");
        }

        this.petNickname = tag.contains("PetNickname") ? tag.getString("PetNickname") : null;
        this.bondLevel = Math.max(1, tag.getInt("BondLevel"));
        this.bondExperience = tag.getInt("BondExperience");

        this.skillPoints = tag.getInt("SkillPoints");

        this.unlockedSkills.clear();
        NbtList skillsList = tag.getList("UnlockedSkills", NbtElement.STRING_TYPE);
        for (int i = 0; i < skillsList.size(); i++) this.unlockedSkills.add(skillsList.getString(i));
        if (this.unlockedSkills.isEmpty()) this.unlockedSkills.add("sit");

        this.registeredPets.clear();
        if (tag.contains("RegisteredPets")) {
            NbtCompound petsTag = tag.getCompound("RegisteredPets");
            for (String key : petsTag.getKeys()) this.registeredPets.put(key, petsTag.getString(key));
        }

        this.bedPos = tag.contains("BedX") ? new BlockPos(tag.getInt("BedX"), tag.getInt("BedY"), tag.getInt("BedZ")) : null;

        this.sitting = tag.getBoolean("IsSitting");
        this.teleportEnabled = tag.getBoolean("TeleportEnabled");
        this.protectionMode = tag.getBoolean("ProtectionMode");
        this.auraEnabled = tag.getBoolean("AuraEnabled");
        this.pacifistMode = tag.getBoolean("PacifistMode");
        this.vampiricMode = tag.getBoolean("VampiricMode");
        this.noBreakMode = tag.getBoolean("NoBreakMode");
        this.absorbed = tag.getBoolean("Absorbed");
        this.noInteractMode = tag.getBoolean("NoInteractMode");

        this.blacklistedBlocks.clear();
        NbtList blackList = tag.getList("BlacklistedBlocks", NbtElement.STRING_TYPE);
        for (int i = 0; i < blackList.size(); i++) this.blacklistedBlocks.add(blackList.getString(i));

        this.whitelistedBlocks.clear();
        NbtList whiteList = tag.getList("WhitelistedBlocks", NbtElement.STRING_TYPE);
        for (int i = 0; i < whiteList.size(); i++) this.whitelistedBlocks.add(whiteList.getString(i));
    }

    @Override
    public void writeToNbt(NbtCompound tag) {
        tag.putString("OwnerUUID", this.ownerUUID);
        tag.putString("OwnerName", this.ownerName);
        tag.putInt("TamingState", this.tamingState);
        tag.putInt("TamingProgress", this.tamingProgress);
        if (this.petNickname != null) tag.putString("PetNickname", this.petNickname);
        tag.putInt("BondLevel", this.bondLevel);
        tag.putInt("BondExperience", this.bondExperience);
        tag.putInt("SkillPoints", this.skillPoints);

        NbtList skillsList = new NbtList();
        for (String skill : this.unlockedSkills) skillsList.add(NbtString.of(skill));
        tag.put("UnlockedSkills", skillsList);

        NbtCompound petsTag = new NbtCompound();
        this.registeredPets.forEach(petsTag::putString);
        tag.put("RegisteredPets", petsTag);

        if (this.bedPos != null) {
            tag.putInt("BedX", bedPos.getX());
            tag.putInt("BedY", bedPos.getY());
            tag.putInt("BedZ", bedPos.getZ());
        }

        tag.putBoolean("IsSitting", this.sitting);
        tag.putBoolean("TeleportEnabled", this.teleportEnabled);
        tag.putBoolean("ProtectionMode", this.protectionMode);
        tag.putBoolean("AuraEnabled", this.auraEnabled);
        tag.putBoolean("PacifistMode", this.pacifistMode);
        tag.putBoolean("VampiricMode", this.vampiricMode);
        tag.putBoolean("NoBreakMode", this.noBreakMode);
        tag.putBoolean("Absorbed", this.absorbed);
        tag.putBoolean("NoInteractMode", this.noInteractMode);

        NbtList blackList = new NbtList();
        for (String id : this.blacklistedBlocks) blackList.add(NbtString.of(id));
        tag.put("BlacklistedBlocks", blackList);

        NbtList whiteList = new NbtList();
        for (String id : this.whitelistedBlocks) whiteList.add(NbtString.of(id));
        tag.put("WhitelistedBlocks", whiteList);
    }

    @Override public boolean hasOwner() { return !ownerUUID.isEmpty(); }

    @Override public int getTamingState() { return this.tamingState; }

    @Override public void setTamingState(int state) {
        this.tamingState = state;
        ModComponents.PLAYER_BOND.sync(this.provider);
    }

    @Override public boolean isFullPet() { return this.tamingState == 2; }

    @Override public String getOwnerUUID() { return ownerUUID; }
    @Override public String getOwnerName() { return ownerName; }

    @Override public void setOwner(String uuid, String name) {
        this.ownerUUID = uuid;
        this.ownerName = name;
        this.tamingState = 2; // При стандартном контракте сразу ставим стадию полного подчинения
        ModComponents.PLAYER_BOND.sync(this.provider);
    }
    @Override public void setOwnerForced(String uuid, String name) {
        this.ownerUUID = uuid;
        this.ownerName = name;
        ModComponents.PLAYER_BOND.sync(this.provider);
    }

    @Override public void clearOwner() {
        this.ownerUUID = "";
        this.ownerName = "";
        this.petNickname = null;
        this.tamingState = 0;
        this.tamingProgress = 0;// Сбрасываем стадию при разрыве связи
        ModComponents.PLAYER_BOND.sync(this.provider);
    }

    @Nullable @Override public String getPetNickname() { return petNickname; }

    @Override public void setPetNickname(@Nullable String name) {
        this.petNickname = name;
        ModComponents.PLAYER_BOND.sync(this.provider);
    }

    @Override public int getBondLevel() { return bondLevel; }

    @Override public void setBondLevel(int level) {
        this.bondLevel = level;
        ModComponents.PLAYER_BOND.sync(this.provider);
    }

    @Override public int getBondExperience() { return bondExperience; }

    @Override public void setBondExperience(int exp) {
        this.bondExperience = exp;
        ModComponents.PLAYER_BOND.sync(this.provider);
    }

    @Override public void addBondExperience(int exp) {
        int stage = -1;
        try {
            var sscComp = net.onixary.shapeShifterCurseFabric.player_form.ability.RegPlayerFormComponent.PLAYER_FORM.get(this.provider);
            if (sscComp != null && sscComp.getCurrentForm() != null) {
                stage = sscComp.getCurrentForm().getIndex();
            }
        } catch (Exception ignored) {}

        if (stage == 2) {
            if (this.bondLevel > 1) {
                this.bondLevel = 1;
                this.bondExperience = 0;
            }
            if (this.bondLevel >= 1) {
                ModComponents.PLAYER_BOND.sync(this.provider);
                return;
            }
        }

        this.bondExperience += exp;
        int maxExp = this.bondLevel * 100;
        boolean leveledUp = false;

        while (this.bondExperience >= maxExp) {
            this.bondExperience -= maxExp;
            this.bondLevel++;
            this.skillPoints++;
            maxExp = this.bondLevel * 100;
            leveledUp = true;
        }

        if (leveledUp && this.bondLevel >= 5 && this.provider instanceof ServerPlayerEntity sp) {
            com.bondofthebeast.BondOfTheBeast.grantAdvancement(sp, "pet_story/bond_level");
            ServerPlayerEntity owner = sp.getServer().getPlayerManager().getPlayer(java.util.UUID.fromString(this.ownerUUID));
            if (owner != null) {
                com.bondofthebeast.BondOfTheBeast.grantAdvancement(owner, "owner_story/bond_level");
            }
        }
        ModComponents.PLAYER_BOND.sync(this.provider);
    }

    @Override public int getSkillPoints() { return this.skillPoints; }

    @Override public void setSkillPoints(int points) {
        this.skillPoints = points;
        ModComponents.PLAYER_BOND.sync(this.provider);
    }

    @Override public void addSkillPoints(int points) {
        this.skillPoints += points;
        ModComponents.PLAYER_BOND.sync(this.provider);
    }

    @Override public Set<String> getUnlockedSkills() { return this.unlockedSkills; }

    @Override public void unlockSkill(String skill) {
        this.unlockedSkills.add(skill);
        ModComponents.PLAYER_BOND.sync(this.provider);
    }

    @Override public void clearSkills() {
        this.unlockedSkills.clear();
        this.unlockedSkills.add("sit");
        ModComponents.PLAYER_BOND.sync(this.provider);
    }

    @Override public boolean isSkillUnlocked(String skill) { return this.unlockedSkills.contains(skill); }

    @Override public Map<String, String> getRegisteredPets() { return registeredPets; }

    @Override public void addPetToRegistry(String uuid, String name) {
        registeredPets.put(uuid, name);
        ModComponents.PLAYER_BOND.sync(this.provider);
    }

    @Override public void removePetFromRegistry(String uuid) {
        registeredPets.remove(uuid);
        ModComponents.PLAYER_BOND.sync(this.provider);
    }

    @Override public BlockPos getBedPos() { return bedPos; }

    @Override public void setBedPos(BlockPos pos) {
        this.bedPos = pos;
        ModComponents.PLAYER_BOND.sync(this.provider);
    }

    @Override public boolean isSitting() { return sitting; }

    @Override public void setSitting(boolean sitting) {
        this.sitting = sitting;
        ModComponents.PLAYER_BOND.sync(this.provider);
    }

    @Override public boolean isTeleportEnabled() { return teleportEnabled; }

    @Override public void setTeleportEnabled(boolean teleportEnabled) {
        this.teleportEnabled = teleportEnabled;
        ModComponents.PLAYER_BOND.sync(this.provider);
    }

    @Override public boolean isProtectionMode() { return protectionMode; }

    @Override public int getTamingProgress() { return this.tamingProgress; }
    @Override public void setTamingProgress(int progress) { this.tamingProgress = progress; }
    @Override public void setProtectionMode(boolean protectionMode) {
        this.protectionMode = protectionMode;
        ModComponents.PLAYER_BOND.sync(this.provider);
    }

    @Override public boolean isAuraEnabled() { return auraEnabled; }

    @Override public void setAuraEnabled(boolean auraEnabled) {
        this.auraEnabled = auraEnabled;
        ModComponents.PLAYER_BOND.sync(this.provider);
    }

    @Override public boolean isPacifistMode() { return pacifistMode; }

    @Override public void setPacifistMode(boolean pacifistMode) {
        this.pacifistMode = pacifistMode;
        ModComponents.PLAYER_BOND.sync(this.provider);
    }

    @Override public boolean isVampiricMode() { return vampiricMode; }

    @Override public void setVampiricMode(boolean vampiricMode) {
        this.vampiricMode = vampiricMode;
        ModComponents.PLAYER_BOND.sync(this.provider);
    }

    @Override public boolean isNoBreakMode() { return noBreakMode; }

    @Override public void setNoBreakMode(boolean noBreakMode) {
        this.noBreakMode = noBreakMode;
        ModComponents.PLAYER_BOND.sync(this.provider);
    }

    @Override public boolean isAbsorbed() { return absorbed; }

    @Override public void setAbsorbed(boolean absorbed) {
        this.absorbed = absorbed;
        ModComponents.PLAYER_BOND.sync(this.provider);
    }

    @Override public boolean isNoInteractMode() { return noInteractMode; }

    @Override public void setNoInteractMode(boolean noInteractMode) {
        this.noInteractMode = noInteractMode;
        ModComponents.PLAYER_BOND.sync(this.provider);
    }

    @Override public Set<String> getBlacklistedBlocks() { return blacklistedBlocks; }

    @Override public void setBlacklistedBlocks(Set<String> blocks) {
        this.blacklistedBlocks = new HashSet<>(blocks);
        ModComponents.PLAYER_BOND.sync(this.provider);
    }

    @Override public Set<String> getWhitelistedBlocks() { return whitelistedBlocks; }

    @Override public void setWhitelistedBlocks(Set<String> blocks) {
        this.whitelistedBlocks = new HashSet<>(blocks);
        ModComponents.PLAYER_BOND.sync(this.provider);
    }
}