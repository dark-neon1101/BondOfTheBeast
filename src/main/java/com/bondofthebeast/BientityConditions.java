package com.bondofthebeast;

import com.bondofthebeast.additional_power.IsOwnerCondition;
import io.github.apace100.apoli.power.factory.condition.ConditionFactory;
import io.github.apace100.apoli.registry.ApoliRegistries;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registry;
import net.minecraft.util.Pair;

public class BientityConditions {
    public static void register(){
        register(IsOwnerCondition.getFactory());
    }
    private static void register(ConditionFactory<Pair<Entity, Entity>> conditionFactory) {
        Registry.register(ApoliRegistries.BIENTITY_CONDITION, conditionFactory.getSerializerId(), conditionFactory);

    }
}
