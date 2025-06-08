package game.evo.ecs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages all entities and their components in the game world.
 */
public class World implements Serializable { // << 2. ADICIONE 'implements Serializable'

    private static final long serialVersionUID = 1L;
    private Set<Entity> entities;
    private Map<Class<? extends Component>, Map<Entity, Component>> componentsByType;
    private Map<Entity, Set<Class<? extends Component>>> entityComponentTypes;

    /**
     * Flag to control verbose logging for World operations. Set to true for
     * detailed debugging of entity/component management.
     */
    public static boolean MODO_VERBOSE_WORLD = false; // Nova flag específica para World

    public World() {
        entities = new HashSet<>();
        componentsByType = new HashMap<>();
        entityComponentTypes = new HashMap<>();
    }

    public Entity createEntity() {
        Entity entity = new Entity();
        entities.add(entity);
        entityComponentTypes.put(entity, new HashSet<>());
        if (MODO_VERBOSE_WORLD) { // Usando a nova flag
            System.out.println("[INFO World] Created " + entity);
        }
        return entity;
    }

    public void destroyEntity(Entity entity) {
        if (entity == null || !entities.contains(entity)) {
            System.err.println("[WARN World] Attempted to destroy a null or non-existent entity: " + entity);
            return;
        }

        Set<Class<? extends Component>> componentTypes = entityComponentTypes.get(entity);
        if (componentTypes != null) {
            List<Class<? extends Component>> typesToRemove = new ArrayList<>(componentTypes);
            for (Class<? extends Component> componentType : typesToRemove) {
                removeComponent(entity, componentType); // removeComponent já tem seu próprio log verboso
            }
        }
        entityComponentTypes.remove(entity);
        entities.remove(entity);
        if (MODO_VERBOSE_WORLD) { // Usando a nova flag
            System.out.println("[INFO World] Destroyed " + entity);
        }
    }

    public void addComponent(Entity entity, Component component) {
        if (entity == null || component == null) {
            System.err.println("[ERRO World] Cannot add null entity or component.");
            return;
        }
        if (!entities.contains(entity)) {
            System.err.println("[ERRO World] Entity " + entity + " does not exist. Cannot add component " + component.getClass().getSimpleName());
            return;
        }

        Class<? extends Component> componentType = component.getClass();
        componentsByType.computeIfAbsent(componentType, k -> new HashMap<>());
        componentsByType.get(componentType).put(entity, component);
        entityComponentTypes.get(entity).add(componentType);

        if (MODO_VERBOSE_WORLD) { // Usando a nova flag
            System.out.println("[INFO World] Added " + componentType.getSimpleName() + " to " + entity);
        }
    }

    public <T extends Component> void removeComponent(Entity entity, Class<T> componentType) {
        if (entity == null || componentType == null) {
            System.err.println("[ERRO World] Cannot remove component with null entity or componentType.");
            return;
        }

        Map<Entity, Component> entityToComponentMap = componentsByType.get(componentType);
        if (entityToComponentMap != null) {
            Component removedComponent = entityToComponentMap.remove(entity);
            if (removedComponent != null && MODO_VERBOSE_WORLD) { // Usando a nova flag
                System.out.println("[INFO World] Removed " + componentType.getSimpleName() + " from " + entity);
            }
        }

        Set<Class<? extends Component>> typesForEntity = entityComponentTypes.get(entity);
        if (typesForEntity != null) {
            typesForEntity.remove(componentType);
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends Component> T getComponent(Entity entity, Class<T> componentType) {
        if (entity == null || componentType == null) {
            System.err.println("[WARN World.getComponent] Entity or componentType is null.");
            return null;
        }
        Map<Entity, Component> entityToComponentMap = componentsByType.get(componentType);
        if (entityToComponentMap != null) {
            return (T) entityToComponentMap.get(entity);
        }
        return null;
    }

    public boolean hasComponent(Entity entity, Class<? extends Component> componentType) {
        if (entity == null || componentType == null) {
            return false;
        }
        Map<Entity, Component> entityToComponentMap = componentsByType.get(componentType);
        return entityToComponentMap != null && entityToComponentMap.containsKey(entity);
    }

    public <T extends Component> Set<Entity> getEntitiesWithComponent(Class<T> componentType) {
        Map<Entity, Component> entityToComponentMap = componentsByType.get(componentType);
        if (entityToComponentMap != null) {
            return new HashSet<>(entityToComponentMap.keySet());
        }
        return new HashSet<>();
    }

    public Set<Entity> getEntitiesWithComponents(Class<? extends Component>... componentTypes) {
        if (componentTypes == null || componentTypes.length == 0) {
            return new HashSet<>(entities);
        }

        Set<Entity> resultSet = getEntitiesWithComponent(componentTypes[0]);
        if (resultSet.isEmpty()) {
            return resultSet; // Otimização
        }
        for (int i = 1; i < componentTypes.length; i++) {
            if (componentTypes[i] == null) {
                continue; // Pula tipos de componentes nulos
            }
            resultSet.retainAll(getEntitiesWithComponent(componentTypes[i]));
            if (resultSet.isEmpty()) {
                break;
            }
        }
        return resultSet;
    }
}
