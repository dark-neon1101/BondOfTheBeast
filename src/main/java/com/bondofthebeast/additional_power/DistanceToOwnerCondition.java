package com.bondofthebeast.additional_power;

import com.bondofthebeast.BondOfTheBeast;
import io.github.apace100.apoli.Apoli;
import io.github.apace100.apoli.data.ApoliDataTypes;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.data.SerializableDataTypes;
import net.minecraft.entity.Entity;
import com.bondofthebeast.component.ModComponents;
import net.minecraft.entity.player.PlayerEntity;
import io.github.apace100.apoli.power.factory.condition.ConditionFactory;

import java.util.UUID;

public class DistanceToOwnerCondition {
    public static boolean condition(SerializableData.Instance data, Entity entity) {
        if (entity instanceof PlayerEntity) {
            var bond = ModComponents.PLAYER_BOND.get(entity);
            if (bond.hasOwner()){
                var owner = UUID.fromString(bond.getOwnerUUID());
            }
        }
        return false;
    }
    public static ConditionFactory<Entity> getFactory() {
        return new ConditionFactory<>(
                BondOfTheBeast.identifier("has_owner"),
                new SerializableData()
                        .add("comparison", ApoliDataTypes.COMPARISON)
                        .add("compare_to", SerializableDataTypes.FLOAT, 0.0f),
                HasOwnerCondition::condition
        );

    }
}
