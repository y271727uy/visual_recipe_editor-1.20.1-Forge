package com.visual_recipe_editor.screen;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.visual_recipe_editor.ExportFileNames;
import com.visual_recipe_editor.menu.CookingPotRecipeMenu;
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

public class CookingPotRecipeScreen extends AbstractContainerScreen<CookingPotRecipeMenu> {
    private static final ResourceLocation COOKING_POT_LOCATION = ResourceLocation.fromNamespaceAndPath("visual_recipe_editor", "textures/gui/cooking_pot.png");
    private static final String[] RECIPE_BOOK_TABS = {"meals", "drinks", "misc"};
    // 整体 GUI 偏移：改这里可以把整张厨锅界面（背景、按钮、输入框）一起平移。
    // X 正数向右，负数向左；Y 正数向下，负数向上。
    private static final int GUI_OFFSET_X = 40;
    private static final int GUI_OFFSET_Y = 74;

    private Button exportJsonButton;
    private Button recipeBookTabButton;
    private EditBox cookingTimeInput;
    private EditBox experienceInput;
    private int recipeBookTabIndex = 0;

    public CookingPotRecipeScreen(CookingPotRecipeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.minecraft = Minecraft.getInstance();
        this.imageWidth = 256;
        this.imageHeight = 256;
        this.inventoryLabelY = 280;
        // 标题文字位置：
        // "gui.visual_recipe_editor.cooking_pot.title" 这种翻译 key 只决定“显示什么字”，不决定位置。
        // 真正控制标题横向位置的是 titleLabelX；如果你想单独控制纵向，也可以设置 titleLabelY。
        this.titleLabelX = 100;
    }

    @Override
    protected void init() {
        super.init();
        int buttonWidth = 40;
        // guiX / guiY 是当前厨锅 GUI 左上角的最终绘制基准点。
        int guiX = this.leftPos + GUI_OFFSET_X;
        int guiY = this.topPos + GUI_OFFSET_Y;
        // startX / sideX 是右侧控件区域的基准 X。
        int startX = guiX + 173;
        int sideX = guiX + 173;

        // 分类按钮位置：这里控制 recipe_book_tab（分类）按钮的位置。
        // 按钮上显示的文字来自翻译 key："gui.visual_recipe_editor.button.recipe_book_tab"。
        // 想改“分类”这几个字显示在哪，就改 .pos(sideX, guiY + 12) 这里的坐标。
        this.recipeBookTabButton = Button.builder(getRecipeBookTabMessage(), button -> cycleRecipeBookTab())
                .pos(sideX, guiY + 12)
                .size(80, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.visual_recipe_editor.tooltip.recipe_book_tab")))
                .build();

        // 时间输入框位置：这里控制 cooking time 文本框。
        // 文本框占位文字来自翻译 key："gui.visual_recipe_editor.input.cooking_time"。
        // 想改“烧制时间”这几个字所在输入框的位置，就改下面这一行的 sideX / guiY + 42。
        this.cookingTimeInput = new EditBox(this.font, sideX, guiY + 42, 60, 20,
                Component.translatable("gui.visual_recipe_editor.input.cooking_time"));
        this.cookingTimeInput.setValue("200");
        this.cookingTimeInput.setTooltip(Tooltip.create(Component.translatable("gui.visual_recipe_editor.tooltip.cooking_time", 200)));

        // 经验输入框位置：这里控制 experience 文本框。
        // 文本框占位文字来自翻译 key："gui.visual_recipe_editor.input.experience"。
        // 想改“经验值”这几个字所在输入框的位置，就改下面这一行的 sideX / guiY + 72。
        this.experienceInput = new EditBox(this.font, sideX, guiY + 72, 60, 20,
                Component.translatable("gui.visual_recipe_editor.input.experience"));
        this.experienceInput.setValue("0.0");
        this.experienceInput.setTooltip(Tooltip.create(Component.translatable("gui.visual_recipe_editor.tooltip.experience", 0.0F)));

        // JSON 导出按钮位置。
        this.exportJsonButton = Button.builder(Component.translatable("gui.visual_recipe_editor.button.json"), button -> {
                    if (!isRecipeValid()) {
                        return;
                    }
                    if (Screen.hasControlDown()) {
                        Minecraft.getInstance().keyboardHandler.setClipboard(generateJson());
                    } else {
                        exportRecipe();
                    }
                })
                .pos(startX, guiY + 102)
                .size(buttonWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("gui.visual_recipe_editor.tooltip.json")))
                .build();

        this.addRenderableWidget(this.recipeBookTabButton);
        this.addRenderableWidget(this.cookingTimeInput);
        this.addRenderableWidget(this.experienceInput);
        this.addRenderableWidget(this.exportJsonButton);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        // 背景贴图位置：如果你想让背景整体移动，优先改上面的 GUI_OFFSET_X / GUI_OFFSET_Y。
        graphics.blit(COOKING_POT_LOCATION, this.leftPos + GUI_OFFSET_X, this.topPos + GUI_OFFSET_Y, 0, 0, this.imageWidth, this.imageHeight, 256, 256);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 故意不调用 super.renderLabels(graphics, mouseX, mouseY)
        // 这样会一并隐藏：
        // 1. gui.visual_recipe_editor.cooking_pot.title（界面标题）
        // 2. 物品栏 / Inventory 标题
        // 同时这里也不再手动画 “容器” 文字，达到只移除用户指定的 3 处文字的目的。
    }

    private void cycleRecipeBookTab() {
        this.recipeBookTabIndex = (this.recipeBookTabIndex + 1) % RECIPE_BOOK_TABS.length;
        this.recipeBookTabButton.setMessage(getRecipeBookTabMessage());
    }

    private Component getRecipeBookTabMessage() {
        return Component.translatable("gui.visual_recipe_editor.button.recipe_book_tab",
                Component.translatable("gui.visual_recipe_editor.recipe_book_tab." + RECIPE_BOOK_TABS[this.recipeBookTabIndex]));
    }

    private boolean isRecipeValid() {
        if (this.menu.getSlot(CookingPotRecipeMenu.OUTPUT_SLOT).getItem().isEmpty()) {
            return false;
        }

        for (int slot = 0; slot < CookingPotRecipeMenu.INPUT_SLOT_COUNT; slot++) {
            if (!this.menu.getSlot(slot).getItem().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private JsonObject serializeIngredient(ItemStack stack) {
        JsonObject item = new JsonObject();
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId != null) {
            item.addProperty("item", itemId.toString());
        }
        return item;
    }

    private JsonObject serializeResult(ItemStack stack) {
        JsonObject item = new JsonObject();
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (itemId != null) {
            item.addProperty("item", itemId.toString());
            item.addProperty("count", Math.max(stack.getCount(), 1));
        }
        return item;
    }

    private int getCookingTime() {
        try {
            return Integer.parseInt(this.cookingTimeInput.getValue());
        } catch (NumberFormatException ignored) {
            return 200;
        }
    }

    private float getExperience() {
        try {
            return Float.parseFloat(this.experienceInput.getValue());
        } catch (NumberFormatException ignored) {
            return 0.0F;
        }
    }

    private String generateJson() {
        JsonObject json = new JsonObject();
        json.addProperty("type", "farmersdelight:cooking");
        json.addProperty("recipe_book_tab", RECIPE_BOOK_TABS[this.recipeBookTabIndex]);

        JsonArray ingredients = new JsonArray();
        for (int slot = 0; slot < CookingPotRecipeMenu.INPUT_SLOT_COUNT; slot++) {
            ItemStack stack = this.menu.getSlot(slot).getItem();
            if (!stack.isEmpty()) {
                ingredients.add(serializeIngredient(stack));
            }
        }
        json.add("ingredients", ingredients);

        json.add("result", serializeResult(this.menu.getSlot(CookingPotRecipeMenu.OUTPUT_SLOT).getItem()));

        ItemStack container = this.menu.getSlot(CookingPotRecipeMenu.CONTAINER_SLOT).getItem();
        if (!container.isEmpty()) {
            json.add("container", serializeResult(container));
        }

        int cookingTime = getCookingTime();
        if (cookingTime != 200) {
            json.addProperty("cookingtime", cookingTime);
        }

        float experience = getExperience();
        if (experience != 0.0F) {
            json.addProperty("experience", experience);
        }

        return new GsonBuilder().setPrettyPrinting().create().toJson(json);
    }

    private void exportRecipe() {
        try {
            Path recipesDir = FMLPaths.GAMEDIR.get().resolve("exported_recipes");
            Path recipePath = ExportFileNames.createUniqueJsonExportPath(recipesDir);
            Files.writeString(recipePath, generateJson());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.cookingTimeInput.isFocused()) {
            this.cookingTimeInput.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        if (this.experienceInput.isFocused()) {
            this.experienceInput.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.cookingTimeInput.isMouseOver(mouseX, mouseY) && this.cookingTimeInput.isFocused()) {
            this.cookingTimeInput.setFocused(false);
        }
        if (!this.experienceInput.isMouseOver(mouseX, mouseY) && this.experienceInput.isFocused()) {
            this.experienceInput.setFocused(false);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}









