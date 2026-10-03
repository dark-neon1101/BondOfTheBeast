package com.bondofthebeast;

import com.bondofthebeast.additional_power.DistanceToOwnerCondition;
import com.bondofthebeast.additional_power.HasOwnerCondition;
import com.bondofthebeast.additional_power.IsOwnerCondition;
import io.github.apace100.apoli.power.factory.condition.ConditionFactory;
import io.github.apace100.apoli.registry.ApoliRegistries;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registry;

public class EntityConditions {
    public static void register() {

        register(HasOwnerCondition.getFactory());
        register(DistanceToOwnerCondition.getFactory());
    }
    private static void register(ConditionFactory<Entity> conditionFactory) {
        Registry.register(ApoliRegistries.ENTITY_CONDITION, conditionFactory.getSerializerId(), conditionFactory);
    }
}
