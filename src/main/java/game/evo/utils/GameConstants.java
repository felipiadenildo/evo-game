package game.evo.utils;

public final class GameConstants {

    private GameConstants() {
    }

    // Map and Tile Dimensions
    public static final int CELL_SIZE = 64;
    public static final int DEFAULT_MAP_WIDTH_TILES = 30;
    public static final int DEFAULT_MAP_HEIGHT_TILES = 20;

    // Screen/View Dimensions (in tiles)
    public static final int SCREEN_WIDTH_TILES = 20;
    public static final int SCREEN_HEIGHT_TILES = 15;

    // Rendering Layers
    public static final int LAYER_BACKGROUND = 0;
    public static final int LAYER_ENVIRONMENT = 1;
    public static final int LAYER_ITEMS = 2;
    public static final int LAYER_PLAYER = 3;        // << NOVA CONSTANTE
    public static final int LAYER_NPC = 3;           // NPCs podem estar na mesma camada que o player
    public static final int LAYER_PROJECTILES = 4;   // << NOVA CONSTANTE (para o futuro Fogo)
    public static final int LAYER_EFFECTS = 5;       // Era 4, ajustado
    public static final int LAYER_UI = 6;            // Era 5, ajustado

    // Game Loop
    public static final int GAME_LOOP_DELAY_MS = 100;

    // Caminho para os Assets (recursos como imagens, sons)
    // O caminho começa a partir da raiz do classpath (que em projetos Maven é tipicamente 'src/main/resources')
    public static final String ASSETS_PATH = "assets/imgs/";

    public static boolean DEBUG_MODE_ON = false; // Por padrão, desligado
    
    // --- Gameplay Rules ---
    public static final int EVOLUTION_POINTS_FOR_PORTAL = 30; // Pontos necessários para o portal aparecer
    public static final int MAX_LEVELS = 5;
}
