package com.soprasteria.flindas.aws_demo.upload;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.GetParameterRequest;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class UploadHandler implements RequestHandler<S3Event, String> {

    private static final String TEXT = "AWS Workshop";

    private AmazonS3 s3 = AmazonS3ClientBuilder.defaultClient();
    private AWSSimpleSystemsManagement systemsManagement= AWSSimpleSystemsManagementClientBuilder.defaultClient();

    public String handleRequest(S3Event s3Event, Context context) {
        var logger = context.getLogger();
        var record = s3Event.getRecords().get(0);
        var sourceBucket = record.getS3().getBucket().getName();
        var sourceKey = record.getS3().getObject().getKey();

        var s3Object = s3.getObject(sourceBucket, sourceKey);

        var metadata = new ObjectMetadata();
        try (var inputStream = update(s3Object.getObjectContent(), metadata)) {
            var paramRequest = new GetParameterRequest();
            paramRequest.setName("s3.image-in");
            var result = systemsManagement.getParameter(paramRequest);
            var targetBucket = result.getParameter().getValue();
            s3.putObject(targetBucket, sourceKey, inputStream, metadata);

        } catch (IOException ioException) {
            logger.log("Failed to write image.");
            throw new RuntimeException(ioException);
        }
        return null;
    }

    private static ByteArrayInputStream update(InputStream inputStream, ObjectMetadata metadata) throws IOException {
        var bufferedImage = ImageIO.read(inputStream);
        if (bufferedImage == null) {
            throw new IllegalArgumentException("Failed to read image.");
        }
        addText(bufferedImage);
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            ImageIO.write(bufferedImage, "jpg", stream);
            var bytes = stream.toByteArray();
            metadata.setContentLength(bytes.length);
            return new ByteArrayInputStream(bytes);
        }
    }

    private static void addText(BufferedImage bufferedImage) {
        var g = bufferedImage.getGraphics();
        var currentFont = g.getFont();
        var newFont = currentFont.deriveFont(currentFont.getSize() * 2.0F);
        g.setFont(newFont);
        var x = bufferedImage.getWidth() - g.getFontMetrics().stringWidth(TEXT);
        var y = bufferedImage.getHeight() - g.getFontMetrics().getAscent();
        g.setColor(Color.BLACK);
        g.fillRect(x,y,bufferedImage.getWidth(), bufferedImage.getHeight());
        g.setColor(Color.WHITE);
        g.drawString(TEXT, x, bufferedImage.getHeight());
        g.dispose();
    }

}