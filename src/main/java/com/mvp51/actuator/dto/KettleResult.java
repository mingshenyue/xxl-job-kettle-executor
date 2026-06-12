package com.mvp51.actuator.dto;


import lombok.Data;
import org.pentaho.di.core.Result;
import org.pentaho.di.job.Job;
import org.pentaho.di.trans.Trans;

import java.util.Date;

@Data
public class KettleResult {

    //执行状态
    private String status;
    //kettle执行日志id
    private String logChannelId;
    //执行错误数
    private int errors;
    //开始时间
    private Date startDate;
    //结束时间
    private Date endDate;
    //结果集合
    private Result result;

    /**
     * 传入Job设置属性
     *
     * @param job
     */
    public void setJob(Job job) {
        this.status = job.getStatus();
        this.logChannelId = job.getLogChannelId();
        this.errors = job.getErrors();
        this.result = job.getResult().lightClone();
        this.startDate = job.getStartDate();
        this.endDate = job.getEndDate();
    }

    /**
     * 传入Trans设置属性
     *
     * @param trans
     */
    public void setTrans(Trans trans) {
        this.status = trans.getStatus();
        this.logChannelId = trans.getLogChannelId();
        this.errors = trans.getErrors();
        this.result = trans.getResult().lightClone();
        this.startDate = trans.getStartDate();
        this.endDate = trans.getEndDate();
    }

}
