package game.evo.config;

import java.util.List;

/**
 * Represents the complete configuration for a single game level.
 * It now defines map dimensions and procedural population rules by biome,
 * making it fully data-driven.
 */
public class LevelConfig {
    public int levelNumber;
    public String levelName;
    public long proceduralSeed;
    public String description;
    
    // Configurações do mapa, lidas do arquivo JSON
    public int mapWidth;
    public int mapHeight;
    public double noiseScale;

    // Configuração inicial do jogador para este nível
    public PlayerConfig player;
    
    // --- MUDANÇA PRINCIPAL ---
    // A lista antiga de 'entities' foi substituída por uma lista de 'biomeRules'.
    public List<BiomeRuleConfig> biomeRules; 
    public List<TerrainRuleConfig> terrainRules; 
}
