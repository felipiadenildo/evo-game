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
import game.evo.components.StartGameplayRequestComponent;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.Component;
import java.io.Serializable;
import java.util.List;

/**
 * Classe principal da aplicação que orquestra todo o fluxo do jogo. REFATORADO:
 * Gerencia uma sequência de carregamento visual em etapas para uma experiência
 * de usuário mais suave e profissional.
 */
public class Main implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Enum para controlar os estágios de carregamento do nível.
     */
    public enum LoadingPhase {
        STARTING, // Estado inicial, não utilizado na prática
        SHOWING_LOADING_SCREEN, // Exibindo a tela "Carregando Nível..."
        MAP_LOADED, // O mapa de fundo foi gerado e está visível
        PLAYER_SPAWNED, // O jogador foi criado e está visível
        WORLD_POPULATED, // NPCs e objetos de cenário foram criados
        SHOWING_LEVEL_INTRO, // Exibindo o título e a descrição do nível, aguardando input
        COMPLETE                // Carregamento finalizado, o jogo está rodando
    }
    

    // --- Componentes Principais do Jogo ---
    private World world;
    private GameWindow gameWindow;
    private GamePanel gamePanel;
    private Timer gameLoopTimer;
    private PlayerInputSystem playerInputSystem;
    private List<GameSystem> logicSystems;
    private Entity playerEntity;
    private RenderSystem renderSystem; // Mantém uma referência para atualizar seu estado

    // --- Estado do Jogo e Gerenciadores ---
    private int currentLevelNumber = 1;
    private final SaveManager saveManager = new SaveManager();

    // --- Variáveis de Controle de Carregamento ---
    private LoadingPhase currentLoadingPhase = LoadingPhase.STARTING;
    private int loadingDelayCounter = 0;
    private static final int FRAMES_PER_LOADING_STEP = 30; // Aprox. 0.5s por etapa a 60fps

    /**
     * Ponto de entrada da aplicação.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Main().showStartMenu());
    }

    /**
     * Exibe o menu principal com as opções "New Game" e "Continue".
     */
    public void showStartMenu() {
        GameConstants.DEBUG_MODE_ON = true;
        if (playerInputSystem != null) {
            playerInputSystem.resetActionKeyStates();
        }
        while (true) {
            String[] options = {"New Game", "Continue"};
            int choice = JOptionPane.showOptionDialog(
                    null, "Welcome to Evo! What would you like to do?",
                    "Evo - Main Menu", JOptionPane.DEFAULT_OPTION,
                    JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

            switch (choice) {
                case 0: // New Game
                    loadLevel(1, null);
                    return;
                case 1: // Continue
                    String fileToLoad = showLoadGameDialog(null);
                    if (fileToLoad != null) {
                        loadGame(fileToLoad);
                        return;
                    }
                    break;
                default: // Fechar janela
                    System.exit(0);
                    return;
            }
        }
    }

    /**
     * Exibe um diálogo para o jogador escolher qual jogo salvo carregar.
     */
    private String showLoadGameDialog(Component parent) {
        List<String> saveFiles = saveManager.getAvailableSaveFiles();
        if (saveFiles.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "No saved games found.", "Load Game", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        Object selectedFile = JOptionPane.showInputDialog(
                parent, "Choose a save file to load:", "Load Game",
                JOptionPane.QUESTION_MESSAGE, null,
                saveFiles.toArray(), saveFiles.get(0));
        return (selectedFile != null) ? selectedFile.toString() : null;
    }

    /**
     * Salva o estado atual do jogo em um arquivo.
     */
    private void quickSave() {
        String filename = saveManager.generateSaveFilename(this.currentLevelNumber);
        GameState currentState = new GameState(this.currentLevelNumber, this.world);
        world.removeComponent(playerEntity, NotificationComponent.class);

        if (saveManager.saveStateToFile(currentState, filename)) {
            world.addComponent(playerEntity, new NotificationComponent("Game Saved!", NotificationComponent.NotificationType.SUCCESS, 3.0f));
        } else {
            world.addComponent(playerEntity, new NotificationComponent("Error: Could not save game.", NotificationComponent.NotificationType.WARNING, 3.0f));
        }
    }

    /**
     * Inicia o processo de carregamento de um jogo salvo a partir de um
     * arquivo.
     */
    private void loadGame(String filename) {
        GameState loadedState = saveManager.loadStateFromFile(filename);
        if (loadedState != null) {
            // Usa o método de carregamento imediato para pular a sequência visual
            loadLevelImmediately(loadedState.levelNumber, loadedState);
        } else {
            JOptionPane.showMessageDialog(gameWindow, "Could not load the selected save file.", "Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Carrega o save mais recente.
     */
    private void quickLoad() {
        List<String> saves = saveManager.getAvailableSaveFiles();
        if (saves.isEmpty()) {
            world.addComponent(playerEntity, new NotificationComponent("No save files found.", NotificationComponent.NotificationType.INFO, 3.0f));
            return;
        }
        String fileToLoad = saves.get(saves.size() - 1);
        loadGame(fileToLoad); // Reutiliza a lógica de loadGame
    }

    /**
     * Inicia a sequência de carregamento visual para um novo nível.
     */
    public void loadLevel(int levelNumber, GameState loadedState) {
        if (gameLoopTimer != null) {
            gameLoopTimer.stop();
        }
        if (loadedState == null && levelNumber > GameConstants.MAX_LEVELS) {
            showEndGameMessage();
            return;
        }
        // Se for um jogo salvo, usa o carregamento imediato.
        if (loadedState != null) {
            loadLevelImmediately(levelNumber, loadedState);
            return;
        }

        this.world = new World();
        this.currentLevelNumber = levelNumber;
        System.out.println("\n--- STARTING SEQUENTIAL LOAD FOR LEVEL " + this.currentLevelNumber + " ---");

        LevelLoader levelLoader = new LevelLoader();
        String levelPath = "assets/levels/level-" + this.currentLevelNumber + ".json";
        LevelConfig config = levelLoader.loadLevelFromResource(levelPath);
        if (config == null) {
            System.err.println("[CRITICAL] Failed to load level file: " + levelPath);
            return;
        }

        EntityFactory entityFactory = new EntityFactory(world);
        InputManager inputManager = new InputManager();
        this.renderSystem = new RenderSystem(world); // Armazena a referência
        this.gamePanel = new GamePanel(world, null, renderSystem, inputManager, entityFactory);

        // Informa ao RenderSystem em qual fase de carregamento estamos para ele desenhar a tela certa.
        // (Lembre-se de adicionar estes métodos setter no seu RenderSystem)
        renderSystem.setLoadingPhase(LoadingPhase.SHOWING_LOADING_SCREEN);
        renderSystem.setLevelConfig(config);

        if (gameWindow == null) {
            gameWindow = new GameWindow("Evo", gamePanel);
            gameWindow.display();
        } else {
            gameWindow.setTitle("Evo");
            gameWindow.switchPanel(this.gamePanel);
        }

        this.currentLoadingPhase = LoadingPhase.SHOWING_LOADING_SCREEN;
        this.loadingDelayCounter = 0;
        startNewGameLoop(entityFactory, config);
    }

    /**
     * Carrega um nível instantaneamente, pulando a sequência visual. Usado para
     * "Continuar".
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
        this.renderSystem = new RenderSystem(world);

        this.logicSystems = List.of(new PlayerInputSystem(world, inputManager, gameMap), new AISystem(world, gameMap), new CombatSystem(world, entityFactory), new InteractionSystem(world), new GameLogicSystem(world, entityFactory), new NotificationSystem(world));
        this.gamePanel = new GamePanel(world, gameMap, renderSystem, inputManager, entityFactory);
        gameWindow.setTitle("Evo - " + config.levelName);
        gameWindow.switchPanel(this.gamePanel);

        this.currentLoadingPhase = LoadingPhase.COMPLETE;
        renderSystem.setLoadingPhase(LoadingPhase.COMPLETE);
        startNewGameLoop(entityFactory, config);
    }

    /**
     * Inicia o timer principal do jogo (game loop). O loop agora diferencia
     * entre o modo de "carregamento" e o modo de "jogo".
     */
    private void startNewGameLoop(EntityFactory entityFactory, LevelConfig config) {
        this.gameLoopTimer = new Timer(GameConstants.GAME_LOOP_DELAY_MS, e -> {
            
            // --- INÍCIO DA LÓGICA CORRIGIDA ---

            // 1. Processa o input do jogador. Roda SEMPRE.
            if (playerInputSystem != null) {
                playerInputSystem.update();
            }

            // 2. Processa eventos do jogo (como 'iniciar gameplay' ou 'salvar'). Roda SEMPRE.
            handleGameEvents();

            // 3. Executa a lógica principal do jogo APENAS se o carregamento estiver completo.
            if (currentLoadingPhase == LoadingPhase.COMPLETE) {
                if (logicSystems != null) {
                     logicSystems.forEach(GameSystem::update); // IA, Combate, etc.
                }
                updateCameraForPlayer();
            } else {
                // 4. Se não, continua a sequência de carregamento.
                updateLoadingSequence(entityFactory, config);
            }

            // --- FIM DA LÓGICA CORRIGIDA ---

            // 5. Repinta a tela ao final de cada frame.
            if (gamePanel != null) {
                gamePanel.repaint();
            }
        });
        gameLoopTimer.start();
    }

    /**
     * Controla cada passo da sequência de carregamento visual, executando uma
     * etapa por vez.
     */
    private void updateLoadingSequence(EntityFactory entityFactory, LevelConfig config) {
        loadingDelayCounter++;
        int requiredFrames = (currentLoadingPhase == LoadingPhase.SHOWING_LEVEL_INTRO) ? Integer.MAX_VALUE : FRAMES_PER_LOADING_STEP;
        if (loadingDelayCounter < requiredFrames) {
            return;
        }

        loadingDelayCounter = 0;

        switch (currentLoadingPhase) {
            case SHOWING_LOADING_SCREEN:
                System.out.println("[Loader] Phase 1: Drawing Map...");
                GameMap gameMap = new GameMap(world, config);
                gamePanel.setGameMap(gameMap);
                currentLoadingPhase = LoadingPhase.MAP_LOADED;
                renderSystem.setLoadingPhase(currentLoadingPhase);
                break;
            case MAP_LOADED:
                System.out.println("[Loader] Phase 2: Spawning Player...");
                this.playerEntity = entityFactory.createPlayerCharacter(config.player);
                currentLoadingPhase = LoadingPhase.PLAYER_SPAWNED;
                renderSystem.setLoadingPhase(currentLoadingPhase);
                break;
            case PLAYER_SPAWNED:
                System.out.println("[Loader] Phase 3: Populating World...");
                new PopulationSystem(world, gamePanel.getGameMap(), entityFactory, config).update();
                currentLoadingPhase = LoadingPhase.WORLD_POPULATED;
                renderSystem.setLoadingPhase(currentLoadingPhase);
                break;
            case WORLD_POPULATED:
                System.out.println("[Loader] Phase 4: Awaiting player start...");

                // CRIA E ARMAZENA O PLAYER INPUT SYSTEM SEPARADAMENTE
                this.playerInputSystem = new PlayerInputSystem(world, (InputManager) gamePanel.getKeyListeners()[0], gamePanel.getGameMap());
                this.playerInputSystem.resetActionKeyStates(); // Garante que a flag do Enter esteja limpa

                // A lista de sistemas de lógica agora NÃO INCLUI o sistema de input
                this.logicSystems = List.of(
                        new AISystem(world, gamePanel.getGameMap()),
                        new PortalSystem(world), 
                        new CombatSystem(world, entityFactory),
                        new InteractionSystem(world),
                        new GameLogicSystem(world, entityFactory),
                        new NotificationSystem(world)
                );

                // Adiciona o componente que coloca o PlayerInputSystem no modo "intro"
//                if (GameConstants.DEBUG_MODE_ON) {
//                    System.out.println("[DEBUG] Main: Adicionando AwaitingInputComponent ao jogador.");
//                }
                world.addComponent(playerEntity, new AwaitingInputComponent());

                currentLoadingPhase = LoadingPhase.SHOWING_LEVEL_INTRO;
                renderSystem.setLoadingPhase(currentLoadingPhase);
                break;
            case SHOWING_LEVEL_INTRO:
                // O loop fica "preso" aqui. A transição para 'COMPLETE' é feita pelo input
                // do jogador, que é detectado em handleGameEvents.
                break;
        }
    }

    /**
     * Processa eventos globais do jogo, como salvar, carregar ou passar de
     * nível.
     */
    private void handleGameEvents() {
        if (playerEntity == null) {
            return;
        }
        

        if (world.hasComponent(playerEntity, StartGameplayRequestComponent.class)) {
//            if (GameConstants.DEBUG_MODE_ON) {
//                System.out.println("[DEBUG] Main: StartGameplayRequestComponent detectado! Iniciando jogo.");
//            }
            world.removeComponent(playerEntity, StartGameplayRequestComponent.class);

            // ADICIONE ESTA LINHA para remover o estado de espera
            world.removeComponent(playerEntity, AwaitingInputComponent.class);
            this.currentLoadingPhase = LoadingPhase.COMPLETE;
            renderSystem.setLoadingPhase(LoadingPhase.COMPLETE);

            // Pega o nome do nível do config para o título final da janela
            LevelConfig config = renderSystem.getLevelConfig(); // Supondo que você tenha um getter
            if (config != null) {
                gameWindow.setTitle("Evo - " + config.levelName);
            }

            // Adiciona a notificação final de início!
            world.addComponent(playerEntity, new NotificationComponent("Evolve!", NotificationComponent.NotificationType.SUCCESS, 2.0f));
        }

        // --- Outros eventos ---
        if (world.hasComponent(playerEntity, SaveGameRequestComponent.class)) {
            world.removeComponent(playerEntity, SaveGameRequestComponent.class);
            quickSave();
        } else if (world.hasComponent(playerEntity, LoadGameRequestComponent.class)) {
            world.removeComponent(playerEntity, LoadGameRequestComponent.class);
            quickLoad();
        } else if (world.hasComponent(playerEntity, GoToNextLevelComponent.class)) {
            world.removeComponent(playerEntity, GoToNextLevelComponent.class);
            loadLevel(this.currentLevelNumber + 1, null);
        }
    }

    /**
     * Atualiza a posição da câmera para seguir o jogador.
     */
    private void updateCameraForPlayer() {
        if (playerEntity == null || gamePanel == null || world == null || gamePanel.getGameMap() == null) {
            return;
        }
        PositionComponent playerPos = world.getComponent(playerEntity, PositionComponent.class);
        if (playerPos == null) {
            return;
        }

        int playerPixelX = playerPos.column * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2;
        int playerPixelY = playerPos.row * GameConstants.CELL_SIZE + GameConstants.CELL_SIZE / 2;
        int newCamX = playerPixelX - gamePanel.getWidth() / 2;
        int newCamY = playerPixelY - gamePanel.getHeight() / 2;

        gamePanel.setCameraPosition(newCamX, newCamY);
    }

    /**
     * Exibe a mensagem final do jogo e encerra a aplicação.
     */
    private void showEndGameMessage() {
        System.out.println("[INFO GAME] All levels completed! Congratulations!");
        JOptionPane.showMessageDialog(gameWindow,
                "You have completed the evolutionary journey!\n\nGame created by:\nFelipi",
                "End of Game!", JOptionPane.INFORMATION_MESSAGE);

        if (gameWindow != null) {
            gameWindow.dispose();
        }
        System.exit(0);
    }
}
