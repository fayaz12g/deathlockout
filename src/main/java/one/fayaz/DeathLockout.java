package one.fayaz;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeathLockout implements ModInitializer {
    public static final String MOD_ID = "deathlockout";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Death Lockout for 1.21.11");

        // 1. Register Networking
        LockoutNetworking.registerCommon();

        // 2. Register Death Event
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayer player) {
                LockoutGame.INSTANCE.handleDeath(player, damageSource);
            }
        });

        // 3. Register Commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("lockout")
                    .then(Commands.literal("start")
                            .then(Commands.argument("goal", IntegerArgumentType.integer(1))
                                    .then(Commands.argument("p1", EntityArgument.player())
                                            .then(Commands.argument("p2", EntityArgument.player())
                                                    .executes(ctx -> {
                                                        int goal = IntegerArgumentType.getInteger(ctx, "goal");
                                                        ServerPlayer p1 = EntityArgument.getPlayer(ctx, "p1");
                                                        ServerPlayer p2 = EntityArgument.getPlayer(ctx, "p2");

                                                        // This method inside LockoutGame already handles the UUID extraction and Broadcasting!
                                                        LockoutGame.INSTANCE.start(goal, p1, p2);

                                                        ctx.getSource().sendSystemMessage(Component.literal("Lockout Started! Goal: " + goal));
                                                        return 1;
                                                    })
                                            )
                                    )
                            )
                    )
                    .then(Commands.literal("reset")
                            .executes(ctx -> {
                                LockoutGame.INSTANCE.stop(ctx.getSource().getServer());
                                ctx.getSource().sendSystemMessage(Component.literal("Lockout Reset."));
                                return 1;
                            })
                    )
            );
        });
    }
}