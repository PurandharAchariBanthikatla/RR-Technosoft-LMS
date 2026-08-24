package com.rrtechnosoft.lms.repository;

import java.math.BigDecimal;

/**
 * Generic (month, value) row for the dashboard's monthly trend charts
 * (student growth, revenue, attendance %) — backed by native queries that
 * alias their group-by month as {@code monthLabel} and the aggregate as
 * {@code val}. {@code val} is BigDecimal since Postgres returns all three
 * aggregates (count::numeric, sum, round(...,2)) as numeric.
 */
public interface MonthValueProjection {
    String getMonthLabel();
    BigDecimal getVal();
}
