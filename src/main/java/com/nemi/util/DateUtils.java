package com.nemi.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@UtilityClass
@Slf4j
public class DateUtils {

    private static final String DATE_FORMAT = "yyyy-MM-dd";
    private static final String UI_DATE_FORMAT = "dd/MM/yyyy";
    private static final String UI_DATETIME_FORMAT = "dd/MM/yyyy HH:mm";

    public static LocalDate stringToDate(String date) {
        if (StringUtils.isEmpty(date)) {
            return null;
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
            return LocalDate.parse(date, formatter);
        } catch (Exception e) {
            log.warn("[DateUtils] Failed to parse date {}", date, e);
            return null;
        }
    }

    public static String dateToString(LocalDate date) {
        if (date == null) {
            return null;
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(UI_DATE_FORMAT);
            return date.format(formatter);
        } catch (Exception e) {
            log.warn("[DateUtils] Failed to format date {}", date, e);
            return null;
        }
    }

    public static String instantToTimeString(Instant instant) {
        try {
            ZonedDateTime timeInVietnam = instant
                    .atZone(ZoneId.of("Asia/Ho_Chi_Minh")); // UTC+7

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(UI_DATETIME_FORMAT);

            return timeInVietnam.format(formatter);
        } catch (Exception e) {
            log.warn("[DateUtils] Failed to convert Instant to time string", e);
            return null;
        }
    }

    public static OffsetDateTime nowUtc() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    public static OffsetDateTime convertOffsetDateTime(OffsetDateTime utcTime, double timezoneOffset) {
        // Tách phần giờ và phút từ offset dạng double (ví dụ: 7.5 -> 7h30)
        int hours = (int) timezoneOffset;
        int minutes = (int) ((Math.abs(timezoneOffset) * 60) % 60);

        // Tạo ZoneOffset mới
        ZoneOffset newOffset = ZoneOffset.ofHoursMinutes(hours, minutes);

        // Chuyển đổi thời gian
        return utcTime.withOffsetSameInstant(newOffset);
    }

    public static Integer countDaysFrom(OffsetDateTime date) {
        if (date == null) {
            return null;
        }
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        return (int) (now.toEpochSecond() - date.toEpochSecond()) / (24 * 3600);
    }
}
