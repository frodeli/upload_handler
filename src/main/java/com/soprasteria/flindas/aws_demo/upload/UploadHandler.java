package com.soprasteria.flindas.aws_demo.upload;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class UploadHandler implements RequestHandler<S3Event, String> {

    private static final String TEXT = "Rubiks 2019";
    private static final String TARGET_BUCKET = "rubiks-ok";

    private AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();

    public String handleRequest(S3Event s3Event, Context context) {
        long start = System.currentTimeMillis();
        LambdaLogger logger = context.getLogger();
        S3EventNotification.S3EventNotificationRecord record = s3Event.getRecords().get(0);
        String sourceBucket = record.getS3().getBucket().getName();
        String sourceKey = record.getS3().getObject().getKey();
        S3Object s3Object = s3.getObject(sourceBucket, sourceKey);
        ObjectMetadata metadata = new ObjectMetadata();
        try (ByteArrayInputStream inputStream = update(s3Object.getObjectContent(), metadata)) {
            s3.putObject(TARGET_BUCKET, sourceKey, inputStream, metadata);
        } catch (IOException ioException) {
            logger.log("Failed to write image.");
            throw new RuntimeException(ioException);
        }
        logger.log("Time spent: " + (System.currentTimeMillis() - start) + " ms");
        return null;
    }

    private static ByteArrayInputStream update(InputStream inputStream, ObjectMetadata metadata) throws IOException {
        BufferedImage bufferedImage = ImageIO.read(inputStream);
        if (bufferedImage == null) {
            throw new IllegalArgumentException("Failed to read image.");
        }
        addText(bufferedImage);
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            ImageIO.write(bufferedImage, "jpg", stream);
            byte[] bytes = stream.toByteArray();
            metadata.setContentLength(bytes.length);
            return new ByteArrayInputStream(bytes);
        }
    }

    private static void addText(BufferedImage bufferedImage) {
        Graphics g = bufferedImage.getGraphics();
        Font currentFont = g.getFont();
        Font newFont = currentFont.deriveFont(currentFont.getSize() * 2.0F);
        g.setFont(newFont);
        int x = bufferedImage.getWidth() - g.getFontMetrics().stringWidth(TEXT);
        int y = bufferedImage.getHeight() - g.getFontMetrics().getAscent();
        g.setColor(Color.BLACK);
        g.fillRect(x,y,bufferedImage.getWidth(), bufferedImage.getHeight());
        g.setColor(Color.WHITE);
        g.drawString("Rubiks 2019", x, bufferedImage.getHeight());
        g.dispose();
    }


}