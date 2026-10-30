package dev.xardas.nekororandomdrop.compat.jei;

import java.util.List;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;

import dev.xardas.nekororandomdrop.DropDisplay;
import dev.xardas.nekororandomdrop.RandomDrop;
import dev.xardas.nekororandomdrop.Rule;
import dev.xardas.nekororandomdrop.RuleConfig;
import dev.xardas.nekororandomdrop.compat.VersionCompat;

/**
 * Only loaded when JEI is installed. Reads the player's own copy of the config file every time JEI starts
 * (joining a world, /reload), so it shows the rules of the modpack the player has. Nothing is sent by the server.
 */
@JeiPlugin
public final class RandomDropJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = VersionCompat.id(RandomDrop.MOD_ID + ":jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new RandomDropCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<Rule> rules = RuleConfig.readOnly();
        registration.addRecipes(RandomDropCategory.TYPE, rules.stream().map(DropDisplay::of).toList());
    }
}
