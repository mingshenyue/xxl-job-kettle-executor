package com.mvp51.actuator.kettle;

import com.mvp51.actuator.dto.KettleResult;
import lombok.SneakyThrows;
import org.pentaho.di.core.KettleEnvironment;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.plugins.PluginRegistry;
import org.pentaho.di.job.Job;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;

import java.util.Map;
/**
 * kettle 执行类
 * @author 石泽旭
 */
public class KettleExecutor {

    /**
     * 初始化插件和配置文件
     */
    public static synchronized void init(String kettleHome) throws KettleException {
        if (!KettleEnvironment.isInitialized()) {
            if (kettleHome != null && !kettleHome.isBlank()) {
                System.setProperty("KETTLE_HOME", kettleHome);
            }
            KettleEnvironment.init();
            PluginRegistry.init();
        }
    }

    public static void init() throws KettleException {
        init(null);
    }

    /**
     * 执行ktr文件-转换任务
     *
     * @param fileName  ktr文件地址
     * @param params 传入参数
     * @return 执行结果
     */
    public static KettleResult runKtr(String fileName, Map<String, String> params) throws KettleException {
        if (!KettleEnvironment.isInitialized()) {
            throw new IllegalStateException("Kettle environment has not been initialized. Please call init() first.");
        }

        KettleResult kettleResult = new KettleResult();
        TransMeta tm = new TransMeta(fileName);
        Trans trans = new Trans(tm);
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                trans.setParameterValue(entry.getKey(), entry.getValue());
            }
        }
        trans.execute(null);
        trans.waitUntilFinished();

        kettleResult.setTrans(trans);
        return kettleResult;
    }

    /**
     * 执行kjb文件-作业任务
     *
     * @param fileName kjb文件地址
     * @param params 参数
     * @return 执行结果
     */
    public static KettleResult runKjb(String fileName, Map<String, String> params) throws KettleException {
        if (!KettleEnvironment.isInitialized()) {
            throw new IllegalStateException("Kettle environment has not been initialized. Please call init() first.");
        }

        KettleResult kettleResult = new KettleResult();
        JobMeta jm = new JobMeta(fileName, null);
        Job job = new Job(null, jm);
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                job.setVariable(entry.getKey(), entry.getValue());
            }
        }
        job.start();
        job.waitUntilFinished();
        kettleResult.setJob(job);
        return kettleResult;
    }



    @SneakyThrows
    public static void main(String[] args) {
        init();
        //runKtr("C:\\dev-tool\\kettle_file\\测试从Oracle抽取数据.ktr",null);
        runKtr("C:\\dev-tool\\kettle_file\\测试从Oracle抽取数据.ktr",null);
    }


}
