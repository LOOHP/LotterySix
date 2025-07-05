/*
 * This file is part of LotterySix.
 *
 * Copyright (C) 2025. LoohpJames <jamesloohp@gmail.com>
 * Copyright (C) 2025. Contributors
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

package com.loohp.lotterysix.gui;

public enum GUIType {

    MAIN_MENU("MainMenu"),
    BETTING_ACCOUNT("BettingAccount"),
    TRANSACTION_MENU("TransactionMenu"),
    TRANSACTION_INPUT("TransactionInput"),
    FIXED_TRANSACTION_DEPOSIT("FixedTransactionDeposit"),
    BET_TYPE_CHOOSER("BetTypeChooser"),
    RANDOM_ENTRY_CHOOSER("RandomEntryChooser"),
    NUMBER_CHOOSER("NumberChooser"),
    SINGLE_NUMBER_CONFIRM("SingleNumberConfirm"),
    COMPLEX_NUMBER_CONFIRM("ComplexNumberConfirm"),
    SINGLE_BULK_NUMBER_CONFIRM("SingleBulkNumberConfirm"),
    COMPLEX_BULK_NUMBER_CONFIRM("ComplexBulkNumberConfirm"),
    PAST_RESULTS_NO_GAMES("PastResultsNoGames"),
    PAST_RESULTS("PastResults"),
    PAST_RESULTS_LIST("PastResultsList"),
    NUMBER_STATISTICS("NumberStatistics");

    private final String key;

    GUIType(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public static GUIType fromKey(String key) {
        for (GUIType type : values()) {
            if (type.getKey().equalsIgnoreCase(key)) {
                return type;
            }
        }
        return null;
    }

}
