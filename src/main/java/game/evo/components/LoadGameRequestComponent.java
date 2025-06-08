package game.evo.components;

import game.evo.ecs.Component;
import java.io.Serializable;

/**
 * A temporary marker component added to the player entity when the user
 * requests to load a saved game. The main game loop detects this component,
 * performs the load action, and then removes the component.
 */
public class LoadGameRequestComponent implements Component, Serializable {
    private static final long serialVersionUID = 1L;
    // This is a marker component and does not need any data fields.
}