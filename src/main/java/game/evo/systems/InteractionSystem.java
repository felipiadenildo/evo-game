package game.evo.systems;

import game.evo.components.ActivatingPortalComponent;
import game.evo.ecs.Entity;
import game.evo.ecs.World;
import game.evo.components.FoodComponent;
import game.evo.components.PlayerControlledComponent;
import game.evo.components.PositionComponent;
import game.evo.components.StatusComponent;
import game.evo.components.PortalComponent;
import game.evo.components.GoToNextLevelComponent;
import game.evo.components.NotificationComponent; // Importa o componente de notificação

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Lida com interações entre o jogador e outras entidades, como
 * coletar itens (comida) e entrar em portais.
 * REFATORADO: Agora cria notificações visuais para as interações.
 */
public class InteractionSystem extends GameSystem {

    public InteractionSystem(World world) {
        super(world);
    }

    /**
     * O método de atualização, chamado a cada frame pelo game loop principal.
     */
    @Override
    public void update() {
        // Encontra a entidade do jogador
        Set<Entity> playerEntities = world.getEntitiesWithComponent(PlayerControlledComponent.class);
        if (playerEntities.isEmpty()) {
            return; // Sem jogador, sem interações
        }
        Entity player = playerEntities.iterator().next();

        PositionComponent playerPos = world.getComponent(player, PositionComponent.class);
        if (playerPos == null) {
            return; // Jogador precisa de uma posição para interagir
        }

        // Encontra todas as entidades na mesma posição que o jogador
        List<Entity> entitiesAtPlayerPosition = findEntitiesAt(playerPos.row, playerPos.column);

        // Itera sobre as entidades para verificar interações
        for (Entity otherEntity : entitiesAtPlayerPosition) {
            if (otherEntity.equals(player)) {
                continue; // Uma entidade não pode interagir consigo mesma
            }

            // Verifica se a entidade é um item de comida
            if (world.hasComponent(otherEntity, FoodComponent.class)) {
                eatFood(player, otherEntity);
                break; // Interage com apenas um item por frame
            }

            // Verifica se a entidade é um portal
            if (world.hasComponent(otherEntity, PortalComponent.class)) {
                enterPortal(player);
                break; // Interage com apenas um item por frame
            }
        }
    }

    /**
     * Lida com a lógica para um jogador consumir um item alimentar.
     * MODIFICADO: Diferencia comida normal e venenosa e cria notificações visuais.
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
            int damage = foodData.nutritionValue;
            playerStatus.health -= damage;
            
            // Adiciona uma notificação de AVISO na tela
            world.addComponent(player, new NotificationComponent("Poison! -" + damage + " health", NotificationComponent.NotificationType.WARNING, 3.0f));

        } else {
            // Comida normal: adiciona pontos de evolução e regenera vida
            playerStatus.evolutionPoints += foodData.nutritionValue + 20;
            playerStatus.health = Math.min(playerStatus.maxHealth, playerStatus.health + foodData.nutritionValue);

            // Adiciona uma notificação de SUCESSO na tela
            world.addComponent(player, new NotificationComponent("+" + foodData.nutritionValue + " points!", NotificationComponent.NotificationType.SUCCESS, 2.0f));
        }

        // Remove o item do mundo após a interação
        world.destroyEntity(foodItem);
    }

    /**
     * Lida com a lógica para um jogador entrar em um portal.
     * MODIFICADO: Agora cria uma notificação visual.
     * @param player A entidade do jogador.
     */
    private void enterPortal(Entity player) {
        // Se o jogador já estiver ativando um portal, não faz nada
        if (world.hasComponent(player, ActivatingPortalComponent.class)) {
            return;
        }

        System.out.println("[InteractionSystem] Player entered the portal! Starting activation sequence...");
        
        // Adiciona o componente de ativação com um delay de 2 segundos
        world.addComponent(player, new ActivatingPortalComponent(2.0f));
        
        // Adiciona uma notificação para o jogador saber o que está acontecendo
        world.addComponent(player, new NotificationComponent("Portal activating...", NotificationComponent.NotificationType.INFO, 2.0f));
    }

    /**
     * Método auxiliar para encontrar todas as entidades em uma localização específica da grade.
     * @param row A linha alvo.
     * @param col A coluna alvo.
     * @return Uma lista de todas as entidades naquela posição.
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