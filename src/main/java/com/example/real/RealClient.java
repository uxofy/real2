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

    private static boolean blocked = false;

    private static void dbg(String msg) {
        Minecraft mc = Minecraft.getInstance();
        if (Config.INSTANCE.debug && mc.player != null) {
            mc.player.displayClientMessage(Component.literal("Real: " + msg), true);
        }
    }

    /** Вызывается из миксина вместо обычного ПКМ. true = отменить ванильное использование. */
    public static boolean onUse() {
        Minecraft mc = Minecraft.getInstance();
        if (!Config.INSTANCE.enabled || mc.player == null || mc.gameMode == null) return false;
        Player p = mc.player;
        // заряд ветра в правой руке + перл в левой -> ПКМ кидает перл, потом сам заряд
        if (!(p.getMainHandItem().is(Items.WIND_CHARGE) && p.getOffhandItem().is(Items.ENDER_PEARL))) return false;
        if (pending || blocked) return true;
        blocked = true;
        dbg("ПКМ перехвачен, кидаю перл");
        use(mc, InteractionHand.OFF_HAND); // afterUse сам запланирует заряд
        return true;
    }

    // ---- вызывается из миксина ----

    public static void beforeUse(Player player, InteractionHand hand) {
        pearlInUse = !firing && player.getItemInHand(hand).is(Items.ENDER_PEARL);
    }

    public static void afterUse(InteractionResult result) {
        if (pearlInUse && result != null && result.consumesAction() && Config.INSTANCE.enabled) {
            pending = true;
            countdown = Config.INSTANCE.delay;
            dbg("перл брошен, заряд через " + Config.INSTANCE.delay + " т.");
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

        if (!mc.options.keyUse.isDown()) blocked = false;

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

        // кидаем только если заряд ветра именно в правой (основной) руке
        if (!p.getMainHandItem().is(Items.WIND_CHARGE)) {
            dbg("в правой руке нет заряда ветра");
            return;
        }

        firing = true;
        try {
            use(mc, InteractionHand.MAIN_HAND);
            dbg("заряд ветра брошен");
        } finally {
            firing = false;
        }
    }

    private static void use(Minecraft mc, InteractionHand hand) {
        mc.gameMode.useItem(mc.player, hand);
        mc.player.swing(hand);
    }
}
