package com.diamantino.multiblocklib.assets;

import com.hypixel.hytale.assetstore.AssetExtraInfo;
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec;
import com.hypixel.hytale.assetstore.map.DefaultAssetMap;
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;

public class MultiblockPatternAsset implements JsonAssetWithMap<String, DefaultAssetMap<String, MultiblockPatternAsset>> {
    public static final AssetBuilderCodec<String, MultiblockPatternAsset> CODEC = AssetBuilderCodec.builder(
                    MultiblockPatternAsset.class,
                    MultiblockPatternAsset::new,
                    Codec.STRING,
                    (multiblockPattern, s) -> multiblockPattern.id = s,
                    multiblockPattern -> multiblockPattern.id,
                    (multiblockPattern, data) -> multiblockPattern.extraData = data,
                    multiblockPattern -> multiblockPattern.extraData
            )
            .appendInherited(
                    new KeyedCodec<>("CoreBlock", Codec.STRING),
                    (multiblockPattern, s) -> multiblockPattern.coreBlockId = s,
                    multiblockPattern -> multiblockPattern.coreBlockId,
                    (multiblockPattern, parent) -> multiblockPattern.coreBlockId = parent.coreBlockId
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("Layers", new ArrayCodec<>(Codec.STRING_ARRAY, String[][]::new)),
                    (layer, s) -> layer.layers = s,
                    layer -> layer.layers,
                    (layer, parent) -> layer.layers = parent.layers
            )
            .add()
            .appendInherited(
                    new KeyedCodec<>("BlockMap", new ArrayCodec<>(LayerBlock.CODEC, LayerBlock[]::new)),
                    (pattern, s) -> pattern.blockMap = s,
                    pattern -> pattern.blockMap,
                    (pattern, parent) -> pattern.blockMap = parent.blockMap
            )
            .add()
            .build();

    protected AssetExtraInfo.Data extraData;
    protected String id;

    protected String coreBlockId;
    protected String[][] layers;
    protected LayerBlock[] blockMap;

    protected MultiblockPatternAsset() { }

    public MultiblockPatternAsset(String id, String coreBlockId, String[][] layers, LayerBlock[] blockMap) {
        this.id = id;
        this.coreBlockId = coreBlockId;
        this.layers = layers;
        this.blockMap = blockMap;
    }

    public String getCoreBlockId() {
        return this.coreBlockId;
    }

    public String[][] getLayers() {
        return this.layers;
    }

    public LayerBlock[] getBlockMap() {
        return this.blockMap;
    }

    @Override
    public String getId() {
        return this.id;
    }

    public static class LayerBlock {
        public static final BuilderCodec<LayerBlock> CODEC = BuilderCodec.builder(
                        LayerBlock.class,
                        LayerBlock::new
                )
                .append(new KeyedCodec<>("BlockSymbol", Codec.STRING), (block, s) -> block.blockSymbol = s, block -> block.blockSymbol)
                .add()
                .append(new KeyedCodec<>("BlockId", Codec.STRING), (block, s) -> block.blockId = s, block -> block.blockId)
                .add()
                .append(new KeyedCodec<>("AcceptedTags", new ArrayCodec<>(Codec.STRING, String[]::new)), (block, s) -> block.acceptedTags = s, block -> block.acceptedTags)
                .add()
                .append(new KeyedCodec<>("Rotation", new EnumCodec<>(Rotation.class)), (block, rot) -> block.rotation = rot, block -> block.rotation)
                .add()
                .build();

        protected String blockSymbol;
        protected String blockId;
        protected String[] acceptedTags;
        protected Rotation rotation;

        protected LayerBlock() { }

        public String getBlockSymbol() {
            return this.blockSymbol;
        }

        public String getBlockId() {
            return this.blockId;
        }

        public String[] getAcceptedTags() {
            return this.acceptedTags;
        }

        public Rotation getRotation() {
            return this.rotation;
        }
    }
}
