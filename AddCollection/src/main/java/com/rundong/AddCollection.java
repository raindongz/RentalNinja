package com.rundong;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
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
public class AddCollection implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

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
//        Collection collectionKey = new Collection();
//        collectionKey.setUserId(username);
//        DynamoDBQueryExpression<Collection> queryExpression = new DynamoDBQueryExpression<Collection>()
//                .withHashKeyValues(collectionKey);
//        List<Collection> result = dynamoDBMapper.query(Collection.class, queryExpression);

        Request request = gson.fromJson(event.getBody(), Request.class);
        if(request.isAdd() == 1){
            Collection collection = new Collection();
            collection.setUserId(username);
            collection.setPostId(request.postId());
            collection.setCreateTime(new Date());
            try {
                dynamoDBMapper.save(collection);
            }catch (Exception e){
                logger.log("save collection to dynamodb error:  " + e, LogLevel.ERROR);
                return returnApiResponse(500, null, "duplicate collect", "500", logger);
            }
        }else {
            Collection load = dynamoDBMapper.load(Collection.class, username, request.postId());
            Optional<Collection> loadedResul = Optional.ofNullable(load);
            if (loadedResul.isPresent()) {
                try {
                    dynamoDBMapper.delete(loadedResul.get());
                } catch (Exception e) {
                    logger.log("delete collection to dynamodb error:  " + e, LogLevel.ERROR);
                    return returnApiResponse(500, null, "delete record not exist", "500", logger);
                }
            }else {
                return returnApiResponse(500, null, "record not exist", "500", logger);
            }
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
