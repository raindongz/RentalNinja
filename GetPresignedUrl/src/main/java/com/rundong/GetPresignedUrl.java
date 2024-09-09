package com.rundong;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.google.gson.Gson;


import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Rundong Zhong
 *
 */
public class GetPresignedUrl implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent event, final Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Function" + context.getFunctionName() + "is called", LogLevel.INFO);

        // Get user ID from request context
        Object claims = event.getRequestContext().getAuthorizer().get("claims");
        if (!(claims instanceof Map)) {
            //todo :throw new error
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(500)
                    .withBody("internal server error!");
        }
        Map<String, Object> claimsMap = (Map<String, Object>) claims;
        String username = (String)claimsMap.get("cognito:username");

        // Log or use the user ID as needed
        logger.log("Authenticated User ID: " + username, LogLevel.INFO);

        // generate pre-signed url
        logger.log("Generating pre-signed URL...");
        String bucketName = "rentalninja";
        String objectKey = System.currentTimeMillis() + ".png";

        // Set expiration for the pre-signed URL (e.g., 15 minutes)
        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000 * 60 * 15;  // 15 minutes
        expiration.setTime(expTimeMillis);

        // Generate the pre-signed URL
        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucketName, objectKey)
                        .withMethod(HttpMethod.PUT)
                        .withExpiration(expiration);

        String preSignedUrl = s3Client.generatePresignedUrl(generatePresignedUrlRequest).toString();
        logger.log("Pre-signed URL generated: " + preSignedUrl);

        // Prepare response header
        Map<String, String> responseHeaders = new HashMap<>();
        responseHeaders.put("Content-Type", "application/json");

        //prepare response body
        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("presignedUrl", preSignedUrl);
        responseBody.put("objectKey", objectKey);

        // Convert the response to JSON
        Gson gson = new Gson();
        String json = gson.toJson(responseBody);

        // String responseBody = "{\"presignedUrl\": \"" + preSignedUrl + "\", \"objectKey\": \"" + objectKey + "\"}";

        return new APIGatewayProxyResponseEvent()
                .withHeaders(responseHeaders)
                .withStatusCode(200)
                .withBody(json);
    }
}
