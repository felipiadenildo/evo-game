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
 * Classe principal da aplicação. REFATORADO: Agora gerencia uma sequência de
 * carregamento visual em etapas.
 */
public class Main implements Serializable {

    private static final long serialVersionUID = 1L;

    // Enum para controlar os estágios de carregamento
    private enum LoadingPhase {
        STARTING,
        WINDOW_VISIBLE,
        MAP_LOADED,
        PLAYER_SPAWNED,
        WORLD_POPULATED,
        COMPLETE
    }

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

    // --- Variáveis de Controle de Carregamento ---
    private LoadingPhase currentLoadingPhase = LoadingPhase.STARTING;
    private int loadingDelayCounter = 0;
    private static final int FRAMES_PER_LOADING_STEP = 30; // Aprox. 0.5s por etapa a 60fps

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

    
    private void loadGame(String filename) {
        GameState loadedState = saveManager.loadStateFromFile(filename);
        if (loadedState != null) {
            // Usa o método de carregamento imediato para pular a sequência visual
            loadLevelImmediately(loadedState.levelNumber, loadedState);
        } else {
            JOptionPane.showMessageDialog(gameWindow, "Could not load the selected save file.", "Load Error", JOptionPane.ERROR_MESSAGE);
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

    public void loadLevel(int levelNumber, GameState loadedState) {
        if (gameLoopTimer != null) {
            gameLoopTimer.stop();
        }

        if (loadedState == null && levelNumber > GameConstants.MAX_LEVELS) {
            showEndGameMessage();
            return;
        }

        // Se estiver carregando um save, a lógica de carregamento em etapas é pulada por simplicidade.
        if (loadedState != null) {
            loadLevelImmediately(levelNumber, loadedState);
            return;
        }

        this.world = new World();
        this.currentLevelNumber = levelNumber;
        System.out.println("\n--- STARTING SEQUENTIAL LOAD FOR LEVEL " + this.currentLevelNumber + " ---");

        // Prepara todas as ferramentas necessárias para o carregamento
        EntityFactory entityFactory = new EntityFactory(world);
        LevelLoader levelLoader = new LevelLoader();
        String levelPath = "assets/levels/level-" + this.currentLevelNumber + ".json";
        LevelConfig config = levelLoader.loadLevelFromResource(levelPath);
        if (config == null) {
            // ... (tratamento de erro)
            return;
        }

        InputManager inputManager = new InputManager();
        RenderSystem renderSystem = new RenderSystem(world);

        // Cria o GamePanel, mas passa 'null' para o GameMap, que será criado depois
        this.gamePanel = new GamePanel(world, null, renderSystem, inputManager, entityFactory);

        if (gameWindow == null) {
            gameWindow = new GameWindow("Evo - Loading...", gamePanel);
            gameWindow.display();
        } else {
            gameWindow.setTitle("Evo - Loading...");
            gameWindow.switchPanel(this.gamePanel);
        }

        // Inicia a máquina de estados e o game loop que vai gerenciá-la
        this.currentLoadingPhase = LoadingPhase.WINDOW_VISIBLE;
        this.loadingDelayCounter = 0;
        startNewGameLoop(entityFactory, config);
    }

    /**
     * Carrega um nível instantaneamente (usado para 'Continue'). Este é o seu
     * método loadLevel antigo, para manter a funcionalidade de load.
     */
    private void loadLevelImmediately(int levelNumber, GameState loadedState) {
        this.world = loadedState.world;
        this.currentLevelNumber = loadedState.levelNumber;
        this.playerEntity = world.getEntitiesWithComponent(PlayerControlledComponent.class).iterator().next();

        System.out.println("\n--- LOADING SAVED LEVEL " + this.currentLevelNumber + " ---");
        EntityFactory entityFactory = new EntityFactory(world);
        LevelLoader levelLoader = new LevelLoader();
        LevelConfig config = levelLoader.loadLevelFromResource("assets/levels/level-" + this.currentLevelNumber + ".json");

        GameMap gameMap = new GameMap(world, config);
        InputManager inputManager = new InputManager();
        RenderSystem renderSystem = new RenderSystem(world);

        this.logicSystems = List.of(new PlayerInputSystem(world, inputManager, gameMap), new AISystem(world, gameMap), new CombatSystem(world, entityFactory), new InteractionSystem(world), new GameLogicSystem(world, entityFactory), new NotificationSystem(world));
        this.gamePanel = new GamePanel(world, gameMap, renderSystem, inputManager, entityFactory);
        gameWindow.setTitle("Evo - " + config.levelName);
        gameWindow.switchPanel(this.gamePanel);

        this.currentLoadingPhase = LoadingPhase.COMPLETE; // Pula direto para o final
        startNewGameLoop(entityFactory, config);
    }

    /**
     * MÉTODO REESCRITO: O game loop agora pode estar em modo "carregando" ou
     * "jogando".
     */
    private void startNewGameLoop(EntityFactory entityFactory, LevelConfig config) {
        this.gameLoopTimer = new Timer(GameConstants.GAME_LOOP_DELAY_MS, e -> {
            if (currentLoadingPhase != LoadingPhase.COMPLETE) {
                updateLoadingSequence(entityFactory, config);
            } else {
                if (logicSystems != null) {
                    logicSystems.forEach(GameSystem::update);
                }
                handleGameEvents();
                updateCameraForPlayer();
            }
            if (gamePanel != null) {
                gamePanel.repaint();
            }
        });
        gameLoopTimer.start();
    }

    /**
     * NOVO MÉTODO: Controla cada passo da sequência de carregamento.
     */
    private void updateLoadingSequence(EntityFactory entityFactory, LevelConfig config) {
        loadingDelayCounter++;
        if (loadingDelayCounter < FRAMES_PER_LOADING_STEP) {
            return; // Espera o tempo definido antes de ir para o próximo passo
        }
        loadingDelayCounter = 0; // Reseta o contador

        switch (currentLoadingPhase) {
            case WINDOW_VISIBLE:
                System.out.println("[Loader] Phase 1: Drawing Map...");
                GameMap gameMap = new GameMap(world, config);
                gamePanel.setGameMap(gameMap);
                currentLoadingPhase = LoadingPhase.MAP_LOADED;
                break;
            case MAP_LOADED:
                System.out.println("[Loader] Phase 2: Spawning Player...");
                this.playerEntity = entityFactory.createPlayerCharacter(config.player);
                currentLoadingPhase = LoadingPhase.PLAYER_SPAWNED;
                break;
            case PLAYER_SPAWNED:
                System.out.println("[Loader] Phase 3: Populating World...");
                new PopulationSystem(world, gamePanel.getGameMap(), entityFactory, config).update();
                currentLoadingPhase = LoadingPhase.WORLD_POPULATED;
                break;
            case WORLD_POPULATED:
                System.out.println("[Loader] Phase 4: Loading Complete!");
                this.logicSystems = List.of(
                        new PlayerInputSystem(world, (InputManager) gamePanel.getKeyListeners()[0], gamePanel.getGameMap()),
                        new AISystem(world, gamePanel.getGameMap()),
                        new CombatSystem(world, entityFactory),
                        new InteractionSystem(world),
                        new GameLogicSystem(world, entityFactory),
                        new NotificationSystem(world)
                );
                gameWindow.setTitle("Evo - " + config.levelName);
                currentLoadingPhase = LoadingPhase.COMPLETE;
                break;
        }
    }

    /**
     * REWRITTEN: Handles events in a non-blocking way using an on-screen
     * notification system.
     */
    private void handleGameEvents() {
        if (playerEntity == null) {
            return;
        }

        // --- Quick Save Request ---
        if (world.hasComponent(playerEntity, SaveGameRequestComponent.class)) {
            world.removeComponent(playerEntity, SaveGameRequestComponent.class);
            quickSave(); // Instantaneous action
        } // --- Quick Load Request ---
        else if (world.hasComponent(playerEntity, LoadGameRequestComponent.class)) {
            world.removeComponent(playerEntity, LoadGameRequestComponent.class);
            quickLoad(); // This will restart the loop by calling loadLevel
        } // --- Level Transition Request ---
        else if (world.hasComponent(playerEntity, GoToNextLevelComponent.class)) {
            world.removeComponent(playerEntity, GoToNextLevelComponent.class);
            loadLevel(this.currentLevelNumber + 1, null);
        }
    }

    private void updateCameraForPlayer() {
        if (playerEntity == null || gamePanel == null || world == null) {
            return;
        }
        PositionComponent playerPos = world.getComponent(playerEntity, PositionComponent.class);
        if (playerPos == null) {
            return;
        }

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
