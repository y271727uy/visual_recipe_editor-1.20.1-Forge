package com.visual_recipe_editor.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.visual_recipe_editor.menu.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class RecipeCommands {
    private static final String[] WORKSTATION_TYPES = {
        "crafting", "furnace", "blast_furnace", "smoker", "smithing", "stonecutter",
        "cutting_board", "cooking_pot", "farmersdelight:cutting_board", "farmersdelight:cooking_pot"
    };

    private static final SuggestionProvider<CommandSourceStack> WORKSTATION_SUGGESTIONS = 
        (context, builder) -> SharedSuggestionProvider.suggest(WORKSTATION_TYPES, builder);

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("recipeexporter")
                        .then(Commands.argument("workstation", StringArgumentType.string())
                                .suggests(WORKSTATION_SUGGESTIONS)
                                .executes(RecipeCommands::openWorkstationMenu))
                        .executes(context -> openWorkstationMenu(context, "crafting"))
        );

        dispatcher.register(
                Commands.literal("re")
                        .then(Commands.argument("workstation", StringArgumentType.string())
                                .suggests(WORKSTATION_SUGGESTIONS)
                                .executes(RecipeCommands::openWorkstationMenu))
                        .executes(context -> openWorkstationMenu(context, "crafting"))
        );
    }

    private static int openWorkstationMenu(CommandContext<CommandSourceStack> context) {
        String workstation = StringArgumentType.getString(context, "workstation");
        return openWorkstationMenu(context, workstation);
    }

    private static int openWorkstationMenu(CommandContext<CommandSourceStack> context, String workstation) {
        if (context.getSource().getEntity() instanceof Player player) {
            MenuProvider menuProvider = createMenuProvider(workstation);
            if (menuProvider != null) {
                player.openMenu(menuProvider);
            } else {
                player.sendSystemMessage(Component.translatable("command.visual_recipe_editor.unknown_workstation", workstation));
                player.sendSystemMessage(Component.translatable("command.visual_recipe_editor.available_types"));
            }
        }
        return 1;
    }

    private static MenuProvider createMenuProvider(String workstation) {
        return switch (workstation.toLowerCase()) {
            case "crafting", "workbench" -> createProvider(Component.translatable("gui.visual_recipe_editor.crafting.title"), CraftingRecipeMenu::new);
            case "furnace" -> createProvider(Component.translatable("gui.visual_recipe_editor.furnace.title"), FurnaceRecipeMenu::new);
            case "blast_furnace" -> createProvider(Component.translatable("gui.visual_recipe_editor.blast_furnace.title"), BlastFurnaceRecipeMenu::new);
            case "smoker" -> createProvider(Component.translatable("gui.visual_recipe_editor.smoker.title"), SmokerRecipeMenu::new);
            case "smithing" -> createProvider(Component.translatable("gui.visual_recipe_editor.smithing.title"), SmithingRecipeMenu::new);
            case "stonecutter" -> createProvider(Component.translatable("gui.visual_recipe_editor.stonecutter.title"), StonecutterRecipeMenu::new);
            case "cutting_board", "cutting", "farmersdelight:cutting_board" -> createProvider(Component.translatable("gui.visual_recipe_editor.cutting_board.title"), CuttingBoardRecipeMenu::new);
            case "cooking_pot", "cooking", "farmersdelight:cooking_pot" -> createProvider(Component.translatable("gui.visual_recipe_editor.cooking_pot.title"), CookingPotRecipeMenu::new);
            default -> null;
        };
    }

    private static MenuProvider createProvider(Component title, MenuFactory factory) {
        return new SimpleMenuProvider((id, inventory, player) -> factory.create(id, player), title);
    }

    @FunctionalInterface
    private interface MenuFactory {
        AbstractContainerMenu create(int id, Player player);
    }
}