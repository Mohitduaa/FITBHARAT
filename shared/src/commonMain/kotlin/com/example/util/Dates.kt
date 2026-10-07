package com.example.util

import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

fun todayDate(): LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

fun currentHour(): Int = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour

fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()

/** "yyyy-MM-dd", the key format used for daily logs. */
fun LocalDate.toKey(): String = toString()

/** "21 Jan 2027" */
fun LocalDate.toDisplayString(): String = "$day ${MONTHS[month.ordinal]} $year"

fun LocalDate.plusWeeks(weeks: Int): LocalDate = plus(weeks * 7, DateTimeUnit.DAY)

fun LocalDate.minusDays(days: Int): LocalDate = plus(-days, DateTimeUnit.DAY)
