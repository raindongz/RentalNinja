package com.rundong;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.logging.LogLevel;
import com.google.gson.Gson;


import java.util.HashMap;
import java.util.Map;

/**
 * Rundong Zhong
 *
 */
public class UploadPictures implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {


    @Override
    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent event, final Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Function" + context.getFunctionName() + "is called", LogLevel.INFO);

        Gson gson = new Gson();
        RequestBody bodyInput = gson.fromJson(event.getBody(), RequestBody.class);

        ResponseBody body = new ResponseBody(bodyInput.field1(), "world");
        String json = gson.toJson(body);

        Map<String, String> responseHeaders = new HashMap<>();
        responseHeaders.put("Content-Type", "application/json");
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent().withHeaders(responseHeaders);

        return response
                .withStatusCode(200)
                .withBody(json);
    }
}
