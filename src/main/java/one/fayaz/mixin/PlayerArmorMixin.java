package one.fayaz.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import one.fayaz.LockoutGame;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class PlayerArmorMixin {

    @Inject(method = "onEquipItem", at = @At("TAIL"))
    private void onEquipItem(
            EquipmentSlot slot,
            ItemStack oldStack,
            ItemStack newStack,
            CallbackInfo ci
    ) {
        LivingEntity self = (LivingEntity)(Object)this;

        if (!(self instanceof ServerPlayer player)) return;

        if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR) {
            LockoutGame.INSTANCE.handleArmor(player);
        }
    }
}