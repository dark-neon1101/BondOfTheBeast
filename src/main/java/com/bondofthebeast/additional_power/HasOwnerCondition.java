package com.bondofthebeast.additional_power;

import com.bondofthebeast.BondOfTheBeast;
import io.github.apace100.apoli.Apoli;
import io.github.apace100.calio.data.SerializableData;
import net.minecraft.entity.Entity;
import com.bondofthebeast.component.ModComponents;
import net.minecraft.entity.player.PlayerEntity;
import io.github.apace100.apoli.power.factory.condition.ConditionFactory;

public class HasOwnerCondition {
    public static boolean condition(SerializableData.Instance data, Entity entity) {
        if (entity instanceof PlayerEntity) {
            var bond = ModComponents.PLAYER_BOND.get(entity);
            return bond.hasOwner();
        }
        return false;
    }
    public static ConditionFactory<Entity> getFactory() {
        return new ConditionFactory<>(
                BondOfTheBeast.identifier("has_owner"),
                new SerializableData(),
                HasOwnerCondition::condition
        );

    }
}
