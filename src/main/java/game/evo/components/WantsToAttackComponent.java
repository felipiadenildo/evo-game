package game.evo.components;

import game.evo.ecs.Component;
import java.io.Serializable;

/**
 * A temporary marker component added to an entity when it intends to perform an attack.
 * The CombatSystem looks for this component, processes the attack, and then removes it.
 */
public class WantsToAttackComponent implements Component , Serializable { // << 2. ADICIONE 'implements Serializable'

    private static final long serialVersionUID = 1L; 
    // This component is a marker and doesn't need data for a simple tackle.
    // For special attacks, it could hold information about which attack to use.
}