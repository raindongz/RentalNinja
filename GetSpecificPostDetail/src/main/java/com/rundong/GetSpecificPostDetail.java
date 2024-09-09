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
import com.google.gson.Gson;

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
    @Override
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent event, final Context context) {
        // set up response header
        Map<String, String> responseHeaders = new HashMap<>();
        responseHeaders.put("Content-Type", "application/json");
        responseHeaders.put("X-Custom-Header", "application/json");
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent().withHeaders(responseHeaders);
        // initialize logger
        LambdaLogger logger = context.getLogger();
        logger.log("Function" + context.getFunctionName() + "  is called", LogLevel.INFO);

        Gson gson = new Gson();
        Request request = gson.fromJson(event.getBody(), Request.class);
        String postId = request.postId();
        logger.log("this is post ID: " + postId, LogLevel.INFO);

        // get specific post from dynamodb
        Post post;
        try {
             post = dynamoDBMapper.load(Post.class, postId);
        }catch (Exception e){
            //todo: throw new exception
            logger.log("load post with postId error "+e, LogLevel.ERROR);
            return new APIGatewayProxyResponseEvent()
                    .withHeaders(responseHeaders)
                    .withBody("internal server error!!")
                    .withStatusCode(500);
        }
        // convert to json object then return to client
        String json = gson.toJson(post);

        return new APIGatewayProxyResponseEvent()
                .withHeaders(responseHeaders)
                .withBody(json)
                .withStatusCode(200);
    }
}
