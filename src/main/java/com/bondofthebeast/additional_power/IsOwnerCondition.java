package com.bondofthebeast.additional_power;


import com.bondofthebeast.BondOfTheBeast;
import io.github.apace100.apoli.Apoli;
import io.github.apace100.apoli.power.factory.condition.BiEntityConditions;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.data.SerializableDataType;
import io.github.apace100.calio.data.SerializableDataTypes;
import net.minecraft.entity.Entity;
import com.bondofthebeast.component.ModComponents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import io.github.apace100.apoli.power.factory.condition.ConditionFactory;
import net.minecraft.util.Pair;

public class IsOwnerCondition {
    public static boolean condition(SerializableData.Instance data, Pair<Entity, Entity> ActorAndTarget) {
        boolean reversed = data.getBoolean("reversed");
        Entity potential_owner;
        Entity potential_pet;
        if (reversed) {
            potential_owner = ActorAndTarget.getLeft();
            potential_pet = ActorAndTarget.getRight();
        }
        else {
            potential_owner = ActorAndTarget.getRight();
            potential_pet = ActorAndTarget.getLeft();
        }
        if ((potential_owner instanceof PlayerEntity) && (potential_pet instanceof PlayerEntity)) {
            var bond = ModComponents.PLAYER_BOND.get(potential_pet);
            return bond.getOwnerUUID().equals(potential_owner.getUuid().toString());
        }
        return false;
    }
    public static ConditionFactory<Pair<Entity,Entity>>  getFactory() {
        return new ConditionFactory<>(
                BondOfTheBeast.identifier("is_owner"),
                new SerializableData().add("reversed", SerializableDataTypes.BOOLEAN, false),
                IsOwnerCondition::condition
        );

    }
}
