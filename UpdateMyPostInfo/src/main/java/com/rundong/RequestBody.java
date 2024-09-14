package com.rundong;

public record RequestBody(String postId,
                          String title,
                          String content,
                          String contactInfo,
                          String picUrls,
                          String country,
                          String state,
                          String city,
                          String area) {

}
