package com.teachquest.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.teachquest.model.CourseCertificate;
import com.teachquest.model.PracticeCourse;
import com.teachquest.model.PracticeQuizAttempt;
import com.teachquest.model.QuizLevelProgress;
import com.teachquest.model.User;
import com.teachquest.repository.CourseCertificateRepository;
import com.teachquest.repository.PracticeCourseRepository;
import com.teachquest.repository.PracticeQuizAttemptRepository;
import com.teachquest.repository.QuizLevelProgressRepository;
import com.teachquest.repository.UserRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class CourseCertificateService {
    private static final List<Integer> REQUIRED_LEVELS = List.of(1, 2, 3, 4);
    private static final DateTimeFormatter ISSUE_DATE = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.US);

    private final CourseCertificateRepository certificateRepository;
    private final PracticeQuizAttemptRepository attemptRepository;
    private final QuizLevelProgressRepository progressRepository;
    private final PracticeCourseRepository courseRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final boolean expertEnabled;
    private final String publicBaseUrl;

    public CourseCertificateService(CourseCertificateRepository certificateRepository,
                                    PracticeQuizAttemptRepository attemptRepository,
                                    QuizLevelProgressRepository progressRepository,
                                    PracticeCourseRepository courseRepository,
                                    UserRepository userRepository,
                                    ObjectMapper objectMapper,
                                    ApplicationEventPublisher eventPublisher,
                                    @Value("${teachquest.practice.expert-enabled:false}") boolean expertEnabled,
                                    @Value("${teachquest.certificates.public-base-url:http://localhost:5173}") String publicBaseUrl) {
        this.certificateRepository = certificateRepository;
        this.attemptRepository = attemptRepository;
        this.progressRepository = progressRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
        this.expertEnabled = expertEnabled;
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    @Transactional
    public Optional<Map<String, Object>> issueIfImproved(Long userId, Long courseId) {
        QuizLevelProgress progress = progressRepository.findByUserIdAndCourseIdForUpdate(userId, courseId)
                .orElseThrow(() -> new IllegalStateException("Quiz progress was not initialized."));
        if (progress.getHighestUnlockedLevel() < 4) return Optional.empty();

        Map<Integer, Double> scores = bestPassingScores(userId, courseId);
        if (!scores.keySet().containsAll(REQUIRED_LEVELS)) return Optional.empty();
        double average = scores.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);
        boolean passedExpert = expertEnabled && scores.containsKey(5);
        String calculatedTier = passedExpert || average >= 95 ? "GOLD"
                : average >= 85 ? "SILVER" : average >= 70 ? "BRONZE" : null;
        if (calculatedTier == null) return Optional.empty();

        CourseCertificate current = certificateRepository
                .findFirstByUserIdAndCourseIdAndStatusOrderByIssuedAtDesc(userId, courseId, "CURRENT")
                .orElse(null);
        boolean expertNewlyPassed = current != null && expertEnabled && scores.containsKey(5)
            && !readScores(current.getScoreBreakdownJson()).containsKey(5);
        if (current != null && tierRank(calculatedTier) < tierRank(current.getTier())) {
            calculatedTier = current.getTier();
        }
        if (current != null && tierRank(calculatedTier) == tierRank(current.getTier())
            && average <= current.getAverageScore() + 0.005 && !expertNewlyPassed) {
            return Optional.empty();
        }

        if (current != null) {
            current.setStatus("SUPERSEDED");
            certificateRepository.save(current);
        }
        CourseCertificate certificate = new CourseCertificate();
        certificate.setCertificateId(UUID.randomUUID().toString());
        certificate.setUserId(userId);
        certificate.setCourseId(courseId);
        certificate.setTier(calculatedTier);
        certificate.setAverageScore(average);
        certificate.setScoreBreakdownJson(writeScores(scores));
        certificate.setIssuedAt(LocalDateTime.now());
        certificate.setStatus("CURRENT");
        certificateRepository.save(certificate);
        eventPublisher.publishEvent(new CertificateIssuedEvent(userId, courseId, calculatedTier,
            current == null ? null : current.getTier()));
        return Optional.of(certificateView(certificate));
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> certificatesForUser(Long userId) {
        if (userRepository.findById(userId).isEmpty()) throw new IllegalArgumentException("User not found.");
        return certificateRepository.findByUserIdOrderByIssuedAtDesc(userId).stream()
                .map(this::certificateView).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> verify(String certificateId) {
        CourseCertificate certificate = certificateRepository.findByCertificateId(certificateId)
                .orElseThrow(() -> new IllegalArgumentException("Not found"));
        User user = userRepository.findById(certificate.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Not found"));
        PracticeCourse course = courseRepository.findById(certificate.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Not found"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("valid", true);
        result.put("name", user.getUsername());
        result.put("course", course.getName());
        result.put("tier", titleCase(certificate.getTier()));
        result.put("issuedAt", certificate.getIssuedAt());
        result.put("superseded", "SUPERSEDED".equals(certificate.getStatus()));
        return result;
    }

    @Transactional(readOnly = true)
    public byte[] pdf(String certificateId, Long userId) {
        CourseCertificate certificate = ownedCertificate(certificateId, userId);
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found."));
        PracticeCourse course = courseRepository.findById(certificate.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found."));
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.setStrokingColor(new Color(20, 107, 88));
                content.setLineWidth(4);
                content.addRect(28, 28, page.getMediaBox().getWidth() - 56, page.getMediaBox().getHeight() - 56);
                content.stroke();
                drawPdfText(content, PDType1Font.HELVETICA_BOLD, 14, 65, 765, "TEACHQUEST  /  COURSE CERTIFICATE", new Color(20, 107, 88));
                drawPdfText(content, PDType1Font.HELVETICA_BOLD, 31, 65, 700, "Certificate of Achievement", new Color(23, 60, 61));
                drawPdfText(content, PDType1Font.HELVETICA, 14, 65, 653, "This certificate is presented to", new Color(101, 119, 121));
                drawPdfText(content, PDType1Font.HELVETICA_BOLD, 25, 65, 615,
                    fitPdfText(user.getUsername(), PDType1Font.HELVETICA_BOLD, 25, 465), new Color(23, 60, 61));
                drawPdfText(content, PDType1Font.HELVETICA, 14, 65, 579, "for completing", new Color(101, 119, 121));
                drawPdfText(content, PDType1Font.HELVETICA_BOLD, 20, 65, 548,
                    fitPdfText(course.getName(), PDType1Font.HELVETICA_BOLD, 20, 465), new Color(23, 60, 61));
                drawPdfText(content, PDType1Font.HELVETICA_BOLD, 16, 65, 505, titleCase(certificate.getTier()) + " Tier", tierColor(certificate.getTier()));
                drawPdfText(content, PDType1Font.HELVETICA, 13, 65, 470,
                        String.format(Locale.US, "Average score: %.2f%%", certificate.getAverageScore()), new Color(101, 119, 121));

                drawPdfText(content, PDType1Font.HELVETICA_BOLD, 13, 65, 420, "LEVEL SCORE BREAKDOWN", new Color(23, 60, 61));
                Map<Integer, Double> scores = readScores(certificate.getScoreBreakdownJson());
                int y = 393;
                for (Map.Entry<Integer, Double> entry : scores.entrySet()) {
                    drawPdfText(content, PDType1Font.HELVETICA, 12, 65, y, "Level " + entry.getKey(), new Color(101, 119, 121));
                    drawPdfText(content, PDType1Font.HELVETICA_BOLD, 12, 220, y,
                            String.format(Locale.US, "%.2f%%", entry.getValue()), new Color(23, 60, 61));
                    y -= 23;
                }
                drawPdfText(content, PDType1Font.HELVETICA, 11, 65, 125, "Issued " + ISSUE_DATE.format(certificate.getIssuedAt()), new Color(101, 119, 121));
                drawPdfText(content, PDType1Font.HELVETICA, 9, 65, 105, "Verification ID: " + certificate.getCertificateId(), new Color(101, 119, 121));

                byte[] qrBytes = qrCode(verificationUrl(certificate.getCertificateId()), 150);
                PDImageXObject qr = PDImageXObject.createFromByteArray(document, qrBytes, "certificate-verification-qr");
                content.drawImage(qr, 385, 78, 130, 130);
                drawPdfText(content, PDType1Font.HELVETICA, 9, 383, 65, "Scan to verify", new Color(101, 119, 121));
            }
            document.save(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate the certificate PDF.", exception);
        }
    }

    @Transactional(readOnly = true)
    public byte[] socialCard(String certificateId, Long userId) {
        CourseCertificate certificate = ownedCertificate(certificateId, userId);
        User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found."));
        PracticeCourse course = courseRepository.findById(certificate.getCourseId())
                .orElseThrow(() -> new IllegalArgumentException("Course not found."));
        BufferedImage image = new BufferedImage(1200, 630, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(23, 60, 61));
        graphics.fillRect(0, 0, 1200, 630);
        graphics.setColor(tierColor(certificate.getTier()));
        graphics.fillRect(0, 0, 18, 630);
        graphics.setColor(new Color(248, 250, 247));
        graphics.setFont(new Font("SansSerif", Font.BOLD, 20));
        graphics.drawString("TEACHQUEST  /  COURSE CERTIFICATE", 76, 82);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 50));
        graphics.drawString(titleCase(certificate.getTier()) + " Achievement", 76, 190);
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 30));
        graphics.drawString("Awarded to " + fitText(graphics, user.getUsername(), 780), 76, 270);
        graphics.setFont(new Font("SansSerif", Font.BOLD, 27));
        graphics.drawString("For " + fitText(graphics, course.getName(), 780), 76, 332);
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 21));
        graphics.drawString(String.format(Locale.US, "Average score  %.2f%%", certificate.getAverageScore()), 76, 398);
        graphics.drawString("Issued " + ISSUE_DATE.format(certificate.getIssuedAt()), 76, 444);
        graphics.setColor(new Color(159, 225, 199));
        graphics.setFont(new Font("SansSerif", Font.PLAIN, 14));
        graphics.drawString("Verification ID  " + certificate.getCertificateId(), 76, 570);
        try {
            BufferedImage qr = ImageIO.read(new java.io.ByteArrayInputStream(qrCode(verificationUrl(certificate.getCertificateId()), 180)));
            graphics.setColor(Color.WHITE);
            graphics.fillRect(936, 355, 200, 200);
            graphics.drawImage(qr, 946, 365, 180, 180, null);
            graphics.setColor(new Color(248, 250, 247));
            graphics.setFont(new Font("SansSerif", Font.PLAIN, 15));
            graphics.drawString("Scan to verify", 983, 580);
        } catch (Exception exception) {
            graphics.dispose();
            throw new IllegalStateException("Unable to generate the certificate social card.", exception);
        }
        graphics.dispose();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "PNG", output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to generate the certificate social card.", exception);
        }
    }

    private Map<Integer, Double> bestPassingScores(Long userId, Long courseId) {
        Map<Integer, Double> bestScores = new LinkedHashMap<>();
        List<PracticeQuizAttempt> attempts = attemptRepository.findByUserIdAndCourseId(userId, courseId);
        for (int level : expertEnabled ? List.of(1, 2, 3, 4, 5) : REQUIRED_LEVELS) {
            attempts.stream().filter(attempt -> attempt.getLevel() == level && Boolean.TRUE.equals(attempt.getPassed())
                            && attempt.getScorePercent() != null)
                    .mapToDouble(PracticeQuizAttempt::getScorePercent)
                    .max().ifPresent(score -> bestScores.put(level, score));
        }
        return bestScores;
    }

    private CourseCertificate ownedCertificate(String certificateId, Long userId) {
        CourseCertificate certificate = certificateRepository.findByCertificateId(certificateId)
                .orElseThrow(() -> new IllegalArgumentException("Certificate not found."));
        if (!certificate.getUserId().equals(userId)) throw new IllegalArgumentException("Certificate not found.");
        return certificate;
    }

    private Map<String, Object> certificateView(CourseCertificate certificate) {
        PracticeCourse course = courseRepository.findById(certificate.getCourseId())
                .orElseThrow(() -> new IllegalStateException("Certificate course is no longer available."));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("certificateId", certificate.getCertificateId());
        result.put("courseName", course.getName());
        result.put("tier", titleCase(certificate.getTier()));
        result.put("averageScore", certificate.getAverageScore());
        result.put("scoreBreakdown", readScores(certificate.getScoreBreakdownJson()));
        result.put("issuedAt", certificate.getIssuedAt());
        result.put("status", certificate.getStatus());
        result.put("verificationUrl", verificationUrl(certificate.getCertificateId()));
        return result;
    }

    private String writeScores(Map<Integer, Double> scores) {
        try { return objectMapper.writeValueAsString(scores); }
        catch (Exception exception) { throw new IllegalStateException("Unable to store certificate scores.", exception); }
    }

    private Map<Integer, Double> readScores(String json) {
        try { return objectMapper.readValue(json, new TypeReference<>() { }); }
        catch (Exception exception) { throw new IllegalStateException("Certificate scores could not be read.", exception); }
    }

    private byte[] qrCode(String value, int size) throws Exception {
        var matrix = new QRCodeWriter().encode(value, BarcodeFormat.QR_CODE, size, size);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        }
    }

    private String verificationUrl(String certificateId) { return publicBaseUrl + "/verify/" + certificateId; }

    private void drawPdfText(PDPageContentStream content, PDType1Font font, int size, float x, float y,
                             String text, Color color) throws Exception {
        content.beginText();
        content.setFont(font, size);
        content.setNonStrokingColor(color);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();
    }

    private String safePdf(String value) {
        StringBuilder result = new StringBuilder();
        for (char character : value.toCharArray()) result.append(character >= 32 && character <= 255 ? character : '?');
        return result.toString();
    }

    private String fitPdfText(String value, PDType1Font font, int fontSize, float maxWidth) throws Exception {
        String fitted = safePdf(value);
        while (!fitted.isEmpty() && font.getStringWidth(fitted) * fontSize / 1000 > maxWidth) {
            fitted = fitted.substring(0, fitted.length() - 1);
        }
        return fitted.equals(safePdf(value)) ? fitted : fitted + "...";
    }

    private int tierRank(String tier) {
        return switch (tier) { case "BRONZE" -> 1; case "SILVER" -> 2; case "GOLD" -> 3; default -> 0; };
    }

    private String titleCase(String tier) { return tier.charAt(0) + tier.substring(1).toLowerCase(Locale.ROOT); }

    private Color tierColor(String tier) {
        return switch (tier) {
            case "BRONZE" -> new Color(185, 111, 63);
            case "SILVER" -> new Color(157, 173, 177);
            case "GOLD" -> new Color(221, 174, 62);
            default -> new Color(20, 107, 88);
        };
    }

    private String fitText(Graphics2D graphics, String text, int maxWidth) {
        String result = text;
        while (!result.isEmpty() && graphics.getFontMetrics().stringWidth(result) > maxWidth) {
            result = result.substring(0, result.length() - 1);
        }
        return result.equals(text) ? result : result + "...";
    }
}