package com.mvp51.actuator.jobhandler;

import com.alibaba.fastjson2.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mvp51.actuator.dto.KettleResult;
import com.mvp51.actuator.kettle.KettleExecutor;
import com.mvp51.actuator.kettle.KettleRepositoryExecutor;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;

import java.util.HashMap;
import java.util.Map;

/**
 * kettle调用执行器实现
 *
 * @author 石泽旭
 */
public class KettleJob {

    @XxlJob("kettle")
    public void kettleExecute() {
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        XxlJobHelper.log("任务执行开始");
        JSONObject param = JSONObject.parseObject(XxlJobHelper.getJobParam());
        try {

            // 连接名称
            String type = param.getString("type");
            String fullPath = param.getString("fullPath");
            if (fullPath == null || fullPath.isBlank()) {
                XxlJobHelper.handleFail("任务执行文件未找到");
                return;
            }
            int i = fullPath.lastIndexOf("/");
            if (i < 0) {
                XxlJobHelper.handleFail("任务执行文件未找到");
                return;
            }
            String directoryPath = "/";
            String name = fullPath.substring(i + 1);
            if (i != 0) {
                directoryPath = fullPath.substring(0, i);
            }
            KettleResult result = null;
            if ("transformation".equals(type)) {
                result = KettleRepositoryExecutor.trans(directoryPath, name);
            } else if ("job".equals(type)) {
                result = KettleRepositoryExecutor.job(directoryPath, name);
            }
            //JSONObject.toJSONString(result)
            if (result != null && result.getErrors() > 0) {
                XxlJobHelper.handleFail("执行失败！");
            }
            XxlJobHelper.handleSuccess("执行成功！");
        } catch (Exception e) {
            XxlJobHelper.log("执行异常: " + e.getMessage());
            XxlJobHelper.handleFail(e.getMessage());
        } finally {
            // 计算总耗时
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            // 输出结束信息和总耗时
            XxlJobHelper.log("任务执行结束. 总耗时: " + duration + "ms.");
        }

    }


    /**
     * 参数模板：
     */
    @XxlJob("kettlePan")
    public void pan() {
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        // 输出开始信息
        XxlJobHelper.log("Pan Task start.");
        Map<String, String> parameters = parsingParameters();
        ObjectMapper mapper = new ObjectMapper();
        KettleResult result = null;
        try {
            result = KettleExecutor.runKtr(parameters.get("file"), parameters);
            // 处理成功后输出成功信息
            XxlJobHelper.handleSuccess(mapper.writeValueAsString(result));
        } catch (Exception e) {
            // 捕获异常并输出错误信息
            e.printStackTrace();
            XxlJobHelper.log("Error occurred: " + e.getMessage());
            XxlJobHelper.handleFail(e.getMessage());
        } finally {
            // 计算总耗时
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            // 输出结束信息和总耗时
            XxlJobHelper.log("Pan Task end. Total time spent: " + duration + "ms.");
        }
    }

    @XxlJob("kettleKitchen")
    public void kitchen() {
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        // 输出开始信息
        XxlJobHelper.log("Kitchen task start.");
        Map<String, String> parameters = parsingParameters();
        ObjectMapper mapper = new ObjectMapper();
        KettleResult result = null;
        try {
            result = KettleExecutor.runKjb(parameters.get("file"), parameters);
            // 处理成功后输出成功信息
            XxlJobHelper.handleSuccess(mapper.writeValueAsString(result));
        } catch (Exception e) {
            // 捕获异常并输出错误信息
            e.printStackTrace();
            XxlJobHelper.log("Error occurred in Kitchen task: " + e.getMessage());
            XxlJobHelper.handleFail(e.getMessage());
        } finally {
            // 计算总耗时
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            // 输出结束信息和总耗时
            XxlJobHelper.log("Kitchen task end. Total time spent: " + duration + "ms.");
        }
    }


    private Map<String, String> parsingParameters() {
        // 获取参数
        String param = XxlJobHelper.getJobParam();
        String[] params = param.split(";");
        Map<String, String> paramMap = new HashMap<String, String>();
        for (String s : params) {
            String[] split = s.split("=");
            if (split.length == 2) {
                paramMap.put(split[0], split[1]);
            }
        }
        return paramMap;
    }

}
