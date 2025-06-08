package game.evo.components;

import game.evo.ecs.Component;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Stores the set of unique traits (passive abilities or "genes")
 * that an entity has acquired.
 */
public class TraitComponent implements Component, Serializable {
    private static final long serialVersionUID = 1L;

    public Set<String> acquiredTraits;

    public TraitComponent() {
        this.acquiredTraits = new HashSet<>();
    }
}