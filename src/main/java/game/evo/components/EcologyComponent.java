package game.evo.components;

import game.evo.ecs.Component;
import java.io.Serializable;

/**
 * Stores ecological traits of a creature, such as its diet and temperament.
 */
public class EcologyComponent implements Component, Serializable {
    private static final long serialVersionUID = 1L;

    public enum DietaryType {
        HERBIVORE, // Come plantas
        CARNIVORE, // Come outras criaturas
        OMNIVORE   // Come ambos
    }

    public enum Temperament {
        SKITTISH, // Medroso: Foge de ameaças (mapeado de "medrosa")
        NEUTRAL,  // Passivo: Ignora outras criaturas a menos que seja atacado (mapeado de "neutra")
        AGGRESSIVE // Agressivo: Ataca alvos próximos (mapeado de "agressiva")
    }

    public DietaryType diet;
    public Temperament temperament;

    public EcologyComponent(DietaryType diet, Temperament temperament) {
        this.diet = diet;
        this.temperament = temperament;
    }
}