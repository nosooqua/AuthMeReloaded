package fr.xephi.authme.datasource;

import ch.jalu.datasourcecolumns.data.DataSourceValue;
import ch.jalu.datasourcecolumns.data.DataSourceValueImpl;
import ch.jalu.datasourcecolumns.data.DataSourceValues;
import ch.jalu.datasourcecolumns.predicate.AlwaysTruePredicate;
import fr.xephi.authme.ThreadSafetyUtils;
import fr.xephi.authme.annotation.ShouldBeAsync;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.columnshandler.AuthMeColumns;
import fr.xephi.authme.datasource.columnshandler.AuthMeColumnsHandler;
import fr.xephi.authme.security.crypts.HashedPassword;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import static ch.jalu.datasourcecolumns.data.UpdateValues.with;
import static ch.jalu.datasourcecolumns.predicate.StandardPredicates.eq;
import static ch.jalu.datasourcecolumns.predicate.StandardPredicates.eqIgnoreCase;
import static fr.xephi.authme.datasource.SqlDataSourceUtils.logSqlException;

/**
 * Common type for SQL-based data sources. Classes implementing this
 * must ensure that {@link #columnsHandler} is initialized on creation.
 */
public abstract class AbstractSqlDataSource implements DataSource {

    protected AuthMeColumnsHandler columnsHandler;

    @Override
    @ShouldBeAsync
    public boolean isAuthAvailable(String user) {
        ThreadSafetyUtils.shouldBeAsync();
        try {
            return columnsHandler.retrieve(user, AuthMeColumns.NAME).rowExists();
        } catch (SQLException e) {
            logSqlException(e);
            return false;
        }
    }

    @Override
    @ShouldBeAsync
    public HashedPassword getPassword(String user) {
        ThreadSafetyUtils.shouldBeAsync();
        try {
            DataSourceValues values = columnsHandler.retrieve(user, AuthMeColumns.PASSWORD, AuthMeColumns.SALT);
            if (values.rowExists()) {
                return new HashedPassword(values.get(AuthMeColumns.PASSWORD), values.get(AuthMeColumns.SALT));
            }
        } catch (SQLException e) {
            logSqlException(e);
        }
        return null;
    }

    @Override
    @ShouldBeAsync
    public boolean saveAuth(PlayerAuth auth) {
        ThreadSafetyUtils.shouldBeAsync();
        return columnsHandler.insert(auth,
            AuthMeColumns.NAME, AuthMeColumns.NICK_NAME, AuthMeColumns.PASSWORD, AuthMeColumns.SALT,
            AuthMeColumns.EMAIL, AuthMeColumns.REGISTRATION_DATE, AuthMeColumns.REGISTRATION_IP,
            AuthMeColumns.UUID);
    }

    @Override
    @ShouldBeAsync
    public boolean hasSession(String user) {
        ThreadSafetyUtils.shouldBeAsync();
        try {
            DataSourceValue<Integer> result = columnsHandler.retrieve(user, AuthMeColumns.HAS_SESSION);
            return result.rowExists() && Integer.valueOf(1).equals(result.getValue());
        } catch (SQLException e) {
            logSqlException(e);
            return false;
        }
    }

    @Override
    @ShouldBeAsync
    public boolean updateSession(PlayerAuth auth) {
        ThreadSafetyUtils.shouldBeAsync();
        return columnsHandler.update(auth, AuthMeColumns.LAST_IP, AuthMeColumns.LAST_LOGIN, AuthMeColumns.NICK_NAME);
    }

    @Override
    @ShouldBeAsync
    public boolean updatePassword(PlayerAuth auth) {
        ThreadSafetyUtils.shouldBeAsync();
        return updatePassword(auth.getNickname(), auth.getPassword());
    }

    @Override
    @ShouldBeAsync
    public boolean updatePassword(String user, HashedPassword password) {
        ThreadSafetyUtils.shouldBeAsync();
        return columnsHandler.update(user,
            with(AuthMeColumns.PASSWORD, password.getHash())
            .and(AuthMeColumns.SALT, password.getSalt()).build());
    }

    @Override
    @ShouldBeAsync
    public boolean updateQuitLoc(PlayerAuth auth) {
        ThreadSafetyUtils.shouldBeAsync();
        return columnsHandler.update(auth,
            AuthMeColumns.LOCATION_X, AuthMeColumns.LOCATION_Y, AuthMeColumns.LOCATION_Z,
            AuthMeColumns.LOCATION_WORLD, AuthMeColumns.LOCATION_YAW, AuthMeColumns.LOCATION_PITCH);
    }

    @Override
    @ShouldBeAsync
    public List<String> getAllAuthsByIp(String ip) {
        ThreadSafetyUtils.shouldBeAsync();
        try {
            return columnsHandler.retrieve(eq(AuthMeColumns.LAST_IP, ip), AuthMeColumns.NAME);
        } catch (SQLException e) {
            logSqlException(e);
            return Collections.emptyList();
        }
    }

    @Override
    @ShouldBeAsync
    public int countAuthsByEmail(String email) {
        ThreadSafetyUtils.shouldBeAsync();
        return columnsHandler.count(eqIgnoreCase(AuthMeColumns.EMAIL, email));
    }

    @Override
    @ShouldBeAsync
    public boolean updateEmail(PlayerAuth auth) {
        ThreadSafetyUtils.shouldBeAsync();
        return columnsHandler.update(auth, AuthMeColumns.EMAIL);
    }

    @Override
    @ShouldBeAsync
    public boolean isLogged(String user) {
        ThreadSafetyUtils.shouldBeAsync();
        try {
            DataSourceValue<Integer> result = columnsHandler.retrieve(user, AuthMeColumns.IS_LOGGED);
            return result.rowExists() && Integer.valueOf(1).equals(result.getValue());
        } catch (SQLException e) {
            logSqlException(e);
            return false;
        }
    }

    @Override
    @ShouldBeAsync
    public void setLogged(String user) {
        ThreadSafetyUtils.shouldBeAsync();
        columnsHandler.update(user, AuthMeColumns.IS_LOGGED, 1);
    }

    @Override
    @ShouldBeAsync
    public void setUnlogged(String user) {
        ThreadSafetyUtils.shouldBeAsync();
        columnsHandler.update(user, AuthMeColumns.IS_LOGGED, 0);
    }

    @Override
    @ShouldBeAsync
    public void grantSession(String user) {
        ThreadSafetyUtils.shouldBeAsync();
        columnsHandler.update(user, AuthMeColumns.HAS_SESSION, 1);
    }

    @Override
    @ShouldBeAsync
    public void revokeSession(String user) {
        ThreadSafetyUtils.shouldBeAsync();
        columnsHandler.update(user, AuthMeColumns.HAS_SESSION, 0);
    }

    @Override
    @ShouldBeAsync
    public void purgeLogged() {
        ThreadSafetyUtils.shouldBeAsync();
        columnsHandler.update(eq(AuthMeColumns.IS_LOGGED, 1), AuthMeColumns.IS_LOGGED, 0);
    }

    @Override
    @ShouldBeAsync
    public int getAccountsRegistered() {
        ThreadSafetyUtils.shouldBeAsync();
        return columnsHandler.count(new AlwaysTruePredicate<>());
    }

    @Override
    @ShouldBeAsync
    public boolean updateRealName(String user, String realName) {
        ThreadSafetyUtils.shouldBeAsync();
        return columnsHandler.update(user, AuthMeColumns.NICK_NAME, realName);
    }

    @Override
    @ShouldBeAsync
    public DataSourceValue<String> getEmail(String user) {
        ThreadSafetyUtils.shouldBeAsync();
        try {
            return columnsHandler.retrieve(user, AuthMeColumns.EMAIL);
        } catch (SQLException e) {
            logSqlException(e);
            return DataSourceValueImpl.unknownRow();
        }
    }
}
