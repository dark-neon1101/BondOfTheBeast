package com.bondofthebeast.component;

import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public interface PlayerBondComponent extends Component, AutoSyncedComponent {
    boolean hasOwner();

    // Новые методы для системы подчинения
    int getTamingState();
    void setTamingState(int state);
    boolean isFullPet();

    String getOwnerUUID();
    String getOwnerName();
    void setOwner(String uuid, String name);
    void setOwnerForced(String uuid, String name);
    void clearOwner();

    @Nullable
    String getPetNickname();
    void setPetNickname(@Nullable String name);

    int getBondLevel();
    void setBondLevel(int level);
    int getBondExperience();
    void setBondExperience(int exp);
    void addBondExperience(int exp);

    int getSkillPoints();
    void setSkillPoints(int points);
    void addSkillPoints(int points);

    Set<String> getUnlockedSkills();
    void unlockSkill(String skill);
    void clearSkills();
    boolean isSkillUnlocked(String skill);

    Map<String, String> getRegisteredPets();
    void addPetToRegistry(String uuid, String name);
    void removePetFromRegistry(String uuid);

    BlockPos getBedPos();
    void setBedPos(BlockPos pos);

    boolean isSitting();
    void setSitting(boolean sitting);
    boolean isTeleportEnabled();
    void setTeleportEnabled(boolean teleportEnabled);
    boolean isProtectionMode();
    void setProtectionMode(boolean protectionMode);
    boolean isAuraEnabled();
    void setAuraEnabled(boolean auraEnabled);
    boolean isPacifistMode();
    void setPacifistMode(boolean pacifistMode);
    boolean isVampiricMode();
    void setVampiricMode(boolean vampiricMode);
    boolean isNoBreakMode();
    void setNoBreakMode(boolean noBreakMode);
    boolean isAbsorbed();
    void setAbsorbed(boolean absorbed);
    boolean isNoInteractMode();
    void setNoInteractMode(boolean noInteractMode);

    Set<String> getBlacklistedBlocks();
    void setBlacklistedBlocks(Set<String> blocks);
    Set<String> getWhitelistedBlocks();
    void setWhitelistedBlocks(Set<String> blocks);
}