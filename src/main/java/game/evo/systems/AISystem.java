package game.evo.systems;

import game.evo.ecs.Entity;
import game.evo.ecs.World;
import game.evo.world.GameMap;
import game.evo.utils.CollisionUtil;
import game.evo.components.*;

import java.util.Random;
import java.util.Set;

/**
 * AISystem controls the behavior of Non-Player Characters (NPCs).
 * It reads the EcologyComponent to determine an NPC's temperament (Aggressive, Skittish, Neutral)
 * and updates its movement and actions accordingly.
 */
public class AISystem extends GameSystem {

    private final GameMap gameMap;
    private final Random random = new Random();
    private Entity playerEntityCache; // Cache for the player entity to avoid repeated lookups per frame

    public AISystem(World world, GameMap gameMap) {
        super(world);
        this.gameMap = gameMap;
    }

    /**
     * The main update loop for the AI. Called every frame.
     */
    @Override
    public void update() {
        // Find the player entity once per frame for all NPCs to reference
        this.playerEntityCache = findPlayer();
        
        Set<Entity> npcs = world.getEntitiesWithComponent(NpcComponent.class);
        long currentTime = System.currentTimeMillis();

        for (Entity npc : npcs) {
            // Get all necessary components for the NPC
            AiComponent ai = world.getComponent(npc, AiComponent.class);
            EcologyComponent ecology = world.getComponent(npc, EcologyComponent.class);
            ProceduralSpriteComponent sprite = world.getComponent(npc, ProceduralSpriteComponent.class);
            
            if (ai == null || ecology == null || sprite == null) continue;

            // Check timing to control movement speed
            if (currentTime - ai.lastMoveTime < ai.moveDelay) {
                sprite.isMoving = false; // If not moving, ensure animation state is idle
                continue; 
            }
            ai.lastMoveTime = currentTime;
            sprite.isMoving = true; // Assume movement will happen unless it stands still

            // Decide which action to take based on temperament
            switch (ecology.temperament) {
                case AGGRESSIVE:
                    handleAggressive(npc);
                    break;
                case SKITTISH:
                    handleSkittish(npc);
                    break;
                case NEUTRAL:
                default:
                    handleNeutral(npc);
                    break;
            }
        }
    }

    /**
     * Handles AGGRESSIVE behavior: find and chase the player if nearby, otherwise wander.
     */
    private void handleAggressive(Entity npc) {
        PositionComponent npcPos = world.getComponent(npc, PositionComponent.class);
        PositionComponent playerPos = (playerEntityCache != null) ? world.getComponent(playerEntityCache, PositionComponent.class) : null;
        
        int detectionRange = 8; // How many tiles away the NPC can "see" the player
        int attackRange = 1; // How close it needs to be to attack

        if (playerPos != null && isWithinDistance(npcPos, playerPos, detectionRange)) {
            // If right next to the player, try to attack
            if (isWithinDistance(npcPos, playerPos, attackRange)) {
                if (!world.hasComponent(npc, WantsToAttackComponent.class)) {
                    world.addComponent(npc, new WantsToAttackComponent());
                }
                world.getComponent(npc, ProceduralSpriteComponent.class).isMoving = false; // Stop moving to attack
            } else {
                // Otherwise, move towards the player
                moveTowards(npc, playerPos);
            }
        } else {
            // If player is not in range, just wander around
            handleNeutral(npc);
        }
    }

    /**
     * Handles SKITTISH behavior: flee from the player if nearby, otherwise wander.
     */
    private void handleSkittish(Entity npc) {
        PositionComponent npcPos = world.getComponent(npc, PositionComponent.class);
        PositionComponent playerPos = (playerEntityCache != null) ? world.getComponent(playerEntityCache, PositionComponent.class) : null;
        int fleeRange = 6; // How close the player has to be to scare the NPC

        if (playerPos != null && isWithinDistance(npcPos, playerPos, fleeRange)) {
            moveAwayFrom(npc, playerPos);
        } else {
            handleNeutral(npc);
        }
    }

    /**
     * Handles NEUTRAL behavior: wander randomly.
     */
    private void handleNeutral(Entity npc) {
        // This is a simple random walk
        int moveChoice = random.nextInt(5); // 0-3 for movement, 4 for standing still
        if (moveChoice == 4) {
            world.getComponent(npc, ProceduralSpriteComponent.class).isMoving = false;
            return;
        }
        
        DirectionComponent.Direction moveDirection = DirectionComponent.Direction.values()[moveChoice];
        moveInDirection(npc, moveDirection);
    }

    // --- Helper Methods for AI Movement ---

    /**
     * Calculates the best single step to take to get closer to a target position.
     */
    private void moveTowards(Entity npc, PositionComponent targetPos) {
        PositionComponent npcPos = world.getComponent(npc, PositionComponent.class);
        if (npcPos == null) return;

        int dr = targetPos.row - npcPos.row;
        int dc = targetPos.column - npcPos.column;

        // Move along the axis with the greatest distance first to close the gap
        if (Math.abs(dr) > Math.abs(dc)) {
            moveInDirection(npc, dr > 0 ? DirectionComponent.Direction.DOWN : DirectionComponent.Direction.UP);
        } else if (dc != 0) { // Check dc != 0 to avoid standing still if on same column
            moveInDirection(npc, dc > 0 ? DirectionComponent.Direction.RIGHT : DirectionComponent.Direction.LEFT);
        } else { // Already on the same column, move vertically
            moveInDirection(npc, dr > 0 ? DirectionComponent.Direction.DOWN : DirectionComponent.Direction.UP);
        }
    }

    /**
     * Calculates the best single step to take to get further away from a target position.
     */
    private void moveAwayFrom(Entity npc, PositionComponent targetPos) {
        PositionComponent npcPos = world.getComponent(npc, PositionComponent.class);
        if (npcPos == null) return;

        int dr = npcPos.row - targetPos.row;
        int dc = npcPos.column - targetPos.column;
        
        // Move along the axis that creates the most distance
        if (Math.abs(dr) > Math.abs(dc)) {
            moveInDirection(npc, dr > 0 ? DirectionComponent.Direction.DOWN : DirectionComponent.Direction.UP);
        } else if (dc != 0) {
            moveInDirection(npc, dc > 0 ? DirectionComponent.Direction.RIGHT : DirectionComponent.Direction.LEFT);
        } else { // Flee vertically if on same column
             moveInDirection(npc, dr > 0 ? DirectionComponent.Direction.DOWN : DirectionComponent.Direction.UP);
        }
    }

    /**
     * Tries to move an entity one step in a given direction after checking for collisions.
     * @param entity The entity to move.
     * @param direction The direction to move in.
     */
    private void moveInDirection(Entity entity, DirectionComponent.Direction direction) {
        PositionComponent position = world.getComponent(entity, PositionComponent.class);
        DirectionComponent dirComponent = world.getComponent(entity, DirectionComponent.class);
        if (position == null || dirComponent == null) return;
        
        dirComponent.facing = direction; // Update direction component regardless of successful move
        
        int targetRow = position.row;
        int targetCol = position.column;
        
        switch (direction) {
            case UP:    targetRow--; break;
            case DOWN:  targetRow++; break;
            case LEFT:  targetCol--; break;
            case RIGHT: targetCol++; break;
        }

        if (CollisionUtil.isPositionOpen(world, gameMap, targetRow, targetCol, entity)) {
            position.row = targetRow;
            position.column = targetCol;
        } else {
             // If move failed, it's not in a "moving" state for animation
             world.getComponent(entity, ProceduralSpriteComponent.class).isMoving = false;
        }
    }

    private boolean isWithinDistance(PositionComponent pos1, PositionComponent pos2, int distance) {
        if (pos1 == null || pos2 == null) return false;
        // Using Manhattan distance for simplicity and performance on a grid
        return (Math.abs(pos1.row - pos2.row) + Math.abs(pos1.column - pos2.column)) <= distance;
    }

    private Entity findPlayer() {
        Set<Entity> players = world.getEntitiesWithComponent(PlayerControlledComponent.class);
        return players.isEmpty() ? null : players.iterator().next();
    }
}
