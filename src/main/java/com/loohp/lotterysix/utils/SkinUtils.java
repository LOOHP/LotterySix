/*
 * This file is part of InteractiveChat.
 *
 * Copyright (C) 2022. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2022. Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.loohp.lotterysix.utils;

import com.cryptomorin.xseries.XMaterial;
import com.loohp.lotterysix.LotterySixPlugin;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.UUID;

public class SkinUtils {

    private static Class<?> craftPlayerClass;
    private static Class<?> nmsEntityPlayerClass;
    private static Method craftPlayerGetHandleMethod;
    private static Method nmsEntityPlayerGetProfileMethod;
    private static Method playerGetPlayerProfileMethod;
    private static Method mojangPropertyGetValueMethod;

    static {
        if (LotterySixPlugin.version.isOlderThan(MCVersion.V1_19)) {
            try {
                craftPlayerClass = getLegacyNMSClass("org.bukkit.craftbukkit.%s.entity.CraftPlayer");
                nmsEntityPlayerClass = getLegacyNMSClass("net.minecraft.server.%s.EntityPlayer", "net.minecraft.server.level.EntityPlayer");
                craftPlayerGetHandleMethod = craftPlayerClass.getMethod("getHandle");
                nmsEntityPlayerGetProfileMethod = reflectiveLookup(Method.class, () -> {
                    return nmsEntityPlayerClass.getMethod("getProfile");
                }, () -> {
                    Method method = nmsEntityPlayerClass.getMethod("fp");
                    if (!method.getReturnType().equals(GameProfile.class)) {
                        throw new NoSuchMethodException();
                    }
                    return method;
                }, () -> {
                    Method method = nmsEntityPlayerClass.getMethod("fq");
                    if (!method.getReturnType().equals(GameProfile.class)) {
                        throw new NoSuchMethodException();
                    }
                    return method;
                }, () -> {
                    return nmsEntityPlayerClass.getMethod("fQ");
                });
                mojangPropertyGetValueMethod = Property.class.getMethod("getValue");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                if (LotterySixPlugin.version.isNewerOrEqualTo(MCVersion.V1_20_2)) {
                    mojangPropertyGetValueMethod = Property.class.getMethod("value");
                } else {
                    mojangPropertyGetValueMethod = Property.class.getMethod("getValue");
                }
                playerGetPlayerProfileMethod = Player.class.getMethod("getPlayerProfile");
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
    }

    public static String getSkinValue(Player player) throws Exception {
        if (LotterySixPlugin.version.isNewerOrEqualTo(MCVersion.V1_19)) {
            Object playerProfile = playerGetPlayerProfileMethod.invoke(player);
            Method craftPlayerProfileGetPropertyMethod = playerProfile.getClass().getDeclaredMethod("getProperty", String.class);
            craftPlayerProfileGetPropertyMethod.setAccessible(true);
            Property property = (Property) craftPlayerProfileGetPropertyMethod.invoke(playerProfile, "textures");
            if (property == null) {
                return null;
            }
            return (String) mojangPropertyGetValueMethod.invoke(property);
        } else {
            Object playerNMS = craftPlayerGetHandleMethod.invoke(craftPlayerClass.cast(player));
            GameProfile profile = (GameProfile) nmsEntityPlayerGetProfileMethod.invoke(playerNMS);
            Collection<Property> textures = profile.getProperties().get("textures");
            if (textures == null || textures.isEmpty()) {
                return null;
            }
            return (String) mojangPropertyGetValueMethod.invoke(textures.iterator().next());
        }
    }

    @SuppressWarnings("deprecation")
    public static ItemStack getSkull(UUID uuid) {
        ItemStack head = XMaterial.PLAYER_HEAD.parseItem();

        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (LotterySixPlugin.version.isNewerOrEqualTo(MCVersion.V1_12)) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));
        } else {
            meta.setOwner(uuid.toString());
        }
        head.setItemMeta(meta);

        Player player = Bukkit.getPlayer(uuid);
        try {
            if (player != null) {
                String base64 = getSkinValue(player);
                if (LotterySixPlugin.version.isNewerOrEqualTo(MCVersion.V1_20_5)) {
                    head = Bukkit.getUnsafe().modifyItemStack(head, "minecraft:player_head[minecraft:profile={properties:[{name:\"textures\",value:\"" + base64 + "\"}]}]");
                } else {
                    head = Bukkit.getUnsafe().modifyItemStack(head, "{SkullOwner: {Properties: {textures: [{Value: \"" + base64 + "\"}]}}}");
                }
            }
        } catch (Throwable ignore) {
        }

        return head;
    }

    private static Class<?> getLegacyNMSClass(String path, String... paths) throws ClassNotFoundException {
        String version = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
        if (!version.matches("v[0-9]+_[0-9]+_R[0-9]+")) {
            version = "";
        }
        ClassNotFoundException error;
        try {
            return Class.forName(path.replace("%s", version).replaceAll("\\.+", "."));
        } catch (ClassNotFoundException e) {
            error = e;
        }
        for (String classpath : paths) {
            try {
                return Class.forName(classpath.replace("%s", version).replaceAll("\\.+", "."));
            } catch (ClassNotFoundException e) {
                error = e;
            }
        }
        throw error;
    }

    @SafeVarargs
    private static <T extends AccessibleObject> T reflectiveLookup(Class<T> lookupType, ReflectionLookupSupplier<T> methodLookup, ReflectionLookupSupplier<T>... methodLookups) throws ReflectiveOperationException {
        ReflectiveOperationException error;
        try {
            return methodLookup.lookup();
        } catch (ReflectiveOperationException e) {
            error = e;
        }
        for (ReflectionLookupSupplier<T> supplier : methodLookups) {
            try {
                return supplier.lookup();
            } catch (ReflectiveOperationException e) {
                error = e;
            }
        }
        throw error;
    }

    @FunctionalInterface
    private interface ReflectionLookupSupplier<T> {

        T lookup() throws ReflectiveOperationException;

    }

}
