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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class UploadHandler implements RequestHandler<S3Event, String> {

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
            paramRequest.setName("s3.image-out");
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
        // TODO do something with image
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
            ImageIO.write(bufferedImage, "jpg", stream);
            var bytes = stream.toByteArray();
            metadata.setContentLength(bytes.length);
            return new ByteArrayInputStream(bytes);
        }
    }

}