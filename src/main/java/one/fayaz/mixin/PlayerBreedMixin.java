package one.fayaz.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import one.fayaz.LockoutGame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Animal.class)
public class PlayerBreedMixin {

    @Inject(method = "spawnChildFromBreeding", at = @At("HEAD"))
    private void onBreed(ServerLevel level, Animal mate, CallbackInfo ci) {
        Animal self = (Animal)(Object)this;
        Player player = self.getLoveCause();

        if (player instanceof ServerPlayer serverPlayer) {
            LockoutGame.INSTANCE.handleBreed(serverPlayer, self);
        }
    }
}