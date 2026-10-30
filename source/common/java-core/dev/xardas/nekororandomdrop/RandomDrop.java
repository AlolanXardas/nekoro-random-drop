package dev.xardas.nekororandomdrop;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/** Shared constants. Each loader has its own entry point that wires the events to {@link DropLogic}. */
public final class RandomDrop {
    public static final String MOD_ID = "nekoro_random_drop";
    public static final Logger LOGGER = LogUtils.getLogger();

    private RandomDrop() { }
}
