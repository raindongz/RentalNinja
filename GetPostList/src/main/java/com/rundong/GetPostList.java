package com.rundong;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedScanList;
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
import java.util.stream.Collectors;

/**
 * Hello world!
 *
 */
public class GetPostList implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>
{
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    private static final DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(client);
    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Function" + context.getFunctionName() + "is called", LogLevel.INFO);
        Map<String, Object> responseBody = new HashMap<>();

        SearchParam searchParam = gson.fromJson(event.getBody(), SearchParam.class);
        // validate the body
        if (searchParam.pageNum()<1 || searchParam.pageSize()<1){
            logger.log("page info not valid", LogLevel.ERROR);
            responseBody.put("errorMsg", "need valid page info");
            return returnApiResponse(500, responseBody, "need valid page info", "500", logger);
        }
        List<Post> postList = dynamoDBMapper.scan(Post.class, new DynamoDBScanExpression());
        List<Post> collect;

        // 1. both keywords stateCode and cityCode exist
        if ( !searchParam.keyword().isEmpty() && !searchParam.stateCode().isEmpty() && searchParam.cityCode().isEmpty()){
            responseBody.put("msg", searchParam);
            return  returnApiResponse(200, responseBody, null, null, logger);
        }
        // 2. keywords exist other two null
        // 3. keywords exists, stateCode exist, cityCode null
        // 4. keywords null, stateCode exist, cityCode exists
        // 5. keywords null, stateCode exists, cityCode null
        // 6. both keywords and stateCode and cityCode is null
        collect = postList.stream()
                .filter(post -> post.getDeleteFlag() == 0)
                .sorted(Comparator.comparing(Post::getUpdateTime).reversed())
                .toList();
        // pagination
        int fromIdx = (searchParam.pageNum()-1) * searchParam.pageSize();
        int toIdx = ((searchParam.pageNum()-1) * searchParam.pageSize()) + (searchParam.pageSize()-1);
        if (fromIdx > collect.size()){
            logger.log("invalid page start index", LogLevel.ERROR);
            responseBody.put("errorMsg", "invalid page start index");
            return returnApiResponse(500, responseBody, "need valid page info", "500", logger);
        }
        if (toIdx >= collect.size()){
            toIdx = collect.size()-1;
        }
        List<Post> paginatedPosts = collect.subList(fromIdx, toIdx);
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
