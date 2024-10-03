package com.rundong;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import com.amazonaws.util.StringUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Hello world!
 *
 */
public class UploadPost implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {


    private static final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();

//    private static final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
//            .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration("http://172.21.0.2:8000", "us-east-2"))
//            .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials("1234567", "asdfghjkl")))
//            .build();

    private static final DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(client);

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    @Override
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent event, final Context context) {
        // initialize logger
        LambdaLogger logger = context.getLogger();
        logger.log("Function" + context.getFunctionName() + "  is called", LogLevel.INFO);

        // Get user ID from request context
        Object claims = event.getRequestContext().getAuthorizer().get("claims");
        try {
            if (!(claims instanceof Map)) {
                throw new RuntimeException("request authorization header claims not type of map");
            }
        }catch (RuntimeException e){
            logger.log("convert claims in auth header to map failed", LogLevel.ERROR);
            return returnApiResponse(400, null, "auth header not valid.", "400", logger);
        }
        Map<String, Object> claimsMap = (Map<String, Object>) claims;
        String username = (String)claimsMap.get("cognito:username");

        Post post = gson.fromJson(event.getBody(), Post.class);
        if (post.getTitle().isEmpty()){
            return returnApiResponse(400, null, "invalid request", "400", logger);
        }else if (post.getContent().isEmpty()){
            return returnApiResponse(400, null, "invalid request", "400", logger);
        }else if (post.getContactInfo().isEmpty()){
            return returnApiResponse(400, null, "invalid request", "400", logger);
        }else if (username.isEmpty()){
            return returnApiResponse(400, null, "invalid request", "400", logger);
        }
        post.setUserId(username);
        post.setCreateTime(new Date());
        post.setUpdateTime(new Date());
        post.setDeleteFlag(DeleteStates.ACTIVE);

        try {
            dynamoDBMapper.save(post);
        }catch (Exception e){
            logger.log("save post to dynamodb error:  " + e, LogLevel.ERROR);
            return returnApiResponse(500, "db error", "db error", "500", logger);
        }
        String json = gson.toJson("success!");

        return returnApiResponse(200, json, null, null, logger);
    }
    public APIGatewayProxyResponseEvent returnApiResponse(int statusCode, String responseBody,
                                                          String errorMessage, String errorCode, LambdaLogger logger){
        final Error error = new Error();
        if(!StringUtils.isNullOrEmpty(errorCode)){
            error.setErrorCode(errorCode);
            error.setErrorMessage(errorMessage);
        }

        // Prepare response header
        Map<String, String> responseHeaders = new HashMap<>();
        responseHeaders.put("Content-Type", "application/json");
        responseHeaders.put("X-Custom-Header", "application/json");
        responseHeaders.put("Access-Control-Allow-Origin", "*");

        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent()
                .withHeaders(responseHeaders)
                .withStatusCode(statusCode)
                .withBody(gson.toJson(new Response<String>(statusCode, responseBody, error)));
        logger.log("\n" + responseEvent.toString(), LogLevel.INFO);

        return responseEvent;

    }

}
