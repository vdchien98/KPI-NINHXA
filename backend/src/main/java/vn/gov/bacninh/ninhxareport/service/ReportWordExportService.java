package vn.gov.bacninh.ninhxareport.service;

import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.util.Units;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageSz;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
        
        // Tên phường (có thể lấy từ organization hoặc mặc định)
        String tenPhuong = "NINH XÁ";
        if (request.getCreatedBy() != null && 
            request.getCreatedBy().getOrganizations() != null && 
            !request.getCreatedBy().getOrganizations().isEmpty()) {
            tenPhuong = request.getCreatedBy().getOrganizations().iterator().next().getName().toUpperCase();
        }
        
        // Số báo cáo (dùng request ID)
        String soBaoCao = String.valueOf(requestId);
        
        // Tạo table 1 row, 2 columns cho header
        XWPFTable headerTable = document.createTable(1, 2);
        headerTable.setWidth("100%");
        
        // Cột trái
        XWPFTableCell leftCell = headerTable.getRow(0).getCell(0);
        leftCell.setWidth("50%");
        XWPFParagraph leftPara1 = leftCell.addParagraph();
        leftPara1.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun leftRun1 = leftPara1.createRun();
        leftRun1.setText("PHƯỜNG " + tenPhuong);
        leftRun1.setBold(true);
        leftRun1.setFontSize(13);
        leftRun1.setFontFamily("Times New Roman");
        leftRun1.setUnderline(UnderlinePatterns.SINGLE);
        
        XWPFParagraph leftPara2 = leftCell.addParagraph();
        XWPFRun leftRun2 = leftPara2.createRun();
        leftRun2.setText("Số: " + soBaoCao + "/BC-PVHXH");
        leftRun2.setFontSize(13);
        leftRun2.setFontFamily("Times New Roman");
        
        // Cột phải
        XWPFTableCell rightCell = headerTable.getRow(0).getCell(1);
        rightCell.setWidth("50%");
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
        
        // Dòng địa danh, ngày tháng
        XWPFParagraph datePara = document.createParagraph();
        datePara.setAlignment(ParagraphAlignment.CENTER);
        datePara.setSpacingAfter(200);
        XWPFRun dateRun = datePara.createRun();
        dateRun.setText(tenPhuong + ", ngày " + day + " tháng " + month + " năm " + year);
        dateRun.setFontSize(13);
        dateRun.setFontFamily("Times New Roman");
        
        // Tiêu đề BÁO CÁO
        XWPFParagraph titlePara = document.createParagraph();
        titlePara.setAlignment(ParagraphAlignment.CENTER);
        titlePara.setSpacingAfter(200);
        XWPFRun titleRun = titlePara.createRun();
        titleRun.setText("BÁO CÁO");
        titleRun.setBold(true);
        titleRun.setFontSize(13);
        titleRun.setFontFamily("Times New Roman");
        
        // Dòng phụ đề
        XWPFParagraph subtitlePara = document.createParagraph();
        subtitlePara.setAlignment(ParagraphAlignment.CENTER);
        subtitlePara.setSpacingAfter(200);
        XWPFRun subtitleRun = subtitlePara.createRun();
        subtitleRun.setText("Nội dung báo cáo chính là nội dung mà người gửi đến mọi người");
        subtitleRun.setItalic(true);
        subtitleRun.setFontSize(13);
        subtitleRun.setFontFamily("Times New Roman");
        
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

