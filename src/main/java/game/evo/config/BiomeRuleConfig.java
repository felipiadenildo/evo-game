package game.evo.config;

import java.util.List;

/**
 * Defines all population rules for a specific biome type.
 * It links a biome name (e.g., "FOREST") to a list of potential spawns.
 */
public class BiomeRuleConfig {

    /**
     * The name of the biome this rule applies to.
     * Must match a value from the TileType enum (e.g., "FOREST", "OCEAN_DEEP").
     */
    public String biome;
    
    /**
     * A list of all creatures and objects that can potentially spawn in this biome.
     */
    public List<SpawnableConfig> spawnables;
}
