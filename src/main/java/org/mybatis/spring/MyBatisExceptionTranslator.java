/*
 * Copyright 2010-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.mybatis.spring;

import java.sql.SQLException;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.apache.ibatis.exceptions.PersistenceException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;
import org.springframework.transaction.TransactionException;

/**
 * Default exception translator.
 * <p>
 * Translates MyBatis SqlSession returned exception into a Spring {@code DataAccessException} using Spring's
 * {@code SQLExceptionTranslator} Can load {@code SQLExceptionTranslator} eagerly or when the first exception is
 * translated.
 * <p>
 * 将 MyBatis 的异常，翻译为 Spring 的 {@link DataAccessException}。
 * 但是其实也不用特意写太多自己的代码，因为底层的异常大多还是 JDBC API 提供的异常。
 * 所以，这个类只需要将 MyBatis 自己异常里面的 cause 解析出来(期望是 {@link SQLException})，
 * 然后再委派给 Spring 组件 {@link SQLErrorCodeSQLExceptionTranslator} 解析即可
 *
 * @author Eduardo Macarron
 */
public class MyBatisExceptionTranslator implements PersistenceExceptionTranslator {

  private final Supplier<SQLExceptionTranslator> exceptionTranslatorSupplier;

  private SQLExceptionTranslator exceptionTranslator;

  /**
   * Creates a new {@code PersistenceExceptionTranslator} instance with {@code SQLErrorCodeSQLExceptionTranslator}.
   *
   * @param dataSource                  DataSource to use to find metadata and establish which error codes are usable.
   * @param exceptionTranslatorLazyInit if true, the translator instantiates internal stuff only the first time will have the need to translate
   *                                    exceptions.
   */
  public MyBatisExceptionTranslator(DataSource dataSource, boolean exceptionTranslatorLazyInit) {
    this(() -> new SQLErrorCodeSQLExceptionTranslator(dataSource), exceptionTranslatorLazyInit);
  }

  /**
   * Creates a new {@code PersistenceExceptionTranslator} instance with specified {@code SQLExceptionTranslator}.
   *
   * @param exceptionTranslatorSupplier Supplier for creating a {@code SQLExceptionTranslator} instance
   * @param exceptionTranslatorLazyInit if true, the translator instantiates internal stuff only the first time will have the need to translate
   *                                    exceptions.
   * @since 2.0.3
   */
  public MyBatisExceptionTranslator(Supplier<SQLExceptionTranslator> exceptionTranslatorSupplier,
                                    boolean exceptionTranslatorLazyInit) {
    this.exceptionTranslatorSupplier = exceptionTranslatorSupplier;
    if (!exceptionTranslatorLazyInit) {
      this.initExceptionTranslator();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DataAccessException translateExceptionIfPossible(RuntimeException e) {
    // 将属于自己的异常，翻译为 Spring 的 DataAccessException
    // 其实，就是获取内部的 SQLException，然后委托给 Spring

    if (e instanceof PersistenceException) {
      // Batch exceptions come inside another PersistenceException
      // recursion has a risk of infinite loop so better make another if
      // 批量操作时抛出的异常，通常会被再包一层，最外层是 PersistenceException
      // 如果靠递归去解析最里面的异常，可能陷入无限循环
      if (e.getCause() instanceof PersistenceException) {
        e = (PersistenceException) e.getCause();
      }

      // 如果 cause 是 SQLException
      if (e.getCause() instanceof SQLException) {
        this.initExceptionTranslator(); // 确保 Spring 的异常翻译器初始化

        String task = e.getMessage() + "\n";

        SQLException se = (SQLException) e.getCause(); // cause 强转为 SQLException

        // 交给 Spring 翻译异常
        DataAccessException dae = this.exceptionTranslator.translate(task, null, se);

        // 如果能翻译出来就返回，翻译不出来就返回 UncategorizedSQLException
        return dae != null ? dae : new UncategorizedSQLException(task, null, se);
      } else if (e.getCause() instanceof TransactionException) {
        throw (TransactionException) e.getCause();
      }
      return new MyBatisSystemException(e);
    }
    return null;
  }

  /**
   * Initializes the internal translator reference.
   */
  private synchronized void initExceptionTranslator() {
    if (this.exceptionTranslator == null) {
      this.exceptionTranslator = exceptionTranslatorSupplier.get();
    }
  }

}
