package game.evo.config;

import java.io.Serializable;
import java.util.Map;

/**
 * Represents the initial configuration for the Player Character for a level.
 * It now includes a properties map for flexible, data-driven attributes.
 */
public class PlayerConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    public int row;
    public int column;
    public int lives;
    
    // This map holds all custom properties like "size", "diet", "bodyType", etc.
    public Map<String, Object> properties; 
}
