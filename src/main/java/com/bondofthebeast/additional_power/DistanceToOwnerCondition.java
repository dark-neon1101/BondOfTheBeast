package com.bondofthebeast.additional_power;

import com.bondofthebeast.BondOfTheBeast;
import com.bondofthebeast.component.PlayerBondComponent;
import io.github.apace100.apoli.Apoli;
import io.github.apace100.apoli.data.ApoliDataTypes;
import io.github.apace100.apoli.util.Comparison;
import io.github.apace100.calio.data.SerializableData;
import io.github.apace100.calio.data.SerializableDataTypes;
import net.minecraft.entity.Entity;
import com.bondofthebeast.component.ModComponents;
import net.minecraft.entity.player.PlayerEntity;
import io.github.apace100.apoli.power.factory.condition.ConditionFactory;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

public class DistanceToOwnerCondition {
    public static boolean condition(SerializableData.Instance data, Entity entity) {
        Comparison comparison = (Comparison) data.get("comparison");
        if (comparison == null) {
            return false;
        }
        if (entity instanceof PlayerEntity) {
            PlayerBondComponent bond = ModComponents.PLAYER_BOND.get(entity);
            if (bond.hasOwner()){
                var owner = bond.getOwnerUUID();
                for (ServerPlayerEntity potentialOwner : entity.getServer().getPlayerManager().getPlayerList()) {
                    if (potentialOwner.getUuidAsString().equals(owner)) {
                        boolean sameDimension = entity.getWorld().getRegistryKey() == potentialOwner.getWorld().getRegistryKey();
                        double distanceSq = sameDimension ? entity.squaredDistanceTo(potentialOwner) : Double.MAX_VALUE;
                        float distance;
                        if (sameDimension) {
                            distance = (float) Math.sqrt(distanceSq);
                            float compareTo = data.getFloat("compare_to");

                            return comparison.compare(distance, compareTo);
                        }else {
                            return false;
                        }
                    }
                }
            }
        }
        return false;
    }
    public static ConditionFactory<Entity> getFactory() {
        return new ConditionFactory<>(
                BondOfTheBeast.identifier("distance_to_owner"),
                new SerializableData()
                        .add("comparison", ApoliDataTypes.COMPARISON)
                        .add("compare_to", SerializableDataTypes.FLOAT, 0.0f),
                HasOwnerCondition::condition
        );

    }
}
