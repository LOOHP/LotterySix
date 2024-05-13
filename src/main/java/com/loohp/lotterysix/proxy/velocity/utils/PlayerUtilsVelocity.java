/*
 * This file is part of LotterySix.
 *
 * Copyright (C) 2023. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2023. Contributors
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

package com.loohp.lotterysix.proxy.velocity.utils;

import com.loohp.lotterysix.proxy.velocity.LotterySixVelocity;
import com.loohp.lotterysix.utils.HTTPRequestUtils;
import com.velocitypowered.api.proxy.Player;
import org.json.simple.JSONObject;

import java.util.Optional;
import java.util.UUID;

public class PlayerUtilsVelocity {

    private static final String PLAYER_PROFILE_URL = "https://sessionserver.mojang.com/session/minecraft/profile/%s";
    private static final String PLAYER_UUID_URL = "https://api.mojang.com/users/profiles/minecraft/%s";

    public static String getPlayerName(UUID uuid) {
        Optional<Player> player = LotterySixVelocity.proxyServer.getPlayer(uuid);
        if (player.isPresent()) {
            return player.get().getUsername();
        }
        JSONObject jsonResponse = HTTPRequestUtils.getJSONResponse(PLAYER_PROFILE_URL.replaceFirst("%s", uuid.toString()));
        if (jsonResponse == null) {
            return null;
        }
        return (String) jsonResponse.get("name");
    }

    public static UUID getPlayerUUID(String name) {
        Optional<Player> player = LotterySixVelocity.proxyServer.getPlayer(name);
        if (player.isPresent()) {
            return player.get().getUniqueId();
        }
        JSONObject jsonResponse = HTTPRequestUtils.getJSONResponse(PLAYER_UUID_URL.replaceFirst("%s", name));
        if (jsonResponse == null) {
            return null;
        }
        Object id = jsonResponse.get("id");
        if (!(id instanceof String)) {
            return null;
        }
        String idStr = (String) id;
        if (!idStr.contains("-")) {
            idStr = idStr.replaceFirst("(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5");
        }
        try {
            return UUID.fromString(idStr);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

}
