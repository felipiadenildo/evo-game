package game.evo.view;

import game.evo.components.NotificationComponent;
import game.evo.components.PlayerControlledComponent;
import game.evo.config.EntityConfig;
import game.evo.ecs.World;
import game.evo.input.InputManager;
import game.evo.systems.RenderSystem;
import game.evo.utils.GameConstants;
import game.evo.world.EntityFactory;
import game.evo.world.GameMap;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.List;
import java.util.zip.ZipInputStream;

/**
 * GamePanel is the custom JPanel where the game is rendered. REFATORADO: Agora
 * lida com carregamento de mapa assíncrono e inclui Drag-and-Drop.
 */
public class GamePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final World world;
    private final RenderSystem renderSystem;
    private GameMap gameMap; // Removido 'final' para permitir setar depois
    private final EntityFactory entityFactory;
    private final InputManager inputManager;

    private int cameraX = 0;
    private int cameraY = 0;

    public GamePanel(World world, GameMap gameMap, RenderSystem renderSystem, InputManager inputManager, EntityFactory entityFactory) {
        // A verificação de nulidade do gameMap foi removida daqui
        if (world == null || renderSystem == null || inputManager == null || entityFactory == null) {
            throw new IllegalArgumentException("Argumentos do construtor (exceto gameMap) não podem ser nulos.");
        }

        this.world = world;
        this.gameMap = gameMap; // Inicializa com o valor passado (pode ser null)
        this.renderSystem = renderSystem;
        this.entityFactory = entityFactory;
        this.inputManager = inputManager;

        int panelWidth = GameConstants.SCREEN_WIDTH_TILES * GameConstants.CELL_SIZE;
        int panelHeight = GameConstants.SCREEN_HEIGHT_TILES * GameConstants.CELL_SIZE;
        this.setPreferredSize(new Dimension(panelWidth, panelHeight));
        this.setBackground(Color.BLACK);
        this.setFocusable(true);
        this.addKeyListener(inputManager);

        // Lógica de Drag-and-Drop
        new DropTarget(this, new DropTargetListener() {
            public void dragEnter(DropTargetDragEvent dtde) {
            }

            public void dragOver(DropTargetDragEvent dtde) {
            }

            public void dropActionChanged(DropTargetDragEvent dtde) {
            }

            public void dragExit(DropTargetEvent dte) {
            }

            @Override
            public void drop(DropTargetDropEvent event) {
                try {
                    event.acceptDrop(DnDConstants.ACTION_COPY);
                    Transferable transferable = event.getTransferable();
                    if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                        List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                        if (!files.isEmpty()) {
                            processDroppedFile(files.get(0), event.getLocation());
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[ERROR DragNDrop] Ocorreu um erro durante o drop.");
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * NOVO MÉTODO: Processa o arquivo que foi arrastado e solto na janela.
     *
     * @param file O arquivo recebido.
     * @param dropPoint O ponto (em pixels da tela) onde o arquivo foi solto.
     */
    private void processDroppedFile(File file, Point dropPoint) {
        if (!file.getName().toLowerCase().endsWith(".zip")) {
            System.out.println("[DragNDrop] Arquivo ignorado. Não é um .zip.");
            return;
        }
        System.out.println("[DragNDrop] Arquivo .zip detectado: " + file.getName());

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(file))) {
            // Procura pelo nosso arquivo de dados "creature.dat" dentro do zip
            if (zis.getNextEntry().getName().equals("creature.dat")) {

                // Lê os bytes do arquivo de dados para a memória
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }

                // Desserializa os bytes de volta para um objeto EntityConfig
                ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                ObjectInputStream ois = new ObjectInputStream(bais);
                EntityConfig customConfig = (EntityConfig) ois.readObject();
                ois.close();

                // Converte a posição do drop na tela (pixels) para a grade do mapa (tiles)
                int gridCol = (cameraX + dropPoint.x) / GameConstants.CELL_SIZE;
                int gridRow = (cameraY + dropPoint.y) / GameConstants.CELL_SIZE;

                // Atualiza a posição no config e usa a factory para criar a entidade no mundo
                customConfig.row = gridRow;
                customConfig.column = gridCol;
                entityFactory.createGameEntity(customConfig);

                entityFactory.createGameEntity(customConfig);
                world.getEntitiesWithComponent(PlayerControlledComponent.class).stream().findFirst().ifPresent(player -> {
                    world.addComponent(player, new NotificationComponent("Custom creature added!", NotificationComponent.NotificationType.SUCCESS, 3.0f));
                });

                System.out.println("[DragNDrop] Criatura customizada '" + customConfig.type + "' adicionada ao mundo na posição (" + gridRow + ", " + gridCol + ")");
            }
        } catch (Exception e) {
            System.err.println("[ERROR DragNDrop] Falha ao processar o arquivo da criatura.");
            e.printStackTrace();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        try {
            // Se o mapa ainda não foi carregado, apenas pinta um fundo preto e sai.
            if (gameMap == null) {
                g2d.setColor(Color.BLACK);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                return; // Encerra o desenho aqui para esta frame
            }

            BufferedImage mapImage = gameMap.getMapImage();
            if (mapImage != null) {
                g2d.drawImage(mapImage, 0, 0, getWidth(), getHeight(), cameraX, cameraY, cameraX + getWidth(), cameraY + getHeight(), null);
            }
            this.renderSystem.update(g2d, cameraX, cameraY, this.getWidth(), this.getHeight());

        } catch (Exception e) {
            System.err.println("[CRITICAL ERROR in GamePanel.paintComponent] Exception during rendering:");
            e.printStackTrace();
        } finally {
            g2d.dispose();
        }
    }

    public void setCameraPosition(int newCamX, int newCamY) {
        // Adiciona uma verificação para evitar NullPointerException antes do mapa ser carregado
        if (gameMap == null) {
            return;
        }

        int worldPixelWidth = gameMap.getWidthInTiles() * GameConstants.CELL_SIZE;
        int worldPixelHeight = gameMap.getHeightInTiles() * GameConstants.CELL_SIZE;
        int screenPixelWidth = getWidth();
        int screenPixelHeight = getHeight();
        int maxCamX = Math.max(0, worldPixelWidth - screenPixelWidth);
        int maxCamY = Math.max(0, worldPixelHeight - screenPixelHeight);
        this.cameraX = Math.max(0, Math.min(newCamX, maxCamX));
        this.cameraY = Math.max(0, Math.min(newCamY, maxCamY));
    }

    // --- NOVOS MÉTODOS GETTER E SETTER ---
    public void setGameMap(GameMap gameMap) {
        this.gameMap = gameMap;
    }

    public GameMap getGameMap() {
        return this.gameMap;
    }

    public InputManager getInputManager() {
        return this.inputManager;
    }
}
