package com.diamantino.multiblocklib.blocks;

import com.diamantino.multiblocklib.MultiblockLib;
import com.diamantino.multiblocklib.assets.MultiblockPatternAsset;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.debug.DebugUtils;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiblockActions {
    public static class MultiblockBreakBlockSystem extends EntityEventSystem<EntityStore, BreakBlockEvent> {
        public MultiblockBreakBlockSystem() {
            super(BreakBlockEvent.class);
        }

        @Override
        public void handle(int index, @Nonnull ArchetypeChunk<EntityStore> archetypeChunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer, @Nonnull BreakBlockEvent event) {
            BlockType blockType = event.getBlockType();

            World world = commandBuffer.getExternalData().getWorld();

            world.execute(() -> {

            });
        }

        // TODO: Prob add this
        @Nullable
        @Override
        public Query<EntityStore> getQuery() {
            return Archetype.empty();
        }
    }

    public static class MultiblockControllerSystem extends EntityTickingSystem<ChunkStore> {
        private int tickTimer = 0;

        protected enum ValidationFailureReason {
            MISSING_BLOCK,
            WRONG_BLOCK
        }

        @Override
        public void tick(float dt, int index, @NonNullDecl ArchetypeChunk<ChunkStore> archetypeChunk, @NonNullDecl Store<ChunkStore> store, CommandBuffer<ChunkStore> commandBuffer) {
            World world = commandBuffer.getExternalData().getWorld();

            if (tickTimer < world.getTps()) {
                tickTimer++;

                return;
            }

            tickTimer = 0;

            Ref<ChunkStore> ref = archetypeChunk.getReferenceTo(index);
            MultiblockController instance = commandBuffer.getComponent(ref, MultiblockController.getComponentType());

            if (instance == null) {
                return;
            }

            MultiblockPatternAsset pattern = MultiblockLib.getInstance().getMultiblockPatternStore().getAssetMap().getAsset(instance.multiblockId);

            if (pattern != null) {
                BlockModule.BlockStateInfo info = archetypeChunk.getComponent(index, BlockModule.BlockStateInfo.getComponentType());

                if (info == null) {
                    return;
                }

                WorldChunk worldChunk = store.getComponent(info.getChunkRef(), WorldChunk.getComponentType());

                if (worldChunk == null) {
                    return;
                }

                int blockIndex = info.getIndex();

                int localX = ChunkUtil.worldCoordFromLocalCoord(worldChunk.getX(), ChunkUtil.xFromBlockInColumn(blockIndex));
                int localY = ChunkUtil.yFromBlockInColumn(blockIndex);
                int localZ = ChunkUtil.worldCoordFromLocalCoord(worldChunk.getZ(), ChunkUtil.zFromBlockInColumn(blockIndex));

                List<Vector3i> functionalBlocks = new ArrayList<>();
                boolean formed = this.matchesPattern(
                        world,
                        pattern,
                        new Vector3i(localX, localY, localZ),
                        world.getBlockRotationIndex(localX, localY, localZ),
                        functionalBlocks
                );

                instance.setFormed(formed);
                instance.setFunctionalBlockPositions(functionalBlocks.toArray(new Vector3i[0]));
                instance.setLastUpdate(System.currentTimeMillis());
            } else {
                instance.setFormed(false);
                instance.setFunctionalBlockPositions(new Vector3i[0]);
                instance.setLastUpdate(System.currentTimeMillis());
            }
        }

        private boolean matchesPattern(World world, MultiblockPatternAsset pattern, Vector3i controllerPos, int rotationIndex, List<Vector3i> functionalBlocks) {
            String[][] layers = pattern.getLayers();
            MultiblockPatternAsset.LayerBlock[] blockMap = pattern.getBlockMap();

            if (layers == null || layers.length == 0 || blockMap == null || blockMap.length == 0) {
                return false;
            }

            Map<String, MultiblockPatternAsset.LayerBlock> symbolLookup = this.buildSymbolLookup(blockMap);
            Vector3i patternPivot = this.findPatternPivot(pattern, layers, symbolLookup);

            if (patternPivot == null) {
                return false;
            }

            RotationTuple tuple = RotationTuple.get(rotationIndex);
            boolean hasMismatch = false;

            for (int y = 0; y < layers.length; y++) {
                String[] rows = layers[y];

                if (rows == null) {
                    return false;
                }

                for (int z = 0; z < rows.length; z++) {
                    String[] columns = this.parseLayerRow(rows[z]);

                    for (int x = 0; x < columns.length; x++) {
                        String symbol = this.normalizeSymbol(columns[x]);

                        if (symbol.isEmpty() || symbol.equals(".") || symbol.equals("_")) {
                            continue;
                        }

                        Vector3i relativeOffset = new Vector3i(
                                x - patternPivot.x,
                                y - patternPivot.y,
                                z - patternPivot.z
                        );
                        Vector3i rotatedOffset = Rotation.rotate(relativeOffset, tuple.yaw(), tuple.pitch(), tuple.roll());
                        Vector3i worldPos = controllerPos.clone().add(rotatedOffset);

                        MultiblockPatternAsset.LayerBlock expectedBlock = symbolLookup.get(symbol);
                        boolean expectsNullBlock = "NULL".equals(symbol) || this.isExpectedNullBlock(expectedBlock);

                        if (expectedBlock == null && !expectsNullBlock) {
                            hasMismatch = true;

                            this.onPatternValidationMismatch(
                                    world,
                                    pattern,
                                    controllerPos,
                                    worldPos,
                                    null,
                                    world.getBlockType(worldPos.x, worldPos.y, worldPos.z),
                                    ValidationFailureReason.WRONG_BLOCK
                            );

                            continue;
                        }

                        BlockType placedBlock = world.getBlockType(worldPos.x, worldPos.y, worldPos.z);

                        boolean matches = expectsNullBlock || this.matchesExpectedBlock(placedBlock, expectedBlock);

                        if (!matches) {
                            hasMismatch = true;
                            ValidationFailureReason reason = (placedBlock == null || (placedBlock.getGroup() != null && placedBlock.getGroup().equals("Air")) || placedBlock.getId().endsWith("_MLGhost"))
                                    ? ValidationFailureReason.MISSING_BLOCK
                                    : ValidationFailureReason.WRONG_BLOCK;

                            this.onPatternValidationMismatch(
                                    world,
                                    pattern,
                                    controllerPos,
                                    worldPos,
                                    expectedBlock,
                                    placedBlock,
                                    reason
                            );

                            continue;
                        }

                        if (relativeOffset.x != 0 || relativeOffset.y != 0 || relativeOffset.z != 0) {
                            functionalBlocks.add(worldPos);
                        }
                    }
                }
            }

            if (hasMismatch) {
                functionalBlocks.clear();
            }

            return !hasMismatch;
        }

        protected void onPatternValidationMismatch(
                World world,
                MultiblockPatternAsset pattern,
                Vector3i controllerPos,
                Vector3i mismatchWorldPos,
                @NullableDecl MultiblockPatternAsset.LayerBlock expectedBlock,
                @NullableDecl BlockType foundBlock,
                ValidationFailureReason reason
        ) {
            // Template hook for visuals/logging when a structure block is missing or incorrect.

            if (reason == ValidationFailureReason.WRONG_BLOCK) {
                DebugUtils.addCube(world, mismatchWorldPos.toVector3d().add(0.5, 0.5, 0.5), new Vector3f(1, 0, 0), 1.01, 2);
            } else {
                if (expectedBlock != null) {
                    DebugUtils.addCube(world, mismatchWorldPos.toVector3d().add(0.5, 0.5, 0.5), new Vector3f(0, 1, 0), 0.51, 2);
                    world.setBlock(mismatchWorldPos.x, mismatchWorldPos.y, mismatchWorldPos.z, expectedBlock.getBlockId() + "_MLGhost", 0);
                }
            }
        }

        private Map<String, MultiblockPatternAsset.LayerBlock> buildSymbolLookup(MultiblockPatternAsset.LayerBlock[] blockMap) {
            Map<String, MultiblockPatternAsset.LayerBlock> lookup = new HashMap<>();

            for (MultiblockPatternAsset.LayerBlock block : blockMap) {
                String symbol = block == null ? "" : this.normalizeSymbol(block.getBlockSymbol());

                if (block == null || symbol.isEmpty() || symbol.equals(".") || symbol.equals("_")) {
                    continue;
                }

                lookup.put(symbol, block);
            }

            return lookup;
        }

        @NullableDecl
        private Vector3i findPatternPivot(MultiblockPatternAsset pattern, String[][] layers, Map<String, MultiblockPatternAsset.LayerBlock> symbolLookup) {
            String coreBlockId = pattern.getCoreBlockId();

            if (coreBlockId == null || coreBlockId.isBlank()) {
                return null;
            }

            for (int y = 0; y < layers.length; y++) {
                String[] rows = layers[y];

                if (rows == null) {
                    continue;
                }

                for (int z = 0; z < rows.length; z++) {
                    String[] columns = this.parseLayerRow(rows[z]);

                    for (int x = 0; x < columns.length; x++) {
                        String symbol = this.normalizeSymbol(columns[x]);
                        MultiblockPatternAsset.LayerBlock block = symbolLookup.get(symbol);

                        if (block != null && coreBlockId.equals(block.getBlockId())) {
                            return new Vector3i(x, y, z);
                        }
                    }
                }
            }

            return null;
        }

        private String[] parseLayerRow(@NullableDecl String row) {
            if (row == null || row.isBlank()) {
                return new String[0];
            }

            return row.split(",");
        }

        private String normalizeSymbol(@NullableDecl String symbol) {
            if (symbol == null) {
                return "";
            }

            return symbol.trim().toUpperCase();
        }

        private boolean isExpectedNullBlock(@NullableDecl MultiblockPatternAsset.LayerBlock expectedBlock) {
            return expectedBlock != null
                    && expectedBlock.getBlockId() != null
                    && "Null".equalsIgnoreCase(expectedBlock.getBlockId());
        }

        private boolean matchesExpectedBlock(@NullableDecl BlockType blockType, MultiblockPatternAsset.LayerBlock expectedBlock) {
            if (blockType == null || expectedBlock == null) {
                return false;
            }

            String expectedBlockId = expectedBlock.getBlockId();

            if (expectedBlockId != null && !expectedBlockId.isBlank() && expectedBlockId.equals(blockType.getId())) {
                return true;
            }

            String[] acceptedTags = expectedBlock.getAcceptedTags();

            if (acceptedTags == null || acceptedTags.length == 0 || blockType.getData() == null || blockType.getData().getRawTags().isEmpty()) {
                return false;
            }

            for (String acceptedTag : acceptedTags) {
                if (acceptedTag != null && blockType.getData().getRawTags().containsKey(acceptedTag)) {
                    return true;
                }
            }

            return false;
        }

        @NullableDecl
        @Override
        public Query<ChunkStore> getQuery() {
            return MultiblockController.getComponentType();
        }
    }
}
