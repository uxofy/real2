package com.example.real;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.IntConsumer;

public class ConfigScreen extends Screen {
    public ConfigScreen() {
        super(Component.literal("Real"));
    }

    @Override
    protected void init() {
        Config c = Config.INSTANCE;
        int w = 220, x = this.width / 2 - w / 2, y = this.height / 2 - 40;

        addRenderableWidget(Button.builder(label(c.enabled), b -> {
            c.enabled = !c.enabled;
            b.setMessage(label(c.enabled));
        }).bounds(x, y, w, 20).build());

        addRenderableWidget(new IntSlider(x, y + 26, w, "Задержка заряда (тиков)", 0, 20, c.delay,
                v -> c.delay = v));

        addRenderableWidget(Button.builder(Component.literal("Готово"), b -> onClose())
                .bounds(x, y + 58, w, 20).build());
    }

    private static Component label(boolean on) {
        return Component.literal("Включено: " + (on ? "да" : "нет"));
    }

    @Override
    public void onClose() {
        Config.INSTANCE.save();
        super.onClose();
    }

    private static class IntSlider extends AbstractSliderButton {
        private final String label;
        private final int min, max;
        private final IntConsumer setter;

        IntSlider(int x, int y, int w, String label, int min, int max, int current, IntConsumer setter) {
            super(x, y, w, 20, Component.empty(), (double) (current - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.setter = setter;
            updateMessage();
        }

        private int real() {
            return (int) Math.round(min + this.value * (max - min));
        }

        @Override
        protected void updateMessage() {
            if (label == null) return;
            setMessage(Component.literal(label + ": " + real()));
        }

        @Override
        protected void applyValue() {
            setter.accept(real());
        }
    }
}
