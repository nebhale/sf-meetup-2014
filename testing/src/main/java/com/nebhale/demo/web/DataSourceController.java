/*
 * Copyright 2013-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nebhale.demo.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@RestController
public final class DataSourceController {

    private final DataSource dataSource;

    @Autowired
    DataSourceController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @RequestMapping(method = RequestMethod.GET, value = "")
    String url() {
        return getUrl(this.dataSource);
    }

    private String getUrl(DataSource dataSource) {
        if (isClass(dataSource, "com.jolbox.bonecp.BoneCPDataSource")) {
            return invokeMethod(dataSource, "getJdbcUrl");
        } else if (isClass(dataSource, "org.apache.commons.dbcp.BasicDataSource")) {
            return invokeMethod(dataSource, "getUrl");
        } else if (isClass(dataSource, "org.apache.tomcat.dbcp.dbcp.BasicDataSource")) {
            return invokeMethod(dataSource, "getUrl");
        } else if (isClass(dataSource, "org.apache.tomcat.jdbc.pool.DataSource")) {
            return invokeMethod(dataSource, "getUrl");
        } else if (isClass(dataSource, "org.springframework.jdbc.datasource.embedded" +
                ".EmbeddedDatabaseFactory$EmbeddedDataSourceProxy")) {
            return getUrl(getDataSource(dataSource));
        } else if (isClass(dataSource, "org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy")) {
            return getUrl(getTargetDataSource(dataSource));
        } else if (isClass(dataSource, "org.springframework.jdbc.datasource.SimpleDriverDataSource")) {
            return invokeMethod(dataSource, "getUrl");
        } else if (isClass(dataSource, "org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy")) {
            return getUrl(getTargetDataSource(dataSource));
        }

        return String.format("Unable to determine URL for DataSource of type %s", dataSource.getClass().getName());
    }

    private boolean isClass(DataSource dataSource, String className) {
        return dataSource.getClass().getName().equals(className);
    }

    private String invokeMethod(DataSource dataSource, String methodName) {
        Method method = ReflectionUtils.findMethod(dataSource.getClass(), methodName);
        return (String) ReflectionUtils.invokeMethod(method, dataSource);
    }

    private DataSource getTargetDataSource(DataSource dataSource) {
        Method method = ReflectionUtils.findMethod(dataSource.getClass(), "getTargetDataSource");
        return (DataSource) ReflectionUtils.invokeMethod(method, dataSource);
    }

    private DataSource getDataSource(DataSource dataSource) {
        try {
            Field field = dataSource.getClass().getDeclaredField("dataSource");
            ReflectionUtils.makeAccessible(field);

            return (DataSource) field.get(dataSource);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

}
