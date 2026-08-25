package com.passwordmanager.backend.service;

import com.passwordmanager.backend.entity.Device;
import com.passwordmanager.backend.entity.Folder;
import com.passwordmanager.backend.entity.User;
import com.passwordmanager.backend.repository.DeviceRepository;
import com.passwordmanager.backend.repository.FolderRepository;
import com.passwordmanager.backend.repository.UserRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ExcelImportService {

    private final DeviceRepository deviceRepository;
    private final FolderRepository folderRepository;
    private final DatacenterService datacenterService;
    private final CredentialService credentialService;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public ExcelImportService(DeviceRepository deviceRepository,
                              FolderRepository folderRepository,
                              DatacenterService datacenterService,
                              CredentialService credentialService,
                              UserRepository userRepository,
                              AuditService auditService) {
        this.deviceRepository = deviceRepository;
        this.folderRepository = folderRepository;
        this.datacenterService = datacenterService;
        this.credentialService = credentialService;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    public List<Map<String, Object>> previewImport(MultipartFile file, Long folderId, String username) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!datacenterService.hasWriteAccess(folder.getDatacenter().getId(), user.getId())) {
            throw new SecurityException("Permission denied to import devices in this datacenter");
        }

        List<Map<String, Object>> previewRows = new ArrayList<>();

        try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rowIterator = sheet.iterator();

            if (!rowIterator.hasNext()) {
                throw new IllegalArgumentException("Excel file is empty");
            }

            Row headerRow = rowIterator.next();
            Map<String, Integer> colIndexes = parseHeaders(headerRow);

            int rowNum = 1;
            Set<String> seenHostnames = new HashSet<>();

            while (rowIterator.hasNext()) {
                rowNum++;
                Row row = rowIterator.next();
                if (isRowEmpty(row)) continue;

                Map<String, Object> rowData = new HashMap<>();
                rowData.put("rowNum", rowNum);

                String hostname = getCellValue(row, colIndexes.get("hostname"));
                String model = getCellValue(row, colIndexes.get("model"));
                String serialNumber = getCellValue(row, colIndexes.get("serial_number"));
                String capacity = getCellValue(row, colIndexes.get("capacity"));
                String serviceTag = getCellValue(row, colIndexes.get("service_tag"));
                String supportEod = getCellValue(row, colIndexes.get("support_eod"));
                String idracPassword = getCellValue(row, colIndexes.get("idrac_password"));
                String sysadminPassword = getCellValue(row, colIndexes.get("sysadmin_password"));
                String secoffPassword = getCellValue(row, colIndexes.get("secoff_password"));
                String passphrase = getCellValue(row, colIndexes.get("passphrase"));

                rowData.put("hostname", hostname);
                rowData.put("model", model);
                rowData.put("serialNumber", serialNumber);
                rowData.put("capacity", capacity);
                rowData.put("serviceTag", serviceTag);
                rowData.put("supportEod", supportEod);
                rowData.put("idracPassword", idracPassword != null ? "••••••••" : "");
                rowData.put("sysadminPassword", sysadminPassword != null ? "••••••••" : "");
                rowData.put("secoffPassword", secoffPassword != null ? "••••••••" : "");
                rowData.put("passphrase", passphrase != null ? "••••••••" : "");
                rowData.put("rawIdracPassword", idracPassword);
                rowData.put("rawSysadminPassword", sysadminPassword);
                rowData.put("rawSecoffPassword", secoffPassword);
                rowData.put("rawPassphrase", passphrase);

                List<String> errors = new ArrayList<>();
                if (hostname == null || hostname.isBlank()) {
                    errors.add("Hostname is required");
                } else {
                    if (seenHostnames.contains(hostname.toLowerCase())) {
                        errors.add("Duplicate hostname in spreadsheet");
                    }
                    seenHostnames.add(hostname.toLowerCase());

                    if (deviceRepository.existsByHostnameAndFolderId(hostname.trim(), folderId)) {
                        errors.add("Device already exists in destination folder");
                    }
                }

                rowData.put("valid", errors.isEmpty());
                rowData.put("errors", errors);

                previewRows.add(rowData);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Excel file: " + e.getMessage(), e);
        }

        return previewRows;
    }

    @Transactional
    public int commitImport(Long folderId, List<Map<String, Object>> rows, String username) {
        Folder folder = folderRepository.findById(folderId)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Long datacenterId = folder.getDatacenter().getId();
        if (!datacenterService.hasWriteAccess(datacenterId, user.getId())) {
            throw new SecurityException("Permission denied to import devices in this datacenter");
        }

        int count = 0;
        for (Map<String, Object> row : rows) {
            String hostname = (String) row.get("hostname");
            if (hostname == null || hostname.isBlank()) continue;

            String model = (String) row.get("model");
            String serialNumber = (String) row.get("serialNumber");
            String capacity = (String) row.get("capacity");
            String serviceTag = (String) row.get("serviceTag");
            String supportEodStr = (String) row.get("supportEod");

            LocalDate supportEndDate = parseDate(supportEodStr);

            Device device = new Device(hostname.trim(), model, serialNumber, capacity, serviceTag, supportEndDate, folder);
            Device saved = deviceRepository.save(device);

            credentialService.saveCredential(saved, "IDRAC", "root", (String) row.get("rawIdracPassword"), username);
            credentialService.saveCredential(saved, "SYSADMIN", "sysadmin", (String) row.get("rawSysadminPassword"), username);
            credentialService.saveCredential(saved, "SECOFF", "secoff", (String) row.get("rawSecoffPassword"), username);
            credentialService.saveCredential(saved, "PASSPHRASE", "admin", (String) row.get("rawPassphrase"), username);

            count++;
        }

        auditService.log(user.getId(), user.getUsername(), "IMPORT_EXCEL", "FOLDER", folderId, datacenterId, "Imported " + count + " devices from Excel");

        return count;
    }

    private Map<String, Integer> parseHeaders(Row headerRow) {
        Map<String, Integer> map = new HashMap<>();
        for (Cell cell : headerRow) {
            String headerStr = cell.getStringCellValue().trim().toLowerCase().replaceAll("[^a-z0-9_]", "_");
            map.put(headerStr, cell.getColumnIndex());
        }
        return map;
    }

    private String getCellValue(Row row, Integer colIndex) {
        if (colIndex == null) return null;
        Cell cell = row.getCell(colIndex);
        if (cell == null) return null;

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getLocalDateTimeCellValue().toLocalDate().toString()
                    : String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) return true;
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;
        try {
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            try {
                return LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (Exception ex) {
                return null;
            }
        }
    }
}
