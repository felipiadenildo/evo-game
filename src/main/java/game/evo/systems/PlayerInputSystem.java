package game.evo.systems;

import game.evo.components.*;
import game.evo.ecs.Entity;
import game.evo.ecs.World;
import game.evo.input.InputManager;
import game.evo.utils.CollisionUtil;
import game.evo.world.GameMap;

import java.awt.event.KeyEvent;
import java.util.Set;

/**
 * Processes player input from the InputManager and updates the player's entity state.
 * This includes handling movement intent, direction changes, animation
 * state, and action requests.
 */
public class PlayerInputSystem extends GameSystem {

    private final InputManager inputManager;
    private final GameMap gameMap;

    // State flags to track key presses for single-trigger actions.
    private boolean oKeyWasPressed = false;
    private boolean pKeyWasPressed = false;
    private boolean spaceKeyWasPressed = false;

    public PlayerInputSystem(World world, InputManager inputManager, GameMap gameMap) {
        super(world);
        this.inputManager = inputManager;
        this.gameMap = gameMap;
    }

    /**
     * Executes the logic for player input. Called on every tick of the main
     * game loop.
     */
    @Override
    public void update() {
        Set<Entity> playerEntities = world.getEntitiesWithComponent(PlayerControlledComponent.class);
        if (playerEntities.isEmpty()) {
            return;
        }
        Entity player = playerEntities.iterator().next();
        handleMovement(player);
        handleActions(player);
    }

    /**
     * Handles movement-related input (arrow keys or WASD).
     *
     * @param player The player entity.
     */
    private void handleMovement(Entity player) {
        PositionComponent position = world.getComponent(player, PositionComponent.class);
        DirectionComponent direction = world.getComponent(player, DirectionComponent.class);
        ProceduralSpriteComponent sprite = world.getComponent(player, ProceduralSpriteComponent.class);

        if (position == null || direction == null || sprite == null) {
            return;
        }

        int targetRow = position.row;
        int targetCol = position.column;
        boolean moveRequested = false;

        if (inputManager.isKeyPressed(KeyEvent.VK_UP) || inputManager.isKeyPressed(KeyEvent.VK_W)) {
            targetRow--;
            direction.facing = DirectionComponent.Direction.UP;
            moveRequested = true;
        } else if (inputManager.isKeyPressed(KeyEvent.VK_DOWN) || inputManager.isKeyPressed(KeyEvent.VK_S)) {
            targetRow++;
            direction.facing = DirectionComponent.Direction.DOWN;
            moveRequested = true;
        } else if (inputManager.isKeyPressed(KeyEvent.VK_LEFT) || inputManager.isKeyPressed(KeyEvent.VK_A)) {
            targetCol--;
            direction.facing = DirectionComponent.Direction.LEFT;
            moveRequested = true;
        } else if (inputManager.isKeyPressed(KeyEvent.VK_RIGHT) || inputManager.isKeyPressed(KeyEvent.VK_D)) {
            targetCol++;
            direction.facing = DirectionComponent.Direction.RIGHT;
            moveRequested = true;
        }

        sprite.isMoving = moveRequested;

        if (moveRequested) {
            if (CollisionUtil.isPositionOpen(world, gameMap, targetRow, targetCol, player)) {
                position.row = targetRow;
                position.column = targetCol;
            }
        }
    }

    /**
     * Handles action-related input using edge detection for single-trigger events.
     *
     * @param player The player entity.
     */
    private void handleActions(Entity player) {
        boolean spaceIsDown = inputManager.isKeyPressed(KeyEvent.VK_SPACE);
        boolean oIsDown = inputManager.isKeyPressed(KeyEvent.VK_O);
        boolean pIsDown = inputManager.isKeyPressed(KeyEvent.VK_P);

        // Trigger attack only on the frame the key goes from UP to DOWN.
        if (spaceIsDown && !this.spaceKeyWasPressed) {
            if (!world.hasComponent(player, WantsToAttackComponent.class)) {
                world.addComponent(player, new WantsToAttackComponent());
            }
        }

        // Trigger save only on the frame the key goes from UP to DOWN.
        if (oIsDown && !this.oKeyWasPressed) {
            if (!world.hasComponent(player, SaveGameRequestComponent.class)) {
                world.addComponent(player, new SaveGameRequestComponent());
            }
        }

        // Trigger load only on the frame the key goes from UP to DOWN.
        if (pIsDown && !this.pKeyWasPressed) {
            if (!world.hasComponent(player, LoadGameRequestComponent.class)) {
                world.addComponent(player, new LoadGameRequestComponent());
            }
        }

        // Update the state flags for the next frame.
        this.spaceKeyWasPressed = spaceIsDown;
        this.oKeyWasPressed = oIsDown;
        this.pKeyWasPressed = pIsDown;
    }

/**
     * FIX: New public method to forcefully reset the internal state of single-press action keys.
     * This is called by the Main class after a modal dialog is closed to prevent input state bugs.
     */
    public void resetActionKeyStates() {
        this.spaceKeyWasPressed = false;
        this.oKeyWasPressed = false;
        this.pKeyWasPressed = false;
    }
}
