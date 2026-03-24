package com.diamantino.multiblocklib;

import com.diamantino.multiblocklib.assets.MultiblockBlockType;
import com.diamantino.multiblocklib.assets.MultiblockPatternAsset;
import com.diamantino.multiblocklib.blocks.MultiblockActions;
import com.diamantino.multiblocklib.blocks.MultiblockBlock;
import com.diamantino.multiblocklib.blocks.MultiblockController;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.common.plugin.PluginManifest;
import com.hypixel.hytale.common.semver.Semver;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.asset.AssetModule;
import com.hypixel.hytale.server.core.asset.HytaleAssetStore;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.bson.BsonDocument;
import org.bson.json.JsonWriterSettings;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class MultiblockLib extends JavaPlugin {
    private static MultiblockLib instance;

    private Path runtimeAssetsPath;
    private Path runtimeGhostsPath;
    private Path runtimeBlockTexturesPath;

    private HytaleAssetStore<String, MultiblockPatternAsset, DefaultAssetMap<String, MultiblockPatternAsset>> multiblockPatternStore;

    public ComponentType<ChunkStore, MultiblockBlock> multiblockBlockType;
    public ComponentType<ChunkStore, MultiblockController> multiblockControllerType;

    public MultiblockLib(@Nonnull JavaPluginInit init) {
        super(init);

        instance = this;
    }

    @Override
    protected void setup() {
        this.runtimeAssetsPath = this.getDataDirectory().getParent().resolve("MultiblockLibRuntime");

        this.runtimeGhostsPath = this.runtimeAssetsPath.resolve("Server/Item/Block/Blocks");
        this.runtimeBlockTexturesPath = this.runtimeAssetsPath.resolve("Common/BlockTextures");

        if (!Files.exists(this.runtimeGhostsPath)) {
            this.runtimeGhostsPath.toFile().mkdirs();
        }

        if (!Files.exists(this.runtimeBlockTexturesPath)) {
            this.runtimeBlockTexturesPath.toFile().mkdirs();
        }

        try {
            PluginManifest manifest = PluginManifest.CoreBuilder.corePlugin(MultiblockLib.class)
                    .description("Runtime assets for MultiblockLib")
                    .build();
            manifest.setName("MultiblockLibRuntime");
            manifest.setVersion(Semver.fromString("1.0.0"));

            this.getLogger().at(Level.INFO).log("Registering runtime asset pack at: %s", runtimeAssetsPath);
            AssetModule.get().registerPack("MultiblockLibRuntime", runtimeAssetsPath, manifest, true);
        } catch (Exception e) {
            this.getLogger().at(Level.SEVERE).withCause(e).log("Failed to register runtime asset pack");
        }

        this.multiblockPatternStore = HytaleAssetStore
                .builder(MultiblockPatternAsset.class, new DefaultAssetMap<>())
                .setPath("Multiblocks/Pattens")
                .setCodec(MultiblockPatternAsset.CODEC)
                .setKeyFunction(MultiblockPatternAsset::getId)
                .build();

        this.getAssetRegistry().register(this.multiblockPatternStore);

        this.multiblockBlockType = this.getChunkStoreRegistry().registerComponent(MultiblockBlock.class, "MultiblockLibMultiblockBlock", MultiblockBlock.CODEC);
        this.multiblockControllerType = this.getChunkStoreRegistry().registerComponent(MultiblockController.class, "MultiblockLibMultiblockController", MultiblockController.CODEC);

        this.getChunkStoreRegistry().registerSystem(new MultiblockActions.MultiblockControllerSystem());
    }

    @Override
    protected void start() {
        this.getEntityStoreRegistry().registerSystem(new MultiblockActions.MultiblockBreakBlockSystem());

        List<String> blockIds = new ArrayList<>();
        List<String> blockTags = new ArrayList<>();

        this.multiblockPatternStore.getAssetMap().getAssetMap().forEach((_, asset) -> {
            if (asset.getBlockMap() != null) {
                for (MultiblockPatternAsset.LayerBlock block : asset.getBlockMap()) {
                    if (!blockIds.contains(block.getBlockId())) {
                        blockIds.add(block.getBlockId());
                    }

                    if (block.getAcceptedTags() != null) {
                        for (String tag : block.getAcceptedTags()) {
                            if (!blockTags.contains(tag)) {
                                blockTags.add(tag);
                            }
                        }
                    }
                }
            }
        });

        for (String blockId : blockIds) {
            this.registerGhostBlock(blockId);
        }

        BlockType.getAssetMap().getAssetMap().forEach((id, blockType) -> {
            if (blockType.getData() != null) {
                for (String tag : blockType.getData().getRawTags().keySet()) {
                    if (blockTags.contains(tag)) {
                        this.registerGhostBlock(id);

                        break;
                    }
                }
            }
        });
    }

    private void registerGhostBlock(String blockId) {
        BlockType ghostBlock = BlockType.getAssetMap().getAsset(blockId);

        if (ghostBlock != null) {
            MultiblockBlockType editGhostBlock = new MultiblockBlockType(BlockType.getAssetMap().getAsset(blockId), blockId + "_MLGhost");

            Path ghostFilePath = this.runtimeGhostsPath.resolve(editGhostBlock.getId() + ".json");

            BsonDocument ghostDoc = BlockType.CODEC.encode(editGhostBlock, new ExtraInfo());

            try {
                Files.writeString(ghostFilePath, ghostDoc.toJson(JsonWriterSettings.builder().indent(true).build()), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } catch (IOException e) {
                this.getLogger().at(Level.SEVERE).withCause(e).log("Failed to write ghost block asset for block id: %s", blockId);
            }

            BlockType.getAssetStore().loadAssetsFromPaths("MultiblockLibRuntime", List.of(ghostFilePath));
        }
    }

    public HytaleAssetStore<String, MultiblockPatternAsset, DefaultAssetMap<String, MultiblockPatternAsset>> getMultiblockPatternStore() {
        return this.multiblockPatternStore;
    }

    public Path getRuntimeAssetsPath() {
        return this.runtimeAssetsPath;
    }

    public Path getRuntimeBlockTexturesPath() {
        return runtimeBlockTexturesPath;
    }

    public static MultiblockLib getInstance() {
        return instance;
    }
}
