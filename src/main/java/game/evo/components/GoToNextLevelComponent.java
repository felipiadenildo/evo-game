package game.evo.components;

import game.evo.ecs.Component;
import java.io.Serializable;

/**
 * A temporary "event" component added to the player when they interact with a portal.
 * The main game loop will detect this component and trigger the level transition.
 */
public class GoToNextLevelComponent implements Component , Serializable { // << 2. ADICIONE 'implements Serializable'

    private static final long serialVersionUID = 1L;     // This is a marker component. Its presence signals an event.
}