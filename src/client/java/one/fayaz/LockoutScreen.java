package one.fayaz;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class LockoutScreen extends Screen {

    private final List<LockoutClient.PlayerData> players;
    private String currentMode;
    private int currentGoal;

    // Tab system
    private enum Tab {
        STATUS("Status"),
        PLAYERS("Players"),
        SETTINGS("Settings");

        private final String name;
        Tab(String name) { this.name = name; }
    }

    private Tab currentTab = Tab.STATUS;

    // Track which widgets belong to which tab
    private final List<Button> tabButtons = new ArrayList<>();

    // Chat message history
    private final LinkedList<String> chatMessages = new LinkedList<>();
    private static final int MAX_CHAT_MESSAGES = 5;

    // UI Components for STATUS tab
    private Button startButton;
    private Button stopButton;
    private Button pauseButton;
    private Button unpauseButton;
    private Button refreshStatusButton;

    // UI Components for PLAYERS tab
    private EditBox playerNameInput;
    private Button addPlayerButton;
    private Button colorPickerButton;
    private Integer selectedColor = null;

    // UI Components for SETTINGS tab
    private EditBox goalInput;
    private Button setGoalButton;

    // Mode selection buttons
    private Button modeDeathButton;
    private Button modeKillsButton;
    private Button modeArmorButton;
    private Button modeAdvancementsButton;
    private Button modeFoodsButton;
    private Button modeBreedButton;
    private Button modeMixedButton;

    // Armor sub-mode
    private Button armorSetButton;
    private Button armorPieceButton;

    // Death sub-mode
    private Button deathSourceButton;
    private Button deathMessageButton;

    // Configuration toggles
    private Checkbox switchCheckbox;
    private Checkbox resetWorldCheckbox;
    private Checkbox snarkyMessagesCheckbox;

    // Mixed mode toggles
    private Checkbox mixedDeathCheckbox;
    private Checkbox mixedKillsCheckbox;
    private Checkbox mixedArmorCheckbox;
    private Checkbox mixedAdvancementsCheckbox;
    private Checkbox mixedFoodsCheckbox;
    private Checkbox mixedBreedCheckbox;

    // Status info
    private final List<String> statusInfo = new ArrayList<>();

    public LockoutScreen(List<LockoutClient.PlayerData> players, String mode, int goal) {
        super(Component.literal("Lockout Configuration"));
        this.players = players;
        this.currentMode = mode;
        this.currentGoal = goal;
    }

    @Override
    protected void init() {
        super.init();

        // Clear tab buttons list
        tabButtons.clear();

        // Create tab buttons
        int tabWidth = 80;
        int tabY = 30;
        for (int i = 0; i < Tab.values().length; i++) {
            Tab tab = Tab.values()[i];
            int tabX = (width / 2) - (Tab.values().length * tabWidth / 2) + (i * tabWidth);
            Button tabButton = this.addRenderableWidget(Button.builder(
                    Component.literal(tab.name),
                    btn -> switchTab(tab)
            ).bounds(tabX, tabY, tabWidth - 5, 20).build());
            tabButtons.add(tabButton);
        }

        // Build widgets for current tab
        buildCurrentTab();
    }

    private void switchTab(Tab newTab) {
        this.currentTab = newTab;
        this.rebuildWidgets();
    }

    @Override
    protected void rebuildWidgets() {
        // Clear all widgets except tab buttons
        this.children().removeIf(widget -> !tabButtons.contains(widget));

        // Rebuild for current tab
        buildCurrentTab();
    }

    private void buildCurrentTab() {
        int startY = 60;

        switch (currentTab) {
            case STATUS -> buildStatusTab(startY);
            case PLAYERS -> buildPlayersTab(startY);
            case SETTINGS -> buildSettingsTab(startY);
        }
    }

    private void buildStatusTab(int startY) {
        int centerX = width / 2;

        // Game control buttons
        boolean isActive = LockoutClient.clientPaused || players.stream().anyMatch(p -> !p.claims.isEmpty());
        boolean isPaused = LockoutClient.clientPaused;

        startButton = this.addRenderableWidget(Button.builder(
                Component.literal("Start Game"),
                btn -> sendCommand("/lockout start")
        ).bounds(centerX - 205, startY, 100, 20).build());
        startButton.active = !isActive;

        stopButton = this.addRenderableWidget(Button.builder(
                Component.literal("Stop Game"),
                btn -> sendCommand("/lockout stop")
        ).bounds(centerX - 100, startY, 100, 20).build());
        stopButton.active = isActive;

        pauseButton = this.addRenderableWidget(Button.builder(
                Component.literal("Pause"),
                btn -> sendCommand("/lockout pause")
        ).bounds(centerX + 5, startY, 95, 20).build());
        pauseButton.active = isActive && !isPaused;

        unpauseButton = this.addRenderableWidget(Button.builder(
                Component.literal("Unpause"),
                btn -> sendCommand("/lockout unpause")
        ).bounds(centerX + 5, startY, 95, 20).build());
        unpauseButton.active = isActive && isPaused;
        unpauseButton.visible = isPaused;
        pauseButton.visible = !isPaused;

        refreshStatusButton = this.addRenderableWidget(Button.builder(
                Component.literal("Refresh Status"),
                btn -> {
                    statusInfo.clear();
                    sendCommand("/lockout status");
                }
        ).bounds(centerX + 105, startY, 100, 20).build());
    }

    private void buildPlayersTab(int startY) {
        int centerX = width / 2;

        // Join button (for self)
        this.addRenderableWidget(Button.builder(
                Component.literal("Join Game (Self)"),
                btn -> sendCommand("/lockout join")
        ).bounds(centerX - 75, startY, 150, 20).build());

        startY += 30;

        // Player name input
        final int playerInputY = startY;
        this.addRenderableOnly((graphics, mouseX, mouseY, delta) ->
                graphics.drawString(font, "Player Name:", centerX - 150, playerInputY + 5, 0xFFFFFF));

        playerNameInput = this.addRenderableWidget(new EditBox(
                font, centerX - 60, startY, 100, 20, Component.literal("Player Name")
        ));
        playerNameInput.setMaxLength(16);

        startY += 25;

        // Color picker
        final int colorPickerY = startY;
        this.addRenderableOnly((graphics, mouseX, mouseY, delta) -> {
            graphics.drawString(font, "Color (optional):", centerX - 150, colorPickerY + 5, 0xFFFFFF);
            if (selectedColor != null) {
                graphics.fill(centerX - 50, colorPickerY + 2, centerX - 30, colorPickerY + 18, selectedColor | 0xFF000000);
            }
        });

        colorPickerButton = this.addRenderableWidget(Button.builder(
                Component.literal("Pick Color"),
                btn -> cycleColor()
        ).bounds(centerX - 25, startY, 80, 20).build());

        Button clearColorButton = this.addRenderableWidget(Button.builder(
                Component.literal("Clear"),
                btn -> selectedColor = null
        ).bounds(centerX + 60, startY, 50, 20).build());

        startY += 30;

        // Add player button
        addPlayerButton = this.addRenderableWidget(Button.builder(
                Component.literal("Add Player"),
                btn -> {
                    String playerName = playerNameInput.getValue().trim();
                    if (!playerName.isEmpty()) {
                        if (selectedColor != null) {
                            sendCommand("/lockout player add " + playerName + " " + String.format("%06X", selectedColor & 0xFFFFFF));
                        } else {
                            sendCommand("/lockout player add " + playerName);
                        }
                        playerNameInput.setValue("");
                        selectedColor = null;
                    }
                }
        ).bounds(centerX - 50, startY, 100, 20).build());
    }

    private void cycleColor() {
        // Define available colors
        int[] colors = {
                0xFF5555, 0x5555FF, 0x55FF55, 0xFFFF55, 0xFF55FF,
                0x55FFFF, 0xFFAA00, 0xAA00AA, 0x00AA00, 0xFFAAAA
        };

        if (selectedColor == null) {
            selectedColor = colors[0];
        } else {
            // Find current color index
            int currentIndex = -1;
            for (int i = 0; i < colors.length; i++) {
                if (colors[i] == selectedColor) {
                    currentIndex = i;
                    break;
                }
            }
            // Cycle to next color
            selectedColor = colors[(currentIndex + 1) % colors.length];
        }
    }

    private void buildSettingsTab(int startY) {
        int centerX = width / 2;
        int leftCol = centerX - 150;
        int rightCol = centerX + 10;
        int currentY = startY;

        // Goal setting
        final int goalLabelY = currentY;
        this.addRenderableOnly((graphics, mouseX, mouseY, delta) ->
                graphics.drawString(font, "Goal:", leftCol, goalLabelY + 5, 0xFFFFFF));

        goalInput = this.addRenderableWidget(new EditBox(
                font, leftCol + 40, currentY, 50, 20, Component.literal("Goal")
        ));
        goalInput.setValue(String.valueOf(currentGoal));
        goalInput.setMaxLength(3);

        setGoalButton = this.addRenderableWidget(Button.builder(
                Component.literal("Set"),
                btn -> {
                    try {
                        int newGoal = Integer.parseInt(goalInput.getValue());
                        if (newGoal > 0) {
                            sendCommand("/lockout configure goal " + newGoal);
                        }
                    } catch (NumberFormatException e) {
                        // Invalid input
                    }
                }
        ).bounds(leftCol + 95, currentY, 40, 20).build());

        currentY += 30;

        // Game Mode Selection
        final int modeLabelY = currentY;
        this.addRenderableOnly((graphics, mouseX, mouseY, delta) ->
                graphics.drawString(font, "Game Mode:", leftCol, modeLabelY, 0xFFFFFF));
        currentY += 15;

        modeDeathButton = this.addRenderableWidget(Button.builder(
                Component.literal("Death" + (currentMode.equals("DEATH") ? " ✓" : "")),
                btn -> sendCommand("/lockout configure mode death source")
        ).bounds(leftCol, currentY, 70, 20).build());

        modeKillsButton = this.addRenderableWidget(Button.builder(
                Component.literal("Kills" + (currentMode.equals("KILLS") ? " ✓" : "")),
                btn -> sendCommand("/lockout configure mode kills")
        ).bounds(leftCol + 75, currentY, 70, 20).build());

        currentY += 25;

        modeArmorButton = this.addRenderableWidget(Button.builder(
                Component.literal("Armor" + (currentMode.equals("ARMOR") ? " ✓" : "")),
                btn -> sendCommand("/lockout configure mode armor set")
        ).bounds(leftCol, currentY, 70, 20).build());

        modeAdvancementsButton = this.addRenderableWidget(Button.builder(
                Component.literal("Advancements" + (currentMode.equals("ADVANCEMENTS") ? " ✓" : "")),
                btn -> sendCommand("/lockout configure mode advancements")
        ).bounds(leftCol + 75, currentY, 110, 20).build());

        currentY += 25;

        modeFoodsButton = this.addRenderableWidget(Button.builder(
                Component.literal("Foods" + (currentMode.equals("FOODS") ? " ✓" : "")),
                btn -> sendCommand("/lockout configure mode foods")
        ).bounds(leftCol, currentY, 70, 20).build());

        modeBreedButton = this.addRenderableWidget(Button.builder(
                Component.literal("Breed" + (currentMode.equals("BREED") ? " ✓" : "")),
                btn -> sendCommand("/lockout configure mode breed")
        ).bounds(leftCol + 75, currentY, 70, 20).build());

        currentY += 25;

        modeMixedButton = this.addRenderableWidget(Button.builder(
                Component.literal("Mixed" + (currentMode.equals("MIXED") ? " ✓" : "")),
                btn -> sendCommand("/lockout configure mode mixed")
        ).bounds(leftCol, currentY, 70, 20).build());

        currentY += 30;

        // Sub-mode settings (Death) - only show if DEATH mode
        if (currentMode.equals("DEATH")) {
            final int deathLabelY = currentY;
            this.addRenderableOnly((graphics, mouseX, mouseY, delta) ->
                    graphics.drawString(font, "Death Mode:", leftCol, deathLabelY, 0xFFFFFF));
            currentY += 15;

            deathSourceButton = this.addRenderableWidget(Button.builder(
                    Component.literal("By Source"),
                    btn -> sendCommand("/lockout configure mode death source")
            ).bounds(leftCol, currentY, 90, 20).build());

            deathMessageButton = this.addRenderableWidget(Button.builder(
                    Component.literal("By Message"),
                    btn -> sendCommand("/lockout configure mode death message")
            ).bounds(leftCol + 95, currentY, 90, 20).build());

            currentY += 25;
        }

        // Sub-mode settings (Armor) - only show if ARMOR mode
        if (currentMode.equals("ARMOR")) {
            final int armorLabelY = currentY;
            this.addRenderableOnly((graphics, mouseX, mouseY, delta) ->
                    graphics.drawString(font, "Armor Mode:", leftCol, armorLabelY, 0xFFFFFF));
            currentY += 15;

            armorSetButton = this.addRenderableWidget(Button.builder(
                    Component.literal("Full Set"),
                    btn -> sendCommand("/lockout configure mode armor set")
            ).bounds(leftCol, currentY, 90, 20).build());

            armorPieceButton = this.addRenderableWidget(Button.builder(
                    Component.literal("Any Piece"),
                    btn -> sendCommand("/lockout configure mode armor piece")
            ).bounds(leftCol + 95, currentY, 90, 20).build());

            currentY += 25;
        }

        // Mixed mode configuration - only show if MIXED mode
        if (currentMode.equals("MIXED")) {
            final int mixedLabelY = startY;
            this.addRenderableOnly((graphics, mouseX, mouseY, delta) ->
                    graphics.drawString(font, "Include in Mixed:", rightCol, mixedLabelY, 0xFFFFFF));
            int mixedY = startY + 15;

            mixedDeathCheckbox = this.addRenderableWidget(Checkbox.builder(
                    Component.literal("Death"), font
            ).pos(rightCol, mixedY).onValueChange((cb, val) -> {
                if (val) sendCommand("/lockout configure mixed include death");
                else sendCommand("/lockout configure mixed exclude death");
            }).build());
            mixedY += 25;

            mixedKillsCheckbox = this.addRenderableWidget(Checkbox.builder(
                    Component.literal("Kills"), font
            ).pos(rightCol, mixedY).onValueChange((cb, val) -> {
                if (val) sendCommand("/lockout configure mixed include kills");
                else sendCommand("/lockout configure mixed exclude kills");
            }).build());
            mixedY += 25;

            mixedArmorCheckbox = this.addRenderableWidget(Checkbox.builder(
                    Component.literal("Armor"), font
            ).pos(rightCol, mixedY).onValueChange((cb, val) -> {
                if (val) sendCommand("/lockout configure mixed include armor");
                else sendCommand("/lockout configure mixed exclude armor");
            }).build());
            mixedY += 25;

            mixedAdvancementsCheckbox = this.addRenderableWidget(Checkbox.builder(
                    Component.literal("Advancements"), font
            ).pos(rightCol, mixedY).onValueChange((cb, val) -> {
                if (val) sendCommand("/lockout configure mixed include advancements");
                else sendCommand("/lockout configure mixed exclude advancements");
            }).build());
            mixedY += 25;

            mixedFoodsCheckbox = this.addRenderableWidget(Checkbox.builder(
                    Component.literal("Foods"), font
            ).pos(rightCol, mixedY).onValueChange((cb, val) -> {
                if (val) sendCommand("/lockout configure mixed include foods");
                else sendCommand("/lockout configure mixed exclude foods");
            }).build());
            mixedY += 25;

            mixedBreedCheckbox = this.addRenderableWidget(Checkbox.builder(
                    Component.literal("Breed"), font
            ).pos(rightCol, mixedY).onValueChange((cb, val) -> {
                if (val) sendCommand("/lockout configure mixed include breed");
                else sendCommand("/lockout configure mixed exclude breed");
            }).build());
        }

        // General toggles
        currentY = startY + 200;
        final int optionsLabelY = currentY;
        this.addRenderableOnly((graphics, mouseX, mouseY, delta) ->
                graphics.drawString(font, "Options:", leftCol, optionsLabelY, 0xFFFFFF));
        currentY += 15;

        switchCheckbox = this.addRenderableWidget(Checkbox.builder(
                Component.literal("Player Switching"), font
        ).pos(leftCol, currentY).onValueChange((cb, val) ->
                sendCommand("/lockout configure switch " + val)
        ).build());
        currentY += 25;

        resetWorldCheckbox = this.addRenderableWidget(Checkbox.builder(
                Component.literal("Reset World on Start"), font
        ).pos(leftCol, currentY).onValueChange((cb, val) ->
                sendCommand("/lockout configure reset_world " + val)
        ).build());
        currentY += 25;

        snarkyMessagesCheckbox = this.addRenderableWidget(Checkbox.builder(
                Component.literal("Snarky Messages"), font
        ).pos(leftCol, currentY).onValueChange((cb, val) ->
                sendCommand("/lockout configure snarky_messages " + val)
        ).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderTransparentBackground(graphics);

        // Title
        graphics.drawCenteredString(
                font,
                "Lockout Configuration",
                width / 2,
                10,
                0xFFFFFF
        );

        // Render tab-specific content
        switch (currentTab) {
            case STATUS -> renderStatusTab(graphics, mouseX, mouseY);
            case PLAYERS -> renderPlayersTab(graphics, mouseX, mouseY);
            case SETTINGS -> renderSettingsTab(graphics, mouseX, mouseY);
        }

        // Render chat message box at bottom
        renderChatMessages(graphics);

        super.render(graphics, mouseX, mouseY, delta);
    }

    private void renderStatusTab(GuiGraphics graphics, int mouseX, int mouseY) {
        int centerX = width / 2;
        int startY = 95;

        // Display status info if available
        if (!statusInfo.isEmpty()) {
            int boxX = centerX - 150;
            int boxY = startY;
            int boxWidth = 300;
            int boxHeight = statusInfo.size() * 12 + 10;

            // Draw background box
            graphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0x88000000);
            graphics.fill(boxX, boxY, boxX + boxWidth, boxY + 1, 0xFFAAAAAA);
            graphics.fill(boxX, boxY + boxHeight - 1, boxX + boxWidth, boxY + boxHeight, 0xFFAAAAAA);
            graphics.fill(boxX, boxY, boxX + 1, boxY + boxHeight, 0xFFAAAAAA);
            graphics.fill(boxX + boxWidth - 1, boxY, boxX + boxWidth, boxY + boxHeight, 0xFFAAAAAA);

            int textY = boxY + 5;
            for (String line : statusInfo) {
                graphics.drawString(font, line, boxX + 5, textY, 0xFFFFFF);
                textY += 12;
            }

            startY = boxY + boxHeight + 10;
        } else {
            graphics.drawCenteredString(font, "Mode: " + currentMode, centerX, startY, 0xAAFFAA);
            graphics.drawCenteredString(font, "Goal: " + currentGoal, centerX, startY + 15, 0xAAFFAA);

            if (LockoutClient.clientPaused) {
                String pauseMsg = LockoutClient.clientPausedPlayerName.isEmpty()
                        ? "Game Paused"
                        : "Waiting for " + LockoutClient.clientPausedPlayerName;
                graphics.drawCenteredString(font, pauseMsg, centerX, startY + 30, 0xFFAA00);
            }
            startY += 50;
        }

        // Render the lockout HUD display
        if (!players.isEmpty() && currentGoal > 0) {
            LockoutHud.HoverInfo hoverInfo = LockoutHud.renderLockout(
                    graphics,
                    players,
                    currentMode,
                    currentGoal,
                    width,
                    startY + 10,
                    font,
                    mouseX,
                    mouseY
            );

            if (hoverInfo != null) {
                renderTooltip(graphics, hoverInfo);
            }
        }
    }

    private void renderPlayersTab(GuiGraphics graphics, int mouseX, int mouseY) {
        int centerX = width / 2;
        int startY = 180;

        if (players.isEmpty()) {
            graphics.drawCenteredString(font, "No players in game", centerX, startY + 20, 0xAAAAAA);
        } else {
            graphics.drawCenteredString(font, "Players (" + players.size() + "):", centerX, startY, 0xFFFFFF);

            int y = startY + 20;
            for (LockoutClient.PlayerData player : players) {
                String playerText = player.name + " - " + player.claims.size() + "/" + currentGoal;
                graphics.drawCenteredString(font, playerText, centerX, y, player.color);
                y += 15;
            }
        }
    }

    private void renderSettingsTab(GuiGraphics graphics, int mouseX, int mouseY) {
        // Settings are rendered via widgets
    }

    private void renderChatMessages(GuiGraphics graphics) {
        if (chatMessages.isEmpty()) return;

        int boxX = 10;
        int boxY = height - 80;
        int boxWidth = width - 20;
        int boxHeight = chatMessages.size() * 12 + 10;

        // Draw background
        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xAA000000);
        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + 1, 0xFFAAAAAA);

        int textY = boxY + 5;
        for (String message : chatMessages) {
            graphics.drawString(font, message, boxX + 5, textY, 0xFFFFFF);
            textY += 12;
        }
    }

    public void addChatMessage(String message) {
        // Check if message contains mode change confirmation
        if (message.contains("Mode set to:")) {
            String[] parts = message.split("Mode set to: ");
            if (parts.length > 1) {
                currentMode = parts[1].trim();
                this.rebuildWidgets(); // Rebuild to update checkmarks and show/hide sub-options
            }
        }

        // Check if message contains goal change confirmation
        if (message.contains("Goal set to:")) {
            String[] parts = message.split("Goal set to: ");
            if (parts.length > 1) {
                try {
                    currentGoal = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }

        // Handle status messages
        if (message.startsWith("---") || message.startsWith("Active:") ||
                message.startsWith("Paused:") || message.startsWith("Mode:") ||
                message.startsWith("Goal:") || message.startsWith("Spawnpoint:") ||
                message.startsWith("Players:") || message.startsWith("  -") ||
                message.startsWith("  Mixed Modes:")) {
            statusInfo.add(message);
            return; // Don't add to chat box
        }

        chatMessages.addFirst(message);
        if (chatMessages.size() > MAX_CHAT_MESSAGES) {
            chatMessages.removeLast();
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void renderTooltip(GuiGraphics graphics, LockoutHud.HoverInfo hoverInfo) {
        LockoutClient.PlayerData player = hoverInfo.player;
        int index = hoverInfo.slotIndex;

        if (index >= player.claims.size()) return;

        ItemStack stack = player.icons.get(index);
        LockoutNetworking.ClaimData claim = player.claims.get(index);
        String claimText = claim.id();

        List<Component> tooltip = stack.getTooltipLines(
                Item.TooltipContext.EMPTY,
                this.minecraft.player,
                this.minecraft.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL
        );

        tooltip.add(Component.literal(""));
        tooltip.add(Component.literal(player.name).withStyle(ChatFormatting.BOLD));
        tooltip.add(Component.literal(claimText).withStyle(ChatFormatting.GRAY));

        graphics.setComponentTooltipForNextFrame(font, tooltip, hoverInfo.mouseX, hoverInfo.mouseY);
    }

    private void sendCommand(String command) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.connection.sendCommand(command.substring(1));
        }
    }
}