package dev.xardas.nekororandomdrop.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import dev.xardas.nekororandomdrop.DropDisplay;
import dev.xardas.nekororandomdrop.RandomDrop;

/** Source -> drop on top, chance next to it, description and notes below. */
public final class RandomDropCategory implements IRecipeCategory<DropDisplay> {
    public static final RecipeType<DropDisplay> TYPE = RecipeType.create(RandomDrop.MOD_ID, "random_drop", DropDisplay.class);

    private static final int WIDTH = 160;
    // the output slot background is 26x26 around the 18x18 slot, so the top row starts 4 px down to keep it inside
    private static final int ROW = 5;
    private static final int TEXT_TOP = 30;
    private static final int LINE = 10;
    private static final int MAX_LINES = 5;
    private static final int TEXT_COLOR = 0xFF404040;

    private final IDrawable icon;
    private final IDrawableStatic arrow;

    public RandomDropCategory(IGuiHelper gui) {
        // the Amethyst Cat Coin of Nekoro Coins, the icon of all Nekoro mods (a copy of its 32x32 texture, drawn at 16x16)
        this.icon = new IDrawable() {
            private final ResourceLocation texture = ResourceLocation.tryBuild(RandomDrop.MOD_ID, "textures/gui/nekoro_icon.png");

            @Override
            public int getWidth() {
                return 16;
            }

            @Override
            public int getHeight() {
                return 16;
            }

            @Override
            public void draw(GuiGraphics graphics, int x, int y) {
                graphics.blit(texture, x, y, 16, 16, 0, 0, 32, 32, 32, 32);
            }
        };
        this.arrow = gui.getRecipeArrow();
    }

    @Override
    public RecipeType<DropDisplay> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.nekororandomdrop.title");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return TEXT_TOP + LINE * MAX_LINES;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, DropDisplay recipe, IFocusGroup focuses) {
        if (!recipe.sources().isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, 1, ROW).addItemStacks(recipe.sources()).setStandardSlotBackground();
        } else if (recipe.sourceIcon() != null) {
            builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 1, ROW).addItemStack(recipe.sourceIcon()).setStandardSlotBackground();
        }
        if (recipe.output() != null) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 55, ROW).addItemStack(recipe.output()).setOutputSlotBackground();
        }
    }

    @Override
    public void draw(DropDisplay recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        Font font = Minecraft.getInstance().font;
        arrow.draw(graphics, 25, ROW + 1);
        graphics.drawString(font, recipe.chance(), 82, ROW + 5, TEXT_COLOR, false);
        int y = TEXT_TOP;
        int drawn = 0;
        for (Component line : recipe.lines()) {
            for (FormattedCharSequence part : font.split(line, WIDTH)) {
                if (drawn++ == MAX_LINES) return;
                graphics.drawString(font, part, 0, y, TEXT_COLOR, false);
                y += LINE;
            }
        }
    }
}
