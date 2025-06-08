package game.evo.components;

import game.evo.ecs.Component;
import java.io.Serializable;

/**
 * A marker component that identifies an entity as a Portal to the next level.
 */
public class PortalComponent implements Component , Serializable { // << 2. ADICIONE 'implements Serializable'

    private static final long serialVersionUID = 1L;     // This is a marker component and does not need data for now.
}