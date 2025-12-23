package one.fayaz;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class DeathLockout implements ModInitializer {
    public static final String MOD_ID = "deathlockout";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Named colors for convenience
    private static final Map<String, Integer> NAMED_COLORS = new HashMap<>();
    static {
        NAMED_COLORS.put("red", 0xFF5555);
        NAMED_COLORS.put("orange", 0xFFAA00);
        NAMED_COLORS.put("yellow", 0xFFFF55);
        NAMED_COLORS.put("lime", 0x55FF55);
        NAMED_COLORS.put("green", 0x00AA00);
        NAMED_COLORS.put("cyan", 0x55FFFF);
        NAMED_COLORS.put("blue", 0x5555FF);
        NAMED_COLORS.put("purple", 0xAA00AA);
        NAMED_COLORS.put("magenta", 0xFF55FF);
        NAMED_COLORS.put("pink", 0xFFAAAA);
        NAMED_COLORS.put("white", 0xFFFFFF);
        NAMED_COLORS.put("gray", 0xAAAAAA);
        NAMED_COLORS.put("black", 0x000000);
    }

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
                    // /lockout start
                    .then(Commands.literal("start")
                            .executes(ctx -> {
                                if (!LockoutGame.INSTANCE.canStart()) {
                                    ctx.getSource().sendFailure(Component.literal("❌ Need at least 2 players and a goal > 0!"));
                                    return 0;
                                }
                                LockoutGame.INSTANCE.start(ctx.getSource().getServer());
                                return 1;
                            })
                    )
                    // /lockout goal <num>
                    .then(Commands.literal("goal")
                            .then(Commands.argument("number", IntegerArgumentType.integer(1))
                                    .executes(ctx -> {
                                        int goal = IntegerArgumentType.getInteger(ctx, "number");
                                        LockoutGame.INSTANCE.setGoal(goal);
                                        ctx.getSource().sendSystemMessage(Component.literal("✓ Goal set to: " + goal).withStyle(style -> style.withColor(0x55FF55)));
                                        return 1;
                                    })
                            )
                    )
                    // /lockout add <player> <color>
                    .then(Commands.literal("add")
                            .then(Commands.argument("player", EntityArgument.player())
                                    .then(Commands.argument("color", StringArgumentType.word())
                                            .suggests((context, builder) -> {
                                                // Suggest all named colors
                                                for (String colorName : NAMED_COLORS.keySet()) {
                                                    builder.suggest(colorName);
                                                }
                                                return builder.buildFuture();
                                            })
                                            .executes(ctx -> {
                                                ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
                                                String colorStr = StringArgumentType.getString(ctx, "color");

                                                int color = parseColor(colorStr);
                                                if (color == -1) {
                                                    ctx.getSource().sendFailure(Component.literal("❌ Invalid color! Use hex (#FF5555) or name (red, blue, etc.)"));
                                                    return 0;
                                                }

                                                if (LockoutGame.INSTANCE.addPlayer(player, color) == 0) {
                                                    ctx.getSource().sendSystemMessage(Component.literal("✓ Added " + player.getName().getString()).withStyle(style -> style.withColor(color)));
                                                }
                                                return 1;
                                            })
                                    )
                            )
                    )
                    // /lockout reset
                    .then(Commands.literal("reset")
                            .executes(ctx -> {
                                LockoutGame.INSTANCE.stop(ctx.getSource().getServer());
                                ctx.getSource().sendSystemMessage(Component.literal("✓ Lockout reset."));
                                return 1;
                            })
                    )
                    // /lockout status (helpful for debugging)
                    .then(Commands.literal("status")
                            .executes(ctx -> {
                                int goal = LockoutGame.INSTANCE.getGoal();
                                int playerCount = LockoutGame.INSTANCE.getPlayers().size();
                                boolean active = LockoutGame.INSTANCE.isActive();

                                ctx.getSource().sendSystemMessage(Component.literal("--- Lockout Status ---"));
                                ctx.getSource().sendSystemMessage(Component.literal("Active: " + (active ? "Yes" : "No")));
                                ctx.getSource().sendSystemMessage(Component.literal("Goal: " + goal));
                                ctx.getSource().sendSystemMessage(Component.literal("Players: " + playerCount));

                                for (PlayerEntry entry : LockoutGame.INSTANCE.getPlayers().values()) {
                                    ctx.getSource().sendSystemMessage(Component.literal("  - " + entry.getName() + " (" + entry.getScore() + " deaths)").withStyle(style -> style.withColor(entry.getColor())));
                                }

                                return 1;
                            })
                    )
            );
        });
    }

    private static int parseColor(String colorStr) {
        // Try named color first
        String lower = colorStr.toLowerCase();
        if (NAMED_COLORS.containsKey(lower)) {
            return NAMED_COLORS.get(lower);
        }

        // Try hex color
        try {
            if (colorStr.startsWith("#")) {
                return Integer.parseInt(colorStr.substring(1), 16);
            } else if (colorStr.startsWith("0x")) {
                return Integer.parseInt(colorStr.substring(2), 16);
            } else {
                return Integer.parseInt(colorStr, 16);
            }
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}