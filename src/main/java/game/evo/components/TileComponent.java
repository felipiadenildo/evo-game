package game.evo.components;

import game.evo.ecs.Component;
import game.evo.world.TileType; // Import the Enum we just created
import java.io.Serializable;

/**
 * Stores information specific to a map tile entity.
 */
public class TileComponent implements Component , Serializable { // << 2. ADICIONE 'implements Serializable'

    private static final long serialVersionUID = 1L; 
    public TileType type;
    public boolean isWalkable;
    public boolean blocksVision; // For camouflage or line-of-sight
    // Add other tile-specific properties here, e.g.:
    // float movementCostModifier;
    // String onEnterEffect; // (e.g., "SLOW", "DAMAGE_OVER_TIME")

    /**
     * Constructs a TileComponent.
     * @param type The type of the tile (e.g., GRASS, WATER).
     * @param isWalkable True if characters can normally walk on this tile, false otherwise.
     * @param blocksVision True if this tile blocks line of sight or provides cover.
     */
    public TileComponent(TileType type, boolean isWalkable, boolean blocksVision) {
        this.type = type;
        this.isWalkable = isWalkable;
        this.blocksVision = blocksVision;
    }

    @Override
    public String toString() {
        return "TileComponent[type=" + type + ", walkable=" + isWalkable + ", blocksVision=" + blocksVision + "]";
    }
}