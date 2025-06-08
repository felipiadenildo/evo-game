package game.evo.components;

import game.evo.ecs.Component;
import java.io.Serializable;

/**
 * Stores the current direction an entity is facing.
 */
public class DirectionComponent implements Component , Serializable { // << 2. ADICIONE 'implements Serializable'

    private static final long serialVersionUID = 1L; 
    public enum Direction {
        UP, DOWN, LEFT, RIGHT
    }

    public Direction facing = Direction.DOWN; // Default direction
}