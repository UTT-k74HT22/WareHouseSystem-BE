# BA Document - Module 8: Reporting & Export
## Business Requirements Specification

---

## 📋 Document Information

| Property | Value |
|----------|-------|
| **Module** | Reporting & Export |
| **Version** | 1.0 |
| **Date** | February 1, 2026 |
| **Status** | Draft |
| **Author** | Business Analyst |

---

## 🎯 Business Context

### Current Pain Points

1. **Manual Report Generation**: Staff spend hours creating Excel reports manually
2. **Outdated Data**: Reports based on yesterday's data, not real-time
3. **Inconsistent Formats**: Different people create reports differently
4. **No Export Capability**: Cannot export data for external analysis
5. **Slow Large Reports**: Heavy reports crash Excel or take forever
6. **No Scheduling**: Cannot schedule automatic report delivery

### Business Value

✅ **Time Savings** - Automated reports reduce manual work by 80%  
✅ **Real-Time Insights** - Reports based on current data  
✅ **Consistency** - Standardized report formats  
✅ **Flexibility** - Export to PDF, Excel, CSV  
✅ **Performance** - Async generation for large reports  
✅ **Compliance** - Scheduled regulatory reports  

---

## 📦 Module Overview

### Core Capabilities

1. **On-Demand Reports** - Generate reports immediately (small reports)
2. **Async Reports** - Queue large reports, notify when ready
3. **Scheduled Reports** - Daily/weekly/monthly automatic reports
4. **Export Formats** - PDF, Excel (XLSX), CSV
5. **Filters & Parameters** - Customize report scope

### Report Categories

#### Inventory Reports
- Current Stock Report
- Stock Valuation Report
- Low Stock Report
- Expiring Batches Report
- Batch Traceability Report

#### Movement Reports
- Inbound Activity Report
- Outbound Activity Report
- Stock Adjustment Report
- Movement Summary Report

#### Operational Reports
- Warehouse Performance Report
- Supplier Performance Report
- Customer Order Report
- Picking Efficiency Report

---

## 📋 Features

### Feature 1: Current Stock Report

**US-RPT-01**: As a Warehouse Manager, I want to see current stock levels for all products, so that I can plan replenishment.

**Report Contents:**
- Product (SKU, name, category)
- Warehouse and location
- Batch (if applicable)
- On-hand quantity
- Reserved quantity
- Available quantity
- Last movement date
- Status (OK, Low Stock, Expiring Soon)

**Filters:**
- Warehouse
- Product category
- Stock level (All, Low Stock, Zero Stock)
- Batch status (All, Available, Expiring)

**Output:** PDF, Excel, CSV

**Business Rules:**
- BR-RPT-01: Show real-time data (no caching)
- BR-RPT-02: Sort by warehouse, then product SKU
- BR-RPT-03: Highlight low stock items in red

---

### Feature 2: Stock Valuation Report

**US-RPT-02**: As an Accountant, I want to see total inventory value by product and category, so that I can prepare financial statements.

**Report Contents:**
- Product details
- Total quantity on hand
- Unit cost (cost_price)
- Total value (quantity × cost)
- Subtotals by category
- Grand total

**Filters:**
- Warehouse
- Product category
- Valuation date (default: today)

**Output:** PDF, Excel

**Business Rules:**
- BR-RPT-04: Use cost_price from products table
- BR-RPT-05: Include only ACTIVE products
- BR-RPT-06: Group by category with subtotals

---

### Feature 3: Movement Summary Report

**US-RPT-03**: As a Warehouse Manager, I want to see inbound/outbound movements for a period, so that I can analyze warehouse activity.

**Report Contents:**
- Date range
- Movement type (INBOUND, OUTBOUND, ADJUSTMENT)
- Product
- Quantity
- Reference (PO/SO number)
- User

**Filters:**
- Date range (required)
- Warehouse
- Movement type
- Product

**Output:** PDF, Excel, CSV

**Business Rules:**
- BR-RPT-07: Default date range: last 7 days
- BR-RPT-08: Sort by movement_date DESC
- BR-RPT-09: Show summary totals by movement type

---

### Feature 4: Batch Traceability Report

**US-RPT-04**: As a Compliance Officer, I want to generate batch traceability report, so that I can respond to regulatory inquiries.

**Report Contents:**
- Batch information (batch number, manufacturing date, expiry)
- Inbound: supplier, PO, receipt date, quantity
- Current locations and quantities
- Outbound: customers, SO, shipment date, quantity
- Complete movement history

**Input:** Batch ID or batch number

**Output:** PDF (official report format)

**Business Rules:**
- BR-RPT-10: Include all movements for the batch
- BR-RPT-11: Show forward trace (who received)
- BR-RPT-12: Show backward trace (where from)
- BR-RPT-13: Include batch quality status

---

### Feature 5: Async Report Generation

**US-RPT-05**: As a User, I want large reports generated in the background, so that I don't have to wait.

**How it works:**
1. User requests report with parameters
2. If report estimated size > threshold (e.g., 10,000 rows):
   - System creates report request record
   - Queues job to RabbitMQ
   - Returns request ID to user
   - Shows "Report is being generated..."
3. Background worker:
   - Picks up job
   - Generates report
   - Saves to file storage (or cloud)
   - Updates request status to COMPLETED
   - Sends notification with download link
4. User downloads from link

**Business Rules:**
- BR-RPT-14: Small reports (< 10K rows) generated immediately
- BR-RPT-15: Large reports queued for async processing
- BR-RPT-16: Reports retained for 7 days then deleted
- BR-RPT-17: Notify user via email when ready

---

### Feature 6: Scheduled Reports

**US-RPT-06**: As a Manager, I want daily low stock report emailed automatically, so that I don't forget to check.

**Capabilities:**
- Define report schedule (daily, weekly, monthly)
- Specify recipients (email addresses)
- Set filters and parameters
- Enable/disable schedules

**Examples:**
- Daily low stock alert (every morning 8 AM)
- Weekly inventory summary (Monday 9 AM)
- Monthly valuation report (1st of month)

**Business Rules:**
- BR-RPT-18: Scheduled reports use cron-like syntax
- BR-RPT-19: Reports emailed as attachments
- BR-RPT-20: Failed deliveries logged and alerted

---

## 🔗 API Impact Summary

### On-Demand Reports

| Method | Endpoint | Description | Role |
|--------|----------|-------------|------|
| POST | /api/reports/current-stock | Generate current stock report | VIEWER |
| POST | /api/reports/stock-valuation | Generate valuation report | ACCOUNTANT |
| POST | /api/reports/movements | Generate movement report | VIEWER |
| POST | /api/reports/batch-traceability | Generate batch trace report | COMPLIANCE_OFFICER |
| POST | /api/reports/low-stock | Generate low stock report | WAREHOUSE_MANAGER |
| POST | /api/reports/expiring-batches | Generate expiring batches report | WAREHOUSE_MANAGER |

### Async Reports

| Method | Endpoint | Description | Role |
|--------|----------|-------------|------|
| POST | /api/reports/async/request | Request async report | VIEWER |
| GET | /api/reports/async/{requestId} | Check report status | VIEWER |
| GET | /api/reports/async/{requestId}/download | Download completed report | VIEWER |
| GET | /api/reports/async/my-requests | List user's report requests | VIEWER |

### Scheduled Reports

| Method | Endpoint | Description | Role |
|--------|----------|-------------|------|
| POST | /api/reports/schedules | Create report schedule | WAREHOUSE_MANAGER |
| GET | /api/reports/schedules | List schedules | WAREHOUSE_MANAGER |
| PUT | /api/reports/schedules/{id} | Update schedule | WAREHOUSE_MANAGER |
| DELETE | /api/reports/schedules/{id} | Delete schedule | WAREHOUSE_MANAGER |
| PUT | /api/reports/schedules/{id}/enable | Enable schedule | WAREHOUSE_MANAGER |
| PUT | /api/reports/schedules/{id}/disable | Disable schedule | WAREHOUSE_MANAGER |

---

## 💾 Database Impact

See [DB_MODULE_08_REPORTING.md](./DB_MODULE_08_REPORTING.md)

### New Tables

#### report_requests

Track async report generation requests.

**Key Fields:**
- `request_id` - UUID
- `report_type` - Enum (STOCK_CURRENT, STOCK_VALUATION, etc.)
- `parameters` - JSON (filters, date ranges, etc.)
- `status` - PENDING, PROCESSING, COMPLETED, FAILED
- `output_format` - PDF, EXCEL, CSV
- `file_path` - Where file saved
- `file_size` - Size in bytes
- `requested_by` - User ID
- `requested_at`, `completed_at`
- `error_message` - If failed

#### report_schedules

Scheduled report configurations.

**Key Fields:**
- `schedule_id` - UUID
- `schedule_name` - Human-readable
- `report_type`
- `parameters` - JSON
- `cron_expression` - Schedule (0 8 * * 1-5 = weekdays 8 AM)
- `output_format`
- `recipients` - JSON array of emails
- `enabled` - Boolean
- `last_run_at`, `next_run_at`
- `created_by`

---

## 🔄 Integration Points

### With All Data Modules
- Reports query data from inventory, movements, orders, etc.
- Real-time data for on-demand reports
- Historical data for period reports

### With RabbitMQ
- Async report requests published to queue
- Background workers consume and generate reports

### With Notification Module
- Email notification when async report ready
- Scheduled report delivery via email

### With File Storage
- Generated report files saved to storage
- Accessible via download link
- Cleanup after retention period

---

## 📊 Business Rules Summary

| Rule ID | Description |
|---------|-------------|
| BR-RPT-01 | Real-time data (no caching) |
| BR-RPT-02 | Sort by warehouse, then SKU |
| BR-RPT-03 | Highlight low stock in red |
| BR-RPT-04 | Use cost_price for valuation |
| BR-RPT-05 | Include only ACTIVE products |
| BR-RPT-06 | Group by category with subtotals |
| BR-RPT-07 | Default date range: last 7 days |
| BR-RPT-08 | Sort by movement_date DESC |
| BR-RPT-09 | Show totals by movement type |
| BR-RPT-10 | Include all batch movements |
| BR-RPT-11 | Show forward trace |
| BR-RPT-12 | Show backward trace |
| BR-RPT-13 | Include batch quality status |
| BR-RPT-14 | Small reports immediate |
| BR-RPT-15 | Large reports async |
| BR-RPT-16 | Retain reports 7 days |
| BR-RPT-17 | Email notification when ready |
| BR-RPT-18 | Schedules use cron syntax |
| BR-RPT-19 | Scheduled reports emailed |
| BR-RPT-20 | Failed deliveries logged |

---

## ✅ Acceptance Criteria

1. ✅ All reports generate with correct data
2. ✅ Export to PDF, Excel, CSV functional
3. ✅ Filters work correctly
4. ✅ Async report generation for large reports
5. ✅ Email notification when report ready
6. ✅ Scheduled reports run automatically
7. ✅ Report download links expire after 7 days
8. ✅ Performance: Small reports < 5 seconds

---

## 🧪 Test Scenarios

### Functional Testing

1. **Current Stock Report**:
   - Generate for all warehouses
   - Filter by low stock only
   - Export to Excel

2. **Async Reports**:
   - Request large report (50K rows)
   - Verify status updates to PROCESSING
   - Receive email notification
   - Download completed report

3. **Scheduled Reports**:
   - Create daily schedule
   - Verify job runs at scheduled time
   - Receive email with attachment
   - Disable schedule (no more emails)

---

## 🚀 Implementation Notes

### Libraries to Use

- **PDF Generation**: Apache PDFBox or iText
- **Excel Generation**: Apache POI
- **CSV Export**: OpenCSV
- **Report Templates**: Thymeleaf or JasperReports
- **Async Processing**: RabbitMQ + Spring AMQP
- **Scheduling**: Spring @Scheduled + Quartz (optional)

### Performance Tips

- Use streaming for large Excel files (SXSSFWorkbook)
- Generate reports in chunks for very large datasets
- Cache report templates
- Use database pagination for queries
- Compress large files before download

---

**Document Version:** 1.0  
**Last Updated:** February 1, 2026  
**Status:** 🚧 Draft - Pending Review
