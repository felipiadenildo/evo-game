package game.evo.components;

import game.evo.ecs.Component;
import java.io.Serializable;

/**
 * A marker component indicating that an entity is controlled by the player.
 * It does not hold any data itself, its presence on an entity is what matters.
 */
public class PlayerControlledComponent implements Component , Serializable { // << 2. ADICIONE 'implements Serializable'

    private static final long serialVersionUID = 1L;     // This component is a "tag" or "marker" and doesn't need fields for now.
    // Its presence on an entity signifies player control.
}