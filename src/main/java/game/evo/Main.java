package game.evo;

import game.evo.components.*;
import game.evo.config.LevelConfig;
import game.evo.config.LevelLoader;
import game.evo.ecs.Entity;
import game.evo.ecs.World;
import game.evo.input.InputManager;
import game.evo.state.GameState;
import game.evo.systems.*;
import game.evo.utils.GameConstants;
import game.evo.utils.SaveManager;
import game.evo.view.GamePanel;
import game.evo.view.GameWindow;
import game.evo.world.EntityFactory;
import game.evo.world.GameMap;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Component;
import java.io.Serializable;
import java.util.List;

/**
 * Main application class using a robust architecture for game flow.
 * It uses a non-blocking notification system for in-game actions
 * to avoid all input-related bugs with dialog windows.
 */
public class Main implements Serializable {
    private static final long serialVersionUID = 1L;

    // --- Core Game Components ---
    private World world;
    private GameWindow gameWindow;
    private GamePanel gamePanel;
    private Timer gameLoopTimer;
    private List<GameSystem> logicSystems;
    private Entity playerEntity;

    // --- Game State & Managers ---
    private int currentLevelNumber = 1;
    private final SaveManager saveManager = new SaveManager();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().showStartMenu());
    }

    public void showStartMenu() {
        GameConstants.DEBUG_MODE_ON = true;
        while (true) {
            String[] options = {"New Game", "Continue"};
            int choice = JOptionPane.showOptionDialog(
                    null,
                    "Welcome to Evo! What would you like to do?",
                    "Evo - Main Menu",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null, options, options[0]);

            switch (choice) {
                case 0:
                    loadLevel(1, null);
                    return;
                case 1:
                    String fileToLoad = showLoadGameDialog(null);
                    if (fileToLoad != null) {
                        loadGame(fileToLoad);
                        return;
                    }
                    break;
                default:
                    System.exit(0);
                    return;
            }
        }
    }

    private String showLoadGameDialog(Component parent) {
        List<String> saveFiles = saveManager.getAvailableSaveFiles();
        if (saveFiles.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "No saved games found.", "Load Game", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        Object selectedFile = JOptionPane.showInputDialog(
                parent,
                "Choose a save file to load:",
                "Load Game",
                JOptionPane.QUESTION_MESSAGE,
                null,
                saveFiles.toArray(),
                saveFiles.get(0));
        return (selectedFile != null) ? selectedFile.toString() : null;
    }

    private void quickSave() {
        String filename = saveManager.generateSaveFilename(this.currentLevelNumber);
        GameState currentState = new GameState(this.currentLevelNumber, this.world);
        
        world.removeComponent(playerEntity, NotificationComponent.class);

        if (saveManager.saveStateToFile(currentState, filename)) {
            world.addComponent(playerEntity, new NotificationComponent("Game Saved!", 3.0f));
        } else {
            world.addComponent(playerEntity, new NotificationComponent("Error: Could not save game.", 3.0f));
        }
    }

    private void quickLoad() {
        List<String> saves = saveManager.getAvailableSaveFiles();
        if (saves.isEmpty()) {
            world.removeComponent(playerEntity, NotificationComponent.class);
            world.addComponent(playerEntity, new NotificationComponent("No save files found.", 3.0f));
            return;
        }
        
        String fileToLoad = saves.get(saves.size() - 1);
        
        GameState loadedState = saveManager.loadStateFromFile(fileToLoad);
        if (loadedState != null) {
            loadLevel(loadedState.levelNumber, loadedState);
        } else {
            world.removeComponent(playerEntity, NotificationComponent.class);
            world.addComponent(playerEntity, new NotificationComponent("Failed to load: " + fileToLoad, 3.0f));
        }
    }

    private void loadGame(String filename) {
        GameState loadedState = saveManager.loadStateFromFile(filename);
        if (loadedState != null) {
            loadLevel(loadedState.levelNumber, loadedState);
        } else {
            JOptionPane.showMessageDialog(gameWindow, "Could not load the selected save file.", "Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void loadLevel(int levelNumber, GameState loadedState) {
        if (gameLoopTimer != null) {
            gameLoopTimer.stop();
        }

        if (loadedState == null && levelNumber > GameConstants.MAX_LEVELS) {
            showEndGameMessage();
            return;
        }

        if (loadedState != null) {
            this.world = loadedState.world;
            this.currentLevelNumber = loadedState.levelNumber;
        } else {
            this.world = new World();
            this.currentLevelNumber = levelNumber;
        }
        System.out.println("\n--- LOADING LEVEL " + this.currentLevelNumber + " ---");

        EntityFactory entityFactory = new EntityFactory(world);
        LevelLoader levelLoader = new LevelLoader();
        String levelPath = "assets/levels/level-" + this.currentLevelNumber + ".json";
        LevelConfig config = levelLoader.loadLevelFromResource(levelPath);
        if (config == null) {
            System.err.println("[CRITICAL] Failed to load level file: " + levelPath);
            JOptionPane.showMessageDialog(null, "Critical error: Could not load level data.\n" + levelPath, "Load Error", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
            return;
        }

        GameMap gameMap = new GameMap(config.mapWidth, config.mapHeight, world, config.proceduralSeed, config.noiseScale);
        if (loadedState == null) {
            this.playerEntity = entityFactory.createPlayerCharacter(config.player);
            new PopulationSystem(world, gameMap, entityFactory, config).update();
        } else {
            this.playerEntity = world.getEntitiesWithComponent(PlayerControlledComponent.class).iterator().next();
        }

        InputManager inputManager = new InputManager();
        // The RenderSystem is now created but not part of the logicSystems list
        RenderSystem renderSystem = new RenderSystem(world);
        
        this.logicSystems = List.of(
                new PlayerInputSystem(world, inputManager, gameMap),
                new AISystem(world, gameMap),
                new CombatSystem(world, entityFactory),
                new InteractionSystem(world),
                new GameLogicSystem(world, entityFactory),
                new NotificationSystem(world) // Add the new system to the list
        );

        // GamePanel now receives the RenderSystem to call its update method
        this.gamePanel = new GamePanel(world, gameMap, renderSystem, inputManager);
        if (gameWindow == null) {
            gameWindow = new GameWindow("Evo - " + config.levelName, gamePanel);
            gameWindow.display();
        } else {
            gameWindow.setTitle("Evo - " + config.levelName);
            gameWindow.switchPanel(this.gamePanel);
        }

        startNewGameLoop();
    }
    
    private void startNewGameLoop() {
        this.gameLoopTimer = new Timer(GameConstants.GAME_LOOP_DELAY_MS, e -> {
            logicSystems.forEach(GameSystem::update);
            handleGameEvents();
            updateCameraForPlayer();
            if (gamePanel != null) {
                gamePanel.repaint();
            }
        });
        gameLoopTimer.start();
    }
    
    /**
     * REWRITTEN: Handles events in a non-blocking way using an on-screen notification system.
     */
    private void handleGameEvents() {
        if (playerEntity == null) return;
        
        // --- Quick Save Request ---
        if (world.hasComponent(playerEntity, SaveGameRequestComponent.class)) {
            world.removeComponent(playerEntity, SaveGameRequestComponent.class);
            quickSave(); // Instantaneous action
        }
        // --- Quick Load Request ---
        else if (world.hasComponent(playerEntity, LoadGameRequestComponent.class)) {
            world.removeComponent(playerEntity, LoadGameRequestComponent.class);
            quickLoad(); // This will restart the loop by calling loadLevel
        }
        // --- Level Transition Request ---
        else if (world.hasComponent(playerEntity, GoToNextLevelComponent.class)) {
            world.removeComponent(playerEntity, GoToNextLevelComponent.class);
            loadLevel(this.currentLevelNumber + 1, null);
        }
    }
    
    private void updateCameraForPlayer() {
        if (playerEntity == null || gamePanel == null || world == null) return;
        PositionComponent playerPos = world.getComponent(playerEntity, PositionComponent.class);
        if (playerPos == null) return;

        int playerPixelX = playerPos.column * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2;
        int playerPixelY = playerPos.row * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2;
        int screenWidthPixels = gamePanel.getWidth();
        int screenHeightPixels = gamePanel.getHeight();
        int newCamX = playerPixelX - screenWidthPixels / 2;
        int newCamY = playerPixelY - screenHeightPixels / 2;

        gamePanel.setCameraPosition(newCamX, newCamY);
    }

    private void showEndGameMessage() {
        System.out.println("[INFO GAME] All levels completed! Congratulations!");
        JOptionPane.showMessageDialog(gameWindow,
                "You have completed the evolutionary journey!\n\nGame created by:\nFelipi",
                "End of Game!",
                JOptionPane.INFORMATION_MESSAGE);

        if (gameWindow != null) {
            gameWindow.dispose();
        }
        System.exit(0);
    }
}
