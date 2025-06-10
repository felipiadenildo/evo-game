package game.evo.world;

import game.evo.config.LevelConfig;
import game.evo.config.TerrainRuleConfig;
import game.evo.utils.GameConstants;
import game.evo.utils.OpenSimplexNoise;

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Gera o mapa proceduralmente usando uma técnica híbrida otimizada.
 * 1. Estampa as texturas base em alta resolução.
 * 2. Suaviza apenas as bordas entre biomas diferentes para performance máxima.
 */
public class MapGenerator {

    private final OpenSimplexNoise elevationNoise;
    private final OpenSimplexNoise moistureNoise;
    private final OpenSimplexNoise blendNoise;
    
    private final LevelConfig levelConfig;
    private TileType[][] logicalGrid;
    private BufferedImage mapImage;

    public MapGenerator(LevelConfig config) {
        this.levelConfig = config;
        this.elevationNoise = new OpenSimplexNoise(config.proceduralSeed);
        this.moistureNoise = new OpenSimplexNoise(config.proceduralSeed + 1);
        this.blendNoise = new OpenSimplexNoise(config.proceduralSeed + 2);
        System.out.println("[INFO MapGenerator] Initialized with seed: " + config.proceduralSeed);
    }
    
    /**
     * Orquestra a nova geração de mapa em duas etapas: estampar e suavizar.
     */
    public void generate(int widthInTiles, int heightInTiles, int CELL_SIZE, double noiseScale) {
        // Primeiro, gera a grade lógica de biomas
        this.logicalGrid = generateLogicalGridOnly(widthInTiles, heightInTiles, levelConfig.noiseScale);
        
        // Inicializa a imagem final do mapa
        this.mapImage = new BufferedImage(
            widthInTiles * GameConstants.CELL_SIZE, 
            heightInTiles * GameConstants.CELL_SIZE, 
            BufferedImage.TYPE_INT_RGB
        );
        
        // PASSO 1: Estampa as texturas base em alta resolução
        stampBaseTextures();
        
        // PASSO 2: Suaviza apenas as bordas entre os biomas
        if (GameConstants.MAP_BLENDING_ENABLED) {
            blendTileBorders();
        }

        System.out.println("[INFO MapGenerator] Hybrid map generated successfully.");
    }

    /**
     * PASSO 1: Preenche o mapa com as texturas base de cada bioma, sem mistura.
     */
    private void stampBaseTextures() {
        for (int r = 0; r < logicalGrid.length; r++) {
            for (int c = 0; c < logicalGrid[r].length; c++) {
                TileType biome = logicalGrid[r][c];
                Color color = getColorForBiome(biome); // Pega a cor base
                
                // Pinta todo o tile com a cor base
                for (int y = 0; y < GameConstants.CELL_SIZE; y++) {
                    for (int x = 0; x < GameConstants.CELL_SIZE; x++) {
                        // Poderíamos adicionar ruído de textura aqui se quiséssemos
                        mapImage.setRGB(c * GameConstants.CELL_SIZE + x, r * GameConstants.CELL_SIZE + y, color.getRGB());
                    }
                }
            }
        }
    }

    /**
     * PASSO 2: Percorre o mapa e suaviza apenas as fronteiras entre tiles de biomas diferentes.
     */
    private void blendTileBorders() {
        int blendWidth = 8; // Largura da zona de mistura (em pixels). Ajuste para bordas mais largas ou mais finas.

        for (int r = 0; r < logicalGrid.length; r++) {
            for (int c = 0; c < logicalGrid[r].length; c++) {
                
                // Verifica a borda com o vizinho da DIREITA
                if (c + 1 < logicalGrid[r].length) {
                    TileType biomeA = logicalGrid[r][c];
                    TileType biomeB = logicalGrid[r][c+1];
                    if (biomeA != biomeB) {
                        blendVerticalBorder(c + 1, r, blendWidth, biomeA, biomeB);
                    }
                }

                // Verifica a borda com o vizinho de BAIXO
                if (r + 1 < logicalGrid.length) {
                    TileType biomeA = logicalGrid[r][c];
                    TileType biomeC = logicalGrid[r+1][c];
                    if (biomeA != biomeC) {
                        blendHorizontalBorder(c, r + 1, blendWidth, biomeA, biomeC);
                    }
                }
            }
        }
    }
    
    private void blendVerticalBorder(int tileCol, int tileRow, int blendWidth, TileType biomeLeft, TileType biomeRight) {
        int borderX = tileCol * GameConstants.CELL_SIZE;
        int startY = tileRow * GameConstants.CELL_SIZE;
        
        Color colorLeft = getColorForBiome(biomeLeft);
        Color colorRight = getColorForBiome(biomeRight);
        
        for (int i = 0; i < blendWidth * 2; i++) {
            int currentX = borderX - blendWidth + i;
            
            // Fator de mistura baseado na distância da borda
            double blendFactor = (double) i / (blendWidth * 2 - 1);
            
            int blendedColor = lerpColor(colorLeft.getRGB(), colorRight.getRGB(), blendFactor);
            
            for (int y = 0; y < GameConstants.CELL_SIZE; y++) {
                mapImage.setRGB(currentX, startY + y, blendedColor);
            }
        }
    }

    private void blendHorizontalBorder(int tileCol, int tileRow, int blendWidth, TileType biomeTop, TileType biomeBottom) {
        int borderY = tileRow * GameConstants.CELL_SIZE;
        int startX = tileCol * GameConstants.CELL_SIZE;
        
        Color colorTop = getColorForBiome(biomeTop);
        Color colorBottom = getColorForBiome(biomeBottom);
        
        for (int i = 0; i < blendWidth * 2; i++) {
            int currentY = borderY - blendWidth + i;
            
            double blendFactor = (double) i / (blendWidth * 2 - 1);
            int blendedColor = lerpColor(colorTop.getRGB(), colorBottom.getRGB(), blendFactor);
            
            for (int x = 0; x < GameConstants.CELL_SIZE; x++) {
                mapImage.setRGB(startX + x, currentY, blendedColor);
            }
        }
    }

    // --- MÉTODOS AUXILIARES (a maioria já existe no seu código) ---

    public TileType[][] generateLogicalGridOnly(int widthInTiles, int heightInTiles, double noiseScale) {
        TileType[][] grid = new TileType[heightInTiles][widthInTiles];
        for (int r = 0; r < heightInTiles; r++) {
            for (int c = 0; c < widthInTiles; c++) {
                double e = fractalNoise(c, r, elevationNoise, 5, 2.0, 0.5, noiseScale / GameConstants.CELL_SIZE);
                double m = fractalNoise(c, r, moistureNoise, 5, 2.0, 0.5, noiseScale / GameConstants.CELL_SIZE * 0.75);
                grid[r][c] = getTileTypeForBiome(e, m);
            }
        }
        return grid;
    }

    private double fractalNoise(double x, double y, OpenSimplexNoise noise, int octaves, double lacunarity, double persistence, double scale) {
        double total = 0, frequency = 1, amplitude = 1, maxValue = 0;
        for (int i = 0; i < octaves; i++) {
            total += noise.eval(x * frequency / scale, y * frequency / scale) * amplitude;
            maxValue += amplitude;
            amplitude *= persistence;
            frequency *= lacunarity;
        }
        return total / maxValue;
    }

    private int lerpColor(int c1, int c2, double factor) {
        if (factor > 1.0) factor = 1.0; if (factor < 0.0) factor = 0.0;
        int r = (int) (((c1 >> 16) & 0xFF) * (1.0 - factor) + ((c2 >> 16) & 0xFF) * factor);
        int g = (int) (((c1 >> 8) & 0xFF) * (1.0 - factor) + ((c2 >> 8) & 0xFF) * factor);
        int b = (int) ((c1 & 0xFF) * (1.0 - factor) + (c2 & 0xFF) * factor);
        return (r << 16) | (g << 8) | b;
    }

    private TileType getTileTypeForBiome(double e, double m) {
        for (TerrainRuleConfig rule : levelConfig.terrainRules) {
            if (e < rule.maxElevation) {
                if (rule.minMoisture != null && m < rule.minMoisture) continue;
                if (rule.maxMoisture != null && m > rule.maxMoisture) continue;
                try {
                    return TileType.valueOf(rule.biome);
                } catch (IllegalArgumentException ex) { return TileType.UNKNOWN; }
            }
        }
        return TileType.GRASSLAND;
    }

    private Color getColorForBiome(TileType tileType) {
        // Use a sua paleta de cores ajustada aqui
        switch (tileType) {
            case OCEAN_DEEP:    return new Color(25, 45, 90);
            case OCEAN_SHALLOW: return new Color(60, 125, 180);
            case BEACH_SAND:    return new Color(225, 205, 160);
            case GRASSLAND:     return new Color(115, 165, 80);
            case FOREST:        return new Color(60, 110, 50);
            case JUNGLE:        return new Color(40, 80, 45);
            case DESERT:        return new Color(210, 175, 125);
            case MOUNTAIN_ROCK: return new Color(130, 125, 120);
            case MOUNTAIN_SNOW: return new Color(235, 240, 245);
            default:            return Color.MAGENTA;
        }
    }
    
    // Getters
    public TileType[][] getLogicalGrid() { return logicalGrid; }
    public BufferedImage getMapImage() { return mapImage; }
}