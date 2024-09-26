package com.rundong;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import com.amazonaws.util.StringUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.*;

/**
 * Hello world!
 *
 */
public class GetCollectionList implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

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

        Collection collectionKey = new Collection();
        collectionKey.setUserId(username);

        DynamoDBQueryExpression<Collection> queryExpression = new DynamoDBQueryExpression<Collection>()
                .withHashKeyValues(collectionKey);
        List<String> resultList = dynamoDBMapper.query(Collection.class, queryExpression).stream().map(Collection::getPostId).toList();
        List<Post> postKeys = new ArrayList<>();
        for (String postId : resultList) {
            Post postKey = new Post();
            postKey.setPostId(postId);
            postKeys.add(postKey);
        }

        Map<String, List<Object>> batchLoadResults;
        try{
            batchLoadResults = dynamoDBMapper.batchLoad(postKeys);
        }catch (Exception e){
            logger.log("batch get list error", LogLevel.ERROR);
            return returnApiResponse(400, null, "get list error", "400", logger);
        }
        List<Post> posts = new ArrayList<>();
        List<Object> postObjects = batchLoadResults.get("posts"); // Table name

        if (postObjects != null) {
            for (Object postObject : postObjects) {
                if (postObject instanceof Post) {
                    posts.add((Post) postObject);
                }
            }
        }
        responseBody.put("list", posts);
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
        responseHeaders.put("X-Custom-Header", "application/json");
        responseHeaders.put("Access-Control-Allow-Origin", "*");

        APIGatewayProxyResponseEvent responseEvent = new APIGatewayProxyResponseEvent()
                .withHeaders(responseHeaders)
                .withStatusCode(statusCode)
                .withBody(gson.toJson(new Response<Map<String, Object>>(statusCode, responseBody, error)));
        logger.log("\n" + responseEvent.toString());

        return responseEvent;
    }

}
