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
package org.mybatis.spring.support;

import static org.springframework.util.Assert.notNull;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.dao.support.DaoSupport;

/**
 * Convenient super class for MyBatis SqlSession data access objects. It gives you access to the template which can then
 * be used to execute SQL methods.
 * <p>
 * 方便的基础类，使用 mybatis sqlSession 进行数据访问操作。它使得你可以通过 {@link SqlSessionTemplate} 来执行 SQL 操作。
 *
 * <p>
 * This class needs a SqlSessionTemplate or a SqlSessionFactory. If both are set the SqlSessionFactory will be ignored.
 * <p>
 *
 * @author Putthiphong Boonphong
 * @author Eduardo Macarron
 * @see #setSqlSessionFactory
 * @see #setSqlSessionTemplate
 * @see SqlSessionTemplate
 */
public abstract class SqlSessionDaoSupport extends DaoSupport {

  /**
   * 使用这个特殊的 {@link SqlSessionTemplate} 来执行 SQL 操作。
   */
  private SqlSessionTemplate sqlSessionTemplate;

  /**
   * Set MyBatis SqlSessionFactory to be used by this DAO. Will automatically create SqlSessionTemplate for the given
   * SqlSessionFactory.
   * <p>
   * 通过这个方法隐式创建了 {@link SqlSessionTemplate}
   *
   * @param sqlSessionFactory a factory of SqlSession
   */
  public void setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
    if (this.sqlSessionTemplate == null || sqlSessionFactory != this.sqlSessionTemplate.getSqlSessionFactory()) {
      this.sqlSessionTemplate = createSqlSessionTemplate(sqlSessionFactory);
    }
  }

  /**
   * Create a SqlSessionTemplate for the given SqlSessionFactory. Only invoked if populating the DAO with a
   * SqlSessionFactory reference!
   * <p>
   * Can be overridden in subclasses to provide a SqlSessionTemplate instance with different configuration, or a custom
   * SqlSessionTemplate subclass.
   * <p>
   * 使用该方法可以创建 {@link SqlSessionTemplate}
   *
   * @param sqlSessionFactory the MyBatis SqlSessionFactory to create a SqlSessionTemplate for
   * @return the new SqlSessionTemplate instance
   * @see #setSqlSessionFactory
   */
  @SuppressWarnings("WeakerAccess")
  protected SqlSessionTemplate createSqlSessionTemplate(SqlSessionFactory sqlSessionFactory) {
    return new SqlSessionTemplate(sqlSessionFactory);
  }

  /**
   * Return the MyBatis SqlSessionFactory used by this DAO.
   *
   * @return a factory of SqlSession
   */
  public final SqlSessionFactory getSqlSessionFactory() {
    return (this.sqlSessionTemplate != null ? this.sqlSessionTemplate.getSqlSessionFactory() : null);
  }

  /**
   * Set the SqlSessionTemplate for this DAO explicitly, as an alternative to specifying a SqlSessionFactory.
   *
   * @param sqlSessionTemplate a template of SqlSession
   * @see #setSqlSessionFactory
   */
  public void setSqlSessionTemplate(SqlSessionTemplate sqlSessionTemplate) {
    this.sqlSessionTemplate = sqlSessionTemplate;
  }

  /**
   * Users should use this method to get a SqlSession to call its statement methods This is SqlSession is managed by
   * spring. Users should not commit/rollback/close it because it will be automatically done.
   *
   * @return Spring managed thread safe SqlSession
   */
  public SqlSession getSqlSession() {
    return this.sqlSessionTemplate;
  }

  /**
   * Return the SqlSessionTemplate for this DAO, pre-initialized with the SessionFactory or set explicitly.
   * <p>
   * <b>Note: The returned SqlSessionTemplate is a shared instance.</b> You may introspect its configuration, but not
   * modify the configuration (other than from within an {@link #initDao} implementation). Consider creating a custom
   * SqlSessionTemplate instance via {@code new SqlSessionTemplate(getSqlSessionFactory())}, in which case you're
   * allowed to customize the settings on the resulting instance.
   * <p>
   * 这个方法与 {@link SqlSessionDaoSupport#getSqlSession()} 不同的是，返回的是 {@link SqlSessionTemplate}，
   * 你可以通过这个对象查看它的配置，但是不要修改(除了你在 {@link #initDao} 中)
   *
   * @return a template of SqlSession
   */
  public SqlSessionTemplate getSqlSessionTemplate() {
    // 返回类型让调用者可以获取更多信息，但是也更危险 (但是我还不清楚到底有啥可改的，返回 SqlSession 也能拿到配置)
    return this.sqlSessionTemplate;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  protected void checkDaoConfig() {
    notNull(this.sqlSessionTemplate, "Property 'sqlSessionFactory' or 'sqlSessionTemplate' are required");
  }

}
