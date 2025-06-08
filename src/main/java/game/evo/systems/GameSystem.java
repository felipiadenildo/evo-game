package game.evo.systems;

import game.evo.ecs.World;

/**
 * Uma classe abstrata base para todos os sistemas no jogo.
 * Define a estrutura comum que cada sistema (lógica de renderização, input, IA, etc.) deve seguir.
 * O uso de uma classe base abstrata atende aos requisitos do projeto e permite o polimorfismo.
 */
public abstract class GameSystem {
    protected World world; // Mundo ECS, acessível a todas as subclasses

    /**
     * Construtor para um GameSystem.
     * @param world A instância do mundo do jogo com a qual este sistema irá interagir.
     */
    public GameSystem(World world) {
        this.world = world;
    }

    /**
     * Método abstrato de atualização.
     * Cada sistema concreto deve implementar este método com sua lógica específica,
     * que será executada a cada "tick" do loop principal do jogo.
     */
    public abstract void update();

    // Poderíamos adicionar outros parâmetros se necessário, como o tempo delta:
    // public abstract void update(float deltaTime);
}