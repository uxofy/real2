package com.example.real;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

public class RealClient implements ClientModInitializer {
    private static KeyMapping toggleKey;
    private static KeyMapping menuKey;

    private static boolean pearlInUse = false;
    private static boolean firing = false;
    private static boolean pending = false;
    private static int countdown = 0;

    @Override
    public void onInitializeClient() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.real.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_Z, KeyMapping.Category.MISC));
        menuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.real.menu", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_CONTROL, KeyMapping.Category.MISC));

        ClientTickEvents.START_CLIENT_TICK.register(RealClient::startTick);
        ClientTickEvents.END_CLIENT_TICK.register(RealClient::endTick);
    }

    /** Заглушка для совместимости со старой версией файла миксина. */
    public static boolean onUse() {
        return false;
    }

    // ---- вызывается из миксина ----

    public static void beforeUse(Player player, InteractionHand hand) {
        pearlInUse = !firing && player.getItemInHand(hand).is(Items.ENDER_PEARL);
    }

    public static void afterUse(InteractionResult result) {
        if (pearlInUse && result != null && result.consumesAction() && Config.INSTANCE.enabled) {
            pending = true;
            countdown = Config.INSTANCE.delay;
        }
        pearlInUse = false;
    }

    // ---- тики ----

    private static void startTick(Minecraft mc) {
        if (mc.player == null || mc.gameMode == null) {
            pending = false;
            return;
        }
        Config cfg = Config.INSTANCE;

        while (toggleKey.consumeClick()) {
            cfg.enabled = !cfg.enabled;
            cfg.save();
            mc.player.displayClientMessage(
                    Component.literal("Real: " + (cfg.enabled ? "ВКЛ" : "ВЫКЛ")), true);
        }
        while (menuKey.consumeClick()) {
            mc.setScreen(new ConfigScreen());
        }

        if (pending) {
            countdown--;
            if (countdown <= 0) fire(mc);
        }
    }

    private static void endTick(Minecraft mc) {
        // задержка 0: заряд кидается в конце того же тика, что и перл
        if (pending && countdown <= 0 && mc.player != null && mc.gameMode != null) {
            fire(mc);
        }
    }

    private static void fire(Minecraft mc) {
        pending = false;
        Player p = mc.player;
        if (p == null || mc.gameMode == null) return;

        firing = true;
        try {
            if (p.getMainHandItem().is(Items.WIND_CHARGE)) {
                use(mc, InteractionHand.MAIN_HAND);
                return;
            }
            if (p.getOffhandItem().is(Items.WIND_CHARGE)) {
                use(mc, InteractionHand.OFF_HAND);
                return;
            }
            Inventory inv = p.getInventory();
            for (int i = 0; i < 9; i++) {
                if (inv.getItem(i).is(Items.WIND_CHARGE)) {
                    int prev = inv.getSelectedSlot();
                    inv.setSelectedSlot(i);
                    use(mc, InteractionHand.MAIN_HAND);
                    inv.setSelectedSlot(prev);
                    return;
                }
            }
        } finally {
            firing = false;
        }
    }

    private static void use(Minecraft mc, InteractionHand hand) {
        mc.gameMode.useItem(mc.player, hand);
        mc.player.swing(hand);
    }
}
