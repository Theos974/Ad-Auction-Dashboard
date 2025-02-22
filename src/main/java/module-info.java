module com.example.ad_auction_dashboard {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.bootstrapfx.core;

    opens com.example.ad_auction_dashboard to javafx.fxml;
    exports com.example.ad_auction_dashboard;
}