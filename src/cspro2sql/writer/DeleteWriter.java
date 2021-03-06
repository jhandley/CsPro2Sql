package cspro2sql.writer;

import cspro2sql.bean.Answer;
import cspro2sql.bean.Dictionary;
import cspro2sql.bean.Item;
import cspro2sql.bean.Questionnaire;
import cspro2sql.bean.Record;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

/**
 * Copyright 2017 ISTAT
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence. You may
 * obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * Licence for the specific language governing permissions and limitations under
 * the Licence.
 *
 * @author Guido Drovandi <drovandi @ istat.it>
 * @author Mauro Bruno <mbruno @ istat.it>
 * @version 0.9
 */
public class DeleteWriter {

    public static void create(String schema, Dictionary dictionary, Questionnaire quest, Statement stmt) throws SQLException {
        create(schema, dictionary, quest, stmt, null);
    }

    public static void create(String schema, Dictionary dictionary, Questionnaire quest, Statement stmt, StringBuilder script) throws SQLException {
        int id = 0;
        Record mainRecord = null;
        for (Map.Entry<Record, List<List<Answer>>> e : quest.getMicrodataSet()) {
            Record record = e.getKey();

            if (record.isMainRecord()) {
                mainRecord = record;
                String selectSql = "select ID from " + schema + "." + record.getTableName() + " where ";
                int i = 0;
                boolean first = true;
                for (Item item : record.getItems()) {
                    Answer value = e.getValue().get(0).get(i++);
                    if (first) {
                        first = false;
                    } else {
                        selectSql += " AND ";
                    }
                    if (value.getValue() == null) {
                        selectSql += item.getName() + " is null";
                    } else {
                        selectSql += item.getName() + "='" + value.getValue() + "'";
                    }
                }
                try (ResultSet executeQuery = stmt.executeQuery(selectSql)) {
                    if (executeQuery.next()) {
                        id = executeQuery.getInt(1);
                    } else {
                        return;
                    }
                }
            } else {
                if (script != null) {
                    script.append("delete from ").append(schema).append(".").append(record.getTableName()).append(" where ").append(record.getMainRecord().getName()).append("=").append(id).append(";\n");
                }
                stmt.executeUpdate("delete from " + schema + "." + record.getTableName() + " where " + record.getMainRecord().getName() + "=" + id);
            }
        }

        if (script != null) {
            script.append("delete from ").append(schema).append(".").append(mainRecord.getTableName()).append(" where ID = ").append(id).append(";\n");
        }
        stmt.executeUpdate("delete from " + schema + "." + mainRecord.getTableName() + " where ID = " + id);
    }

}
