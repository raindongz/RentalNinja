package com.rundong;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
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
import java.util.Objects;

/**
 * Hello world!
 *
 */
public class UpdateMyPostInfo implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>
{

    private static final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    private static final DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(client);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    @Override
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent event, final Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Function" + context.getFunctionName() + "  is called", LogLevel.INFO);
        Map<String, Object> responseBody = new HashMap<>();

        // 1. check if post belongs to that user
        RequestBody request = gson.fromJson(event.getBody(), RequestBody.class);

        // 1.1. Get user ID from request context
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

        // 1.2. get specific post
        Post post;
        try {
            post = dynamoDBMapper.load(Post.class, request.postId());
        }catch (Exception e){
            logger.log("load post with postId error "+e, LogLevel.ERROR);
            return returnApiResponse(500, null, "db error, please try again", "500", logger);
        }

        // 1.3 compare userId
        if (!Objects.equals(post.getUserId(), username)){
            logger.log("\n" + "not authorized request. userId: "+ username, LogLevel.INFO);
            return returnApiResponse(403, null, "not authorized", "403", logger);
        }

        // 2. do update post
        post.setTitle(request.title());
        post.setContent(request.content());
        post.setContactInfo(request.contactInfo());
        post.setPicUrls(request.picUrls());
        post.setCountry(request.country());
        post.setState(request.state());
        post.setCity(request.city());
        post.setArea(request.area());
        post.setUpdateTime(new Date());
        try{
            dynamoDBMapper.save(post);
        }catch (Exception e){
            logger.log("\n"+ "save delete post failed. "+"\n"+ e, LogLevel.ERROR);
            return returnApiResponse(500, null, "db error", "500", logger);
        }
        responseBody.put("msg", "success");
        return returnApiResponse(200, responseBody, null, null, logger);
    }


    public APIGatewayProxyResponseEvent returnApiResponse(int statusCode, Map<String, Object> responseBody,
                                                          String errorMessage, String errorCode, LambdaLogger logger){
        final Error error = new Error();
        if(!StringUtils.isNullOrEmpty(errorCode)){
            error.setErrorCode(errorCode);
            error.setErrorMessage(errorMessage);
        }

        // set up response header
        Map<String, String> responseHeaders = new HashMap<>();
        responseHeaders.put("Content-Type", "application/json");
        responseHeaders.put("Access-Control-Allow-Origin", "*");

        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent()
                .withHeaders(responseHeaders)
                .withStatusCode(statusCode)
                .withBody(gson.toJson(new Response<Map<String, Object>>(statusCode, responseBody, error)));
        logger.log("\n" + responseEvent.toString(), LogLevel.INFO);

        return responseEvent;
    }
}
