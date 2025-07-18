package com.ancientcurse.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import com.ancientcurse.client.CurseZoneClientSettings;

public class CurseZoneMenuScreen extends Screen {
    private static final int GUI_WIDTH = 200;
    private static final int GUI_HEIGHT = 200;
    private static final int BUTTON_WIDTH = 160;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_SPACING = 5;
    
    private final BlockPos pos1;
    private final BlockPos pos2;
    private final Runnable clearSelectionCallback;
    
    public CurseZoneMenuScreen(BlockPos pos1, BlockPos pos2, Runnable clearSelectionCallback) {
        super(Text.translatable("gui.ancientcurse.curse_zone_menu.title"));
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.clearSelectionCallback = clearSelectionCallback;
    }
    
    @Override
    protected void init() {
        super.init();
        
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        int startX = centerX - GUI_WIDTH / 2;
        int startY = centerY - GUI_HEIGHT / 2;
        
        int buttonY = startY + 40;
        
        // Create Zone button (only if selection exists)
        if (pos1 != null && pos2 != null) {
            this.addDrawableChild(ButtonWidget.builder(
                    Text.translatable("gui.ancientcurse.curse_zone_menu.create"),
                    button -> {
                        this.client.setScreen(new CurseZoneEditorScreen(pos1, pos2));
                    }
                )
                .dimensions(centerX - BUTTON_WIDTH / 2, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build()
            );
            buttonY += BUTTON_HEIGHT + BUTTON_SPACING;
            
            // Clear Selection button
            this.addDrawableChild(ButtonWidget.builder(
                    Text.translatable("gui.ancientcurse.curse_zone_menu.clear"),
                    button -> {
                        if (clearSelectionCallback != null) {
                            clearSelectionCallback.run();
                        }
                        this.close();
                    }
                )
                .dimensions(centerX - BUTTON_WIDTH / 2, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build()
            );
            buttonY += BUTTON_HEIGHT + BUTTON_SPACING;
        }
        
        // Manage All Zones button
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.ancientcurse.curse_zone_menu.manage"),
                button -> {
                    this.client.setScreen(new CurseZoneManagerScreen());
                }
            )
            .dimensions(centerX - BUTTON_WIDTH / 2, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
            .build()
        );
        buttonY += BUTTON_HEIGHT + BUTTON_SPACING;
        
        // Show all borders checkbox
        CheckboxWidget showBordersCheckbox = new CheckboxWidget(
            centerX - BUTTON_WIDTH / 2,
            buttonY,
            BUTTON_WIDTH,
            20,
            Text.translatable("gui.ancientcurse.curse_zone_menu.show_all_borders"),
            CurseZoneClientSettings.shouldShowAllZoneBorders()
        ) {
            @Override
            public void onPress() {
                super.onPress();
                CurseZoneClientSettings.setShowAllZoneBorders(this.isChecked());
            }
        };
        this.addDrawableChild(showBordersCheckbox);
        buttonY += BUTTON_HEIGHT + BUTTON_SPACING;
        
        // Close button
        this.addDrawableChild(ButtonWidget.builder(
                Text.translatable("gui.ancientcurse.common.close"),
                button -> this.close()
            )
            .dimensions(centerX - BUTTON_WIDTH / 2, buttonY + 10, BUTTON_WIDTH, BUTTON_HEIGHT)
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
            Text.translatable("gui.ancientcurse.curse_zone_menu.title"),
            centerX,
            startY + 10,
            0xFFFFFF
        );
        
        // Show current selection info if exists
        if (pos1 != null && pos2 != null) {
            context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.translatable("gui.ancientcurse.curse_zone_menu.current_selection"),
                centerX,
                startY + 25,
                0xAAAAAA
            );
        }
        
        super.render(context, mouseX, mouseY, delta);
    }
}