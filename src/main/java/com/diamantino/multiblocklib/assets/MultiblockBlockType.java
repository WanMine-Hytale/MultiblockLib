package com.diamantino.multiblocklib.assets;

import com.diamantino.multiblocklib.MultiblockLib;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.protocol.DrawType;
import com.hypixel.hytale.server.core.asset.common.CommonAsset;
import com.hypixel.hytale.server.core.asset.common.CommonAssetModule;
import com.hypixel.hytale.server.core.asset.common.CommonAssetRegistry;
import com.hypixel.hytale.server.core.asset.common.asset.FileCommonAsset;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockPlacementSettings;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockTypeTextures;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.CustomModelTexture;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.logging.Level;

public class MultiblockBlockType extends BlockType {
    private static final String UNKNOWN_TEXTURE = "BlockTextures/Unknown.png";

    public MultiblockBlockType(BlockType type, String id) {
        super(type);

        this.id = id;

        if (this.drawType != DrawType.Model && this.drawType != DrawType.CubeWithModel) {
            this.drawType = DrawType.Model;
            this.customModelScale = 0.5f;

            this.createGhostCustomTexture();

            this.customModelTexture = new CustomModelTexture[] { new CustomModelTexture("BlockTextures/" + this.id + ".png", 1) };
            this.customModel = "Blocks/MultiblockCube.blockymodel";
        }

        this.placementSettings = new MultiblockPlacementSettings();
        this.material = BlockMaterial.Empty;
        this.ignoreSupportWhenPlaced = true;
    }

    private void createGhostCustomTexture() {
        BlockTypeTextures[] textureVariants = this.getTextures();
        if (textureVariants == null || textureVariants.length == 0 || textureVariants[0] == null) {
            return;
        }

        MultiblockLib plugin = MultiblockLib.getInstance();
        if (plugin == null || plugin.getRuntimeAssetsPath() == null) {
            return;
        }

        try {
            BlockTypeTextures textures = textureVariants[0];
            String anyFace = firstNonBlank(
                    textures.getNorth(),
                    textures.getSouth(),
                    textures.getWest(),
                    textures.getEast(),
                    textures.getUp(),
                    textures.getDown(),
                    UNKNOWN_TEXTURE
            );

            String northId = normalizeTextureId(firstNonBlank(textures.getNorth(), anyFace));
            String southId = normalizeTextureId(firstNonBlank(textures.getSouth(), anyFace));
            String westId = normalizeTextureId(firstNonBlank(textures.getWest(), anyFace));
            String eastId = normalizeTextureId(firstNonBlank(textures.getEast(), anyFace));
            String upId = normalizeTextureId(firstNonBlank(textures.getUp(), anyFace));
            String downId = normalizeTextureId(firstNonBlank(textures.getDown(), anyFace));

            BufferedImage fallback = loadTextureImage(UNKNOWN_TEXTURE);
            if (fallback == null) {
                fallback = createMissingTexture(16, 16);
            }

            BufferedImage north = loadTextureOrFallback(northId, fallback);
            BufferedImage south = loadTextureOrFallback(southId, fallback);
            BufferedImage west = loadTextureOrFallback(westId, fallback);
            BufferedImage east = loadTextureOrFallback(eastId, fallback);
            BufferedImage up = loadTextureOrFallback(upId, fallback);
            BufferedImage down = loadTextureOrFallback(downId, fallback);

            int tileWidth = Math.max(
                    1,
                    Math.max(
                            Math.max(Math.max(north.getWidth(), south.getWidth()), Math.max(west.getWidth(), east.getWidth())),
                            Math.max(up.getWidth(), down.getWidth())
                    )
            );
            int tileHeight = Math.max(
                    1,
                    Math.max(
                            Math.max(Math.max(north.getHeight(), south.getHeight()), Math.max(west.getHeight(), east.getHeight())),
                            Math.max(up.getHeight(), down.getHeight())
                    )
            );

            BufferedImage atlas = new BufferedImage(tileWidth * 3, tileHeight * 2, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = atlas.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

            // Layout requested by the multiblock ghost texture: NSW / EUD.
            drawTile(graphics, north, tileWidth, tileHeight, 0, 0);
            drawTile(graphics, south, tileWidth, tileHeight, 1, 0);
            drawTile(graphics, west, tileWidth, tileHeight, 2, 0);
            drawTile(graphics, east, tileWidth, tileHeight, 0, 1);
            drawTile(graphics, up, tileWidth, tileHeight, 1, 1);
            drawTile(graphics, down, tileWidth, tileHeight, 2, 1);
            graphics.dispose();

            String textureAssetName = sanitizeAssetId(this.id) + ".png";
            Path outputPath = plugin.getRuntimeBlockTexturesPath().resolve(textureAssetName);
            Files.createDirectories(outputPath.getParent());

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(atlas, "png", outputStream);
            byte[] textureBytes = outputStream.toByteArray();

            Files.write(outputPath, textureBytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            CommonAssetModule commonAssetModule = CommonAssetModule.get();
            if (commonAssetModule != null) {
                commonAssetModule.addCommonAsset("MultiblockLibRuntime", new FileCommonAsset(outputPath, textureAssetName, textureBytes), false);
            }

            this.customModelTexture = new CustomModelTexture[]{new CustomModelTexture(textureAssetName, 1)};
        } catch (Exception exception) {
            plugin.getLogger().at(Level.WARNING).withCause(exception).log("Failed to build ghost texture atlas for block id: %s", this.id);
        }
    }

    private static void drawTile(Graphics2D graphics, BufferedImage image, int tileWidth, int tileHeight, int column, int row) {
        graphics.drawImage(image, column * tileWidth, row * tileHeight, tileWidth, tileHeight, null);
    }

    private static BufferedImage loadTextureOrFallback(String textureId, BufferedImage fallback) {
        BufferedImage texture = loadTextureImage(textureId);
        return texture != null ? texture : fallback;
    }

    private static BufferedImage loadTextureImage(String textureId) {
        String normalized = normalizeTextureId(textureId);
        CommonAsset asset = CommonAssetRegistry.getByName(normalized);
        if (asset == null && !normalized.contains(".")) {
            asset = CommonAssetRegistry.getByName(normalized + ".png");
        }

        if (asset == null) {
            return null;
        }

        try {
            byte[] bytes = asset.getBlob().join();
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (Exception _) {
            return null;
        }
    }

    private static String normalizeTextureId(String textureId) {
        String normalized = firstNonBlank(textureId, UNKNOWN_TEXTURE).replace('\\', '/');
        return normalized.startsWith("/") ? normalized.substring(1) : normalized;
    }

    private static String sanitizeAssetId(String id) {
        return id.replaceAll("[^A-Za-z0-9._/-]", "_");
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return UNKNOWN_TEXTURE;
    }

    private static BufferedImage createMissingTexture(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(new Color(255, 0, 255));
        graphics.fillRect(0, 0, width, height);
        graphics.setColor(Color.BLACK);
        graphics.fillRect(0, 0, width / 2, height / 2);
        graphics.fillRect(width / 2, height / 2, width / 2, height / 2);
        graphics.dispose();
        return image;
    }

    private static class MultiblockPlacementSettings extends BlockPlacementSettings {
        public MultiblockPlacementSettings() {
            super();

            this.allowBreakReplace = true;
        }
    }
}
