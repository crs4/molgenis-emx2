package org.molgenis.emx2.sql;

import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.exception.DataAccessException;
import org.jooq.impl.DSL;
import org.molgenis.emx2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.jooq.impl.DSL.name;
import static org.molgenis.emx2.sql.Constants.MG_USER_PREFIX;
import static org.molgenis.emx2.sql.SqlDatabaseExecutor.*;
import static org.molgenis.emx2.sql.SqlSchemaMetadataExecutor.executeCreateSchema;

public class SqlDatabase implements Database {
  private static final String ADMIN = "admin";
  private DataSource source;
  private DSLContext jooq;
  private SqlUserAwareConnectionProvider connectionProvider;
  private Map<String, SchemaMetadata> schemaCache = new LinkedHashMap<>(); // cache
  private Collection<String> schemaNames = new ArrayList<>();
  private boolean inTx;
  private static Logger logger = LoggerFactory.getLogger(SqlDatabase.class);
  private DatabaseListener listener =
      new DatabaseListener() {
        @Override
        public void schemaRemoved(String name) {
          // dummy
        }

        @Override
        public void userChanged() {
          // dummy
        }

        @Override
        public void schemaChanged(String schemaName) {
          // dummy
        }
      };

  public SqlDatabase(DataSource source) {
    this.source = source;
    this.connectionProvider = new SqlUserAwareConnectionProvider(source);
    this.jooq = DSL.using(connectionProvider, SQLDialect.POSTGRES);
    MetadataUtils.createMetadataSchemaIfNotExists(jooq);

    // setup default stuff
    this.jooq.execute("CREATE EXTENSION IF NOT EXISTS pg_trgm"); // for fast fuzzy search
    this.jooq.execute("CREATE EXTENSION IF NOT EXISTS pgcrypto;"); // for password hashing
    if (!hasUser("anonymous")) {
      this.addUser("anonymous");
    }
    if (!hasUser(ADMIN)) {
      this.addUser(ADMIN);
      this.setUserPassword(ADMIN, ADMIN);
      this.jooq.execute("ALTER USER {0} WITH SUPERUSER", name(MG_USER_PREFIX + ADMIN));
    }
  }

  @Override
  public void setListener(DatabaseListener listener) {
    this.listener = listener;
  }

  @Override
  public DatabaseListener getListener() {
    return this.listener;
  }

  private void log(long start, String message) {
    if (logger.isInfoEnabled()) {
      logger.info("{} in {}ms", message, (System.currentTimeMillis() - start));
    }
  }

  @Override
  public SqlSchema createSchema(String name) {
    long start = System.currentTimeMillis();
    SqlSchemaMetadata metadata = new SqlSchemaMetadata(this, name);
    this.tx(
        database -> {
          executeCreateSchema(this, metadata);
          // make current user a manager
          if (getActiveUser() != null) {
            getSchema(metadata.getName())
                .addMember(getActiveUser(), DefaultRoles.MANAGER.toString());
          }
          schemaCache.put(name, metadata);
          schemaNames.add(name);
        });
    this.log(start, "created schema " + name);
    return new SqlSchema(this, metadata);
  }

  @Override
  public SqlSchema getSchema(String name) {
    SqlSchemaMetadata metadata = new SqlSchemaMetadata(this, name);
    if (metadata.exists()) {
      SqlSchema schema = new SqlSchema(this, metadata);
      schemaCache.put(name, metadata); // cache
      return schema;
    }
    return null;
  }

  @Override
  public void dropSchema(String name) {
    long start = System.currentTimeMillis();
    Schema schema = getSchema(name);
    tx(d -> SqlSchemaMetadataExecutor.executeDropSchema(getJooq(), schema.getMetadata()));
    schemaCache.remove(name);
    schemaNames.remove(name);
    listener.schemaRemoved(name);
    log(start, "dropped schema " + schema.getMetadata().getName());
  }

  @Override
  public Schema dropCreateSchema(String name) {
    if (this.getSchemaNames().contains(name)) {
      this.dropSchema(name);
    }
    return createSchema(name);
  }

  @Override
  public Collection<String> getSchemaNames() {
    if (this.schemaNames.isEmpty()) {
      this.schemaNames = MetadataUtils.loadSchemaNames(this);
    }
    return this.schemaNames;
  }

  @Override
  public void addUser(String user) {
    if (hasUser(user)) return; // idempotent
    long start = System.currentTimeMillis();
    // need elevated privileges, so not as active user
    String currentUser = getActiveUser();
    try {
      clearActiveUser();
      tx(d -> executeCreateUser(getJooq(), user));
    } finally {
      if (currentUser != null) {
        setActiveUser(currentUser);
      }
    }
    log(start, "created user " + user);
  }

  @Override
  public boolean checkUserPassword(String user, String password) {
    return MetadataUtils.checkUserPassword(getJooq(), user, password);
  }

  @Override
  public void setUserPassword(String user, String password) {
    long start = System.currentTimeMillis();
    tx(d -> MetadataUtils.setUserPassword(getJooq(), user, password));
    log(start, "set password for user" + user);
  }

  @Override
  public boolean hasUser(String user) {
    return !jooq.fetch(
            "SELECT rolname FROM pg_catalog.pg_roles WHERE rolname = {0}", MG_USER_PREFIX + user)
        .isEmpty();
  }

  @Override
  public void removeUser(String user) {
    long start = System.currentTimeMillis();
    if (!hasUser(user))
      throw new MolgenisException(
          "Remove user failed", "User with name '" + user + "' doesn't exist");
    tx(d -> jooq.execute("DROP ROLE {0}", name(MG_USER_PREFIX + user)));
    log(start, "removed user " + user);
  }

  public void addRole(String role) {
    long start = System.currentTimeMillis();
    executeCreateRole(getJooq(), role);
    log(start, "created role " + role);
  }

  @Override
  public void grantCreateSchema(String user) {
    long start = System.currentTimeMillis();
    tx(d -> executeGrantCreateSchema(getJooq(), user));
    log(start, "granted create schema to user " + user);
  }

  @Override
  public void setActiveUser(String username) {
    if (inTx) {
      try {
        jooq.execute("SET SESSION AUTHORIZATION {0}", name(MG_USER_PREFIX + username));
      } catch (DataAccessException dae) {
        throw new SqlMolgenisException("Set active user failed", dae);
      }
    } else {
      this.connectionProvider.setActiveUser(username);
    }
    listener.userChanged();
  }

  @Override
  public String getActiveUser() {
    String user = jooq.fetchOne("SELECT SESSION_USER").get(0, String.class);
    if (user.contains(MG_USER_PREFIX)) return user.substring(MG_USER_PREFIX.length());
    return null;
  }

  @Override
  public void clearActiveUser() {
    if (inTx) {
      // then we don't use the connection provider
      try {
        jooq.execute("RESET SESSION AUTHORIZATION");
      } catch (DataAccessException dae) {
        throw new SqlMolgenisException("Clear active user failed", dae);
      }
    } else {
      this.connectionProvider.clearActiveUser();
    }
  }

  @Override
  public synchronized void tx(Transaction transaction) {
    if (inTx) {
      // we do not nest transactions
      transaction.run(this);
    } else {
      // createColumn independent merge of database with transaction connection
      DSLContext originalContext = jooq;
      try (Connection conn = source.getConnection()) {
        this.inTx = true;
        DSL.using(conn, SQLDialect.POSTGRES)
            .transaction(
                config -> {
                  DSLContext ctx = DSL.using(config);
                  ctx.execute("SET CONSTRAINTS ALL DEFERRED");
                  this.jooq = ctx;
                  if (connectionProvider.getActiveUser() != null) {
                    this.setActiveUser(connectionProvider.getActiveUser());
                  }
                  transaction.run(this);
                  this.clearActiveUser();
                });
      } catch (MolgenisException me) {
        clearCache();
        throw me;
      } catch (DataAccessException dae) {
        clearCache();
        throw new SqlMolgenisException(dae);
      } catch (SQLException e) {
        throw new MolgenisException("Transaction failed", e.getMessage(), e);
      } finally {
        this.inTx = false;
        jooq = originalContext;
      }
    }
  }

  @Override
  public void clearCache() {
    this.schemaCache = new LinkedHashMap<>();
    this.schemaNames = new ArrayList<>();
  }

  protected DSLContext getJooq() {
    return jooq;
  }
}
