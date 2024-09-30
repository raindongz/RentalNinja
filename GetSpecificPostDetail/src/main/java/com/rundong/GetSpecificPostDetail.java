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

/**
 * Hello world!
 *
 */
public class GetSpecificPostDetail implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>
{
    private static final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    private static final DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(client);
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    @Override
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent event, final Context context) {
        // initialize logger
        LambdaLogger logger = context.getLogger();
        logger.log("Function" + context.getFunctionName() + "  is called", LogLevel.INFO);
        Map<String, Object> responseBody = new HashMap<>();

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

        Request request = gson.fromJson(event.getBody(), Request.class);
        logger.log("request body: " + request, LogLevel.INFO);
        String postId = request.postId();
        logger.log("this is post ID: " + postId, LogLevel.INFO);

        if (postId == null || postId.isEmpty()){
            logger.log("post id not present", LogLevel.ERROR);
            return returnApiResponse(400, null, "post id not present", "400", logger);
        }

        // get specific post from dynamodb
        Post post;
        try {
             post = dynamoDBMapper.load(Post.class, postId);
        }catch (Exception e){
            logger.log("load post with postId error "+e, LogLevel.ERROR);
            responseBody.put("errorMsg", "db error");
            return returnApiResponse(500, responseBody, "db error, please try again", "500", logger);
        }

        try {
            Collection myFavorite = dynamoDBMapper.load(Collection.class, username, postId);
            if (myFavorite != null){
                post.setIsFavorite(1);
            }
        }catch (Exception e){
            logger.log("load collection with postId error "+e, LogLevel.ERROR);
            return returnApiResponse(500, null, "get my favorite failed", "500", logger);
        }

        //prepare response body
        responseBody.put("post", post);
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
