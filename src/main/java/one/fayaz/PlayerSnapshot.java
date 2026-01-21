package one.fayaz;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.List;

public class PlayerSnapshot {
    ServerLevel level;
    Vec3 pos;
    float yaw, pitch;

    List<ItemStack> inventory;

    int xpLevel;
    float xpProgress;
    int xpTotal;
    List<MobEffectInstance> effects;

    float health;

    int food;
    float saturation;

    BlockPos respawnPos;
    ResourceKey<Level> respawnDim;
    float respawnYaw;
    float respawnPitch;

    boolean respawnForced;
}
