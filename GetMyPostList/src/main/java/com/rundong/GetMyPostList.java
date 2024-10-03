package com.rundong;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import com.amazonaws.util.StringUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Hello world!
 *
 */
public class GetMyPostList implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>
{
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    private static final DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(client);
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Function" + context.getFunctionName() + "is called", LogLevel.INFO);
        Map<String, Object> responseBody = new HashMap<>();

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
        Request request = gson.fromJson(event.getBody(), Request.class);

        List<Post> postList = dynamoDBMapper.scan(Post.class, new DynamoDBScanExpression());
        List<Post> resultListUnPaged = postList.stream().filter(post -> post.getUserId() != null && post.getUserId().equals(username)).toList();
        // pagination
        int fromIdx = request.pageNum() * request.pageSize();
        int toIdx = (request.pageNum() * request.pageSize() + request.pageSize());
        if (fromIdx > resultListUnPaged.size()){
            logger.log("invalid page start index", LogLevel.ERROR);
            responseBody.put("errorMsg", "invalid page start index");
            return returnApiResponse(500, responseBody, "need valid page info", "500", logger);
        }
        if (toIdx >= resultListUnPaged.size()){
            toIdx = resultListUnPaged.size()-1;
        }
        if (fromIdx > toIdx){
            toIdx = fromIdx;
        }
        List<Post> paginatedPosts = resultListUnPaged.subList(fromIdx, toIdx);
        responseBody.put("post_list", paginatedPosts);
        return returnApiResponse(200, responseBody, null, null, logger);
    }

    public APIGatewayProxyResponseEvent returnApiResponse(int statusCode, Map<String, Object> responseBody,
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
                .withBody(gson.toJson(new Response<Map<String, Object>>(statusCode, responseBody, error)));
        logger.log("\n" + responseEvent.toString(), LogLevel.INFO);

        return responseEvent;

    }
}


