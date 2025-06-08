package game.evo.components;

import game.evo.ecs.Component;
import java.io.Serializable;

/**
 * A marker component that identifies an entity as a piece of food.
 * It can be consumed by the player for evolution points or health.
 */
public class FoodComponent implements Component , Serializable { // << 2. ADICIONE 'implements Serializable'

    private static final long serialVersionUID = 1L; 
    public int nutritionValue; // How much this food contributes to evolution

    public FoodComponent(int nutritionValue) {
        this.nutritionValue = nutritionValue;
    }
}