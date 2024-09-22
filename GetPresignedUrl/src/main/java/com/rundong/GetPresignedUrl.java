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
import com.amazonaws.util.StringUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Rundong Zhong
 *
 */
public class GetPresignedUrl implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    @Override
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent event, final Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Function" + context.getFunctionName() + "is called", LogLevel.INFO);
        Map<String, String> responseBody = new HashMap<>();

        // Get user ID from request context
        Object claims = event.getRequestContext().getAuthorizer().get("claims");
        try {
            if (!(claims instanceof Map)) {
                throw new RuntimeException("request authorization header claims not type of map");
            }
        }catch (RuntimeException e){
            logger.log("convert claims in auth header to map failed", LogLevel.ERROR);
            responseBody.put("errorMsg", "auth header not valid.");
            return returnApiResponse(400, responseBody, "auth header not valid.", "400", logger);
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


        //prepare response body
        responseBody.put("presignedUrl", preSignedUrl);
        responseBody.put("objectKey", objectKey);

        // Convert the response to JSON
//        String json = gson.toJson(responseBody);

        // String responseBody = "{\"presignedUrl\": \"" + preSignedUrl + "\", \"objectKey\": \"" + objectKey + "\"}";
        return returnApiResponse(200, responseBody, null, null, logger);
    }

    public APIGatewayProxyResponseEvent returnApiResponse(int statusCode, Map<String, String> responseBody,
                                                          String errorMessage, String errorCode, LambdaLogger logger){
        final Error error = new Error();
        if(!StringUtils.isNullOrEmpty(errorCode)){
            error.setErrorCode(errorCode);
            error.setErrorMessage(errorMessage);
        }

        // Prepare response header
        Map<String, String> responseHeaders = new HashMap<>();
        responseHeaders.put("Content-Type", "application/json");
        responseHeaders.put("Access-Control-Allow-Origin", "*");

        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent()
                .withHeaders(responseHeaders)
                .withStatusCode(statusCode)
                .withBody(gson.toJson(new Response<Map<String, String>>(statusCode, responseBody, error)));
        logger.log("\n" + responseEvent.toString(), LogLevel.INFO);

        return responseEvent;

    }
}
