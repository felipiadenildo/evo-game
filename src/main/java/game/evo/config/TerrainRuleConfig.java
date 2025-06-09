package game.evo.config;

import java.io.Serializable;

/**
 * Representa uma única regra para definir um bioma baseado em limiares
 * de elevação e, opcionalmente, de umidade.
 */
public class TerrainRuleConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    public String biome;         // O nome do bioma (ex: "OCEAN_DEEP")
    public double maxElevation;  // O limiar máximo de elevação para esta regra
    
    // Propriedades opcionais para regras mais complexas
    public Double minMoisture = null; // Umidade mínima para aplicar esta regra
    public Double maxMoisture = null; // Umidade máxima para aplicar esta regra
}