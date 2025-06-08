package game.evo.world;

import game.evo.ecs.Entity;
import game.evo.ecs.World;
import game.evo.components.PositionComponent;
import game.evo.components.CollisionComponent; // Import para criar obstáculos
import game.evo.utils.GameConstants;

import java.awt.image.BufferedImage;

/**
 * Manages the game map by orchestrating procedural generation and storing
 * both the visual map image and the logical grid for gameplay mechanics.
 */
public class GameMap {
    private final int widthInTiles;
    private final int heightInTiles;
    private final World world;

    private final TileType[][] logicalGrid;
    private final BufferedImage mapImage;

    /**
     * Constructs and generates a procedural GameMap.
     * @param widthInTiles The width of the map in logical tiles.
     * @param heightInTiles The height of the map in logical tiles.
     * @param world The ECS World instance to populate with special tile entities.
     * @param seed The seed for procedural generation.
     * @param noiseScale The "zoom" level for the procedural noise.
     */
    public GameMap(int widthInTiles, int heightInTiles, World world, long seed, double noiseScale) {
        if (world == null) {
            throw new IllegalArgumentException("World instance cannot be null for GameMap initialization.");
        }
        this.widthInTiles = widthInTiles;
        this.heightInTiles = heightInTiles;
        this.world = world;

        // 1. Create a MapGenerator and generate the map data
        MapGenerator generator = new MapGenerator(seed);
        generator.generate(widthInTiles, heightInTiles, GameConstants.CELL_SIZE, noiseScale);
        
        // 2. Store the generated data
        this.logicalGrid = generator.getLogicalGrid();
        this.mapImage = generator.getMapImage();

        // 3. Create ECS entities ONLY for special, non-walkable tiles that need collision
        createEntitiesForSpecialTiles();
    }
    
    /**
     * Iterates through the generated logical grid and creates ECS entities
     * for any tiles that are not walkable, so they can be part of the collision system.
     */
    private void createEntitiesForSpecialTiles() {
        if (logicalGrid == null) {
             System.err.println("[ERROR GameMap] Logical grid is null. Cannot create special tile entities.");
             return;
        }
        System.out.println("[INFO GameMap] Creating ECS entities for non-walkable tiles...");
        for (int r = 0; r < heightInTiles; r++) {
            for (int c = 0; c < widthInTiles; c++) {
                TileType currentType = logicalGrid[r][c];
                
                // If the tile itself is not walkable (e.g., a mountain, deep ocean),
                // create a "phantom" entity with a CollisionComponent at that position.
                if (currentType != null && !currentType.isWalkable) {
                    Entity obstacleEntity = world.createEntity();
                    world.addComponent(obstacleEntity, new PositionComponent(r, c));
                    world.addComponent(obstacleEntity, new CollisionComponent());
                    // It doesn't need a Renderable component because it's already drawn on the background.
                    // Its only purpose is to block movement.
                }
            }
        }
    }

    /**
     * Gets the logical tile type at the specified grid coordinates.
     * @param row The row of the tile.
     * @param column The column of the tile.
     * @return The TileType at that position, or TileType.UNKNOWN if out of bounds.
     */
    public TileType getLogicalTileType(int row, int column) {
        if (logicalGrid != null && row >= 0 && row < heightInTiles && column >= 0 && column < widthInTiles) {
            return logicalGrid[row][column];
        }
        return TileType.UNKNOWN;
    }
    
    /**
     * Returns the pre-rendered image of the entire map.
     * @return The BufferedImage of the map.
     */
    public BufferedImage getMapImage() {
        return this.mapImage;
    }

    /**
     * --- MÉTODO CORRIGIDO ---
     * Returns the 2D array representing the logical grid of the map.
     * @return The TileType[][] logical grid.
     */
    public TileType[][] getLogicalGrid() {
        return this.logicalGrid; // Retorna a grade lógica, em vez de lançar uma exceção.
    }
    
    public int getWidthInTiles() {
        return widthInTiles;
    }

    public int getHeightInTiles() {
        return heightInTiles;
    }
}
