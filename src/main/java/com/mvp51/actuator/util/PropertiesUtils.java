package com.mvp51.actuator.util;

import lombok.extern.log4j.Log4j2;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Log4j2
public class PropertiesUtils {

//    public static Properties loadProperties(String propertyFileName) {
//        InputStreamReader in = null;
//        try {
//            ClassLoader loder = Thread.currentThread().getContextClassLoader();
//
//            in = new InputStreamReader(loder.getResourceAsStream(propertyFileName), "UTF-8");;
//            if (in != null) {
//                Properties prop = new Properties();
//                prop.load(in);
//                return prop;
//            }
//        } catch (IOException e) {
//            log.error("load {} error!", propertyFileName);
//        } finally {
//            if (in != null) {
//                try {
//                    in.close();
//                } catch (IOException e) {
//                    log.error("close {} error!", propertyFileName);
//                }
//            }
//        }
//        return null;
//    }

    /**
     *  实现允许加载jar同级别目录配置文件来覆盖配置参数
     * @param propertyFileName
     * @return
     */
    public static Properties loadProperties(String propertyFileName) {
        Properties props = new Properties();

        // 1️⃣ 再加载 classpath 内部配置（作为兜底 + 默认值）
        try (InputStreamReader in =
                     new InputStreamReader(
                             Thread.currentThread()
                                     .getContextClassLoader()
                                     .getResourceAsStream(propertyFileName),
                             StandardCharsets.UTF_8)) {

            if (in != null) {
                props.load(in);
                log.info("Load internal classpath config: {}", propertyFileName);
            } else {
                log.warn("Internal classpath config not found: {}", propertyFileName);
            }

        } catch (IOException e) {
            log.error("Load internal config error: {}", propertyFileName, e);
        }

        // 2️⃣ 优先加载 JAR 同级目录配置
        File jarDirFile = new File(System.getProperty("user.dir"), propertyFileName);
        if (jarDirFile.exists() && jarDirFile.isFile()) {
            try (InputStreamReader in =
                         new InputStreamReader(new FileInputStream(jarDirFile), StandardCharsets.UTF_8)) {
                props.load(in);
                log.info("Load external config from JAR directory: {}", jarDirFile.getAbsolutePath());
            } catch (IOException e) {
                log.error("Load JAR directory config error: {}", jarDirFile.getAbsolutePath(), e);
            }
        } else {
            log.info("No external config found in JAR directory, fallback to classpath");
        }



        return props;
    }
}
