package game.evo.world;

import game.evo.components.CollisionComponent;
import game.evo.components.PositionComponent;
import game.evo.config.LevelConfig;
import game.evo.ecs.Entity;
import game.evo.ecs.World;
import game.evo.utils.GameConstants;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Gerencia o mapa do jogo, orquestrando a geração procedural e o armazenamento
 * da imagem visual e da grade lógica.
 * REFATORADO: Agora implementa um sistema de cache para acelerar o carregamento.
 */
public class GameMap {
    private final int widthInTiles;
    private final int heightInTiles;
    private final World world;

    private TileType[][] logicalGrid;
    private BufferedImage mapImage;

    /**
     * Constrói e gera um GameMap.
     * Primeiro, tenta carregar o mapa de um arquivo de cache. Se não encontrar,
     * gera um novo mapa proceduralmente e o salva em cache para uso futuro.
     * @param world A instância do mundo ECS.
     * @param config A configuração completa do nível, contendo a seed e outras informações.
     */
    public GameMap(World world, LevelConfig config) {
        if (world == null || config == null) {
            throw new IllegalArgumentException("World e LevelConfig não podem ser nulos para a inicialização do GameMap.");
        }
        this.widthInTiles = config.mapWidth;
        this.heightInTiles = config.mapHeight;
        this.world = world;

        // --- LÓGICA DE CACHE ---
        String cacheFileName = "cache/map_seed_" + config.proceduralSeed + ".png";
        File cacheFile = new File(cacheFileName);

        if (cacheFile.exists() && !GameConstants.DEBUG_MODE_ON) { // O cache é ignorado em modo debug para forçar a regeneração
            System.out.println("[INFO GameMap] Loading map from cache file: " + cacheFileName);
            try {
                // Carrega a imagem do mapa diretamente do arquivo de cache
                this.mapImage = ImageIO.read(cacheFile);

                // A grade lógica ainda precisa ser gerada para colisões e biomas
                MapGenerator logicalGenerator = new MapGenerator(config);
                this.logicalGrid = logicalGenerator.generateLogicalGridOnly(config.mapWidth, config.mapHeight, config.noiseScale);

            } catch (IOException e) {
                System.err.println("Failed to load map from cache. Regenerating...");
                generateAndCacheMap(config);
            }
        } else {
            System.out.println("[INFO GameMap] No cache found or debug mode is on. Generating new map...");
            generateAndCacheMap(config);
        }

        createEntitiesForSpecialTiles();
    }
    
    
    /**
     * Método auxiliar que centraliza a lógica de gerar um novo mapa e salvá-lo em cache.
     */
    private void generateAndCacheMap(LevelConfig config) {
        MapGenerator generator = new MapGenerator(config);
        generator.generate(config.mapWidth, config.mapHeight, GameConstants.CELL_SIZE, config.noiseScale);
        
        this.mapImage = generator.getMapImage();
        this.logicalGrid = generator.getLogicalGrid();

        // Salva a imagem recém-gerada no cache para uso futuro
        try {
            File cacheDir = new File("cache");
            if (!cacheDir.exists()) {
                cacheDir.mkdirs(); // Cria a pasta 'cache' se ela não existir
            }
            String cacheFileName = "cache/map_seed_" + config.proceduralSeed + ".png";
            ImageIO.write(this.mapImage, "PNG", new File(cacheFileName));
            System.out.println("[INFO GameMap] Map saved to cache: " + cacheFileName);
        } catch (IOException e) {
            System.err.println("Error saving map to cache file.");
            e.printStackTrace();
        }
    }

    /**
     * Itera através da grade lógica gerada e cria entidades ECS
     * para quaisquer tiles que não sejam "andáveis", para que possam fazer parte do sistema de colisão.
     */
    private void createEntitiesForSpecialTiles() {
        if (logicalGrid == null) {
             System.err.println("[ERROR GameMap] Logical grid is null. Cannot create special tile entities.");
             return;
        }
        for (int r = 0; r < heightInTiles; r++) {
            for (int c = 0; c < widthInTiles; c++) {
                TileType currentType = logicalGrid[r][c];
                
                if (currentType != null && !currentType.isWalkable) {
                    Entity obstacleEntity = world.createEntity();
                    world.addComponent(obstacleEntity, new PositionComponent(r, c));
                    world.addComponent(obstacleEntity, new CollisionComponent());
                }
            }
        }
    }

    /**
     * Obtém o tipo de tile lógico nas coordenadas da grade especificadas.
     * @param row A linha do tile.
     * @param column A coluna do tile.
     * @return O TileType nessa posição, ou TileType.UNKNOWN se fora dos limites.
     */
    public TileType getLogicalTileType(int row, int column) {
        if (logicalGrid != null && row >= 0 && row < heightInTiles && column >= 0 && column < widthInTiles) {
            return logicalGrid[row][column];
        }
        return TileType.UNKNOWN;
    }
    
    /**
     * Retorna a imagem pré-renderizada de todo o mapa.
     * @return O BufferedImage do mapa.
     */
    public BufferedImage getMapImage() {
        return this.mapImage;
    }

    /**
     * Retorna a matriz 2D que representa a grade lógica do mapa.
     * @return A grade lógica TileType[][].
     */
    public TileType[][] getLogicalGrid() {
        return this.logicalGrid;
    }
    
    public int getWidthInTiles() {
        return widthInTiles;
    }

    public int getHeightInTiles() {
        return heightInTiles;
    }
}