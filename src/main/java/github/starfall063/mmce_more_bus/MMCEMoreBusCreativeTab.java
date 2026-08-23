package github.starfall063.mmce_more_bus;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public final class MMCEMoreBusCreativeTab extends CreativeTabs {

    public static final MMCEMoreBusCreativeTab INSTANCE = new MMCEMoreBusCreativeTab();

    private MMCEMoreBusCreativeTab() {
        super(Tags.MOD_ID);
    }

    @Nonnull
    @Override
    public ItemStack createIcon() {
        return ItemStack.EMPTY;
    }
}
