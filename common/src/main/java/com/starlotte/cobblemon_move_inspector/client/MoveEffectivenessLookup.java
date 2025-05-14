package com.starlotte.cobblemon_move_inspector.client;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.types.ElementalType;

public class MoveEffectivenessLookup {
    private static final HashMap<String, HashMap<String, Integer>> typeChart = new HashMap<>();
    private static final GraalTypeChartGetter typeChartGetter = new GraalTypeChartGetter();

    public static float getModifier(MoveTemplate move, ElementalType defenderType1, ElementalType defenderType2, String abilityName, UUID player) {
        ElementalType moveType = move.getElementalType();
        float damageMult = 1;

        // Check special conditions for move (eg. steel immune to sandstorm)
        if (move != null) {
            if (defenderType1 != null) damageMult *= getMultFromType(move.getName(), defenderType1.getName());
            if (defenderType2 != null) damageMult *= getMultFromType(move.getName(), defenderType2.getName());
        }
        
        // Check type matchup
        if (defenderType1 != null) damageMult *= getMultFromType(moveType.getName(), defenderType1.getName());
        if (defenderType2 != null) damageMult *= getMultFromType(moveType.getName(), defenderType2.getName());

        // Handle abilities that affect type effectiveness
        if (abilityName != null && !abilityName.isEmpty()) {
            // Normalize ability name by removing spaces and converting to lowercase for comparison
            String normalizedAbilityName = normalizeAbilityName(abilityName);
            
            // Handle Levitate
            if (isAbility(normalizedAbilityName, "levitate") && moveType.getName().equalsIgnoreCase("ground")) {
                return 0; // Immune to Ground-type moves
            }
            
            // Handle Water Absorb, Dry Skin, Storm Drain
            if ((isAbility(normalizedAbilityName, "waterabsorb") || 
                 isAbility(normalizedAbilityName, "dryskin") || 
                 isAbility(normalizedAbilityName, "stormdrain")) && 
                moveType.getName().equalsIgnoreCase("water")) {
                return 0;
            }
            
            // Handle Flash Fire
            if (isAbility(normalizedAbilityName, "flashfire") && moveType.getName().equalsIgnoreCase("fire")) {
                return 0;
            }
            
            // Handle Lightning Rod, Motor Drive, Volt Absorb
            if ((isAbility(normalizedAbilityName, "lightningrod") || 
                 isAbility(normalizedAbilityName, "motordrive") || 
                 isAbility(normalizedAbilityName, "voltabsorb")) && 
                moveType.getName().equalsIgnoreCase("electric")) {
                return 0;
            }
            
            // Handle Sap Sipper
            if (isAbility(normalizedAbilityName, "sapsipper") && moveType.getName().equalsIgnoreCase("grass")) {
                return 0;
            }
            
            // Handle Wonder Guard (only super effective moves hit)
            if (isAbility(normalizedAbilityName, "wonderguard") && damageMult <= 1) {
                return 0;
            }
        }

        return damageMult;
    }

    // Keep the old method for backwards compatibility
    public static float getModifier(MoveTemplate move, ElementalType defenderType1, ElementalType defenderType2, List<Ability> defenderAbilities, UUID player) {
        String abilityName = null;
        if (defenderAbilities != null && !defenderAbilities.isEmpty() && defenderAbilities.get(0) != null) {
            abilityName = defenderAbilities.get(0).getName();
        }
        return getModifier(move, defenderType1, defenderType2, abilityName, player);
    }

    // Keep the old method for backwards compatibility
    public static float getModifier(MoveTemplate move, ElementalType defenderType1, ElementalType defenderType2, UUID player) {
        return getModifier(move, defenderType1, defenderType2, (String)null, player);
    }

    public static float getMultFromType(String moveName, String typeName) {
        HashMap<String, Integer> matchupMap = typeChart.get(typeName);
        if (matchupMap == null)
            return 1;

        Integer damageType = matchupMap.get(moveName);
        if (damageType == null)
            return 1;

        return getMult(damageType);
    }

    public static float getMult(int damage) {
        return switch (damage) {
            default -> 1;
            case (1) -> 2;
            case (2) -> 0.5f;
            case (3) -> 0;
        };
    }

    /**
     * Normalizes ability names by removing spaces, underscores, and converting to lowercase
     * This makes comparison more consistent regardless of how the ability name is formatted
     */
    private static String normalizeAbilityName(String abilityName) {
        if (abilityName == null) return "";
        return abilityName.toLowerCase().replace(" ", "").replace("_", "");
    }
    
    /**
     * Checks if the normalized ability name matches any of the expected formats
     */
    private static boolean isAbility(String normalizedAbilityName, String expectedAbility) {
        return normalizedAbilityName.equals(expectedAbility);
    }

    static {
        typeChartGetter.openConnection();
        typeChartGetter.getTypeChart(typeChart);
    }
}
