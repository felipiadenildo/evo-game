package game.evo.utils;

import game.evo.ecs.Entity;
import game.evo.ecs.World;
import game.evo.world.GameMap;
import game.evo.world.TileType;
import game.evo.components.CollisionComponent;
import game.evo.components.PositionComponent;
import java.util.Set;

/**
 * A utility class to handle collision detection logic.
 */
public final class CollisionUtil {

    private CollisionUtil() {} // Private constructor for utility class

    /**
     * Checks if a specific grid cell is a valid position for an entity to move into.
     * It checks both terrain walkability and for collisions with other solid entities.
     * @param world The ECS world.
     * @param gameMap The game map.
     * @param targetRow The target row to check.
     * @param targetCol The target column to check.
     * @param movingEntity The entity that is attempting to move (to avoid self-collision).
     * @return true if the position is open, false otherwise.
     */
    public static boolean isPositionOpen(World world, GameMap gameMap, int targetRow, int targetCol, Entity movingEntity) {
        // 1. Check terrain walkability
        TileType targetTileType = gameMap.getLogicalTileType(targetRow, targetCol);
        if (targetTileType == null || !targetTileType.isWalkable) {
            if (World.MODO_VERBOSE_WORLD) {
                System.out.println("[DEBUG Collision] Move to (" + targetRow + "," + targetCol + ") blocked by terrain: " + targetTileType);
            }
            return false;
        }

        // 2. Check for other solid entities at the target location
        Set<Entity> solidEntities = world.getEntitiesWithComponent(CollisionComponent.class);
        for (Entity otherEntity : solidEntities) {
            // Don't check for collision with oneself
            if (otherEntity.equals(movingEntity)) {
                continue;
            }

            PositionComponent otherPos = world.getComponent(otherEntity, PositionComponent.class);
            if (otherPos != null && otherPos.row == targetRow && otherPos.column == targetCol) {
                if (World.MODO_VERBOSE_WORLD) {
                    System.out.println("[DEBUG Collision] Move to (" + targetRow + "," + targetCol + ") blocked by entity: " + otherEntity);
                }
                return false; // Found a solid entity, position is not open
            }
        }

        // If all checks pass, the position is open
        return true;
    }
}