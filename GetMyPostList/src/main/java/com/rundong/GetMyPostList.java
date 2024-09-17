package com.rundong;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.util.Map;


/**
 * Hello world!
 *
 */
public class GetMyPostList
{
    public String handleRequest(Map<String,Object> input, Context context) {
        LambdaLogger logger = context.getLogger();
        logger.log("hello this is log in docker container!");
        System.out.println(input);
        return "Hello";
    }
}


