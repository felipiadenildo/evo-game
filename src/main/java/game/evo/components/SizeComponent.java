package game.evo.components;

import game.evo.ecs.Component;
import java.io.Serializable;

/**
 * Stores the size attribute of a creature.
 * Size is a key factor in the game's combat and evolution mechanics.
 */
public class SizeComponent implements Component , Serializable { // << 2. ADICIONE 'implements Serializable'

    private static final long serialVersionUID = 1L; 
    public int size;

    public SizeComponent(int size) {
        // We can add validation to ensure size is positive
        this.size = Math.max(1, size);
    }
}