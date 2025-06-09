package game.evo.components;

import game.evo.ecs.Component;
import java.io.Serializable;

/**
 * A marker component that identifies an entity as a piece of food.
 * It can be consumed by the player for evolution points or health.
 */
public class FoodComponent implements Component , Serializable { // << 2. ADICIONE 'implements Serializable'

    private static final long serialVersionUID = 1L; 
    public int nutritionValue; // Quanto este alimento contribui (ou prejudica)
    public boolean isPoisonous; // <-- ADICIONADO: Define se o alimento causa dano

    // Construtor padrão para comida não venenosa (mantém a compatibilidade)
    public FoodComponent(int nutritionValue) {
        this.nutritionValue = nutritionValue;
        this.isPoisonous = false;
    }

    // <-- ADICIONADO: Novo construtor para definir se é venenoso
    public FoodComponent(int nutritionValue, boolean isPoisonous) {
        this.nutritionValue = nutritionValue;
        this.isPoisonous = isPoisonous;
    }
}