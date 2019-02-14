/*
 *  This file is part of Player Analytics (Plan).
 *
 *  Plan is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License v3 as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Plan is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Plan. If not, see <https://www.gnu.org/licenses/>.
 */
package com.djrapitops.plan.db.sql.tables;

import com.djrapitops.plan.db.DBType;
import com.djrapitops.plan.db.SQLDB;
import com.djrapitops.plan.db.access.QueryStatement;
import com.djrapitops.plan.db.sql.parsing.CreateTableParser;
import com.djrapitops.plan.db.sql.parsing.Sql;
import com.djrapitops.plan.system.settings.config.Config;
import com.djrapitops.plan.system.settings.config.ConfigReader;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;

/**
 * Table that represents plan_settings.
 *
 * @author Rsl1122
 */
public class SettingsTable extends Table {

    public static final String TABLE_NAME = "plan_settings";

    public static final String ID = "id";
    public static final String SERVER_UUID = "server_uuid";
    public static final String UPDATED = "updated";
    public static final String CONFIG_CONTENT = "content";

    public static final String INSERT_STATEMENT = "INSERT INTO " + TABLE_NAME + " (" +
            SERVER_UUID + ", " +
            UPDATED + ", " +
            CONFIG_CONTENT + ") VALUES (?,?,?)";
    public static final String UPDATE_STATEMENT = "UPDATE " + TABLE_NAME + " SET " +
            CONFIG_CONTENT + "=?," +
            UPDATED + "=? WHERE " +
            SERVER_UUID + "=? AND " +
            CONFIG_CONTENT + "!=?";

    public SettingsTable(SQLDB db) {
        super(TABLE_NAME, db);
    }

    public static String createTableSQL(DBType dbType) {
        return CreateTableParser.create(TABLE_NAME, dbType)
                .column(ID, Sql.INT).primaryKey()
                .column(SERVER_UUID, Sql.varchar(39)).notNull().unique()
                .column(UPDATED, Sql.LONG).notNull()
                .column(CONFIG_CONTENT, "TEXT").notNull()
                .toString();
    }

    /**
     * Fetch a config that was placed into the database after a certain epoch ms.
     *
     * @param updatedAfter Epoch ms.
     * @param serverUUID   UUID of the server
     * @return Optional Config if a new config is found, empty if not.
     */
    public Optional<Config> fetchNewerConfig(long updatedAfter, UUID serverUUID) {
        String sql = "SELECT " + CONFIG_CONTENT + " FROM " + tableName +
                " WHERE " + UPDATED + ">? AND " +
                SERVER_UUID + "=? LIMIT 1";

        return Optional.ofNullable(query(new QueryStatement<Config>(sql, 10) {
            @Override
            public void prepare(PreparedStatement statement) throws SQLException {
                statement.setLong(1, updatedAfter);
                statement.setString(2, serverUUID.toString());
            }

            @Override
            public Config processResults(ResultSet set) throws SQLException {
                if (set.next()) {
                    try (ConfigReader reader = new ConfigReader(new Scanner(set.getString(CONFIG_CONTENT)))) {
                        return reader.read();
                    }
                } else {
                    return null;
                }
            }
        }));
    }
}
