package game.evo.evolution;

import java.util.List;

/**
 * Represents a single trait or "perk" in the evolution tree.
 */
public class Trait {
    public final String id; // Um identificador único, ex: "fins", "exoskeleton"
    public final String name;
    public final String description;
    public final List<String> prerequisites; // Lista de IDs de traits necessários para desbloquear este

    public Trait(String id, String name, String description, List<String> prerequisites) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.prerequisites = prerequisites;
    }
}