package game.evo.systems;

import game.evo.ecs.Entity;
import game.evo.ecs.World;
import game.evo.components.FoodComponent;
import game.evo.components.PlayerControlledComponent;
import game.evo.components.PositionComponent;
import game.evo.components.StatusComponent;
import game.evo.components.PortalComponent;
import game.evo.components.GoToNextLevelComponent;
import game.evo.components.NotificationComponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Handles interactions between the player and other entities, such as
 * collecting items (food) and entering portals.
 */
public class InteractionSystem extends GameSystem {

    public InteractionSystem(World world) {
        super(world);
    }

    /**
     * The update method, called every frame by the main game loop.
     * It checks for interactions between the player and other entities.
     */
    @Override
    public void update() {
        // Find the player entity
        Set<Entity> playerEntities = world.getEntitiesWithComponent(PlayerControlledComponent.class);
        if (playerEntities.isEmpty()) {
            return; // No player in the world, nothing to do
        }
        Entity player = playerEntities.iterator().next();

        PositionComponent playerPos = world.getComponent(player, PositionComponent.class);
        if (playerPos == null) {
            return; // Player must have a position to interact
        }

        // Find all entities that are at the same position as the player
        List<Entity> entitiesAtPlayerPosition = findEntitiesAt(playerPos.row, playerPos.column);

        // Iterate through the co-located entities to check for interactions
        for (Entity otherEntity : entitiesAtPlayerPosition) {
            if (otherEntity.equals(player)) {
                continue; // An entity cannot interact with itself
            }

            // Check if the entity is a food item
            if (world.hasComponent(otherEntity, FoodComponent.class)) {
                eatFood(player, otherEntity);
                break; // Interact with only one item per frame to avoid consuming multiple stacked items
            }

            // Check if the entity is a portal
            if (world.hasComponent(otherEntity, PortalComponent.class)) {
                enterPortal(player);
                break; // Interact with only one item per frame
            }
        }
    }

    /**
 * Lida com a lógica para um jogador consumir um item alimentar.
 * MODIFICADO: Agora diferencia entre comida normal e venenosa.
 * @param player O jogador.
 * @param foodItem O item alimentar a ser consumido.
 */
private void eatFood(Entity player, Entity foodItem) {
    StatusComponent playerStatus = world.getComponent(player, StatusComponent.class);
    FoodComponent foodData = world.getComponent(foodItem, FoodComponent.class);

    if (playerStatus == null || foodData == null) {
        System.err.println("[WARN InteractionSystem] Player or Food is missing required components for interaction.");
        return;
    }

    // Verifica se o alimento é venenoso 
    if (foodData.isPoisonous) {
        // Causa dano em vez de curar
        int damage = foodData.nutritionValue; // "nutrition" aqui representa a potência do veneno
        playerStatus.health -= damage;
        System.out.println("[INFO GAME] Player ate something poisonous! Lost " + damage + " health.");
        
        // Adiciona uma notificação na tela para o jogador
        world.addComponent(player, new NotificationComponent("You ate something poisonous!", 3.0f));

    } else {
        // Comida normal: adiciona pontos de evolução e regenera vida
        playerStatus.evolutionPoints += foodData.nutritionValue;
        playerStatus.health = Math.min(playerStatus.maxHealth, playerStatus.health + foodData.nutritionValue);

        System.out.println("[INFO GAME] Player ate food! Gained " + foodData.nutritionValue + " evolution points. Total: " + playerStatus.evolutionPoints);
        
        // Adiciona uma notificação positiva
        world.addComponent(player, new NotificationComponent("Yummy! +" + foodData.nutritionValue + " points.", 2.0f));
    }

    // Remove o item do mundo após a interação, seja ele venenoso ou não
    world.destroyEntity(foodItem);
}

    /**
     * Handles the logic for a player entering a portal.
     * It adds a "GoToNextLevelComponent" to the player, which acts as an event
     * for the main game loop to detect and trigger the level transition.
     * @param player The player entity.
     */
    private void enterPortal(Entity player) {
        System.out.println("[INFO GAME] Player entered the portal! Triggering next level load...");
        // Add the event component to signal the main loop
        world.addComponent(player, new GoToNextLevelComponent());
    }

    /**
     * Helper method to find all entities at a specific grid location.
     * @param row The target row.
     * @param col The target column.
     * @return A list of all entities at that position.
     */
    private List<Entity> findEntitiesAt(int row, int col) {
        List<Entity> foundEntities = new ArrayList<>();
        Set<Entity> entitiesWithPosition = world.getEntitiesWithComponent(PositionComponent.class);
        
        for (Entity entity : entitiesWithPosition) {
            PositionComponent pos = world.getComponent(entity, PositionComponent.class);
            if (pos.row == row && pos.column == col) {
                foundEntities.add(entity);
            }
        }
        return foundEntities;
    }
}