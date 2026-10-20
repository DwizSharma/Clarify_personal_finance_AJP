package com.financemanager.controller;

import com.financemanager.entity.Expense;
import com.financemanager.entity.Income;
import com.financemanager.entity.User;
import com.financemanager.service.FinanceService;
import com.financemanager.service.UserService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Controller
public class ReportController {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private final UserService users;
    private final FinanceService finance;
    public ReportController(UserService users, FinanceService finance) { this.users=users; this.finance=finance; }

    @GetMapping("/reports/download")
    public ResponseEntity<byte[]> download(@AuthenticationPrincipal UserDetails principal,
                                           @RequestParam(defaultValue="csv") String format) throws IOException {
        User user=users.findByUsername(principal.getUsername());
        YearMonth month=YearMonth.now();
        BigDecimal income=finance.getMonthlyIncome(user.getId(),month);
        BigDecimal expenses=finance.getMonthlyExpense(user.getId(),month);
        BigDecimal savings=income.subtract(expenses);
        List<ReportRow> rows=rowsFor(user.getId(),month);
        byte[] bytes; MediaType type; String filename;
        if("pdf".equalsIgnoreCase(format)) {
            bytes=buildPdf(month,income,expenses,savings,rows);
            type=MediaType.APPLICATION_PDF; filename="finance-report-"+month+".pdf";
        } else {
            StringBuilder csv=new StringBuilder("Monthly finance report,").append(month).append("\n")
                    .append("Total credit,INR ").append(income).append("\n")
                    .append("Total debit,INR ").append(expenses).append("\n")
                    .append("Monthly savings,INR ").append(savings).append("\n\n")
                    .append("Date,Time,Type,Description,Category,Amount (INR)\n");
            for(ReportRow row:rows) csv.append(csv(row.date().format(DATE))).append(',').append(csv(row.time())).append(',')
                    .append(csv(row.type())).append(',').append(csv(row.description())).append(',')
                    .append(csv(row.category())).append(',').append(row.amount()).append('\n');
            bytes=csv.toString().getBytes(StandardCharsets.UTF_8); type=MediaType.parseMediaType("text/csv"); filename="finance-report-"+month+".csv";
        }
        return ResponseEntity.ok().contentType(type).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+filename+"\"").body(bytes);
    }

    private List<ReportRow> rowsFor(Long userId, YearMonth month) {
        LocalDate start=month.atDay(1), end=month.atEndOfMonth();
        List<ReportRow> rows=new ArrayList<>();
        for(Income income:finance.getIncomeHistory(userId)) if(inMonth(income.getDate(),start,end))
            rows.add(new ReportRow(income.getDate(),time(income.getRecordedAt(),income.getDate()),"Credit",text(income.getDescription(),income.getCategory().getName()),income.getCategory().getName(),income.getAmount()));
        for(Expense expense:finance.getExpenseHistory(userId)) if(inMonth(expense.getDate(),start,end))
            rows.add(new ReportRow(expense.getDate(),time(expense.getRecordedAt(),expense.getDate()),"Debit",text(expense.getDescription(),expense.getCategory().getName()),expense.getCategory().getName(),expense.getAmount()));
        rows.sort(Comparator.comparing(ReportRow::date).thenComparing(ReportRow::time).reversed());
        return rows;
    }
    private boolean inMonth(LocalDate date,LocalDate start,LocalDate end){return date!=null&&!date.isBefore(start)&&!date.isAfter(end);}
    private String time(LocalDateTime recordedAt,LocalDate date){return recordedAt!=null?recordedAt.format(TIME):date.atTime(12,0).format(TIME);}
    private String text(String value,String fallback){return value==null||value.isBlank()?fallback:value;}
    private String csv(String value){return "\""+(value==null?"":value.replace("\"","\"\""))+"\"";}

    private byte[] buildPdf(YearMonth month,BigDecimal income,BigDecimal expenses,BigDecimal savings,List<ReportRow> rows) throws IOException {
        try(PDDocument document=new PDDocument();ByteArrayOutputStream output=new ByteArrayOutputStream()) {
            float y=startPage(document,month,income,expenses,savings);
            PDPageContentStream content=new PDPageContentStream(document,document.getPage(0),PDPageContentStream.AppendMode.APPEND,true);
            for(ReportRow row:rows) {
                if(y<58){content.close();y=startPage(document,month,income,expenses,savings);content=new PDPageContentStream(document,document.getPage(document.getNumberOfPages()-1),PDPageContentStream.AppendMode.APPEND,true);}
                drawRow(content,y,row);y-=22;
            }
            if(rows.isEmpty()){write(content,48,y,PDType1Font.HELVETICA_OBLIQUE,9,"No credit or debit transactions were recorded this month.");}
            content.close();document.save(output);return output.toByteArray();
        }
    }
    private float startPage(PDDocument document,YearMonth month,BigDecimal income,BigDecimal expenses,BigDecimal savings) throws IOException {
        PDPage page=new PDPage(PDRectangle.LETTER);document.addPage(page);
        try(PDPageContentStream c=new PDPageContentStream(document,page)) {
            write(c,48,748,PDType1Font.HELVETICA_BOLD,19,"Clarity monthly report");
            write(c,48,726,PDType1Font.HELVETICA,10,month.getMonth()+" "+month.getYear());
            write(c,48,698,PDType1Font.HELVETICA_BOLD,9,"CREDIT  INR "+income.toPlainString());
            write(c,225,698,PDType1Font.HELVETICA_BOLD,9,"DEBIT  INR "+expenses.toPlainString());
            write(c,405,698,PDType1Font.HELVETICA_BOLD,9,"SAVINGS  INR "+savings.toPlainString());
            c.setNonStrokingColor(38,59,49);c.addRect(42,654,528,22);c.fill();c.setNonStrokingColor(255,255,255);
            write(c,48,662,PDType1Font.HELVETICA_BOLD,8,"DATE");write(c,116,662,PDType1Font.HELVETICA_BOLD,8,"TIME");write(c,157,662,PDType1Font.HELVETICA_BOLD,8,"TYPE");write(c,207,662,PDType1Font.HELVETICA_BOLD,8,"DESCRIPTION / CATEGORY");write(c,485,662,PDType1Font.HELVETICA_BOLD,8,"AMOUNT");
        }
        return 640;
    }
    private void drawRow(PDPageContentStream c,float y,ReportRow row) throws IOException {
        c.setStrokingColor(229,233,225);c.moveTo(42,y-15);c.lineTo(570,y-15);c.stroke();c.setNonStrokingColor(35,39,34);
        write(c,48,y,PDType1Font.HELVETICA,8,row.date().format(DATE));write(c,116,y,PDType1Font.HELVETICA,8,row.time());
        write(c,157,y,PDType1Font.HELVETICA_BOLD,8,row.type());write(c,207,y,PDType1Font.HELVETICA,8,clip(row.description(),32));
        write(c,207,y-10,PDType1Font.HELVETICA_OBLIQUE,7,clip(row.category(),38));write(c,485,y,PDType1Font.HELVETICA_BOLD,8,"INR "+row.amount().toPlainString());
    }
    private String clip(String value,int length){String ascii=value.replaceAll("[^\\x20-\\x7E]","?");return ascii.length()>length?ascii.substring(0,length-1)+"…":ascii;}
    private void write(PDPageContentStream c,float x,float y,org.apache.pdfbox.pdmodel.font.PDFont font,float size,String value) throws IOException {
        c.beginText();c.setFont(font,size);c.newLineAtOffset(x,y);c.showText(value.replaceAll("[^\\x20-\\x7E]","?"));c.endText();
    }
    private record ReportRow(LocalDate date,String time,String type,String description,String category,BigDecimal amount) {}
}
