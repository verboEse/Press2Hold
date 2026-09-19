package com.kwesou;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.sdl.SDLKeyboard;
import org.lwjgl.sdl.SDLMouse;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class Press2Hold implements ModInitializer {
	public static final String MOD_ID = "press2hold";
	private static KeyMapping keyBinding;
	private static boolean isLatched = false;
	Set<Integer> pressedKeys = new HashSet<>();
	Set<Integer> pressedMouseButtons = new HashSet<>();
	Set<String> pressedKeyNames = new HashSet<>();

	@Override
	public void onInitialize() {
		// Only register client events on the physical client
		net.fabricmc.api.EnvType env = net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType();
		if (env != net.fabricmc.api.EnvType.CLIENT) {
			return;
		}

		keyBinding = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.press2hold.latch",
				InputConstants.Type.KEYBOARD,
				InputConstants.KEY_G,
				KeyMapping.Category.register(Identifier.parse("press2hold:press2hold"))
		));

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (keyBinding.consumeClick()) {
				isLatched = !isLatched;
				getCurrentlyPressedInputs();

				if (isLatched && (!pressedKeys.isEmpty() || !pressedMouseButtons.isEmpty())) {
					for (int key : pressedKeys) {
						InputConstants.Key keyObj = InputConstants.Type.KEYBOARD.getOrCreate(key);
						KeyMapping.set(keyObj, true);
						pressedKeyNames.add(getKeyName(key));
					}
					for (int button : pressedMouseButtons) {
						KeyMapping.set(InputConstants.Type.MOUSE.getOrCreate(button), true);
						pressedKeyNames.add("MOUSE" + button);
					}
					if (client.player != null) {
						client.player.sendSystemMessage(Component.literal("Latching: " + pressedKeyNames.toString()));
					}
				} else if (pressedKeys.isEmpty() && pressedMouseButtons.isEmpty()) {
					if (client.player != null) {
						client.player.sendSystemMessage(Component.literal("Invalid inputs pressed"));
					}
					isLatched = false;
					pressedKeys.clear();
					pressedMouseButtons.clear();
					pressedKeyNames.clear();
				} else {
					for (int key : pressedKeys) {
						InputConstants.Key keyObj = InputConstants.Type.KEYBOARD.getOrCreate(key);
						KeyMapping.set(keyObj, false);
					}
					for (int button : pressedMouseButtons) {
						KeyMapping.set(InputConstants.Type.MOUSE.getOrCreate(button), false);
					}
					pressedKeys.clear();
					pressedMouseButtons.clear();
					pressedKeyNames.clear();
					if (client.player != null) {
						client.player.sendSystemMessage(Component.literal("Unlatched"));
					}
				}
			}
			if (isLatched) {
				for (int key : pressedKeys) {
					InputConstants.Key keyObj = InputConstants.Type.KEYBOARD.getOrCreate(key);
					KeyMapping.set(keyObj, true);
				}
				for (int button : pressedMouseButtons) {
					KeyMapping.set(InputConstants.Type.MOUSE.getOrCreate(button), true);
				}
			} else {
				for (int key : pressedKeys) {
					InputConstants.Key keyObj = InputConstants.Type.KEYBOARD.getOrCreate(key);
					KeyMapping.set(keyObj, false);
				}
				for (int button : pressedMouseButtons) {
					KeyMapping.set(InputConstants.Type.MOUSE.getOrCreate(button), false);
				}
			}
		});
	}

	public static String getKeyName(int key) {
		return InputConstants.Type.KEYBOARD.getOrCreate(key)
				.getDisplayName()
				.getString()
				.toUpperCase(Locale.ROOT);
	}

	public void getCurrentlyPressedInputs() {
		int keyCode = keyBinding.getDefaultKey().getValue();

		ByteBuffer keyboardState = SDLKeyboard.SDL_GetKeyboardState();
		if (keyboardState != null) {
			for (int key = 0; key < keyboardState.capacity(); key++) {
				if (key != keyCode && InputConstants.isKeyDown(key)) {
					pressedKeys.add(key);
				}
			}
		}

		int mouseState = SDLMouse.SDL_GetMouseState(null, null);
		for (int button = 1; button <= 8; button++) {
			if ((mouseState & (1 << (button - 1))) != 0) {
				pressedMouseButtons.add(button);
			}
		}
	}
}
