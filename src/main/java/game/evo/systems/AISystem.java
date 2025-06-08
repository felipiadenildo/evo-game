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
    private Entity playerEntityCache; // Cache for the player entity to avoid repeated lookups

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
        if (this.playerEntityCache == null) {
            // If there is no player, NPCs can just wander
        }

        Set<Entity> npcs = world.getEntitiesWithComponent(NpcComponent.class);
        long currentTime = System.currentTimeMillis();

        for (Entity npc : npcs) {
            AiComponent ai = world.getComponent(npc, AiComponent.class);
            if (ai == null) continue;

            // Check timing to control movement speed
            if (currentTime - ai.lastMoveTime < ai.moveDelay) {
                world.getComponent(npc, ProceduralSpriteComponent.class).isMoving = false;
                continue;
            }
            ai.lastMoveTime = currentTime;

            // Decide which action to take based on temperament
            EcologyComponent ecology = world.getComponent(npc, EcologyComponent.class);
            if (ecology != null) {
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
    }

    /**
     * Handles AGGRESSIVE behavior: find and chase the player if nearby, otherwise wander.
     */
    private void handleAggressive(Entity npc) {
        PositionComponent npcPos = world.getComponent(npc, PositionComponent.class);
        PositionComponent playerPos = (playerEntityCache != null) ? world.getComponent(playerEntityCache, PositionComponent.class) : null;
        int detectionRange = 8; // How many tiles away the NPC can "see" the player

        // If player exists and is within range, chase them
        if (playerPos != null && isWithinDistance(npcPos, playerPos, detectionRange)) {
            moveTowards(npc, playerPos);
        } else {
            // Otherwise, just wander around
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

        // If player exists and is too close, run away
        if (playerPos != null && isWithinDistance(npcPos, playerPos, fleeRange)) {
            moveAwayFrom(npc, playerPos);
        } else {
            // Otherwise, just wander around calmly
            handleNeutral(npc);
        }
    }

    /**
     * Handles NEUTRAL behavior: wander randomly.
     */
    private void handleNeutral(Entity npc) {
        PositionComponent position = world.getComponent(npc, PositionComponent.class);
        DirectionComponent direction = world.getComponent(npc, DirectionComponent.class);
        ProceduralSpriteComponent sprite = world.getComponent(npc, ProceduralSpriteComponent.class);
        if (position == null || direction == null || sprite == null) return;
        
        sprite.isMoving = true; // Attempt to move

        int moveChoice = random.nextInt(5); // 0-3 for movement, 4 for standing still
        int targetRow = position.row;
        int targetCol = position.column;

        switch (moveChoice) {
            case 0: targetRow--; direction.facing = DirectionComponent.Direction.UP; break;
            case 1: targetRow++; direction.facing = DirectionComponent.Direction.DOWN; break;
            case 2: targetCol--; direction.facing = DirectionComponent.Direction.LEFT; break;
            case 3: targetCol++; direction.facing = DirectionComponent.Direction.RIGHT; break;
            default: sprite.isMoving = false; return; // Stand still
        }

        if (CollisionUtil.isPositionOpen(world, gameMap, targetRow, targetCol, npc)) {
            position.row = targetRow;
            position.column = targetCol;
        }
    }

    // --- Helper Methods for AI Movement ---

    private void moveTowards(Entity npc, PositionComponent targetPos) {
        // ... (Implement logic to move one step closer to targetPos)
    }

    private void moveAwayFrom(Entity npc, PositionComponent targetPos) {
        // ... (Implement logic to move one step further from targetPos)
    }

    private boolean isWithinDistance(PositionComponent pos1, PositionComponent pos2, int distance) {
        if (pos1 == null || pos2 == null) return false;
        int dr = Math.abs(pos1.row - pos2.row);
        int dc = Math.abs(pos1.column - pos2.column);
        // Using Manhattan distance for simplicity and performance on a grid
        return (dr + dc) <= distance;
    }

    private Entity findPlayer() {
        Set<Entity> players = world.getEntitiesWithComponent(PlayerControlledComponent.class);
        if (!players.isEmpty()) {
            return players.iterator().next();
        }
        return null;
    }
}
