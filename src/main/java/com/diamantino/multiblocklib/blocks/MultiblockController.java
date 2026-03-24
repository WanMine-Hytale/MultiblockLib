package com.diamantino.multiblocklib.blocks;

import com.diamantino.multiblocklib.MultiblockLib;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

public class MultiblockController implements Component<ChunkStore> {
    public static final BuilderCodec<MultiblockController> CODEC = BuilderCodec.builder(
                    MultiblockController.class,
                    MultiblockController::new
            )
            .append(new KeyedCodec<>("MultiblockId", Codec.STRING),
                    (multiblockController, id) -> multiblockController.multiblockId = id,
                    multiblockController -> multiblockController.multiblockId
            )
            .add()
            .append(
                    new KeyedCodec<>("LastUpdate", Codec.LONG),
                    (multiblockController, lu) -> multiblockController.lastUpdate = lu,
                    multiblockController -> multiblockController.lastUpdate
            )
            .add()
            .append(
                    new KeyedCodec<>("IsFormed", Codec.BOOLEAN),
                    (multiblockController, formed) -> multiblockController.isFormed = formed,
                    multiblockController -> multiblockController.isFormed
            )
            .add()
            .append(
                    new KeyedCodec<>("FunctionalBlockPositions", new ArrayCodec<>(Codec.INT_ARRAY, int[][]::new)),
                    (multiblockController, pos) -> {
                        multiblockController.functionalBlockPositions = new Vector3i[pos.length];

                        for (int i = 0; i < pos.length; i++) {
                            multiblockController.functionalBlockPositions[i] = new Vector3i(pos[i][0], pos[i][1], pos[i][2]);
                        }
                    },
                    multiblockController -> {
                        int[][] result = new int[multiblockController.functionalBlockPositions.length][3];

                        for (int i = 0; i < multiblockController.functionalBlockPositions.length; i++) {
                            Vector3i pos = multiblockController.functionalBlockPositions[i];
                            result[i] = new int[] {pos.x, pos.y, pos.z};
                        }

                        return result;
                    }
            )
            .add()
            .build();

    protected String multiblockId;
    protected long lastUpdate;
    protected boolean isFormed;
    protected Vector3i[] functionalBlockPositions;

    public MultiblockController() {
        this.multiblockId = "";
        this.lastUpdate = System.currentTimeMillis();
        this.isFormed = false;
        this.functionalBlockPositions = new Vector3i[0];
    }

    public MultiblockController(MultiblockController other) {
        this.multiblockId = other.multiblockId;
        this.lastUpdate = other.lastUpdate;
        this.isFormed = other.isFormed;
        this.functionalBlockPositions = other.functionalBlockPositions.clone();
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public boolean isFormed() {
        return isFormed;
    }

    public void setFormed(boolean formed) {
        isFormed = formed;
    }

    public Vector3i[] getFunctionalBlockPositions() {
        return functionalBlockPositions;
    }

    public void setFunctionalBlockPositions(Vector3i[] functionalBlockPositions) {
        this.functionalBlockPositions = functionalBlockPositions;
    }

    public static ComponentType<ChunkStore, MultiblockController> getComponentType() {
        return MultiblockLib.getInstance().multiblockControllerType;
    }

    @NullableDecl
    @Override
    public Component<ChunkStore> clone() {
        return new MultiblockController(this);
    }
}
