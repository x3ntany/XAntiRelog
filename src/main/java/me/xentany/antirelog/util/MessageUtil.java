package me.xentany.antirelog.util;

import me.xentany.minimessageprovider.MiniMessageProvider;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class MessageUtil {

  private static final MiniMessage MINI_MESSAGE;

  static {
    MINI_MESSAGE = MiniMessageProvider.getMiniMessage();
  }

  public static @NotNull Component deserialize(final @NotNull String message,
                                               final @NotNull Object @NotNull ... arguments) {
    return MINI_MESSAGE.deserialize(format(message, arguments));
  }

  public static void sendIfNotEmpty(final @NotNull Player player,
                                    final @NotNull String message,
                                    final @NotNull Object @NotNull ... arguments) {
    if (!message.isEmpty()) {
      player.sendMessage(deserialize(message, arguments));
    }
  }

  public static @NotNull String format(final @NotNull String message,
                                       final @NotNull Object @NotNull ... arguments) {
    var result = new StringBuilder();
    int length = message.length();

    for (int i = 0; i < length; i++) {
      var currentChar = message.charAt(i);

      if (currentChar == '{' && i + 1 < length) {
        int endIndex = message.indexOf('}', i);

        if (endIndex > i) {
          var indexStr = message.substring(i + 1, endIndex);

          try {
            int argIndex = Integer.parseInt(indexStr);

            if (argIndex >= 0 && argIndex < arguments.length) {
              result.append(Objects.toString(arguments[argIndex], ""));
            } else {
              result.append("{").append(indexStr).append("}");
            }

            i = endIndex;
            continue;
          } catch (NumberFormatException ignored) {}
        }
      }

      result.append(currentChar);
    }

    return result.toString();
  }
}