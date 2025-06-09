package game.evo.utils;

import javax.swing.ImageIcon;
import java.awt.Image;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages game assets, primarily images.
 * Uses the Singleton pattern and caches assets to improve performance.
 */
public class AssetManager {

    private static AssetManager instance;
    private final Map<String, Image> imageCache;

    private AssetManager() {
        imageCache = new HashMap<>();
    }

    public static AssetManager getInstance() {
        if (instance == null) {
            instance = new AssetManager();
        }
        return instance;
    }

    /**
     * Adds a pre-generated image (like a procedural sprite) directly to the cache.
     * @param key The unique key for this image in the cache.
     * @param image The Image instance to store.
     */
    public void cacheImage(String key, Image image) {
        if (key != null && !key.isEmpty() && image != null) {
            imageCache.put(key, image);
        }
    }

     /**
     * Gets an image, either from the cache or by loading it from a file.
     * CORRIGIDO: Agora usa o caminho passado diretamente, sem prefixo,
     * para carregar o recurso corretamente.
     */
    public Image getImage(String pathOrKey) {
        if (pathOrKey == null || pathOrKey.isEmpty()) {
            return null;
        }

        // 1. Check the cache first.
        if (imageCache.containsKey(pathOrKey)) {
            return imageCache.get(pathOrKey);
        }

        // 2. Handle procedural keys.
        if (pathOrKey.startsWith("proc_")) {
            return null;
        }

        // 3. Load from resources using the pathOrKey directly.
        try {
            // REMOVIDA A CONCATENAÇÃO com GameConstants.ASSETS_PATH
            // Usa-se pathOrKey diretamente, pois ele já contém o caminho completo.
            java.net.URL imageUrl = getClass().getClassLoader().getResource(pathOrKey);

            if (imageUrl == null) {
                System.err.println("[ERROR AssetManager] Recurso não encontrado: " + pathOrKey);
                imageCache.put(pathOrKey, null); // Cache the failure
                return null;
            }

            ImageIcon icon = new ImageIcon(imageUrl);
            
            // A verificação de status de carregamento continua sendo uma boa prática
            if (icon.getImageLoadStatus() != java.awt.MediaTracker.COMPLETE || icon.getIconWidth() <= 0) {
                 System.err.println("[ERROR AssetManager] Falha ao carregar a imagem do recurso: " + pathOrKey);
                 imageCache.put(pathOrKey, null);
                 return null;
            }

            Image image = icon.getImage();
            imageCache.put(pathOrKey, image); // Add to cache
            System.out.println("[INFO AssetManager] Imagem carregada e cacheada: " + pathOrKey);
            return image;

        } catch (Exception e) {
            System.err.println("[ERROR AssetManager] Exceção ao carregar o arquivo " + pathOrKey + ": " + e.getMessage());
            imageCache.put(pathOrKey, null);
            return null;
        }
    }
}