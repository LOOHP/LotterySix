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

package com.loohp.lotterysix.utils;

import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.TimeZone;

public class CronUtils {

    public static final CronDefinition CRON_DEFINITION = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX);
    public static final CronParser PARSER = new CronParser(CRON_DEFINITION);

    public static CronDescriptor getDescriptor(Locale locale) {
        return CronDescriptor.instance(locale);
    }

    public static ZonedDateTime getNow(TimeZone timeZone) {
        return ZonedDateTime.now(timeZone.toZoneId());
    }

    public static ZonedDateTime getTime(long timestamp, TimeZone timeZone) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), timeZone.toZoneId());
    }

    public static ZonedDateTime getLastExecution(Cron cron, ZonedDateTime dateTime) {
        ExecutionTime executionTime = ExecutionTime.forCron(cron);
        return executionTime.lastExecution(dateTime).orElse(null);
    }

    public static ZonedDateTime getNextExecution(Cron cron, ZonedDateTime dateTime) {
        ExecutionTime executionTime = ExecutionTime.forCron(cron);
        return executionTime.nextExecution(dateTime).orElse(null);
    }

    public static boolean satisfyByCurrentMinute(Cron cron, TimeZone timeZone) {
        ZonedDateTime execution = getNextExecution(cron, getTime(System.currentTimeMillis() - 60000, timeZone));
        ZonedDateTime now = getNow(timeZone);
        if (execution == null) {
            return false;
        }
        return execution.withNano(0).equals(now.withNano(0));
    }

}
