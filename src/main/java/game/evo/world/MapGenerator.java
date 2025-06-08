package game.evo.world;

import game.evo.utils.GameConstants;
import game.evo.utils.OpenSimplexNoise;
import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Procedurally generates the game map using a dual-noise system (elevation and moisture)
 * to create diverse biomes. It pre-renders the entire map into a single high-resolution
 * image with procedural texturing for a more natural look.
 */
public class MapGenerator {

    private final OpenSimplexNoise elevationNoise;
    private final OpenSimplexNoise moistureNoise;
    private final OpenSimplexNoise textureNoise;

    private TileType[][] logicalGrid;
    private BufferedImage mapImage;

    /**
     * Constructor for the MapGenerator.
     * @param seed The main seed for the world. Different seeds for elevation, moisture,
     * and texture will be derived from this main seed to ensure consistency.
     */
    public MapGenerator(long seed) {
        this.elevationNoise = new OpenSimplexNoise(seed);
        this.moistureNoise = new OpenSimplexNoise(seed + 1);
        this.textureNoise = new OpenSimplexNoise(seed + 2);
        System.out.println("[INFO MapGenerator] Initialized with main seed: " + seed);
    }

    /**
     * --- MÉTODO CORRIGIDO ---
     * The main generation method. Creates both the logical grid and the visual image.
     * @param widthInTiles Width of the map in logical grid cells.
     * @param heightInTiles Height of the map in logical grid cells.
     * @param resolutionFactor How many pixels per logical grid cell (e.g., GameConstants.CELL_SIZE).
     * @param noiseScale Controls the "zoom" level of the noise pattern. Smaller values = larger features.
     */
    public void generate(int widthInTiles, int heightInTiles, int resolutionFactor, double noiseScale) {
        int imageWidth = widthInTiles * resolutionFactor;
        int imageHeight = heightInTiles * resolutionFactor;

        this.logicalGrid = new TileType[heightInTiles][widthInTiles];
        this.mapImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);

        // --- AQUI USAMOS O NOVO PARÂMETRO noiseScale ---
        // Se a escala do ruído for muito pequena, o mapa ficará muito "zoomado" e sem detalhes.
        // Garantimos que seja um valor razoável.
        double elevationFeatureSize = Math.max(1.0, noiseScale);
        double moistureFeatureSize = Math.max(1.0, noiseScale * 0.75); // A umidade pode ter uma escala um pouco diferente
        double textureFeatureSize = 4.0; // A textura é sempre de alta frequência

        for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
                
                // Calcula o valor do ruído para a elevação e umidade
                double elevation = elevationNoise.eval(x / elevationFeatureSize, y / elevationFeatureSize);
                double moisture = moistureNoise.eval(x / moistureFeatureSize, y / moistureFeatureSize);

                TileType tileType = getTileTypeForBiome(elevation, moisture);
                Color baseColor = getColorForBiome(tileType);

                // Aplica textura
                double textureValue = textureNoise.eval(x / textureFeatureSize, y / textureFeatureSize);
                Color finalColor = applyTexture(baseColor, textureValue, tileType);
                
                mapImage.setRGB(x, y, finalColor.getRGB());

                // Atualiza a grade lógica uma vez por célula
                if (x % resolutionFactor == 0 && y % resolutionFactor == 0) {
                    int tileRow = y / resolutionFactor;
                    int tileCol = x / resolutionFactor;
                    if (tileRow < heightInTiles && tileCol < widthInTiles) {
                        logicalGrid[tileRow][tileCol] = tileType;
                    }
                }
            }
        }
        System.out.println("[INFO MapGenerator] Procedural map generated (Image: " + imageWidth + "x" + imageHeight + "px, Logic Grid: " + widthInTiles + "x" + heightInTiles + ").");
    }

    /**
     * Determines the biome/TileType based on elevation and moisture values.
     */
    private TileType getTileTypeForBiome(double e, double m) {
        if (e < -0.4) return TileType.OCEAN_DEEP;
        if (e < -0.3) return TileType.OCEAN_SHALLOW;
        if (e < -0.25) return TileType.BEACH_SAND;

        if (e > 0.6) {
            if (m < -0.2) return TileType.MOUNTAIN_ROCK;
            return TileType.MOUNTAIN_SNOW;
        }

        if (e > 0.3) {
            if (m > 0.3) return TileType.JUNGLE;
            return TileType.FOREST;
        }

        if (m < -0.4) return TileType.DESERT;
        if (m > 0.4) return TileType.GRASSLAND; 
        
        return TileType.GRASSLAND;
    }
    
    /**
     * Gets a base color for a given biome type.
     */
    private Color getColorForBiome(TileType tileType) {
        switch (tileType) {
            case OCEAN_DEEP:    return new Color(0, 51, 102);
            case OCEAN_SHALLOW: return new Color(0, 102, 153);
            case BEACH_SAND:    return new Color(210, 180, 140);
            case DESERT:        return new Color(230, 210, 170);
            case GRASSLAND:     return new Color(124, 252, 0);
            case FOREST:        return new Color(34, 139, 34);
            case JUNGLE:        return new Color(0, 100, 0);
            case MOUNTAIN_ROCK: return new Color(139, 137, 137);
            case MOUNTAIN_SNOW: return Color.WHITE;
            case TUNDRA:        return new Color(200, 220, 220);
            default:            return Color.MAGENTA;
        }
    }
    
    /**
     * Modifies a base color with a high-frequency noise value to create texture.
     */
    private Color applyTexture(Color baseColor, double textureValue, TileType type) {
        int r = baseColor.getRed();
        int g = baseColor.getGreen();
        int b = baseColor.getBlue();

        int variation = 15;
        switch (type) {
            case BEACH_SAND, DESERT -> {
                variation = 10;
                r += (int) (textureValue * variation);
                g += (int) (textureValue * variation);
                b += (int) (textureValue * variation);
            }
            case OCEAN_DEEP, OCEAN_SHALLOW -> {
                variation = 20;
                r -= (int) (Math.abs(textureValue) * variation);
                g -= (int) (Math.abs(textureValue) * variation);
                b -= (int) (Math.abs(textureValue) * variation);
            }
            default -> g += (int) (textureValue * variation);
        }

        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));

        return new Color(r, g, b);
    }

    // Getters
    public TileType[][] getLogicalGrid() { return logicalGrid; }
    public BufferedImage getMapImage() { return mapImage; }
}
