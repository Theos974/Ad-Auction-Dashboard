# Ad Auction Dashboard

A comprehensive JavaFX application for analyzing online advertising campaign performance. This dashboard allows marketing agencies and their clients to evaluate campaign success through detailed metrics, interactive charts, and data visualization tools.

## Table of Contents
- [Overview](#overview)
- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Usage](#usage)
- [User Roles](#user-roles)
- [Project Structure](#project-structure)
- [Database](#database)
- [Screenshots](#screenshots)
- [Contributing](#contributing)

## Overview

The Ad Auction Dashboard is designed for online marketing agencies to help their clients evaluate advertising campaign performance. The application processes campaign data from impression logs, click logs, and server logs to provide comprehensive analytics and insights.

### Key Metrics Calculated
- **Impressions**: Number of times ads were displayed
- **Clicks**: Number of ad clicks
- **Unique Users**: Number of distinct users who clicked ads
- **Bounces**: Users who left quickly or viewed few pages
- **Conversions**: Users who completed desired actions
- **Cost Metrics**: CPC (Cost-per-Click), CPA (Cost-per-Acquisition), CPM (Cost-per-Mille)
- **Performance Ratios**: CTR (Click-through Rate), Bounce Rate

## Features

### 🔐 User Management
- Multi-user authentication system
- Role-based access control (Admin, Editor, Viewer)
- User registration and profile management
- Secure password authentication

### 📊 Campaign Analytics
- **Metrics Dashboard**: Real-time campaign performance metrics
- **Time-based Filtering**: Filter data by custom date ranges
- **Audience Segmentation**: Filter by gender, age, income, and context
- **Interactive Charts**: Line charts showing metrics over time with multiple granularities (hourly, daily, weekly)
- **Histogram Analysis**: Distribution analysis of click costs
- **Campaign Comparison**: Side-by-side comparison of different campaigns

### 📈 Data Visualization
- **Real-time Charts**: Dynamic chart generation with JavaFX Charts
- **Multiple Chart Types**: Line charts, bar charts, histograms
- **Time Granularity Control**: View data by hour, day, or week
- **Filter Integration**: All charts respond to audience and time filters

### 📁 Data Management
- **ZIP File Import**: Load campaign data from compressed files
- **Database Storage**: Save and retrieve campaigns from embedded H2 database
- **Export Capabilities**: Export charts and data in multiple formats (PNG, PDF, CSV)
- **Print Functionality**: Direct printing of charts and reports

### 👑 Admin Features
- **User Management**: Create, edit, and delete user accounts
- **Role Assignment**: Assign and modify user permissions
- **Campaign Access Control**: Grant/revoke access to specific campaigns
- **System Overview**: Monitor all users and campaigns

### 🎛️ Advanced Features
- **Bounce Criteria Customization**: Define custom bounce thresholds
- **Memory Optimization**: Efficient caching for large datasets
- **Error Handling**: Comprehensive error messages and validation
- **Responsive Design**: Adaptive UI that works with different screen sizes

## Requirements

- **Java**: JDK 11 or higher
- **JavaFX**: 11 or higher
- **Dependencies**:
  - H2 Database Engine
  - OpenCSV
  - iText PDF
  - Apache Commons IO

## Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/yourusername/ad-auction-dashboard.git
   cd ad-auction-dashboard
   ```

2. **Ensure JavaFX is installed**:
   - Download JavaFX SDK from [OpenJFX](https://openjfx.io/)
   - Set JavaFX module path in your IDE or build configuration

3. **Build and run**:
   ```bash
   # Using your IDE: Run MainApp.java
   # Or compile and run manually with JavaFX on classpath
   java --module-path /path/to/javafx/lib --add-modules javafx.controls,javafx.fxml MainApp
   ```

## Usage

### First Time Setup
1. **Start the application**: Run `MainApp.java`
2. **Login with default admin account**:
   - Username: `admin`
   - Password: `admin123`
3. **Create additional users** through the Admin Panel if needed

### Loading Campaign Data
1. **Prepare your data**: Ensure you have ZIP files containing:
   - `impression_log.csv`: Impression data with columns: Date, ID, Gender, Age, Income, Context, Impression Cost
   - `click_log.csv`: Click data with columns: Date, ID, Click Cost
   - `server_log.csv`: Server interaction data with columns: Entry Date, ID, Exit Date, Pages Viewed, Conversion

2. **Import campaign**:
   - Click "Load ZIP File" (Editor/Admin only)
   - Select your campaign ZIP file
   - Click "Create Campaign"

3. **View analytics**:
   - Navigate through different sections using the sidebar
   - Apply filters to segment your data
   - Generate charts and reports

### User Workflow
```
Login → Load/Select Campaign → View Metrics → Analyze Charts → Generate Reports
```

## User Roles

### 👀 Viewer
- View saved campaigns from database
- Access all analytics and charts
- Apply filters and generate reports
- Export data in various formats

### ✏️ Editor
- All Viewer permissions
- Import new campaigns from ZIP files
- Save campaigns to database
- Access campaign comparison features

### 👑 Admin
- All Editor permissions
- Access Admin Panel
- Manage user accounts and roles
- Control campaign access permissions
- System administration tasks

## Project Structure

```
src/main/java/com/example/ad_auction_dashboard/
├── charts/                 # Chart implementations
│   ├── Chart.java         # Chart interface
│   ├── *Chart.java        # Specific chart types
│   └── HistogramGenerator.java
├── controller/            # FXML controllers
│   ├── LoginSceneController.java
│   ├── MetricSceneController.java
│   ├── ChartSceneController.java
│   └── AdminPanelController.java
├── logic/                 # Business logic
│   ├── Campaign.java      # Campaign data model
│   ├── CampaignMetrics.java # Metrics calculations
│   ├── UserDatabase.java  # User management
│   ├── TimeFilteredMetrics.java # Filtering logic
│   └── FileHandler.java   # File operations
├── viewer/                # Scene management
│   ├── MainApp.java       # Application entry point
│   └── *Scene.java        # Scene controllers
└── resources/
    ├── fxml/              # FXML layout files
    ├── css/               # Stylesheets
    └── images/            # UI assets
```

## Database

The application uses an embedded H2 database stored in:
- **Location**: `~/.ad_auction_dashboard/userdb`
- **Tables**:
  - `USERS`: User accounts and roles
  - `CAMPAIGNS`: Saved campaign data
  - `CAMPAIGN_ACCESS`: User-campaign permissions

## Key Classes

### Data Models
- **`Campaign`**: Container for campaign log data
- **`CampaignMetrics`**: Calculates and caches all performance metrics
- **`ImpressionLog`**, **`ClickLog`**, **`ServerLog`**: Individual log entry models

### Controllers
- **`MetricSceneController`**: Main dashboard with metrics display
- **`ChartSceneController`**: Interactive chart visualization
- **`HistogramController`**: Data distribution analysis
- **`AdminPanelController`**: User and system management

### Core Logic
- **`TimeFilteredMetrics`**: Handles filtering and time-based analysis
- **`UserDatabase`**: User authentication and role management
- **`FileHandler`**: ZIP file processing and data import

## Configuration

### Bounce Criteria
Users can customize bounce detection criteria:
- **Page Threshold**: Minimum pages viewed to not be considered a bounce
- **Time Threshold**: Minimum time spent on site (in seconds)

### Export Options
- **Charts**: PNG, PDF formats
- **Data**: CSV format with chart data points
- **Combined**: Multi-page PDF reports

## Error Handling

The application includes comprehensive error handling for:
- Invalid file formats
- Database connection issues
- Permission violations
- Data validation errors
- Memory management for large datasets

## Performance Optimizations

- **Caching**: Intelligent caching of computed metrics
- **Memory Management**: Efficient data structures for large campaigns
- **Lazy Loading**: On-demand calculation of time-granular data
- **Batch Processing**: Optimized file reading and data processing

## Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## License

This project is part of an academic assignment for advertising campaign analysis.

## Support

For questions or issues:
1. Check the error messages displayed in the application
2. Verify your data format matches the required CSV structure
3. Ensure you have appropriate permissions for the operation
4. Contact your system administrator for database or user issues

---

**Note**: This application is designed for educational and professional use in advertising campaign analysis. Ensure your data complies with privacy regulations and company policies before importing.
