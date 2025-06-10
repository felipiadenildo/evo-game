package game.evo.systems;

import game.evo.components.*;
import game.evo.ecs.Entity;
import game.evo.ecs.World;
import game.evo.input.InputManager;
import game.evo.utils.CollisionUtil;
import game.evo.utils.GameConstants;
import game.evo.world.GameMap;

import java.awt.event.KeyEvent;
import java.util.Set;

/**
 * Processa o input do jogador. REFATORADO: Agora lida com o estado de
 * "aguardando input" da tela de introdução.
 */
public class PlayerInputSystem extends GameSystem {

    private final InputManager inputManager;
    private final GameMap gameMap;

    // Flags de estado para teclas de ação de um único toque
    private boolean oKeyWasPressed = false;
    private boolean pKeyWasPressed = false;
    private boolean spaceKeyWasPressed = false;
    private boolean enterKeyWasPressed = false; // <-- ADICIONADO para a tela de intro

    public PlayerInputSystem(World world, InputManager inputManager, GameMap gameMap) {
        super(world);
        this.inputManager = inputManager;
        this.gameMap = gameMap;
    }

    /**
     * Lógica principal do sistema, chamada a cada frame. Agora, ela direciona
     * para o handler de input apropriado com base no estado do jogo.
     */
    @Override
    public void update() {
//        if (GameConstants.DEBUG_MODE_ON) {
//            System.out.println("[DEBUG] PlayerInputSystem: update() chamado.");
//        }

        Set<Entity> playerEntities = world.getEntitiesWithComponent(PlayerControlledComponent.class);
        if (playerEntities.isEmpty()) {
            return;
        }
        Entity player = playerEntities.iterator().next();

        if (world.hasComponent(player, AwaitingInputComponent.class)) {
//            if (GameConstants.DEBUG_MODE_ON) {
//                System.out.println("[DEBUG] PlayerInputSystem: Estado 'AwaitingInputComponent' detectado. Chamando handleIntroInput...");
//            }
            handleIntroInput(player);
        } else {
            // ...caso contrário, processa o input normal do jogo.
            handleMovement(player);
            handleActions(player);
        }
    }

    /**
     * NOVO MÉTODO: Lida apenas com o input da tela de introdução.
     *
     * @param player A entidade do jogador.
     */
    private void handleIntroInput(Entity player) {
        boolean enterIsDown = inputManager.isKeyPressed(KeyEvent.VK_ENTER);

        if (enterIsDown && !this.enterKeyWasPressed) {
//            if (GameConstants.DEBUG_MODE_ON) {
//                System.out.println("[DEBUG] PlayerInputSystem: Tecla ENTER pressionada! Adicionando StartGameplayRequestComponent.");
//            }
            world.addComponent(player, new StartGameplayRequestComponent());
        }
        this.enterKeyWasPressed = enterIsDown;
    }

    /**
     * Lida com o input de movimento (WASD ou Setas).
     */
    private void handleMovement(Entity player) {
        // Este método permanece como estava.
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
     * Lida com inputs de ações (Salvar, Carregar, Atacar).
     */
    private void handleActions(Entity player) {
        // Este método permanece como estava.
        boolean spaceIsDown = inputManager.isKeyPressed(KeyEvent.VK_SPACE);
        boolean oIsDown = inputManager.isKeyPressed(KeyEvent.VK_O);
        boolean pIsDown = inputManager.isKeyPressed(KeyEvent.VK_P);

        if (spaceIsDown && !this.spaceKeyWasPressed) {
            if (!world.hasComponent(player, WantsToAttackComponent.class)) {
                world.addComponent(player, new WantsToAttackComponent());
            }
        }

        if (oIsDown && !this.oKeyWasPressed) {
            if (!world.hasComponent(player, SaveGameRequestComponent.class)) {
                world.addComponent(player, new SaveGameRequestComponent());
            }
        }

        if (pIsDown && !this.pKeyWasPressed) {
            if (!world.hasComponent(player, LoadGameRequestComponent.class)) {
                world.addComponent(player, new LoadGameRequestComponent());
            }
        }

        this.spaceKeyWasPressed = spaceIsDown;
        this.oKeyWasPressed = oIsDown;
        this.pKeyWasPressed = pIsDown;
    }

    /**
     * Reseta o estado interno das teclas de ação.
     */
    public void resetActionKeyStates() {
        this.spaceKeyWasPressed = false;
        this.oKeyWasPressed = false;
        this.pKeyWasPressed = false;
        this.enterKeyWasPressed = true; // Garante que o Enter não seja acionado novamente por engano
    }
}
