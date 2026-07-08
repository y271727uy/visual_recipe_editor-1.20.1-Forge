package com.visual_recipe_editor.menu;

import com.visual_recipe_editor.RecipeMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class CookingPotRecipeMenu extends AbstractContainerMenu {
    public static final int INPUT_SLOT_COUNT = 6;
    public static final int OUTPUT_SLOT = 6;
    public static final int CONTAINER_SLOT = 7;

    // 6 个输入槽位绑定为一个 3x2 区域，后续只需要改这里就能整体平移或改间距。
    private static final int INPUT_START_X = 70;
    private static final int INPUT_START_Y = 91;
    private static final int INPUT_COLUMNS = 3;
    private static final int INPUT_ROWS = 2;
    private static final int INPUT_SPACING_X = 18;
    private static final int INPUT_SPACING_Y = 18;

    protected final Container container;
    protected final Player player;

    public CookingPotRecipeMenu(int id, Player player) {
        super(com.visual_recipe_editor.RecipeMenuTypes.COOKING_POT_TYPE.get(), id);
        this.container = new SimpleContainer(8);
        this.player = player;

        for (int row = 0; row < INPUT_ROWS; row++) {
            for (int column = 0; column < INPUT_COLUMNS; column++) {
                int slotIndex = column + row * INPUT_COLUMNS;
                this.addSlot(new Slot(
                        this.container,
                        slotIndex,
                        INPUT_START_X + column * INPUT_SPACING_X,
                        INPUT_START_Y + row * INPUT_SPACING_Y
                ));
            }
        }
        // 真正输出槽：导出 JSON 时读取这里。
        // 要调整输出槽在 GUI 里的位置，就改下面这行最后两个数字：184, 39（x, y）。
        this.addSlot(new Slot(this.container, OUTPUT_SLOT, 164, 129) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return true;
            }
        });
        // 输入容器槽：比如碗、瓶子等输入容器，导出到 recipe 的 container 字段。
        // 要调整输入容器槽在 GUI 里的位置，就改下面这行最后两个数字：117, 74（x, y）。
        this.addSlot(new Slot(this.container, CONTAINER_SLOT, 132, 129));

        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(player.getInventory(), column + row * 9 + 9, 48 + column * 18, 158 + row * 18));
            }
        }

        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(player.getInventory(), column, 48 + column * 18, 216));
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
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            if (index == OUTPUT_SLOT) {
                if (!this.moveItemStackTo(slotStack, 8, 44, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 8 && index < 35) {
                if (!this.moveItemStackTo(slotStack, 35, 44, false) &&
                        !this.moveItemStackTo(slotStack, 0, 8, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index >= 35 && index < 44) {
                if (!this.moveItemStackTo(slotStack, 8, 35, false) &&
                        !this.moveItemStackTo(slotStack, 0, 8, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(slotStack, 8, 44, false)) {
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
        return this.container;
    }
}






