package org.lintx.plugins.yinwuchat.common.auth;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CaptchaManager {
    private static final int WIDTH = 120;
    private static final int HEIGHT = 40;
    private static final long TTL_MS = 5 * 60 * 1000L;
    private static final String CODE_POOL = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final Map<String, CaptchaEntry> entries = new HashMap<>();

    public synchronized CaptchaResponse createCaptcha() {
        cleanupExpired();
        String code = randomCode(5);
        String id = UUID.randomUUID().toString();
        String imageBase64 = renderImageBase64(code);
        CaptchaEntry entry = new CaptchaEntry(code, System.currentTimeMillis());
        entries.put(id, entry);
        return new CaptchaResponse(id, imageBase64);
    }

    public synchronized boolean validate(String id, String text) {
        cleanupExpired();
        if (id == null || text == null) {
            return false;
        }
        CaptchaEntry entry = entries.remove(id);
        if (entry == null) {
            return false;
        }
        return entry.code.equalsIgnoreCase(text.trim());
    }

    private void cleanupExpired() {
        long now = System.currentTimeMillis();
        entries.entrySet().removeIf(e -> now - e.getValue().createdAt > TTL_MS);
    }

    private String randomCode(int len) {
        StringBuilder sb = new StringBuilder(len);
        byte[] rand = CryptoUtil.randomBytes(len);
        for (int i = 0; i < len; i++) {
            int idx = (rand[i] & 0xFF) % CODE_POOL.length();
            sb.append(CODE_POOL.charAt(idx));
        }
        return sb.toString();
    }

    private String renderImageBase64(String code) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(240, 243, 246));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        for (int i = 0; i < 6; i++) {
            g.setColor(new Color(180 + i * 10, 180, 200));
            int x1 = (i * 20) % WIDTH;
            int y1 = (i * 7) % HEIGHT;
            int x2 = WIDTH - x1;
            int y2 = HEIGHT - y1;
            g.drawLine(x1, y1, x2, y2);
        }
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.setColor(new Color(30, 30, 30));
        g.drawString(code, 18, 28);
        g.dispose();
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, "png", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }

    public static class CaptchaResponse {
        public final String id;
        public final String imageBase64;

        public CaptchaResponse(String id, String imageBase64) {
            this.id = id;
            this.imageBase64 = imageBase64;
        }
    }

    private static class CaptchaEntry {
        private final String code;
        private final long createdAt;

        private CaptchaEntry(String code, long createdAt) {
            this.code = code;
            this.createdAt = createdAt;
        }
    }
}
