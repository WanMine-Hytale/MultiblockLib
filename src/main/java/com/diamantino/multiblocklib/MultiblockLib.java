package com.diamantino.multiblocklib;

import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;

public class MultiblockLib extends JavaPlugin {
    private static MultiblockLib instance;

    public MultiblockLib(@Nonnull JavaPluginInit init) {
        super(init);

        instance = this;
    }

    @Override
    protected void setup() {

    }

    @Override
    protected void start() {

    }

    public static MultiblockLib getInstance() {
        return instance;
    }
}
