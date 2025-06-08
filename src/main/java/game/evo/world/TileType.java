package game.evo.world;

/**
 * Defines the different logical types of tiles.
 * NOTE: For testing purposes, all tiles are currently set to be walkable.
 */
public enum TileType {
    // Biome Types (walkable status)
    OCEAN_DEEP(true),      // Temporarily walkable for testing
    OCEAN_SHALLOW(true),   // Temporarily walkable for testing
    BEACH_SAND(true),
    DESERT(true),
    GRASSLAND(true),
    FOREST(true),
    JUNGLE(true),
    TUNDRA(true),
    MOUNTAIN_ROCK(true),   // Temporarily walkable for testing
    MOUNTAIN_SNOW(true),   // Temporarily walkable for testing

    UNKNOWN(false);

    public final boolean isWalkable;

    TileType(boolean isWalkable) {
        this.isWalkable = isWalkable;
    }
}