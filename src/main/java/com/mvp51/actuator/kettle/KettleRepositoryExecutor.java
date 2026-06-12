package com.mvp51.actuator.kettle;

import com.mvp51.actuator.FramelessApplication;
import com.mvp51.actuator.dto.KettleResult;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.pentaho.di.core.KettleEnvironment;
import org.pentaho.di.core.Result;
import org.pentaho.di.core.database.DatabaseMeta;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.core.plugins.PluginRegistry;
import org.pentaho.di.job.Job;
import org.pentaho.di.job.JobMeta;
import org.pentaho.di.repository.RepositoryDirectoryInterface;
import org.pentaho.di.repository.kdr.KettleDatabaseRepository;
import org.pentaho.di.repository.kdr.KettleDatabaseRepositoryMeta;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

public class KettleRepositoryExecutor {

    private static final Logger logger = LoggerFactory.getLogger(FramelessApplication.class);

    private static DatabaseMeta databaseMeta;

    private static String username;

    private static String password;

    private static GenericObjectPool<KettleDatabaseRepository> pool;

    public static void init(Environment environment) {
        try {
            // 初始化环境
            if (!KettleEnvironment.isInitialized()) {
                KettleEnvironment.init();
            }
            // 加载插件
            PluginRegistry.init();
            String username = environment.getRequiredProperty("kettle.repository.username");
            String password = environment.getRequiredProperty("kettle.repository.password");
            String type = environment.getRequiredProperty("kettle.repository.db.type");
            String host = environment.getRequiredProperty("kettle.repository.db.host");
            String port = environment.getRequiredProperty("kettle.repository.db.port");
            String database = environment.getRequiredProperty("kettle.repository.db.database");
            String dbUsername = environment.getRequiredProperty("kettle.repository.db.username");
            String dbPassword = environment.getRequiredProperty("kettle.repository.db.password");
            int maxTotal = environment.getProperty("kettle.repository.pool.max-total", Integer.class, 5);
            int maxIdle = environment.getProperty("kettle.repository.pool.max-idle", Integer.class, 5);
            int minIdle = environment.getProperty("kettle.repository.pool.min-idle", Integer.class, 1);
            // 初始化kettle环境
            init(
                    new DatabaseMeta("database", type, "JDBC", host, database, port, dbUsername, dbPassword),
                    username,
                    password,
                    maxTotal,
                    maxIdle,
                    minIdle
            );
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    public static void destroy() {
        if (pool != null) {
            pool.close();
        }
    }

    public static void init(DatabaseMeta meta, String myUsername, String myPassword) {
        init(meta, myUsername, myPassword, 5, 5, 1);
    }

    public static void init(DatabaseMeta meta, String myUsername, String myPassword, int maxTotal, int maxIdle, int minIdle) {

        try {


            databaseMeta = meta;
            username = myUsername;
            password = myPassword;
            // 配置对象池
            GenericObjectPoolConfig config = new GenericObjectPoolConfig<>();
            // 设置最大活跃对象数
            config.setMaxTotal(maxTotal);
            // 设置最大空闲对象数
            config.setMaxIdle(maxIdle);
            // 设置最小空闲对象数
            config.setMinIdle(minIdle);

            // 初始化连接池
            pool = new GenericObjectPool<>(new KettleRepositoryFactory(), config);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    public static KettleResult trans(String directoryPath, String transformationName) {
        KettleResult kettleResult = new KettleResult();
        KettleDatabaseRepository repository = null;
        try {
            // 获取链接
            repository = pool.borrowObject();
            if (!repository.isConnected()) {
                throw new Exception("无法连接到 Kettle 数据库资源库！");
            }
            // 定位转换路径 (例如: /example_transformations/my_transformation)
            // 加载转换
            RepositoryDirectoryInterface directory = repository.loadRepositoryDirectoryTree().findDirectory(directoryPath);
            TransMeta transMeta = repository.loadTransformation(transformationName, directory, null, true, null);
            // 创建转换实例
            Trans trans = new Trans(transMeta);
            // 启动转换
            trans.execute(null);  // 没有传递参数
            trans.waitUntilFinished();
            if (trans.getErrors() > 0) {
                logger.error("转换执行失败！");
            } else {
                logger.info("转换执行成功！");
            }
            kettleResult.setTrans(trans);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            // 归还链接
            if (repository != null) {
                pool.returnObject(repository);
            }
        }
        return kettleResult;
    }

    public static KettleResult job(String directoryPath, String jobName) {
        KettleResult kettleResult = new KettleResult();
        KettleDatabaseRepository repository = null;
        try {
            // 获取链接
            repository = pool.borrowObject();
            if (!repository.isConnected()) {
                throw new Exception("无法连接到 Kettle 数据库资源库！");
            }
            // 定位转换路径 (例如: /example_transformations/my_transformation)
            RepositoryDirectoryInterface directory = repository.loadRepositoryDirectoryTree().findDirectory(directoryPath);
            JobMeta jobMeta = repository.loadJob(jobName, directory, null, null);
            // 创建作业实例
            Job job = new Job(repository,jobMeta);
            job.execute(0,null);
            job.waitUntilFinished();
            if (job.getErrors() > 0) {
                logger.error("作业执行失败！");
            } else {
                logger.info("作业执行成功！");
            }
            kettleResult.setJob(job);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 归还链接
            if (repository != null) {
                pool.returnObject(repository);
            }
        }
        return kettleResult;
    }


    public static class KettleRepositoryFactory extends BasePooledObjectFactory<KettleDatabaseRepository> {

        private static int index = 0;

        @Override
        public KettleDatabaseRepository create() throws Exception {
            index++;
            String metaName = "repository_" + index;
            databaseMeta.setName(metaName);
            KettleDatabaseRepositoryMeta repositoryMeta = new KettleDatabaseRepositoryMeta();
            repositoryMeta.setName(metaName);
            repositoryMeta.setConnection(databaseMeta);
            KettleDatabaseRepository repository = new KettleDatabaseRepository();
            repository.init(repositoryMeta);
            return repository;
        }

        @Override
        public PooledObject<KettleDatabaseRepository> wrap(KettleDatabaseRepository kettleDatabaseRepository) {
            return new DefaultPooledObject<>(kettleDatabaseRepository);
        }

        @Override
        public void activateObject(PooledObject<KettleDatabaseRepository> p) throws Exception {
            p.getObject().connect(username, password);
        }

        @Override
        public void destroyObject(PooledObject<KettleDatabaseRepository> p) throws Exception {
            p.getObject().disconnect();
        }

        @Override
        public void passivateObject(PooledObject<KettleDatabaseRepository> p) throws Exception {
            //p.getObject().disconnect();
        }

        @Override
        public boolean validateObject(PooledObject<KettleDatabaseRepository> p) {
            return p.getObject().isConnected();
        }

    }


    public static void main(String[] args) {

            KettleResult kettleResult = new KettleResult();
            try {
                // 1. 初始化 Kettle 环境
                KettleEnvironment.init();
                PluginRegistry.init();
                // 2. 配置 Kettle 数据库资源库连接信息
                KettleDatabaseRepositoryMeta repositoryMeta = new KettleDatabaseRepositoryMeta();
                repositoryMeta.setName("database");
                repositoryMeta.setConnection(new DatabaseMeta("database", "MySQL", "JDBC", "172.18.20.120", "kettle_config", "3306", "root", "mvp51.Com"));
                // 3. 连接到资源库
                KettleDatabaseRepository repository = new KettleDatabaseRepository();
                repository.init(repositoryMeta);
                repository.connect("admin", "admin");
                if (!repository.isConnected()) {
                    throw new Exception("无法连接到 Kettle 数据库资源库！");
                }
                System.out.println("已成功连接到资源库！");
                // 4. 定位转换路径 (例如: /example_transformations/my_transformation)
                // 5. 加载转换
                RepositoryDirectoryInterface directory = repository.loadRepositoryDirectoryTree().findDirectory("/");
                TransMeta transMeta = repository.loadTransformation("转换112", directory, null, true, null);
                // 6. 创建转换实例
                Trans trans = new Trans(transMeta);
                // 7. 启动转换
                trans.execute(null);  // 没有传递参数
                trans.waitUntilFinished();
                if (trans.getErrors() > 0) {
                    System.err.println("转换执行失败！");
                } else {
                    System.out.println("转换执行成功！");
                }
                kettleResult.setTrans(trans);
                // 8. 断开资源库连接
                repository.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }


    }


}
