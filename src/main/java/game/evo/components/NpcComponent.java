package game.evo.components;

import game.evo.ecs.Component;
import java.io.Serializable;

/**
 * A marker component that identifies an entity as an NPC (Non-Player Character).
 */
public class NpcComponent implements Component , Serializable { // << 2. ADICIONE 'implements Serializable'

    private static final long serialVersionUID = 1L;     // Componente marcador, não precisa de dados por enquanto.
}