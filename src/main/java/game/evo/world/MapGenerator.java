package game.evo.world;

import game.evo.config.LevelConfig;
import game.evo.config.TerrainRuleConfig;
import game.evo.utils.GameConstants;
import game.evo.utils.OpenSimplexNoise; // Não precisa mais do AssetManager aqui

import java.awt.Color;
import java.awt.image.BufferedImage;

/**
 * Gera o mapa proceduralmente usando múltiplos ruídos para criar biomas com
 * cores e texturas procedurais, e misturando-os suavemente nas bordas.
 */
public class MapGenerator {

    private final OpenSimplexNoise elevationNoise;
    private final OpenSimplexNoise moistureNoise;
    private final OpenSimplexNoise blendNoise;
    private final OpenSimplexNoise textureNoise; // <-- REINTRODUZIDO: Para a textura fina
    private LevelConfig levelConfig = null;
    private TileType[][] logicalGrid;
    private BufferedImage mapImage;

    public MapGenerator(LevelConfig config) {
        this.levelConfig = config; // Armazena a configuração
        this.elevationNoise = new OpenSimplexNoise(config.proceduralSeed);
        this.moistureNoise = new OpenSimplexNoise(config.proceduralSeed + 1);
        this.blendNoise = new OpenSimplexNoise(config.proceduralSeed + 2);
        this.textureNoise = new OpenSimplexNoise(config.proceduralSeed + 3);
        System.out.println("[INFO MapGenerator] Initialized with seed: " + config.proceduralSeed);
    }

    /**
     * Método de geração principal, modificado para gerar e misturar cores
     * procedurais.
     */
    public void generate(int widthInTiles, int heightInTiles, int resolutionFactor, double noiseScale) {
        int imageWidth = widthInTiles * resolutionFactor;
        int imageHeight = heightInTiles * resolutionFactor;

        this.logicalGrid = new TileType[heightInTiles][widthInTiles];
        this.mapImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);

        // Pré-calcula as cores nos cantos de cada tile
        Color[][] cornerColors = new Color[heightInTiles + 1][widthInTiles + 1];
        for (int r = 0; r <= heightInTiles; r++) {
            for (int c = 0; c <= widthInTiles; c++) {
                cornerColors[r][c] = calculateColorAt(c * resolutionFactor, r * resolutionFactor, noiseScale);
            }
        }

        // Itera através dos tiles (não mais dos pixels)
        for (int r = 0; r < heightInTiles; r++) {
            for (int c = 0; c < widthInTiles; c++) {

                // Pega as 4 cores dos cantos do tile atual
                Color c00 = cornerColors[r][c];       // Top-Left
                Color c10 = cornerColors[r][c + 1];     // Top-Right
                Color c01 = cornerColors[r + 1][c];     // Bottom-Left
                Color c11 = cornerColors[r + 1][c + 1];   // Bottom-Right

                // Itera através dos pixels DENTRO do tile atual
                for (int y = 0; y < resolutionFactor; y++) {
                    for (int x = 0; x < resolutionFactor; x++) {
                        // Fatores de interpolação (0.0 a 1.0)
                        double tx = (double) x / resolutionFactor;
                        double ty = (double) y / resolutionFactor;

                        // Interpola entre as cores de cima e as de baixo
                        int topColor = lerpColor(c00.getRGB(), c10.getRGB(), tx);
                        int bottomColor = lerpColor(c01.getRGB(), c11.getRGB(), tx);

                        // Interpola entre o resultado de cima e o de baixo
                        int finalColor = lerpColor(topColor, bottomColor, ty);

                        mapImage.setRGB(c * resolutionFactor + x, r * resolutionFactor + y, finalColor);
                    }
                }

                // Define o tipo lógico do tile baseado no seu centro
                logicalGrid[r][c] = getTileTypeForBiome(
                        fractalNoise(c * resolutionFactor, r * resolutionFactor, elevationNoise, 5, 2.0, 0.5, noiseScale),
                        fractalNoise(c * resolutionFactor, r * resolutionFactor, moistureNoise, 5, 2.0, 0.5, noiseScale * 0.75)
                );
            }
        }
        System.out.println("[INFO MapGenerator] Procedural map generated using tile-based interpolation.");
    }

    /**
     * NOVO MÉTODO AUXILIAR: Calcula a cor final para uma coordenada específica
     * no mundo. Esta é a lógica que antes estava dentro do loop de pixels.
     */
    private Color calculateColorAt(int x, int y, double noiseScale) {
        double elevationFeatureSize = Math.max(1.0, noiseScale);
        double moistureFeatureSize = Math.max(1.0, noiseScale * 0.75);
        double blendFeatureSize = 64.0;
        double textureFeatureSize = 4.0;
        int blendOffset = 16;

        double mainElevation = fractalNoise(x, y, elevationNoise, 5, 2.0, 0.5, elevationFeatureSize);
        double mainMoisture = fractalNoise(x, y, moistureNoise, 5, 2.0, 0.5, moistureFeatureSize);
        TileType mainBiome = getTileTypeForBiome(mainElevation, mainMoisture);

        if (!GameConstants.MAP_BLENDING_ENABLED) {
            Color baseColor = getColorForBiome(mainBiome);
            double textureValue = textureNoise.eval(x / textureFeatureSize, y / textureFeatureSize);
            return applyTexture(baseColor, textureValue);
        }

        double blendElevation = fractalNoise(x + blendOffset, y + blendOffset, elevationNoise, 5, 2.0, 0.5, elevationFeatureSize);
        double blendMoisture = fractalNoise(x + blendOffset, y + blendOffset, moistureNoise, 5, 2.0, 0.5, moistureFeatureSize);
        TileType blendBiome = getTileTypeForBiome(blendElevation, blendMoisture);

        Color color1 = getColorForBiome(mainBiome);
        Color color2 = getColorForBiome(blendBiome);

        double textureValue = textureNoise.eval(x / textureFeatureSize, y / textureFeatureSize);
        Color texturedColor1 = applyTexture(color1, textureValue);
        Color texturedColor2 = applyTexture(color2, textureValue);

        double blendFactor = (blendNoise.eval(x / blendFeatureSize, y / blendFeatureSize) + 1) / 2.0;

        int finalRGB = lerpColor(texturedColor1.getRGB(), texturedColor2.getRGB(), blendFactor);
        return new Color(finalRGB);

    }

    /**
     * Calcula o ruído fractal (fBm) somando múltiplas oitavas de ruído Simplex.
     *
     * @param x Coordenada X
     * @param y Coordenada Y
     * @param noise O gerador de ruído a ser usado
     * @param octaves O número de camadas de ruído a serem somadas
     * @param lacunarity A rapidez com que a frequência aumenta a cada oitava
     * (geralmente 2.0)
     * @param persistence A rapidez com que a amplitude diminui a cada oitava
     * (geralmente 0.5)
     * @param scale A escala geral do ruído
     * @return Um valor de ruído fractal entre -1.0 e 1.0.
     */
    private double fractalNoise(double x, double y, OpenSimplexNoise noise, int octaves, double lacunarity, double persistence, double scale) {
        double total = 0;
        double frequency = 1;
        double amplitude = 1;
        double maxValue = 0;  // Usado para normalizar o resultado para o intervalo [-1, 1]

        for (int i = 0; i < octaves; i++) {
            total += noise.eval(x * frequency / scale, y * frequency / scale) * amplitude;

            maxValue += amplitude;

            amplitude *= persistence;
            frequency *= lacunarity;
        }

        return total / maxValue;
    }

    public TileType[][] generateLogicalGridOnly(int widthInTiles, int heightInTiles, double noiseScale) {
        TileType[][] grid = new TileType[heightInTiles][widthInTiles];
        for (int r = 0; r < heightInTiles; r++) {
            for (int c = 0; c < widthInTiles; c++) {
                grid[r][c] = getTileTypeForBiome(
                        fractalNoise(c * GameConstants.CELL_SIZE, r * GameConstants.CELL_SIZE, elevationNoise, 5, 2.0, 0.5, noiseScale),
                        fractalNoise(c * GameConstants.CELL_SIZE, r * GameConstants.CELL_SIZE, moistureNoise, 5, 2.0, 0.5, noiseScale * 0.75)
                );
            }
        }
        return grid;
    }

    /**
     * REINTRODUZIDO: Retorna uma cor base para cada tipo de bioma.
     */
    private Color getColorForBiome(TileType tileType) {
        switch (tileType) {
            // Águas profundas e mais escuras
            case OCEAN_DEEP:
                return new Color(25, 45, 90);
            // Águas costeiras, mais claras e com um toque de verde/ciano
            case OCEAN_SHALLOW:
                return new Color(60, 125, 180);
            // Areia clara, nem muito amarela, nem muito branca
            case BEACH_SAND:
                return new Color(225, 205, 160);
            // Grama mais natural, menos "neon"
            case GRASSLAND:
                return new Color(115, 165, 80);
            // Floresta densa, um verde mais escuro e rico
            case FOREST:
                return new Color(60, 110, 50);
            // Selva úmida, um verde ainda mais escuro e profundo
            case JUNGLE:
                return new Color(40, 80, 45);
            // Cor de areia/argila para o deserto
            case DESERT:
                return new Color(210, 175, 125);
            // Rocha da montanha, um cinza com um leve tom de marrom
            case MOUNTAIN_ROCK:
                return new Color(130, 125, 120);
            // Neve, quase branca mas com uma sombra sutil
            case MOUNTAIN_SNOW:
                return new Color(235, 240, 245);
            default:
                return Color.MAGENTA; // Cor de erro
        }
    }

    /**
     * REINTRODUZIDO E MODIFICADO: Aplica uma variação de cor baseada no ruído
     * de textura.
     */
    private Color applyTexture(Color baseColor, double textureValue) {
        int r = baseColor.getRed();
        int g = baseColor.getGreen();
        int b = baseColor.getBlue();

        // A variação de cor que o ruído irá causar
        int variation = 15;

        // Aplica a variação a um canal de cor para dar um efeito (ex: verde para grama)
        g += (int) (textureValue * variation);

        // Garante que os valores de cor permaneçam no intervalo válido [0, 255]
        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));

        return new Color(r, g, b);
    }

    // O método lerpColor permanece o mesmo, é perfeito para esta tarefa
    private int lerpColor(int c1, int c2, double factor) {
        if (factor > 1.0) {
            factor = 1.0;
        }
        if (factor < 0.0) {
            factor = 0.0;
        }

        int r1 = (c1 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = c1 & 0xFF;

        int r2 = (c2 >> 16) & 0xFF;
        int g2 = (c2 >> 8) & 0xFF;
        int b2 = c2 & 0xFF;

        int r = (int) (r1 * (1.0 - factor) + r2 * factor);
        int g = (int) (g1 * (1.0 - factor) + g2 * factor);
        int b = (int) (b1 * (1.0 - factor) + b2 * factor);

        return (r << 16) | (g << 8) | b;
    }

    /**
     * MÉTODO COMPLETAMENTE REESCRITO Determina o tipo de tile iterando através
     * das regras definidas no JSON. A ordem das regras no JSON é crucial.
     */
    private TileType getTileTypeForBiome(double e, double m) {
        // Percorre as regras de terreno carregadas do JSON
        for (TerrainRuleConfig rule : levelConfig.terrainRules) {
            // Verifica se a elevação 'e' se encaixa na regra atual
            if (e < rule.maxElevation) {
                // Se a regra também tem uma condição de umidade...
                if (rule.minMoisture != null && m < rule.minMoisture) {
                    continue; // ...e a umidade não bate, pula para a próxima regra.
                }
                if (rule.maxMoisture != null && m > rule.maxMoisture) {
                    continue; // ...e a umidade não bate, pula para a próxima regra.
                }

                // Se todas as condições bateram, encontramos nosso bioma.
                try {
                    return TileType.valueOf(rule.biome);
                } catch (IllegalArgumentException ex) {
                    System.err.println("Biome '" + rule.biome + "' no JSON é inválido.");
                    return TileType.UNKNOWN; // Retorna um tipo de erro
                }
            }
        }
        return TileType.GRASSLAND; // Um bioma padrão caso nenhuma regra seja encontrada
    }

    public TileType[][] getLogicalGrid() {
        return logicalGrid;
    }

    public BufferedImage getMapImage() {
        return mapImage;
    }
}
