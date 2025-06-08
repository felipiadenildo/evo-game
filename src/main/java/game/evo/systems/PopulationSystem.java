package game.evo.systems;

import game.evo.config.EntityConfig;
import game.evo.config.LevelConfig;
import game.evo.config.BiomeRuleConfig;
import game.evo.config.SpawnableConfig;
import game.evo.ecs.World;
import game.evo.world.EntityFactory;
import game.evo.world.GameMap;
import game.evo.world.TileType;
import game.evo.utils.GridPosition;

import java.util.*;

/**
 * A system responsible for populating the game world with entities
 * at the start of a level, based on procedural population rules.
 * This version uses a two-pass approach to enforce total entity counts.
 */
public class PopulationSystem extends GameSystem {

    private final GameMap gameMap;
    private final EntityFactory entityFactory;
    private final LevelConfig levelConfig;
    private final Random random;

    public PopulationSystem(World world, GameMap gameMap, EntityFactory entityFactory, LevelConfig levelConfig) {
        super(world);
        this.gameMap = gameMap;
        this.entityFactory = entityFactory;
        this.levelConfig = levelConfig;
        this.random = new Random(levelConfig.proceduralSeed);
    }

    /**
     * This update method is called once at the start of a level to populate the world.
     */
    @Override
    public void update() {
        if (levelConfig.biomeRules == null || levelConfig.biomeRules.isEmpty()) {
            System.out.println("[INFO PopulationSystem] No population rules found for this level.");
            return;
        }

        // Step 1: Map all valid spawn locations by biome type.
        Map<TileType, List<GridPosition>> validTilesByBiome = mapAllBiomeLocations();

        // Step 2: Populate the world based on the rules.
        System.out.println("[INFO PopulationSystem] Populating world based on biome density rules...");

        for (BiomeRuleConfig biomeRule : levelConfig.biomeRules) {
            try {
                TileType biomeType = TileType.valueOf(biomeRule.biome);
                List<GridPosition> spawnableLocations = validTilesByBiome.get(biomeType);

                if (spawnableLocations == null || spawnableLocations.isEmpty()) {
                    continue; // No tiles of this biome exist on the map.
                }

                // Shuffle the list of locations to ensure random placement.
                Collections.shuffle(spawnableLocations, random);

                for (SpawnableConfig spawnable : biomeRule.spawnables) {
                    // Calculate the total number of entities to create for this type.
                    int totalToSpawn = (int) (spawnableLocations.size() * spawnable.density);

                    if (World.MODO_VERBOSE_WORLD) {
                        System.out.println("  > Rule for Biome '" + biomeRule.biome + "': Spawning " + totalToSpawn + 
                                           " of type '" + spawnable.type + "' in an area of " + spawnableLocations.size() + " tiles.");
                    }
                    
                    // Create the entities at random locations from the valid list.
                    for (int i = 0; i < totalToSpawn; i++) {
                        if (spawnableLocations.isEmpty()) {
                            System.err.println("[WARN PopulationSystem] Ran out of valid spawn locations for biome " + biomeRule.biome);
                            break; // No more available spots.
                        }
                        
                        // Take the last position from the shuffled list and remove it to prevent reuse.
                        GridPosition spawnPos = spawnableLocations.remove(spawnableLocations.size() - 1);
                        
                        EntityConfig entityConfig = new EntityConfig();
                        entityConfig.type = spawnable.type;
                        entityConfig.row = spawnPos.row(); // Use the record's accessor method
                        entityConfig.column = spawnPos.column();
                        entityConfig.properties = spawnable.properties;
                        
                        entityFactory.createGameEntity(entityConfig);
                    }
                }
            } catch (IllegalArgumentException e) {
                System.err.println("[ERROR PopulationSystem] Biome '" + biomeRule.biome + "' defined in JSON does not match any TileType enum.");
            }
        }
        System.out.println("[INFO PopulationSystem] World population complete.");
    }
    
    /**
     * Scans the entire map once and categorizes every tile by its biome type.
     * @return A map where the key is the TileType and the value is a list of all
     * (row, col) positions belonging to that biome.
     */
    private Map<TileType, List<GridPosition>> mapAllBiomeLocations() {
        Map<TileType, List<GridPosition>> mappedLocations = new EnumMap<>(TileType.class);
        TileType[][] logicalGrid = gameMap.getLogicalGrid();

        for (int r = 0; r < gameMap.getHeightInTiles(); r++) {
            for (int c = 0; c < gameMap.getWidthInTiles(); c++) {
                TileType currentType = logicalGrid[r][c];
                // Add a new list for this biome type if it's the first time we see it.
                mappedLocations.computeIfAbsent(currentType, k -> new ArrayList<>());
                // Add the current coordinate to that biome's list.
                mappedLocations.get(currentType).add(new GridPosition(r, c));
            }
        }
        return mappedLocations;
    }
}
