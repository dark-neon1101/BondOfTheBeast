package com.bondofthebeast;

import com.bondofthebeast.item.InfusedCollarItem;
import com.bondofthebeast.item.LunarOblivionDustItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {

    public static final Item COLLAR = registerItem("collar", new CollarItem(new FabricItemSettings().maxCount(1)));
    public static final Item FIDELITY_CONTRACT = registerItem("fidelity_contract", new ContractItem(new FabricItemSettings().maxCount(1)));
    public static final Item WHISTLE = registerItem("whistle", new WhistleItem(new FabricItemSettings().maxCount(1)));
    public static final Item PET_TREAT = registerItem("pet_treat", new Item(new FabricItemSettings().maxCount(64)));
    public static final Item COMMAND_SCEPTER = registerItem("command_scepter", new CommandScepterItem(new FabricItemSettings().maxCount(1)));
    public static final Item NECKLACE_OF_CLARITY = registerItem("necklace_of_clarity", new NecklaceOfClarity(new FabricItemSettings().maxCount(1)));
    // --- НОВЫЕ ПРЕДМЕТЫ ДЛЯ СИСТЕМЫ ПОДАВЛЕНИЯ ВОЛИ ---
    public static final Item LUNAR_OBLIVION_DUST = registerItem("lunar_oblivion_dust", new LunarOblivionDustItem(new FabricItemSettings().maxCount(16)));
    public static final Item INFUSED_COLLAR = registerItem("infused_collar", new InfusedCollarItem(new FabricItemSettings().maxCount(1)));

    // Заглушки для будущих шагов (раскомментируем, когда создадим их классы):
     public static final Item CATALYST_TREAT = registerItem("catalyst_treat", new Item(new FabricItemSettings().maxCount(64)));
    // public static final Item ECHO_CATALYST_TREAT = registerItem("echo_catalyst_treat", new EchoCatalystTreatItem(new FabricItemSettings().maxCount(64)));
    // --------------------------------------------------

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(BondOfTheBeast.MOD_ID, name), item);
    }

    public static void registerModItems() {
        BondOfTheBeast.LOGGER.info("Registering Mod Items for " + BondOfTheBeast.MOD_ID);
    }
}