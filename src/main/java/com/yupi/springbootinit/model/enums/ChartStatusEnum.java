package com.yupi.springbootinit.model.enums;

public enum ChartStatusEnum {
    WAIT("wait"),
    RUNNING("running"),
    SUCCEED("succeed"),
    FAILED("failed");

    private final String status;

    ChartStatusEnum(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
