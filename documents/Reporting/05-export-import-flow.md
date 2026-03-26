# Export/Import Flow - Chi Tiết

> Modules: WHS-37, WHS-38, WHS-39
> Updated: 2026-03-22

---

## 1. Tổng Quan Export/Import

Module Export/Import cung cấp chức năng nhập/xuất dữ liệu sản phẩm và vị trí hàng loạt.

### 1.1 Endpoints

| Method | Endpoint | Description | Module |
|--------|----------|-------------|--------|
| GET | /api/v1/products/export | Export sản phẩm | WHS-39 |
| POST | /api/v1/products/import | Import sản phẩm | WHS-38 |
| POST | /api/v1/locations/bulk | Import vị trí | WHS-37 |

---

## 2. Product Export Flow (WHS-39)

### 2.1 Export Request

```
GET /api/v1/products/export?format=excel&category_id=uuid&status=ACTIVE

Headers:
Authorization: Bearer {token}
```

### 2.2 Export Flow Diagram

```
┌────────┐     ┌────────────┐     ┌────────────┐     ┌────────────┐     ┌────────┐
│ Client │────►│ Controller │────►│  Service   │────►│ Repository │────►│   DB   │
└────────┘     └────────────┘     └────────────┘     └────────────┘     └────────┘
    │               │                   │                   │                 │
    │ 1. GET        │                   │                   │                 │
    │ /export       │                   │                   │                 │
    │──────────────►│                   │                   │                 │
    │               │ 2. Validate       │                   │                 │
    │               │──────────────────►│                   │                 │
    │               │                   │ 3. Check          │                 │
    │               │                   │    permissions    │                 │
    │               │                   │─────┐             │                 │
    │               │                   │◄────┘             │                 │
    │               │                   │                   │                 │
    │               │                   │ 4. Query Products │                 │
    │               │                   │──────────────────►│                 │
    │               │                   │                   │ 5. Execute      │
    │               │                   │                   │────────────────►│
    │               │                   │                   │ 6. Return List  │
    │               │                   │                   │◄────────────────│
    │               │                   │                   │                 │
    │               │                   │ 7. Generate       │                 │
    │               │                   │    Excel/PDF/CSV  │                 │
    │               │                   │─────┐             │                 │
    │               │                   │     │ Apache POI  │                 │
    │               │                   │     │ OpenCSV     │                 │
    │               │                   │◄────┘             │                 │
    │               │                   │                   │                 │
    │               │ 8. Return File    │                   │                 │
    │               │◄──────────────────│                   │                 │
    │ 9. File       │                   │                   │                 │
    │◄──────────────│                   │                   │                 │
    │               │                   │                   │                 │
```

### 2.3 Excel Export Implementation

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductExportServiceImpl implements ProductExportService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UnitsOfMeasureRepository uomRepository;

    @Override
    @Transactional(readOnly = true)
    public byte[] exportToExcel(ProductExportFilter filter) {
        log.info("Exporting products to Excel with filter: {}", filter);
        
        // 1. Query products
        List<Product> products = productRepository.findAll(buildSpecification(filter));
        
        // 2. Create workbook
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Products");
            
            // 3. Create header style
            CellStyle headerStyle = createHeaderStyle(workbook);
            
            // 4. Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {
                "SKU", "Name", "Description", "Category", "UOM",
                "Weight", "Dimensions", "Status", "Min Stock",
                "Reorder Point", "Cost Price", "Selling Price", "Barcode"
            };
            
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }
            
            // 5. Populate data rows
            int rowNum = 1;
            for (Product product : products) {
                Row row = sheet.createRow(rowNum++);
                populateProductRow(row, product);
            }
            
            // 6. Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            // 7. Write to byte array
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return bos.toByteArray();
            
        } catch (IOException e) {
            throw new ReportExportException("Failed to export products to Excel", e);
        }
    }

    private void populateProductRow(Row row, Product product) {
        row.createCell(0).setCellValue(product.getSku());
        row.createCell(1).setCellValue(product.getName());
        row.createCell(2).setCellValue(product.getDescription());
        row.createCell(3).setCellValue(getCategoryName(product.getCategoryId()));
        row.createCell(4).setCellValue(getUomName(product.getUomId()));
        row.createCell(5).setCellValue(product.getWeight() != null ? product.getWeight().doubleValue() : 0);
        row.createCell(6).setCellValue(product.getDimensions());
        row.createCell(7).setCellValue(product.getStatus().name());
        row.createCell(8).setCellValue(product.getMinStockLevel() != null ? product.getMinStockLevel().doubleValue() : 0);
        row.createCell(9).setCellValue(product.getReOrderPoint() != null ? product.getReOrderPoint().doubleValue() : 0);
        row.createCell(10).setCellValue(product.getCostPrice() != null ? product.getCostPrice().doubleValue() : 0);
        row.createCell(11).setCellValue(product.getSellingPrice() != null ? product.getSellingPrice().doubleValue() : 0);
        row.createCell(12).setCellValue(product.getBarcode());
    }
}
```

### 2.4 CSV Export Implementation

```java
@Override
@Transactional(readOnly = true)
public byte[] exportToCsv(ProductExportFilter filter) {
    List<Product> products = productRepository.findAll(buildSpecification(filter));
    
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
         OutputStreamWriter writer = new OutputStreamWriter(bos, StandardCharsets.UTF_8);
         CSVWriter csvWriter = new CSVWriter(writer)) {
        
        // Write BOM for Excel compatibility
        bos.write(0xEF);
        bos.write(0xBB);
        bos.write(0xBF);
        
        // Header
        String[] headers = {
            "SKU", "Name", "Description", "Category", "UOM",
            "Weight", "Dimensions", "Status", "Min Stock",
            "Reorder Point", "Cost Price", "Selling Price", "Barcode"
        };
        csvWriter.writeNext(headers);
        
        // Data rows
        for (Product product : products) {
            String[] data = {
                product.getSku(),
                product.getName(),
                product.getDescription(),
                getCategoryName(product.getCategoryId()),
                getUomName(product.getUomId()),
                product.getWeight() != null ? product.getWeight().toString() : "",
                product.getDimensions(),
                product.getStatus().name(),
                product.getMinStockLevel() != null ? product.getMinStockLevel().toString() : "",
                product.getReOrderPoint() != null ? product.getReOrderPoint().toString() : "",
                product.getCostPrice() != null ? product.getCostPrice().toString() : "",
                product.getSellingPrice() != null ? product.getSellingPrice().toString() : "",
                product.getBarcode()
            };
            csvWriter.writeNext(data);
        }
        
        csvWriter.flush();
        return bos.toByteArray();
        
    } catch (IOException e) {
        throw new ReportExportException("Failed to export products to CSV", e);
    }
}
```

---

## 3. Product Import Flow (WHS-38)

### 3.1 Import Request

```
POST /api/v1/products/import

Content-Type: multipart/form-data

Form Data:
- file: products.xlsx
- validate_only: false
- update_existing: true
```

### 3.2 Import Flow Diagram

```
┌────────┐     ┌────────────┐     ┌────────────┐     ┌────────────┐     ┌────────┐
│ Client │────►│ Controller │────►│  Service   │────►│ Repository │────►│   DB   │
└────────┘     └────────────┘     └────────────┘     └────────────┘     └────────┘
    │               │                   │                   │                 │
    │ 1. POST       │                   │                   │                 │
    │ /import       │                   │                   │                 │
    │ (file)        │                   │                   │                 │
    │──────────────►│                   │                   │                 │
    │               │ 2. Validate       │                   │                 │
    │               │    file           │                   │                 │
    │               │──────────────────►│                   │                 │
    │               │                   │ 3. Parse file     │                 │
    │               │                   │─────┐             │                 │
    │               │                   │     │ Apache POI  │                 │
    │               │                   │     │ OpenCSV     │                 │
    │               │                   │◄────┘             │                 │
    │               │                   │                   │                 │
    │               │                   │ 4. Validate       │                 │
    │               │                   │    data           │                 │
    │               │                   │─────┐             │                 │
    │               │                   │     │ Check:      │                 │
    │               │                   │     │ - Required  │                 │
    │               │                   │     │ - Format    │                 │
    │               │                   │     │ - Duplicate │                 │
    │               │                   │     │ - Ref data  │                 │
    │               │                   │◄────┘             │                 │
    │               │                   │                   │                 │
    │               │                   │ 5. If validate_   │                 │
    │               │                   │    only=true:     │                 │
    │               │                   │    Return errors  │                 │
    │               │                   │                   │                 │
    │               │                   │ 6. Process rows   │                 │
    │               │                   │─────┐             │                 │
    │               │                   │     │ For each    │                 │
    │               │                   │     │ valid row:  │                 │
    │               │                   │     │ - Create    │                 │
    │               │                   │     │ - Update    │                 │
    │               │                   │     │ - Skip      │                 │
    │               │                   │◄────┘             │                 │
    │               │                   │                   │                 │
    │               │                   │ 7. Save to DB     │                 │
    │               │                   │──────────────────►│                 │
    │               │                   │                   │ 8. Batch save   │
    │               │                   │                   │────────────────►│
    │               │                   │                   │                 │
    │               │ 9. Return Result  │                   │                 │
    │               │◄──────────────────│                   │                 │
    │ 10. Result    │                   │                   │                 │
    │◄──────────────│                   │                   │                 │
    │               │                   │                   │                 │
```

### 3.3 Import Response

```json
{
  "success": true,
  "data": {
    "import_id": "uuid",
    "total_rows": 100,
    "success_count": 85,
    "error_count": 10,
    "skip_count": 5,
    "errors": [
      {
        "row": 5,
        "field": "sku",
        "value": "INVALID-SKU",
        "message": "SKU already exists"
      },
      {
        "row": 12,
        "field": "category_id",
        "value": "non-existent-uuid",
        "message": "Category not found"
      }
    ],
    "warnings": [
      {
        "row": 8,
        "message": "Product updated with new values"
      }
    ]
  }
}
```

### 3.4 Import Implementation

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class ProductImportServiceImpl implements ProductImportService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UnitsOfMeasureRepository uomRepository;

    @Override
    @Transactional
    public ProductImportResponse importProducts(MultipartFile file, boolean validateOnly, boolean updateExisting) {
        log.info("Importing products from file: {}, validateOnly: {}, updateExisting: {}",
                file.getOriginalFilename(), validateOnly, updateExisting);
        
        ProductImportResponse response = new ProductImportResponse();
        List<ProductImportRow> rows = new ArrayList<>();
        
        try {
            // 1. Parse file
            String fileExtension = getFileExtension(file.getOriginalFilename());
            if ("xlsx".equalsIgnoreCase(fileExtension) || "xls".equalsIgnoreCase(fileExtension)) {
                rows = parseExcelFile(file);
            } else if ("csv".equalsIgnoreCase(fileExtension)) {
                rows = parseCsvFile(file);
            } else {
                throw new InvalidFileException("Unsupported file format: " + fileExtension);
            }
            
            response.setTotalRows(rows.size());
            
            // 2. Validate all rows
            List<ProductValidationError> validationErrors = validateRows(rows);
            
            if (validateOnly) {
                response.setErrors(validationErrors);
                response.setSuccessCount(rows.size() - validationErrors.size());
                response.setErrorCount(validationErrors.size());
                return response;
            }
            
            // 3. Process valid rows
            int successCount = 0;
            int errorCount = 0;
            int skipCount = 0;
            
            for (int i = 0; i < rows.size(); i++) {
                ProductImportRow row = rows.get(i);
                
                try {
                    // Check if row has validation error
                    boolean hasError = validationErrors.stream()
                            .anyMatch(e -> e.getRow() == i + 1);
                    
                    if (hasError) {
                        errorCount++;
                        continue;
                    }
                    
                    // Check if product exists
                    Optional<Product> existingProduct = productRepository.findBySku(row.getSku());
                    
                    if (existingProduct.isPresent()) {
                        if (updateExisting) {
                            updateProduct(existingProduct.get(), row);
                            successCount++;
                        } else {
                            skipCount++;
                        }
                    } else {
                        createProduct(row);
                        successCount++;
                    }
                    
                } catch (Exception e) {
                    log.error("Failed to process row {}: {}", i + 1, e.getMessage());
                    validationErrors.add(new ProductValidationError(
                            i + 1, "general", "", "Failed to save: " + e.getMessage()
                    ));
                    errorCount++;
                }
            }
            
            response.setSuccessCount(successCount);
            response.setErrorCount(errorCount);
            response.setSkipCount(skipCount);
            response.setErrors(validationErrors);
            
            return response;
            
        } catch (IOException e) {
            throw new ImportException("Failed to read import file", e);
        }
    }

    private List<ProductImportRow> parseExcelFile(MultipartFile file) throws IOException {
        List<ProductImportRow> rows = new ArrayList<>();
        
        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();
            
            // Skip header row
            if (rowIterator.hasNext()) {
                rowIterator.next();
            }
            
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                ProductImportRow importRow = mapRowToImport(row);
                rows.add(importRow);
            }
        }
        
        return rows;
    }

    private List<ProductValidationError> validateRows(List<ProductImportRow> rows) {
        List<ProductValidationError> errors = new ArrayList<>();
        
        // Load reference data
        Map<String, Category> categoriesByName = categoryRepository.findAll().stream()
                .collect(Collectors.toMap(Category::getName, c -> c));
        Map<String, UnitsOfMeasure> uomsByName = uomRepository.findAll().stream()
                .collect(Collectors.toMap(UnitsOfMeasure::getName, u -> u));
        
        Set<String> skusInBatch = new HashSet<>();
        
        for (int i = 0; i < rows.size(); i++) {
            ProductImportRow row = rows.get(i);
            int rowNum = i + 1;
            
            // Validate SKU
            if (row.getSku() == null || row.getSku().trim().isEmpty()) {
                errors.add(new ProductValidationError(rowNum, "sku", "", "SKU is required"));
            } else if (skusInBatch.contains(row.getSku())) {
                errors.add(new ProductValidationError(rowNum, "sku", row.getSku(), "Duplicate SKU in file"));
            } else {
                skusInBatch.add(row.getSku());
            }
            
            // Validate Name
            if (row.getName() == null || row.getName().trim().isEmpty()) {
                errors.add(new ProductValidationError(rowNum, "name", "", "Name is required"));
            }
            
            // Validate Category
            if (row.getCategoryName() != null && !categoriesByName.containsKey(row.getCategoryName())) {
                errors.add(new ProductValidationError(rowNum, "category_name", row.getCategoryName(), "Category not found"));
            }
            
            // Validate UOM
            if (row.getUomName() != null && !uomsByName.containsKey(row.getUomName())) {
                errors.add(new ProductValidationError(rowNum, "uom_name", row.getUomName(), "UOM not found"));
            }
            
            // Validate numeric fields
            if (row.getCostPrice() != null && row.getCostPrice().compareTo(BigDecimal.ZERO) < 0) {
                errors.add(new ProductValidationError(rowNum, "cost_price", row.getCostPrice().toString(), "Cost price must be positive"));
            }
        }
        
        return errors;
    }
}
```

### 3.5 Excel Template

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      Product Import Template                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  Sheet: Products                                                            │
│                                                                             │
│  ┌─────┬──────────┬────────────┬─────────────┬────────────┬─────────────┐ │
│  │ Row │   SKU    │    Name    │ Description │  Category  │    UOM      │ │
│  ├─────┼──────────┼────────────┼─────────────┼────────────┼─────────────┤ │
│  │  1  │ (Header) │  (Header)  │  (Header)   │  (Header)  │  (Header)   │ │
│  │  2  │ SP001    │ Product A  │ Description │ Electronics│   Piece     │ │
│  │  3  │ SP002    │ Product B  │ Description │ Clothing   │   Box       │ │
│  └─────┴──────────┴────────────┴─────────────┴────────────┴─────────────┘ │
│                                                                             │
│  Columns:                                                                   │
│  ├── A: SKU (required, unique)                                             │
│  ├── B: Name (required, max 200 chars)                                     │
│  ├── C: Description (optional)                                             │
│  ├── D: Category (required, must exist in system)                          │
│  ├── E: UOM (required, must exist in system)                               │
│  ├── F: Weight (optional, number)                                          │
│  ├── G: Dimensions (optional, max 100 chars)                               │
│  ├── H: Status (optional, default: ACTIVE)                                 │
│  ├── I: Min Stock Level (optional, number)                                 │
│  ├── J: Reorder Point (optional, number)                                   │
│  ├── K: Cost Price (optional, number)                                      │
│  ├── L: Selling Price (optional, number)                                   │
│  └── M: Barcode (optional, max 100 chars)                                  │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Location Bulk Import Flow (WHS-37)

### 4.1 Import Request

```
POST /api/v1/locations/bulk

Content-Type: multipart/form-data

Form Data:
- file: locations.csv
- warehouse_id: uuid
```

### 4.2 CSV Format

```
code,name,type,address,aisle,rack,shelf,bin,status
A-01-01,Location A-01-01,PICKING,Aisle A,Rack 01,Shelf 01,Bin 01,ACTIVE
A-01-02,Location A-01-02,STORAGE,Aisle A,Rack 01,Shelf 01,Bin 02,ACTIVE
```

### 4.3 Import Flow

```
┌────────┐     ┌────────────┐     ┌────────────┐     ┌────────────┐
│ Client │────►│ Controller │────►│  Service   │────►│ Repository │
└────────┘     └────────────┘     └────────────┘     └────────────┘
    │               │                   │                   │
    │ 1. POST       │                   │                   │
    │ /locations/   │                   │                   │
    │ bulk (file)   │                   │                   │
    │──────────────►│                   │                   │
    │               │ 2. Validate       │                   │
    │               │──────────────────►│                   │
    │               │                   │ 3. Parse CSV      │
    │               │                   │─────┐             │
    │               │                   │◄────┘             │
    │               │                   │                   │
    │               │                   │ 4. Validate       │
    │               │                   │    rows           │
    │               │                   │─────┐             │
    │               │                   │     │ Check:      │
    │               │                   │     │ - Code      │
    │               │                   │     │ - Type      │
    │               │                   │     │ - Duplicate │
    │               │                   │◄────┘             │
    │               │                   │                   │
    │               │                   │ 5. Check existing │
    │               │                   │──────────────────►│
    │               │                   │                   │
    │               │                   │ 6. Batch save     │
    │               │                   │──────────────────►│
    │               │ 7. Return Result  │                   │
    │               │◄──────────────────│                   │
    │ 8. Result     │                   │                   │
    │◄──────────────│                   │                   │
    │               │                   │                   │
```

### 4.4 Implementation

```java
@Service
@Slf4j
@RequiredArgsConstructor
public class LocationBulkImportServiceImpl implements LocationBulkImportService {

    private final LocationRepository locationRepository;
    private final WareHouseRepository warehouseRepository;

    @Override
    @Transactional
    public LocationBulkImportResponse importLocations(MultipartFile file, String warehouseId) {
        log.info("Importing locations from file: {} for warehouse: {}", 
                file.getOriginalFilename(), warehouseId);
        
        // 1. Validate warehouse exists
        WareHouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + warehouseId));
        
        // 2. Parse CSV file
        List<LocationImportRow> rows = parseCsvFile(file);
        
        // 3. Validate rows
        List<ValidationError> errors = validateRows(rows, warehouseId);
        
        // 4. Process valid rows
        List<Location> locations = new ArrayList<>();
        int successCount = 0;
        
        for (int i = 0; i < rows.size(); i++) {
            LocationImportRow row = rows.get(i);
            
            // Skip rows with errors
            boolean hasError = errors.stream().anyMatch(e -> e.getRow() == i + 1);
            if (hasError) continue;
            
            Location location = Location.builder()
                    .id(UUID.randomUUID().toString())
                    .warehouseId(warehouseId)
                    .code(row.getCode())
                    .name(row.getName())
                    .type(LocationType.valueOf(row.getType()))
                    .aisle(row.getAisle())
                    .rack(row.getRack())
                    .shelf(row.getShelf())
                    .bin(row.getBin())
                    .status(LocationStatus.ACTIVE)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            
            locations.add(location);
            successCount++;
        }
        
        // 5. Batch save
        locationRepository.saveAll(locations);
        
        return LocationBulkImportResponse.builder()
                .totalRows(rows.size())
                .successCount(successCount)
                .errorCount(errors.size())
                .errors(errors)
                .build();
    }

    private List<LocationImportRow> parseCsvFile(MultipartFile file) {
        List<LocationImportRow> rows = new ArrayList<>();
        
        try (CSVReader reader = new CSVReader(new InputStreamReader(file.getInputStream()))) {
            String[] line;
            boolean isFirstLine = true;
            
            while ((line = reader.readNext()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip header
                }
                
                LocationImportRow row = LocationImportRow.builder()
                        .code(line[0])
                        .name(line[1])
                        .type(line[2])
                        .aisle(line[3])
                        .rack(line[4])
                        .shelf(line[5])
                        .bin(line[6])
                        .status(line[7])
                        .build();
                
                rows.add(row);
            }
            
        } catch (IOException | CsvValidationException e) {
            throw new ImportException("Failed to parse CSV file", e);
        }
        
        return rows;
    }
}
```

---

## 5. Validation Rules

### 5.1 Product Validation

| Field | Rule | Error Message |
|-------|------|---------------|
| SKU | Required, unique, max 50 chars | "SKU is required" / "SKU already exists" |
| Name | Required, max 200 chars | "Name is required" |
| Category | Required, must exist | "Category not found" |
| UOM | Required, must exist | "UOM not found" |
| Cost Price | Positive number | "Cost price must be positive" |
| Selling Price | Positive number | "Selling price must be positive" |
| Status | Valid enum value | "Invalid status value" |

### 5.2 Location Validation

| Field | Rule | Error Message |
|-------|------|---------------|
| Code | Required, unique per warehouse | "Code is required" / "Code already exists" |
| Name | Required, max 100 chars | "Name is required" |
| Type | Valid enum (PICKING/STORAGE/STAGING) | "Invalid location type" |
| Warehouse | Must exist | "Warehouse not found" |

---

## 6. Error Handling

### 6.1 Error Response Format

```json
{
  "success": false,
  "error_code": "IMPORT_VALIDATION_FAILED",
  "message": "Import validation failed with 10 errors",
  "data": {
    "import_id": "uuid",
    "total_rows": 100,
    "success_count": 0,
    "error_count": 10,
    "errors": [
      {
        "row": 5,
        "field": "sku",
        "value": "",
        "message": "SKU is required"
      }
    ]
  }
}
```

### 6.2 File Validation Errors

| Error | Code | Message |
|-------|------|---------|
| Invalid file format | INVALID_FORMAT | "Unsupported file format. Use Excel or CSV" |
| File too large | FILE_TOO_LARGE | "File size exceeds maximum limit of 10MB" |
| Empty file | EMPTY_FILE | "File contains no data rows" |
| Invalid headers | INVALID_HEADERS | "File headers do not match template" |

---

## 7. Performance Considerations

### 7.1 Batch Processing

```java
@Configuration
public class BatchConfig {
    
    private static final int BATCH_SIZE = 100;

    public <T> void processInBatches(List<T> items, Consumer<List<T>> processor) {
        for (int i = 0; i < items.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, items.size());
            List<T> batch = items.subList(i, end);
            processor.accept(batch);
        }
    }
}
```

### 7.2 File Size Limits

| Format | Max Size | Max Rows |
|--------|----------|----------|
| Excel | 10 MB | 50,000 |
| CSV | 5 MB | 100,000 |

---

## 8. Documentation References

- Apache POI: [POI Documentation](https://poi.apache.org/)
- OpenCSV: [OpenCSV Documentation](http://opencsv.sourceforge.net/)
- Spring Multipart: [Spring File Upload](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-methods/multipart-forms.html)
