package com.rundong;

import com.amazonaws.services.dynamodbv2.datamodeling.*;

import java.util.Date;

@DynamoDBTable(tableName = "collections")
public class Collection {

    @DynamoDBHashKey(attributeName = "user_id")
    private String userId;

    @DynamoDBRangeKey(attributeName = "post_id")
    private String postId;

    @DynamoDBAttribute(attributeName = "create_time")
    private Date createTime;

    public Collection() {

    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getUserId() {
        return userId;
    }

    public String getPostId() {
        return postId;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public Collection(String userId, String postId, Date createTime) {
        this.userId = userId;
        this.postId = postId;
        this.createTime = createTime;
    }
}
