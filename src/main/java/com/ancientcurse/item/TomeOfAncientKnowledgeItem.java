package com.ancientcurse.item;

import com.ancientcurse.AncientCurse;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

/**
 * The Tome of Ancient Knowledge - A comprehensive guide book for the Ancient Curse mod.
 * Opens the Patchouli book GUI when used.
 */
public class TomeOfAncientKnowledgeItem extends Item {

    public static final Identifier BOOK_ID = new Identifier(AncientCurse.MOD_ID, "ancient_knowledge");

    public TomeOfAncientKnowledgeItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
            // Open the Patchouli book on the server side
            vazkii.patchouli.api.PatchouliAPI.get().openBookGUI(serverPlayer, BOOK_ID);
        }

        return TypedActionResult.success(stack, world.isClient());
    }
}
