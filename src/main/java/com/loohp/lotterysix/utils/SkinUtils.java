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
import io.github.bananapuncher714.nbteditor.NBTEditor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Base64;
import java.util.Collection;
import java.util.UUID;

public class SkinUtils {

    private static final String PLAYER_INFO_URL = "https://sessionserver.mojang.com/session/minecraft/profile/%s";

    private static Class<?> craftPlayerClass;
    private static Class<?> nmsEntityPlayerClass;
    private static Method craftPlayerGetHandleMethod;
    private static Method nmsEntityPlayerGetProfileMethod;
    private static Class<?> craftSkullMetaClass;
    private static Field craftSkullMetaProfileField;
    private static Method playerGetPlayerProfileMethod;
    private static Method mojangPropertyGetValueMethod;

    static {
        if (LotterySixPlugin.version.isOlderThan(MCVersion.V1_19)) {
            try {
                craftPlayerClass = NMSUtils.getNMSClass("org.bukkit.craftbukkit.%s.entity.CraftPlayer");
                nmsEntityPlayerClass = NMSUtils.getNMSClass("net.minecraft.server.%s.EntityPlayer", "net.minecraft.server.level.EntityPlayer");
                craftPlayerGetHandleMethod = craftPlayerClass.getMethod("getHandle");
                nmsEntityPlayerGetProfileMethod = NMSUtils.reflectiveLookup(Method.class, () -> {
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
                craftSkullMetaClass = NMSUtils.getNMSClass("org.bukkit.craftbukkit.%s.inventory.CraftMetaSkull");
                craftSkullMetaProfileField = craftSkullMetaClass.getDeclaredField("profile");
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

    public static String getSkinJsonFromProfile(Player player) throws Exception {
        return new String(Base64.getDecoder().decode(getSkinValue(player)));
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

    public static String getSkinValue(ItemMeta skull) {
        SkullMeta meta = (SkullMeta) skull;

        if (LotterySixPlugin.version.isNewerOrEqualTo(MCVersion.V1_19)) {
            if (meta.hasOwner()) {
                try {
                    PlayerProfile playerProfile = meta.getOwnerProfile();
                    Method craftPlayerProfileGetPropertyMethod = playerProfile.getClass().getDeclaredMethod("getProperty", String.class);
                    craftPlayerProfileGetPropertyMethod.setAccessible(true);
                    Property property = (Property) craftPlayerProfileGetPropertyMethod.invoke(playerProfile, "textures");
                    if (property == null) {
                        return null;
                    }
                    return (String) mojangPropertyGetValueMethod.invoke(property);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            GameProfile profile = null;

            try {
                craftSkullMetaProfileField.setAccessible(true);
                profile = (GameProfile) craftSkullMetaProfileField.get(meta);
            } catch (SecurityException | IllegalAccessException e) {
                e.printStackTrace();
            }

            if (profile != null && !profile.getProperties().get("textures").isEmpty()) {
                for (Property property : profile.getProperties().get("textures")) {
                    try {
                        String value = (String) mojangPropertyGetValueMethod.invoke(property);
                        if (!value.isEmpty()) {
                            return value;
                        }
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }

        }
        return null;
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
                head = NBTEditor.set(head, NBTEditor.getNBTCompound("{textures: [{Value: \"" + base64 + "\"}]}"), "SkullOwner", "Properties");
            }
        } catch (Throwable ignore) {
        }

        return head;
    }

    public static String getSkinURLFromUUID(UUID uuid) throws Exception {
        JSONObject jsonResponse = HTTPRequestUtils.getJSONResponse(PLAYER_INFO_URL.replaceFirst("%s", uuid.toString()));
        if (jsonResponse.containsKey("errorMessage")) {
            throw new RuntimeException("Unable to retrieve skin url from Mojang servers for the player " + uuid);
        }
        JSONArray propertiesArray = (JSONArray) jsonResponse.get("properties");
        for (Object obj : propertiesArray) {
            JSONObject property = (JSONObject) obj;
            if (property.get("name").toString().equals("textures")) {
                String base64 = property.get("value").toString();
                JSONObject textureJson = (JSONObject) new JSONParser().parse(new String(Base64.getDecoder().decode(base64)));
                return ((JSONObject) ((JSONObject) textureJson.get("textures")).get("SKIN")).get("url").toString();
            }
        }
        throw new RuntimeException("Unable to retrieve skin url from Mojang servers for the player " + uuid);
    }

}
