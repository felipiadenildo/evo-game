package game.evo.components;

import game.evo.ecs.Component;
import java.io.Serializable;

/**
 * Component for entities that have a physical presence for collision detection.
 * Marks an entity as "solid", preventing other solid entities from moving into its tile.
 */
public class CollisionComponent implements Component , Serializable { // << 2. ADICIONE 'implements Serializable'

    private static final long serialVersionUID = 1L; 
    // Por enquanto, a simples presença deste componente significa que a entidade é sólida.
    // Poderíamos adicionar propriedades depois, como o formato da caixa de colisão.
}