package com.visual_recipe_editor.screen;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.visual_recipe_editor.ExportFileNames;
import com.visual_recipe_editor.menu.CuttingBoardRecipeMenu;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;

public class CuttingBoardRecipeScreen extends AbstractContainerScreen<CuttingBoardRecipeMenu> {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation CUTTING_BOARD_LOCATION = ResourceLocation.fromNamespaceAndPath("visual_recipe_editor", "textures/gui/cutting_board.png");
    // 概率输入框尺寸：这里只管大小，不管位置。
    private static final int CHANCE_INPUT_WIDTH = 30;
    private static final int CHANCE_INPUT_HEIGHT = 20;
    // 概率输入框位置偏移：只改这里就能挪动四个概率输入栏。
    // X 负数表示在结果槽左边，Y 为 0 表示和结果槽同一行。
    // 如果你要“调位置”，改这两个；不要改上面的宽高。
    private static final int CHANCE_INPUT_OFFSET_X = -170;
    private static final int CHANCE_INPUT_OFFSET_Y = 0;

    private final EditBox[] chanceInputs = new EditBox[CuttingBoardRecipeMenu.OUTPUT_SLOT_COUNT];

    public CuttingBoardRecipeScreen(CuttingBoardRecipeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.minecraft = Minecraft.getInstance();
        // GUI 贴图尺寸：你现在的 cutting_board.png 是 176x176，就改这里。
        this.imageWidth = 176;
        this.imageHeight = 176;
        // 玩家背包标题（Inventory / 物品栏）文字的 Y 位置：只影响文字，不影响槽位。
        this.inventoryLabelY = 80;
    }

    @Override
    protected void init() {
        super.init();
        int buttonWidth = 40;
        // JSON 导出按钮的 X 起点：想左右移动按钮就改这里。
        int startX = this.leftPos + 88;

        // 4 个概率输入框的位置总表：
        // 1. 先用 CuttingBoardRecipeMenu 里的 OUTPUT_START_X / OUTPUT_START_Y / OUTPUT_SPACING_X / OUTPUT_SPACING_Y
        //    算出“对应输出槽”的位置。
        // 2. 再用 CHANCE_INPUT_WIDTH + CHANCE_INPUT_GAP 把输入框放到输出槽左边。
        // 所以：
        // - 想整体移动 4 个输入框：改 CuttingBoardRecipeMenu 里的输出槽坐标常量。
        // - 想只改输入框和输出槽之间的距离：改上面的 CHANCE_INPUT_GAP。
        // - 想改输入框大小：改 CHANCE_INPUT_WIDTH / CHANCE_INPUT_HEIGHT。
        for (int slot = 0; slot < CuttingBoardRecipeMenu.OUTPUT_SLOT_COUNT; slot++) {
            int column = slot % CuttingBoardRecipeMenu.OUTPUT_COLUMNS;
            int row = slot / CuttingBoardRecipeMenu.OUTPUT_COLUMNS;

            // 第 1 个概率输出栏：左上。
            // 第 2 个概率输出栏：右上。
            // 第 3 个概率输出栏：左下。
            // 第 4 个概率输出栏：右下。
            // 这里的 slot 顺序就是：0=左上，1=右上，2=左下，3=右下。

            int outputX = this.leftPos + CuttingBoardRecipeMenu.OUTPUT_START_X + column * CuttingBoardRecipeMenu.OUTPUT_SPACING_X;
            int outputY = this.topPos + CuttingBoardRecipeMenu.OUTPUT_START_Y + row * CuttingBoardRecipeMenu.OUTPUT_SPACING_Y;
            // 概率输入框位置：这里就是“位置”本身，不是尺寸。
            int chanceX = outputX + CHANCE_INPUT_OFFSET_X;

            // 这里就是第 1/2/3/4 个概率输入栏的实际位置。
            this.chanceInputs[slot] = new EditBox(
                    this.font,
                    chanceX,
                    outputY + CHANCE_INPUT_OFFSET_Y,
                    CHANCE_INPUT_WIDTH,
                    CHANCE_INPUT_HEIGHT,
                    Component.translatable("gui.visual_recipe_editor.input.chance")
            );
            this.chanceInputs[slot].setValue("0");
            this.chanceInputs[slot].setMaxLength(8);
            this.addRenderableWidget(this.chanceInputs[slot]);
        }

        Button exportJsonButton = Button.builder(Component.translatable("gui.visual_recipe_editor.button.json"), button -> {
                    if (!isRecipeValid()) {
                        showStatus(getRecipeValidationMessage());
                        return;
                    }
                    if (Screen.hasControlDown()) {
                        Minecraft.getInstance().keyboardHandler.setClipboard(generateJson());
                        showStatus("Cutting board recipe JSON copied.");
                    } else {
                        exportRecipe();
                    }
                })
                // JSON 导出按钮位置：改这里可以调整按钮位置。
                .pos(startX, this.topPos + 58)
                .size(buttonWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.visual_recipe_editor.tooltip.json")))
                .build();

        this.addRenderableWidget(exportJsonButton);
        // 界面标题文字 X 位置：只影响标题文字，不影响背景和槽位。
        this.titleLabelX = 8;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // GUI 背景贴图绘制：如果整张图大小改了，这里的最后两个 176 也要一起改。
        graphics.blit(CUTTING_BOARD_LOCATION, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight, 176, 176);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        // 输入槽文字位置：改这里可以调整“原料”文字的位置。
        graphics.drawString(this.font, Component.translatable("gui.visual_recipe_editor.label.tool"), 8, 18, 4210752, false);
        // 工具槽文字位置：改这里可以调整“工具”文字的位置。
        graphics.drawString(this.font, Component.translatable("gui.visual_recipe_editor.label.ingredient"), 8, 40, 4210752, false);
    }

    private boolean isRecipeValid() {
        // 至少需要：1 个输入 + 4 个输出槽里任意 1 个有内容。
        if (!hasIngredient()) {
            return false;
        }

        for (int slot = CuttingBoardRecipeMenu.FIRST_OUTPUT_SLOT;
             slot < CuttingBoardRecipeMenu.FIRST_OUTPUT_SLOT + CuttingBoardRecipeMenu.OUTPUT_SLOT_COUNT;
             slot++) {
            if (!this.menu.getContainer().getItem(slot).isEmpty()) {
                return true;
            }
        }

        return false;
    }

    private boolean hasIngredient() {
        return !this.menu.getContainer().getItem(CuttingBoardRecipeMenu.INPUT_SLOT).isEmpty();
    }

    private boolean hasResult() {
        for (int slot = CuttingBoardRecipeMenu.FIRST_OUTPUT_SLOT;
             slot < CuttingBoardRecipeMenu.FIRST_OUTPUT_SLOT + CuttingBoardRecipeMenu.OUTPUT_SLOT_COUNT;
             slot++) {
            if (!this.menu.getContainer().getItem(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private String getRecipeValidationMessage() {
        boolean hasIngredient = hasIngredient();
        boolean hasResult = hasResult();
        if (!hasIngredient && !hasResult) {
            return "Cutting board recipe needs an ingredient and at least one result.";
        }
        if (!hasIngredient) {
            return "Cutting board recipe needs an ingredient.";
        }
        return "Cutting board recipe needs at least one result.";
    }

    private JsonObject serializeIngredient(ItemStack stack) {
        // 输入原料导出格式：
        // { "item": "modid:item_name" }
        JsonObject ingredient = new JsonObject();
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId != null) {
            ingredient.addProperty("item", itemId.toString());
        }
        return ingredient;
    }

    private JsonObject serializeResult(ItemStack stack) {
        // 输出结果条目结构：
        // { "item": "farmersdelight:cod_slice" }
        // { "count": 2, "item": "minecraft:magenta_dye" }
        JsonObject result = new JsonObject();
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId != null) {
            result.addProperty("item", itemId.toString());
            if (stack.getCount() > 1) {
                result.addProperty("count", stack.getCount());
            }
        }
        return result;
    }

    private float getChance(int slot) {
        try {
            float chance = Float.parseFloat(this.chanceInputs[slot].getValue());
            return Math.max(chance, 0.0F);
        } catch (NumberFormatException ignored) {
            return 0.0F;
        }
    }

    private JsonObject serializeToolIngredient(ItemStack stack) {
        JsonObject tool = new JsonObject();
        if (stack.isEmpty()) {
            tool.addProperty("tag", "forge:tools/knives");
            // 工具槽为空：兜底使用默认刀具标签。
            tool.addProperty("tag", "forge:tools/knives");
            return tool;
        }

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId != null) {
            tool.addProperty("item", itemId.toString());
            // 工具槽有物品：导出该物品本身。
            tool.addProperty("item", itemId.toString());
        }
        return tool;
    }

    private String generateJson() {
        // 读取输入槽和工具槽内容。
        ItemStack input = this.menu.getContainer().getItem(CuttingBoardRecipeMenu.INPUT_SLOT);
        ItemStack toolStack = this.menu.getContainer().getItem(CuttingBoardRecipeMenu.TOOL_SLOT);

        JsonObject json = new JsonObject();
        json.addProperty("type", "farmersdelight:cutting");

        // ingredients：目前固定导出 1 个输入原料。
        JsonArray ingredients = new JsonArray();
        ingredients.add(serializeIngredient(input));
        json.add("ingredients", ingredients);

        // tool：Farmer's Delight cutting 的实际格式是一个数组。
        // 第 1 项固定是 tool_action。
        // 第 2 项是工具槽内容；如果工具槽为空，就兜底使用 c:tools/knife。
        json.add("tool", serializeToolIngredient(toolStack));

        // result：遍历 4 个输出槽，按槽位顺序把非空输出写进去。
        // 也就是说：左上 -> 右上 -> 左下 -> 右下。
        JsonArray results = new JsonArray();
        for (int slot = 0; slot < CuttingBoardRecipeMenu.OUTPUT_SLOT_COUNT; slot++) {
            ItemStack output = this.menu.getContainer().getItem(CuttingBoardRecipeMenu.FIRST_OUTPUT_SLOT + slot);
            if (!output.isEmpty()) {
                JsonObject resultEntry = new JsonObject();
                // 结果槽里的物品结构，必须保持 Farmer's Delight cutting 的实际格式。
                JsonObject resultItem = serializeResult(output);
                if (resultItem.has("item")) {
                    resultEntry.addProperty("item", resultItem.get("item").getAsString());
                }
                if (resultItem.has("count")) {
                    resultEntry.addProperty("count", resultItem.get("count").getAsInt());
                }

                // chance = 0 时不导出，让 JSON 保持最简结构。
                float chance = getChance(slot);
                if (chance > 0.0F) {
                    resultEntry.addProperty("chance", chance);
                }

                results.add(resultEntry);
            }
        }
        json.add("result", results);

        return new GsonBuilder().setPrettyPrinting().create().toJson(json);
    }

    private void exportRecipe() {
        try {
            // 导出目录：run/exported_recipes/
            Path recipesDir = FMLPaths.GAMEDIR.get().resolve("exported_recipes");
            // 文件名由 ExportFileNames 生成唯一随机名。
            Path recipePath = ExportFileNames.createUniqueJsonExportPath(recipesDir);
            Files.writeString(recipePath, generateJson());
            showStatus("Exported cutting board recipe: " + recipePath.getFileName());
        } catch (Exception e) {
            LOGGER.error("Failed to export cutting board recipe", e);
            showStatus("Failed to export cutting board recipe. See latest.log.");
        }
    }

    private void showStatus(String message) {
        if (this.minecraft != null && this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(Component.literal(message), false);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (EditBox chanceInput : this.chanceInputs) {
            if (chanceInput != null && chanceInput.isFocused()) {
                chanceInput.keyPressed(keyCode, scanCode, modifiers);
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (EditBox chanceInput : this.chanceInputs) {
            if (chanceInput != null && !chanceInput.isMouseOver(mouseX, mouseY) && chanceInput.isFocused()) {
                chanceInput.setFocused(false);
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }


    @Override
    public boolean isPauseScreen() {
        return false;
    }
}





