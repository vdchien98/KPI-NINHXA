package vn.gov.bacninh.ninhxareport.service;

import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.util.Units;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import vn.gov.bacninh.ninhxareport.entity.Department;
import vn.gov.bacninh.ninhxareport.entity.Organization;
import vn.gov.bacninh.ninhxareport.entity.ReportRequest;
import vn.gov.bacninh.ninhxareport.entity.ReportResponse;
import vn.gov.bacninh.ninhxareport.repository.ReportRequestRepository;
import vn.gov.bacninh.ninhxareport.repository.ReportResponseRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ReportWordExportService {
    
    @Autowired
    private ReportRequestRepository reportRequestRepository;
    
    @Autowired
    private ReportResponseRepository reportResponseRepository;
    
    public byte[] generateWordDocument(Long requestId) throws IOException {
        ReportRequest request = reportRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy yêu cầu báo cáo với id: " + requestId));
        
        List<ReportResponse> responses = reportResponseRepository.findByReportRequestIdOrderByCreatedAtDesc(requestId);
        
        // Lọc chỉ các response đã nộp (có items)
        responses = responses.stream()
                .filter(r -> r.getItems() != null && !r.getItems().isEmpty())
                .toList();
        
        if (responses.isEmpty()) {
            throw new RuntimeException("Không có báo cáo nào đã được nộp");
        }
        
        XWPFDocument document = new XWPFDocument();
        
        // Thiết lập page size A4
        CTSectPr sectPr = document.getDocument().getBody().getSectPr();
        if (sectPr == null) {
            sectPr = document.getDocument().getBody().addNewSectPr();
        }
        CTPageSz pageSize = sectPr.getPgSz();
        if (pageSize == null) {
            pageSize = sectPr.addNewPgSz();
        }
        pageSize.setW(BigInteger.valueOf(11906)); // A4 width in twips (210mm)
        pageSize.setH(BigInteger.valueOf(16838)); // A4 height in twips (297mm)
        
        // Lấy thông tin ngày tháng
        LocalDate now = LocalDate.now();
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd");
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MM");
        DateTimeFormatter yearFormatter = DateTimeFormatter.ofPattern("yyyy");
        String day = now.format(dayFormatter);
        String month = now.format(monthFormatter);
        String year = now.format(yearFormatter);
        
        // Tên phường (mặc định là NINH XÁ)
        String tenPhuong = "NINH XÁ";
        // Có thể extract từ organization name nếu cần, nhưng mặc định dùng "NINH XÁ"
        
        // Xác định loại tổ chức và header text
        String leftHeaderText = "";
        String dateLocationText = "Ninh Xá"; // Mặc định là "Ninh Xá" thay vì "ĐẢNG ỦY"
        
        if (request.getCreatedBy() != null) {
            // Nếu có department thì là phòng ban trực thuộc
            if (request.getCreatedBy().getDepartment() != null) {
                Department dept = request.getCreatedBy().getDepartment();
                Organization org = dept.getOrganization();
                if (org != null) {
                    String orgName = org.getName().toUpperCase();
                    String deptName = dept.getName().toUpperCase();
                    
                    // Kiểm tra nếu là UBND
                    if (orgName.contains("ỦY BAN") || orgName.contains("UBND")) {
                        leftHeaderText = "ỦY BAN NHÂN DÂN PHƯỜNG " + tenPhuong;
                    } else {
                        leftHeaderText = orgName;
                    }
                    // Thêm dòng phòng ban
                    // Sẽ thêm sau khi tạo leftPara1
                }
            } else if (request.getCreatedBy().getOrganizations() != null && 
                      !request.getCreatedBy().getOrganizations().isEmpty()) {
                // Nếu không có department, kiểm tra organization
                Organization org = request.getCreatedBy().getOrganizations().iterator().next();
                String orgName = org.getName().toUpperCase();
                
                if (orgName.contains("ĐẢNG ỦY") || orgName.contains("Đảng ủy")) {
                    leftHeaderText = "ĐẢNG ỦY PHƯỜNG " + tenPhuong;
                } else if (orgName.contains("ỦY BAN") || orgName.contains("UBND")) {
                    leftHeaderText = "ỦY BAN NHÂN DÂN PHƯỜNG " + tenPhuong;
                } else {
                    leftHeaderText = orgName;
                }
            }
        }
        
        // Nếu chưa xác định được thì mặc định
        if (leftHeaderText.isEmpty()) {
            leftHeaderText = "PHƯỜNG " + tenPhuong;
        }
        
        // Số báo cáo (dùng request ID)
        String soBaoCao = String.valueOf(requestId);
        
        // Tạo table 1 row, 2 columns cho header
        XWPFTable headerTable = document.createTable(1, 2);
        headerTable.setWidth("100%");
        
        // Bỏ viền của table
        try {
            var tblPr = headerTable.getCTTbl().getTblPr();
            if (tblPr == null) {
                tblPr = headerTable.getCTTbl().addNewTblPr();
            }
            var tblBorders = tblPr.getTblBorders();
            if (tblBorders == null) {
                tblBorders = tblPr.addNewTblBorders();
            }
            if (tblBorders.getTop() == null) {
                tblBorders.addNewTop().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
            } else {
                tblBorders.getTop().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
            }
            if (tblBorders.getLeft() == null) {
                tblBorders.addNewLeft().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
            } else {
                tblBorders.getLeft().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
            }
            if (tblBorders.getBottom() == null) {
                tblBorders.addNewBottom().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
            } else {
                tblBorders.getBottom().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
            }
            if (tblBorders.getRight() == null) {
                tblBorders.addNewRight().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
            } else {
                tblBorders.getRight().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
            }
            if (tblBorders.getInsideH() == null) {
                tblBorders.addNewInsideH().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
            } else {
                tblBorders.getInsideH().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
            }
            if (tblBorders.getInsideV() == null) {
                tblBorders.addNewInsideV().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
            } else {
                tblBorders.getInsideV().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
            }
        } catch (Exception e) {
            // Bỏ qua nếu không thể set border
        }
        
        // Cột trái
        XWPFTableCell leftCell = headerTable.getRow(0).getCell(0);
        leftCell.setWidth("50%");
        // Bỏ viền của cell trái
        try {
            var tcPr = leftCell.getCTTc().getTcPr();
            if (tcPr == null) {
                tcPr = leftCell.getCTTc().addNewTcPr();
            }
            var tcBorders = tcPr.getTcBorders();
            if (tcBorders == null) {
                tcBorders = tcPr.addNewTcBorders();
            }
            if (tcBorders.getTop() == null) {
                tcBorders.addNewTop().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
            } else {
                tcBorders.getTop().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
            }
            if (tcBorders.getLeft() == null) {
                tcBorders.addNewLeft().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
            } else {
                tcBorders.getLeft().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
            }
            if (tcBorders.getBottom() == null) {
                tcBorders.addNewBottom().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
            } else {
                tcBorders.getBottom().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
            }
            if (tcBorders.getRight() == null) {
                tcBorders.addNewRight().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
            } else {
                tcBorders.getRight().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
            }
        } catch (Exception e) {
            // Bỏ qua nếu không thể set border
        }
        XWPFParagraph leftPara1 = leftCell.addParagraph();
        leftPara1.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun leftRun1 = leftPara1.createRun();
        leftRun1.setText(leftHeaderText);
        leftRun1.setBold(true);
        leftRun1.setFontSize(13);
        leftRun1.setFontFamily("Times New Roman");
        leftRun1.setUnderline(UnderlinePatterns.SINGLE);
        
        // Nếu có department, thêm dòng tên phòng ban
        if (request.getCreatedBy() != null && request.getCreatedBy().getDepartment() != null) {
            XWPFParagraph leftParaDept = leftCell.addParagraph();
            XWPFRun leftRunDept = leftParaDept.createRun();
            leftRunDept.setText(request.getCreatedBy().getDepartment().getName().toUpperCase());
            leftRunDept.setBold(true);
            leftRunDept.setFontSize(13);
            leftRunDept.setFontFamily("Times New Roman");
            leftRunDept.setUnderline(UnderlinePatterns.SINGLE);
        }
        
        XWPFParagraph leftPara2 = leftCell.addParagraph();
        XWPFRun leftRun2 = leftPara2.createRun();
        leftRun2.setText("Số: " + soBaoCao + "/BC-PVHXH");
        leftRun2.setFontSize(13);
        leftRun2.setFontFamily("Times New Roman");
        
        // Cột phải
        XWPFTableCell rightCell = headerTable.getRow(0).getCell(1);
        rightCell.setWidth("50%");
        // Bỏ viền của cell phải
        try {
            var rightTcPr = rightCell.getCTTc().getTcPr();
            if (rightTcPr == null) {
                rightTcPr = rightCell.getCTTc().addNewTcPr();
            }
            var rightTcBorders = rightTcPr.getTcBorders();
            if (rightTcBorders == null) {
                rightTcBorders = rightTcPr.addNewTcBorders();
            }
            if (rightTcBorders.getTop() == null) {
                rightTcBorders.addNewTop().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
            } else {
                rightTcBorders.getTop().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
            }
            if (rightTcBorders.getLeft() == null) {
                rightTcBorders.addNewLeft().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
            } else {
                rightTcBorders.getLeft().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
            }
            if (rightTcBorders.getBottom() == null) {
                rightTcBorders.addNewBottom().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
            } else {
                rightTcBorders.getBottom().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
            }
            if (rightTcBorders.getRight() == null) {
                rightTcBorders.addNewRight().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
            } else {
                rightTcBorders.getRight().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
            }
        } catch (Exception e) {
            // Bỏ qua nếu không thể set border
        }
        // Bỏ viền của cell phải
        rightCell.getCTTc().addNewTcPr().addNewTcBorders();
        rightCell.getCTTc().getTcPr().getTcBorders().addNewTop().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
        rightCell.getCTTc().getTcPr().getTcBorders().addNewLeft().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
        rightCell.getCTTc().getTcPr().getTcBorders().addNewBottom().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
        rightCell.getCTTc().getTcPr().getTcBorders().addNewRight().setVal(org.openxmlformats.schemas.wordprocessingml.x2006.main.STBorder.NONE);
        XWPFParagraph rightPara1 = rightCell.addParagraph();
        rightPara1.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun rightRun1 = rightPara1.createRun();
        rightRun1.setText("CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM");
        rightRun1.setBold(true);
        rightRun1.setFontSize(13);
        rightRun1.setFontFamily("Times New Roman");
        
        XWPFParagraph rightPara2 = rightCell.addParagraph();
        rightPara2.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun rightRun2 = rightPara2.createRun();
        rightRun2.setText("Độc lập - Tự do - Hạnh phúc");
        rightRun2.setBold(true);
        rightRun2.setFontSize(13);
        rightRun2.setFontFamily("Times New Roman");
        rightRun2.setUnderline(UnderlinePatterns.SINGLE);
        
        // Dòng địa danh, ngày tháng (thay "ĐẢNG ỦY" thành "Ninh Xá")
        XWPFParagraph datePara = document.createParagraph();
        datePara.setAlignment(ParagraphAlignment.CENTER);
        datePara.setSpacingAfter(200);
        XWPFRun dateRun = datePara.createRun();
        dateRun.setText(dateLocationText + ", ngày " + day + " tháng " + month + " năm " + year);
        dateRun.setFontSize(13);
        dateRun.setFontFamily("Times New Roman");
        
        // Tiêu đề báo cáo (lấy từ request title)
        String reportTitle = request.getTitle() != null ? request.getTitle().toUpperCase() : "BÁO CÁO";
        XWPFParagraph titlePara = document.createParagraph();
        titlePara.setAlignment(ParagraphAlignment.CENTER);
        titlePara.setSpacingAfter(200);
        XWPFRun titleRun = titlePara.createRun();
        titleRun.setText(reportTitle);
        titleRun.setBold(true);
        titleRun.setFontSize(13);
        titleRun.setFontFamily("Times New Roman");
        
        // Dòng mô tả (nếu có)
        if (request.getDescription() != null && !request.getDescription().trim().isEmpty()) {
            XWPFParagraph subtitlePara = document.createParagraph();
            subtitlePara.setAlignment(ParagraphAlignment.CENTER);
            subtitlePara.setSpacingAfter(200);
            XWPFRun subtitleRun = subtitlePara.createRun();
            subtitleRun.setText(request.getDescription());
            subtitleRun.setItalic(true);
            subtitleRun.setFontSize(13);
            subtitleRun.setFontFamily("Times New Roman");
        }
        
        // Mục I: Kết quả thực hiện nhiệm vụ
        XWPFParagraph sectionIPara = document.createParagraph();
        sectionIPara.setAlignment(ParagraphAlignment.LEFT);
        sectionIPara.setSpacingAfter(200);
        XWPFRun sectionIRun = sectionIPara.createRun();
        sectionIRun.setText("I. Kết quả thực hiện nhiệm vụ:");
        sectionIRun.setBold(true);
        sectionIRun.setFontSize(13);
        sectionIRun.setFontFamily("Times New Roman");
        
        // Duyệt qua các responses (mỗi response là một thành viên)
        int memberIndex = 1;
        for (ReportResponse response : responses) {
            String memberName = response.getSubmittedBy().getFullName();
            
            // Mục con: Thành viên thứ X
            XWPFParagraph memberPara = document.createParagraph();
            memberPara.setAlignment(ParagraphAlignment.LEFT);
            memberPara.setSpacingAfter(100);
            XWPFRun memberRun = memberPara.createRun();
            memberRun.setText(memberIndex + ". " + memberName + " :");
            memberRun.setBold(true);
            memberRun.setFontSize(13);
            memberRun.setFontFamily("Times New Roman");
            
            // Duyệt qua các items của response
            if (response.getItems() != null) {
                for (var item : response.getItems()) {
                    // Gạch đầu dòng
                    XWPFParagraph itemPara = document.createParagraph();
                    itemPara.setAlignment(ParagraphAlignment.LEFT);
                    itemPara.setSpacingAfter(100);
                    itemPara.setIndentationLeft(400); // Indent cho bullet
                    
                    XWPFRun itemRun = itemPara.createRun();
                    String itemText = "";
                    
                    // Nếu có title thì dùng title, nếu không thì dùng content
                    if (item.getTitle() != null && !item.getTitle().trim().isEmpty()) {
                        itemText = item.getTitle();
                        if (item.getContent() != null && !item.getContent().trim().isEmpty()) {
                            itemText += ": " + item.getContent();
                        }
                    } else if (item.getContent() != null && !item.getContent().trim().isEmpty()) {
                        itemText = item.getContent();
                    }
                    
                    if (!itemText.isEmpty()) {
                        itemRun.setText("- " + itemText);
                        itemRun.setFontSize(13);
                        itemRun.setFontFamily("Times New Roman");
                    }
                    
                    // Hiển thị trạng thái tiến độ
                    XWPFParagraph progressPara = document.createParagraph();
                    progressPara.setAlignment(ParagraphAlignment.LEFT);
                    progressPara.setSpacingAfter(100);
                    progressPara.setIndentationLeft(400); // Indent cho bullet
                    
                    XWPFRun progressRun = progressPara.createRun();
                    String progressText;
                    if (item.getProgress() != null && item.getProgress() > 0) {
                        progressText = "Đã thực hiện : " + item.getProgress() + "%";
                    } else {
                        progressText = "Chưa cập nhật tiến độ";
                    }
                    progressRun.setText(progressText);
                    progressRun.setFontSize(13);
                    progressRun.setFontFamily("Times New Roman");
                }
            }
            
            memberIndex++;
        }
        
        // Ghi document vào ByteArrayOutputStream
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        document.write(outputStream);
        document.close();
        
        return outputStream.toByteArray();
    }
}

