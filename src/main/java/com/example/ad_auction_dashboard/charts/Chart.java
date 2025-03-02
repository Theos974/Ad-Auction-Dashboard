package com.example.ad_auction_dashboard.charts;

import com.example.ad_auction_dashboard.logic.TimeFilteredMetrics;
import java.time.LocalDateTime;
import javafx.scene.layout.VBox;

public interface Chart {
    VBox createChart(TimeFilteredMetrics timeFilteredMetrics, LocalDateTime start, LocalDateTime end);
}
