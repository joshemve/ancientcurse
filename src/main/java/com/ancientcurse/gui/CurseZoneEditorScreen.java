package com.ancientcurse.gui;

import com.ancientcurse.network.CurseZonePackets;
import com.ancientcurse.util.CurseZoneData;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public class CurseZoneEditorScreen extends Screen {
    private static final int GUI_WIDTH = 248;
    private static final int GUI_HEIGHT = 250;
    
    private final BlockPos pos1;
    private final BlockPos pos2;
    private final String existingZoneId;
    private TextFieldWidget zoneNameField;
    private KhamsinSlider khamsinSlider;
    private AnkhDrainSlider ankhDrainSlider;
    private CheckboxWidget enableEffectsCheckbox;
    
    // Current values
    private String zoneName = "";
    private int khamsinLevel = 0;
    private int ankhDrainRate = 0;
    private boolean effectsEnabled = true;
    
    // Constructor for single position (legacy)
    public CurseZoneEditorScreen(BlockPos pos) {
        super(Text.translatable("gui.ancientcurse.curse_zone_editor.title"));
        this.pos1 = pos;
        this.pos2 = pos;
        this.existingZoneId = null;
    }
    
    // Constructor for new box selection
    public CurseZoneEditorScreen(BlockPos pos1, BlockPos pos2) {
        super(Text.translatable("gui.ancientcurse.curse_zone_editor.title"));
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.existingZoneId = null;
    }
    
    // Constructor for editing existing zone
    public CurseZoneEditorScreen(String zoneId, BlockPos pos1, BlockPos pos2, String zoneName, 
                                int khamsinLevel, int ankhDrainRate, boolean effectsEnabled) {
        super(Text.translatable("gui.ancientcurse.curse_zone_editor.edit_title"));
        this.existingZoneId = zoneId;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.zoneName = zoneName;
        this.khamsinLevel = khamsinLevel;
        this.ankhDrainRate = ankhDrainRate;
        this.effectsEnabled = effectsEnabled;
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int startX = centerX - GUI_WIDTH / 2;
        int startY = centerY - GUI_HEIGHT / 2;
        
        // Zone name field
        this.zoneNameField = new TextFieldWidget(
            this.textRenderer,
            startX + 10,
            startY + 35,
            GUI_WIDTH - 20,
            20,
            Text.translatable("gui.ancientcurse.curse_zone_editor.zone_name")
        );
        this.zoneNameField.setMaxLength(50);
        this.zoneNameField.setText(zoneName);
        this.addDrawableChild(this.zoneNameField);
        
        // Khamsin level slider (0-10)
        this.khamsinSlider = new KhamsinSlider(
            startX + 10,
            startY + 75,
            GUI_WIDTH - 20,
            20,
            khamsinLevel
        );
        this.addDrawableChild(this.khamsinSlider);
        
        // Ankh drain rate slider (0-10)
        this.ankhDrainSlider = new AnkhDrainSlider(
            startX + 10,
            startY + 115,
            GUI_WIDTH - 20,
            20,
            ankhDrainRate
        );
        this.addDrawableChild(this.ankhDrainSlider);
        
        // Enable effects checkbox
        this.enableEffectsCheckbox = new CheckboxWidget(
            startX + 10, 
            startY + 145,
            20,
            20,
            Text.translatable("gui.ancientcurse.curse_zone_editor.enable_effects"), 
            effectsEnabled
        );
        this.addDrawableChild(this.enableEffectsCheckbox);
        
        // Save button
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.ancientcurse.curse_zone_editor.save"),
                button -> this.saveAndClose()
            )
            .dimensions(startX + 10, startY + 210, 100, 20)
            .build()
        );
        
        // Cancel button
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.ancientcurse.curse_zone_editor.cancel"),
                button -> this.close()
            )
            .dimensions(startX + GUI_WIDTH - 110, startY + 210, 100, 20)
            .build()
        );
    }
    
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int startX = centerX - GUI_WIDTH / 2;
        int startY = centerY - GUI_HEIGHT / 2;
        
        // Draw GUI background
        context.fill(startX, startY, startX + GUI_WIDTH, startY + GUI_HEIGHT, 0xCC000000);
        context.drawBorder(startX, startY, GUI_WIDTH, GUI_HEIGHT, 0xFFFFFFFF);
        
        // Title
        context.drawCenteredTextWithShadow(
            this.textRenderer,
            Text.translatable("gui.ancientcurse.curse_zone_editor.title"),
            centerX,
            startY + 10,
            0xFFFFFF
        );
        
        // Area info
        BlockPos min = new BlockPos(
            Math.min(pos1.getX(), pos2.getX()),
            Math.min(pos1.getY(), pos2.getY()),
            Math.min(pos1.getZ(), pos2.getZ())
        );
        BlockPos max = new BlockPos(
            Math.max(pos1.getX(), pos2.getX()),
            Math.max(pos1.getY(), pos2.getY()),
            Math.max(pos1.getZ(), pos2.getZ())
        );
        
        context.drawTextWithShadow(
            this.textRenderer,
            Text.translatable("gui.ancientcurse.curse_zone_editor.area_info", min.toShortString(), max.toShortString()).getString(),
            startX + 10,
            startY + 185,
            0xAAAAAA
        );
        
        int blocks = (max.getX() - min.getX() + 1) * (max.getY() - min.getY() + 1) * (max.getZ() - min.getZ() + 1);
        context.drawTextWithShadow(
            this.textRenderer,
            Text.translatable("gui.ancientcurse.curse_zone_editor.total_blocks", blocks).getString(),
            startX + 10,
            startY + 195,
            0xAAAAAA
        );
        
        // Labels
        context.drawTextWithShadow(
            this.textRenderer,
            Text.translatable("gui.ancientcurse.curse_zone_editor.zone_name").getString() + ":",
            startX + 10,
            startY + 20,
            0xFFFFFF
        );
        
        context.drawTextWithShadow(
            this.textRenderer,
            Text.translatable("gui.ancientcurse.curse_zone_editor.khamsin_level").getString() + ":",
            startX + 10,
            startY + 60,
            0xFFFFFF
        );
        
        context.drawTextWithShadow(
            this.textRenderer,
            Text.translatable("gui.ancientcurse.curse_zone_editor.ankh_drain").getString() + ":",
            startX + 10,
            startY + 100,
            0xFFFFFF
        );
        
        super.render(context, mouseX, mouseY, delta);
    }
    
    private void saveAndClose() {
        // Collect data
        zoneName = zoneNameField.getText();
        khamsinLevel = (int) khamsinSlider.getValue();
        ankhDrainRate = (int) ankhDrainSlider.getValue();
        effectsEnabled = enableEffectsCheckbox.isChecked();
        
        if (existingZoneId != null) {
            // Update existing zone
            CurseZonePackets.sendUpdateZoneRequest(existingZoneId, zoneName, khamsinLevel, ankhDrainRate, effectsEnabled);
        } else {
            // Create new zone
            CurseZonePackets.sendBoxUpdateToServer(pos1, pos2, zoneName, khamsinLevel, ankhDrainRate, effectsEnabled);
        }
        
        this.close();
    }
    
    public boolean isPauseScreen() {
        return false;
    }
    
    // Custom slider for Khamsin Level
    private static class KhamsinSlider extends SliderWidget {
        private final int initialValue;
        
        public KhamsinSlider(int x, int y, int width, int height, int initialValue) {
            super(x, y, width, height, Text.literal("Khamsin Level: " + initialValue), initialValue / 10.0);
            this.initialValue = initialValue;
            this.updateMessage();
        }
        
        @Override
        protected void updateMessage() {
            int level = (int) (this.value * 10);
            this.setMessage(Text.literal("Khamsin Level: " + level));
        }
        
        @Override
        protected void applyValue() {
            // Value is automatically applied through getValue()
        }
        
        public int getValue() {
            return (int) (this.value * 10);
        }
    }
    
    // Custom slider for Ankh Drain Rate
    private static class AnkhDrainSlider extends SliderWidget {
        private final int initialValue;
        
        public AnkhDrainSlider(int x, int y, int width, int height, int initialValue) {
            super(x, y, width, height, Text.literal("Ankh Drain Rate: " + initialValue), initialValue / 10.0);
            this.initialValue = initialValue;
            this.updateMessage();
        }
        
        @Override
        protected void updateMessage() {
            int rate = (int) (this.value * 10);
            this.setMessage(Text.literal("Ankh Drain Rate: " + rate + "/min"));
        }
        
        @Override
        protected void applyValue() {
            // Value is automatically applied through getValue()
        }
        
        public int getValue() {
            return (int) (this.value * 10);
        }
    }
}