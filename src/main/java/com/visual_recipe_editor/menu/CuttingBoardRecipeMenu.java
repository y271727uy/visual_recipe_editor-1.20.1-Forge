package com.visual_recipe_editor.menu;

import com.visual_recipe_editor.RecipeMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CuttingBoardRecipeMenu extends AbstractContainerMenu {
    // 槽位索引定义：Screen 导出 JSON 时也是按这些索引读数据。
    // 0 = 输入槽，1 = 工具槽，2~5 = 4 个输出槽。
    public static final int INPUT_SLOT = 0;
    public static final int TOOL_SLOT = 1;
    public static final int FIRST_OUTPUT_SLOT = 2;
    public static final int OUTPUT_SLOT_COUNT = 4;
    public static final int MENU_SLOT_COUNT = FIRST_OUTPUT_SLOT + OUTPUT_SLOT_COUNT;

    // 输入槽坐标：改这里可以调整原料输入槽位置。
    private static final int INPUT_SLOT_X = 46;
    private static final int INPUT_SLOT_Y = 50;

    // 工具槽坐标：改这里可以调整工具槽位置。
    private static final int TOOL_SLOT_X = 46;
    private static final int TOOL_SLOT_Y = 28;

    // ===== 这组常量就是“四个概率输出栏”的位置基准 =====
    // 也就是：左上、右上、左下、右下 这 4 个输出槽。
    // Screen 会复用这组坐标去放 4 个概率输入框，所以想整体调整位置，就改这里。
    public static final int OUTPUT_START_X = 116;
    public static final int OUTPUT_START_Y = 23;
    public static final int OUTPUT_COLUMNS = 2;
    public static final int OUTPUT_ROWS = 2;
    public static final int OUTPUT_SPACING_X = 18;
    public static final int OUTPUT_SPACING_Y = 18;

    // ===== 玩家“放物品的物品栏”整块布局总表 =====
    // 下面这些不是标题文字位置，而是真正能放物品的槽位位置。
    // 这里把“背包 27 格 + 快捷栏 9 格”当成一个整体处理：
    // 以后只需要调这一组常量，不用再分别折腾背包和快捷栏。

    // 玩家物品栏整块左上角起点：
    // 改这里会把“背包 27 格 + 快捷栏 9 格”一起平移。
    private static final int PLAYER_INVENTORY_ORIGIN_X = 8;

    // 27 格背包三行的 Y 坐标：
    // 这里不是统一间距，而是每一行单独修正。
    // 你说的“第一行低 1 像素、第二行高 1 像素、第三行高 2 像素”，就直接改这三个值。
    private static final int PLAYER_INVENTORY_ROW_1_Y = 89;
    private static final int PLAYER_INVENTORY_ROW_2_Y = 109;
    private static final int PLAYER_INVENTORY_ROW_3_Y = 128;

    // 玩家背包区域尺寸：默认 9 列 3 行。
    private static final int PLAYER_INVENTORY_COLUMNS = 9;
    private static final int PLAYER_INVENTORY_ROWS = 3;

    // 玩家物品栏每个槽位之间的间距。
    // 一般保持 18，不建议乱改；如果你的贴图槽位间隔特殊，再改这里。
    private static final int PLAYER_SLOT_SPACING_X = 18;
    private static final int PLAYER_SLOT_SPACING_Y = 18;

    // 背包 27 格和快捷栏 9 格之间的额外垂直间隔。
    // 改这里可以只调整快捷栏和上面 27 格之间的距离。
    private static final int PLAYER_INVENTORY_TO_HOTBAR_GAP = 5;

    // 快捷栏 Y 直接跟着第三行背包往下走，这样背包三行单独修正后，快捷栏仍然能保持相对位置。
    private static final int HOTBAR_Y = PLAYER_INVENTORY_ROW_3_Y + PLAYER_SLOT_SPACING_Y + PLAYER_INVENTORY_TO_HOTBAR_GAP;

    protected final Container container;
    protected final Player player;

    public CuttingBoardRecipeMenu(int id, Player player) {
        super(RecipeMenuTypes.CUTTING_BOARD_TYPE.get(), id);
        // 容器总槽位数 = 输入 1 + 工具 1 + 输出 4。
        this.container = new SimpleContainer(MENU_SLOT_COUNT);
        this.player = player;

        // 输入槽：放被切的原料。
        this.addSlot(new Slot(this.container, INPUT_SLOT, INPUT_SLOT_X, INPUT_SLOT_Y));
        // 工具槽：可选；如果这里留空，导出 JSON 时会自动使用 forge:tools/knives。
        this.addSlot(new Slot(this.container, TOOL_SLOT, TOOL_SLOT_X, TOOL_SLOT_Y));

        // 4 个输出槽：按 2x2 区域统一生成。
        // 槽位索引映射：
        // row=0 col=0 -> 2
        // row=0 col=1 -> 3
        // row=1 col=0 -> 4
        // row=1 col=1 -> 5
        for (int row = 0; row < OUTPUT_ROWS; row++) {
            for (int column = 0; column < OUTPUT_COLUMNS; column++) {
                int slotIndex = FIRST_OUTPUT_SLOT + column + row * OUTPUT_COLUMNS;
                this.addSlot(new Slot(
                        this.container,
                        slotIndex,
                        OUTPUT_START_X + column * OUTPUT_SPACING_X,
                        OUTPUT_START_Y + row * OUTPUT_SPACING_Y
                ) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return true;
                    }
                });
            }
        }

        // 玩家背包 3 行位置：这里是“放物品的物品栏”上半部分 27 格。
        // 这里按三行分别写死 Y 坐标，方便修正每一行的像素偏差。
        for (int row = 0; row < PLAYER_INVENTORY_ROWS; row++) {
            for (int column = 0; column < PLAYER_INVENTORY_COLUMNS; column++) {
                int slotY = switch (row) {
                    case 0 -> PLAYER_INVENTORY_ROW_1_Y;
                    case 1 -> PLAYER_INVENTORY_ROW_2_Y;
                    default -> PLAYER_INVENTORY_ROW_3_Y;
                };
                this.addSlot(new Slot(
                        player.getInventory(),
                        column + row * 9 + 9,
                        PLAYER_INVENTORY_ORIGIN_X + column * PLAYER_SLOT_SPACING_X,
                        slotY
                ));
            }
        }

        // 玩家快捷栏位置：这里是“放物品的物品栏”最下面那 9 格。
        // X 跟着整块起点一起走，Y 由 HOTBAR_Y 自动推导，不需要单独调两份坐标。
        for (int column = 0; column < PLAYER_INVENTORY_COLUMNS; column++) {
            this.addSlot(new Slot(
                    player.getInventory(),
                    column,
                    PLAYER_INVENTORY_ORIGIN_X + column * PLAYER_SLOT_SPACING_X,
                    HOTBAR_Y
            ));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.clearContainer(player, this.container);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // 注意：这里依赖槽位添加顺序固定为：
        // 菜单槽位 -> 玩家背包 27 格 -> 快捷栏 9 格。
        // 所以上面如果以后要改布局，只改坐标，不要改 addSlot 的顺序和数量。
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            // 从输出槽 Shift+点击：移动到玩家背包。
            if (index >= FIRST_OUTPUT_SLOT && index < MENU_SLOT_COUNT) {
                if (!this.moveItemStackTo(slotStack, MENU_SLOT_COUNT, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            // 从背包主区域 Shift+点击：优先去快捷栏，否则尝试塞进输入/工具槽。
            } else if (index >= MENU_SLOT_COUNT && index < MENU_SLOT_COUNT + 27) {
                if (!this.moveItemStackTo(slotStack, MENU_SLOT_COUNT + 27, this.slots.size(), false) &&
                        !this.moveItemStackTo(slotStack, INPUT_SLOT, FIRST_OUTPUT_SLOT, false)) {
                    return ItemStack.EMPTY;
                }
            // 从快捷栏 Shift+点击：优先去背包主区域，否则尝试塞进输入/工具槽。
            } else if (index >= MENU_SLOT_COUNT + 27 && index < this.slots.size()) {
                if (!this.moveItemStackTo(slotStack, MENU_SLOT_COUNT, MENU_SLOT_COUNT + 27, false) &&
                        !this.moveItemStackTo(slotStack, INPUT_SLOT, FIRST_OUTPUT_SLOT, false)) {
                    return ItemStack.EMPTY;
                }
            // 从输入槽 / 工具槽 Shift+点击：移动到玩家背包。
            } else if (!this.moveItemStackTo(slotStack, MENU_SLOT_COUNT, this.slots.size(), false)) {
                return ItemStack.EMPTY;
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            }

            slot.setChanged();
            if (slotStack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }
            slot.onTake(player, slotStack);
            broadcastChanges();
        }
        return itemstack;
    }

    public Container getContainer() {
        // Screen 通过这个容器读取槽位内容并导出配方 JSON。
        return this.container;
    }
}



