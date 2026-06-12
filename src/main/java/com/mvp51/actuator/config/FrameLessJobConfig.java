package com.mvp51.actuator.config;

import com.mvp51.actuator.jobhandler.KettleJob;
import com.mvp51.actuator.jobhandler.SampleXxlJob;
import com.mvp51.actuator.kettle.KettleExecutor;
import com.mvp51.actuator.kettle.KettleRepositoryExecutor;
import com.xxl.job.core.executor.impl.XxlJobSimpleExecutor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class FrameLessJobConfig implements InitializingBean, DisposableBean {

    private final Environment environment;
    private XxlJobSimpleExecutor xxlJobExecutor;

    public FrameLessJobConfig(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        KettleExecutor.init(environment.getProperty("kettle.home"));
        KettleRepositoryExecutor.init(environment);

        xxlJobExecutor = new XxlJobSimpleExecutor();
        xxlJobExecutor.setAdminAddresses(environment.getRequiredProperty("xxl.job.admin.addresses"));
        xxlJobExecutor.setAccessToken(environment.getProperty("xxl.job.access-token"));
        xxlJobExecutor.setAppname(environment.getRequiredProperty("xxl.job.executor.appname"));
        xxlJobExecutor.setAddress(environment.getProperty("xxl.job.executor.address"));
        xxlJobExecutor.setIp(environment.getProperty("xxl.job.executor.ip"));
        xxlJobExecutor.setPort(environment.getRequiredProperty("xxl.job.executor.port", Integer.class));
        xxlJobExecutor.setLogPath(environment.getRequiredProperty("xxl.job.executor.log-path"));
        xxlJobExecutor.setLogRetentionDays(
                environment.getRequiredProperty("xxl.job.executor.log-retention-days", Integer.class));
        xxlJobExecutor.setXxlJobBeanList(Arrays.asList(new SampleXxlJob(), new KettleJob()));
        xxlJobExecutor.start();
    }

    @Override
    public void destroy() {
        if (xxlJobExecutor != null) {
            xxlJobExecutor.destroy();
        }
        KettleRepositoryExecutor.destroy();
    }
}
