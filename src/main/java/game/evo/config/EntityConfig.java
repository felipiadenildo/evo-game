package game.evo.config;

import java.util.Map;

/**
 * Represents the configuration for a generic game entity (NPC, item, obstacle).
 */
public class EntityConfig {
    public String type;
    public String image;
    public int row;
    public int column;
    public Map<String, Object> properties;
}