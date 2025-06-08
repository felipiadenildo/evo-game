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
     * This method is now "smarter" and will not try to load procedural keys as files.
     * @param pathOrKey The file path or the custom key for a cached image.
     * @return The Image object, or null if it's not in the cache and cannot be loaded as a file.
     */
    public Image getImage(String pathOrKey) {
        if (pathOrKey == null || pathOrKey.isEmpty()) {
            return null;
        }

        // 1. Check the cache first. This works for both file paths and procedural keys.
        if (imageCache.containsKey(pathOrKey)) {
            return imageCache.get(pathOrKey);
        }

        // --- CORREÇÃO PRINCIPAL AQUI ---
        // 2. If the key starts with "proc_", it's for a procedural sprite that hasn't been
        //    generated and cached yet. It's NOT a file path. So, we just return null
        //    and let the RenderSystem handle the generation.
        if (pathOrKey.startsWith("proc_")) {
            return null;
        }

        // 3. If it's not in the cache and not a procedural key, assume it's a file path
        //    and try to load it from the resources.
        try {
            String fullPath = GameConstants.ASSETS_PATH + pathOrKey;
            ImageIcon icon = new ImageIcon(getClass().getClassLoader().getResource(fullPath));
            
            if (icon.getImageLoadStatus() != java.awt.MediaTracker.COMPLETE || icon.getIconWidth() <= 0) {
                 System.err.println("[ERROR AssetManager] Failed to load image from resource: " + fullPath);
                 imageCache.put(pathOrKey, null); // Cache the failure to avoid retrying
                 return null;
            }

            Image image = icon.getImage();
            imageCache.put(pathOrKey, image); // Add the loaded image to the cache
            System.out.println("[INFO AssetManager] Loaded and cached image: " + fullPath);
            return image;

        } catch (Exception e) {
            System.err.println("[ERROR AssetManager] Exception while loading file " + pathOrKey + ": " + e.getMessage());
            imageCache.put(pathOrKey, null);
            return null;
        }
    }
}