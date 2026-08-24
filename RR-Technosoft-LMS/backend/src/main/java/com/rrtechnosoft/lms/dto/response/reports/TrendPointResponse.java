package com.rrtechnosoft.lms.dto.response.reports;

/**
 * One point on a monthly trend line (student growth, revenue, attendance %).
 * {@link #of} accepts any {@link Number} so it can be fed directly from
 * either JPQL {@code avg}/{@code count} results (Double/Long) or native
 * numeric projections (BigDecimal) without the caller having to convert.
 */
public record TrendPointResponse(String month, double value) {
    public static TrendPointResponse of(String month, Number value) {
        return new TrendPointResponse(month, value == null ? 0.0 : value.doubleValue());
    }
}
