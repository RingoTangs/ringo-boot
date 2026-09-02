package io.github.ringotangs.ringoboot.verification.channel.image;

import io.github.ringotangs.ringoboot.verification.context.IssueContext;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Objects;
import javax.imageio.ImageIO;

/**
 * 使用 Java2D 生成带基础字符扰动和噪声的 PNG 验证码。
 *
 * <p>该实现只提供通用基线，不替代专业反机器人服务。面向用户的应用还应提供无障碍替代验证方式。
 */
public final class Java2dImageCaptchaRenderer implements ImageCaptchaRenderer {

    private static final String MEDIA_TYPE = "image/png";
    private static final int WIDTH = 160;
    private static final int HEIGHT = 60;
    private static final int PADDING = 12;
    private static final int MAX_CODE_LENGTH = 8;
    private static final int NOISE_LINES = 6;
    private static final int NOISE_DOTS = 80;

    private final SecureRandom random;

    /**
     * 使用新的密码学安全随机源创建 Renderer。
     */
    public Java2dImageCaptchaRenderer() {
        this(new SecureRandom());
    }

    /**
     * 使用指定的密码学安全随机源创建 Renderer。
     */
    public Java2dImageCaptchaRenderer(SecureRandom random) {
        this.random = Objects.requireNonNull(random, "random must not be null");
    }

    @Override
    public CaptchaImage render(IssueContext context, String code) throws CaptchaRenderingException {
        Objects.requireNonNull(context, "context must not be null");
        Objects.requireNonNull(code, "code must not be null");
        if (code.isBlank() || code.length() > MAX_CODE_LENGTH) {
            throw new IllegalArgumentException("code must be non-blank and contain at most 8 characters");
        }

        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setColor(new Color(245, 247, 250));
            graphics.fillRect(0, 0, WIDTH, HEIGHT);
            drawNoise(graphics);
            drawCode(graphics, code);
        } finally {
            graphics.dispose();
        }

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            if (!ImageIO.write(image, "png", output)) {
                throw new CaptchaRenderingException("PNG image writer is not available");
            }
            return new CaptchaImage(MEDIA_TYPE, output.toByteArray());
        } catch (IOException exception) {
            throw new CaptchaRenderingException("Failed to encode image captcha", exception);
        }
    }

    private void drawNoise(Graphics2D graphics) {
        graphics.setColor(new Color(170, 180, 190));
        for (int index = 0; index < NOISE_LINES; index++) {
            graphics.drawLine(
                    random.nextInt(WIDTH), random.nextInt(HEIGHT), random.nextInt(WIDTH), random.nextInt(HEIGHT));
        }
        for (int index = 0; index < NOISE_DOTS; index++) {
            graphics.fillRect(random.nextInt(WIDTH), random.nextInt(HEIGHT), 1, 1);
        }
    }

    private void drawCode(Graphics2D graphics, String code) {
        int cellWidth = (WIDTH - PADDING * 2) / code.length();
        int fontSize = Math.min(40, Math.max(24, cellWidth + 8));
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, fontSize);
        for (int index = 0; index < code.length(); index++) {
            Graphics2D character = (Graphics2D) graphics.create();
            try {
                int x = PADDING + index * cellWidth + Math.max(0, (cellWidth - fontSize / 2) / 2);
                int y = 42 + random.nextInt(9) - 4;
                character.setFont(font);
                character.setColor(new Color(random.nextInt(100), random.nextInt(100), random.nextInt(100)));
                character.rotate((random.nextDouble() - 0.5) * 0.5, x + cellWidth / 2.0, HEIGHT / 2.0);
                character.drawString(String.valueOf(code.charAt(index)), x, y);
            } finally {
                character.dispose();
            }
        }
    }
}
