package com.rundong;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;

/**
 * Hello world!
 *
 */
public class UploadPost implements RequestHandler<String, String>
{
    @Override
    public String handleRequest(String input, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("Function " + context.getFunctionName() + " is called");
        return input.toUpperCase();
    }
}
