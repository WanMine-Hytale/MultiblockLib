package com.diamantino.multiblocklib.blocks;

import com.diamantino.multiblocklib.MultiblockLib;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.UUID;

public class MultiblockBlock implements Component<ChunkStore> {
    public static final BuilderCodec<MultiblockBlock> CODEC = BuilderCodec.builder(
                    MultiblockBlock.class,
                    MultiblockBlock::new
            )
            .append(
                    new KeyedCodec<>("LinkedControllerPosition", Codec.INT_ARRAY),
                    (multiblockBlock, pos) -> multiblockBlock.linkedControllerPosition = new Vector3i(pos[0], pos[1], pos[2]),
                    multiblockBlock -> new int[] {multiblockBlock.linkedControllerPosition.x, multiblockBlock.linkedControllerPosition.y, multiblockBlock.linkedControllerPosition.z}
            )
            .add()
            .build();

    protected Vector3i linkedControllerPosition;

    public MultiblockBlock() {
        this.linkedControllerPosition = new Vector3i(0, 0, 0);
    }

    public MultiblockBlock(MultiblockBlock other) {
        this.linkedControllerPosition = other.linkedControllerPosition;
    }

    public Vector3i getLinkedControllerPosition() {
        return linkedControllerPosition;
    }

    public void setLinkedControllerPosition(Vector3i linkedControllerPosition) {
        this.linkedControllerPosition = linkedControllerPosition;
    }

    public static ComponentType<ChunkStore, MultiblockBlock> getComponentType() {
        return MultiblockLib.getInstance().multiblockBlockType;
    }

    @NullableDecl
    @Override
    public Component<ChunkStore> clone() {
        return new MultiblockBlock(this);
    }
}
