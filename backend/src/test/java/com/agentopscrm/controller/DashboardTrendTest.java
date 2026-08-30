package com.agentopscrm.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DashboardTrendTest {

    @Test
    void reportsPercentageIncreaseAgainstPreviousPeriod() {
        DashboardController.Trend trend = DashboardController.trend(12, 10, "last month");
        assertEquals("up", trend.direction);
        assertEquals("20% vs last month", trend.label);
    }

    @Test
    void reportsPercentageDecreaseAgainstPreviousPeriod() {
        DashboardController.Trend trend = DashboardController.trend(8, 10, "last week");
        assertEquals("down", trend.direction);
        assertEquals("20% vs last week", trend.label);
    }

    @Test
    void reportsFlatWhenTotalsDoNotChange() {
        DashboardController.Trend trend = DashboardController.trend(10, 10, "yesterday");
        assertEquals("flat", trend.direction);
        assertEquals("No change vs yesterday", trend.label);
    }
}
