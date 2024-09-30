package com.rundong;

import com.google.gson.annotations.SerializedName;

public record Request(@SerializedName(value = "postId") String postId) {
}
