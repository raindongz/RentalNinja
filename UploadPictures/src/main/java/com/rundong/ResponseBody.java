package com.rundong;

import java.util.Objects;

public class ResponseBody {
    String rsp1;
    String rsp2;

    @Override
    public String toString() {
        return "ResponseBody{" +
                "rsp1='" + rsp1 + '\'' +
                ", rsp2='" + rsp2 + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResponseBody that)) return false;
        return Objects.equals(getRsp1(), that.getRsp1()) && Objects.equals(rsp2, that.rsp2);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRsp1(), rsp2);
    }

    public String getRsp1() {
        return rsp1;
    }

    public void setRsp1(String rsp1) {
        this.rsp1 = rsp1;
    }

    public ResponseBody(String rsp1, String rsp2) {
        this.rsp1 = rsp1;
        this.rsp2 = rsp2;
    }
}
