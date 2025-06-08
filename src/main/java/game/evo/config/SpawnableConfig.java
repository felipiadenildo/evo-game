package game.evo.config;

import game.evo.world.TileType;
import java.util.List;
import java.util.Map;

/**
 * Defines a rule for a type of entity that can be spawned within a biome.
 * This class is used in the level configuration files.
 */
public class SpawnableConfig {
    
    /**
     * The behavioral archetype of the entity to spawn.
     * Must match a 'case' in the EntityFactory (e.g., "HerbivoreNPC", "StaticObject").
     */
    public String type; 
    
    /**
     * The probability (from 0.0 to 1.0) that this entity will spawn
     * on any given valid tile within its allowed biomes.
     */
    public double density; 
    
    /**
     * A map of properties passed to the EntityFactory to customize the entity.
     * Examples: "size", "speed", "image", "bodyType".
     */
    public Map<String, Object> properties;
}
