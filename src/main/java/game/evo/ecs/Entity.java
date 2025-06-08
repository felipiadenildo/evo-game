package game.evo.ecs;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Representa uma entidade no mundo do jogo. Uma entidade é essencialmente um ID
 * que agrupa uma coleção de componentes.
 */
public class Entity implements Serializable { // << 2. ADICIONE 'implements Serializable'

    private static final long serialVersionUID = 1L; 

    private static final AtomicInteger nextId = new AtomicInteger(0); // Gerador de ID seguro para threads
    private final int id;

    /**
     * Construtor protegido para garantir que entidades sejam criadas unicamente
     * através de uma classe gerenciadora (como a futura classe World).
     */
    protected Entity() {
        this.id = nextId.incrementAndGet();
    }

    /**
     * Obtém o ID único desta entidade.
     *
     * @return O ID da entidade.
     */
    public int getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Entity entity = (Entity) o;
        return id == entity.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id); // Usar o método estático para consistência
    }

    @Override
    public String toString() {
        return "Entity[" + id + "]";
    }
}
