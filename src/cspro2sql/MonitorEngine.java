package cspro2sql;

import cspro2sql.bean.Dictionary;
import cspro2sql.reader.DictionaryReader;
import cspro2sql.writer.MonitorWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author Istat Cooperation Unit
 */
public class MonitorEngine {

    public static void main(String[] args) throws Exception {
        Properties prop = new Properties();
        try (InputStream in = MonitorEngine.class.getResourceAsStream("/database.properties")) {
            prop.load(in);
        } catch (IOException ex) {
            return;
        }
        try {
            Dictionary dictionary = DictionaryReader.read(
                    prop.getProperty("dictionary.filename"),
                    prop.getProperty("db.dest.table.prefix"));
            execute(dictionary, prop, System.out);
        } catch (Exception ex) {
            System.exit(1);
        }
    }

    static boolean execute(Dictionary dictionary, Properties prop, PrintStream out) {
        String schema = prop.getProperty("db.dest.schema");
        String[] ea = prop.getProperty("column.questionnaire.ea").split(",");
        String[] eaName = prop.getProperty("column.questionnaire.ea.name").split(",");
        String[] ageRangeS = prop.getProperty("range.individual.age").split(",");
        int[] ageRange = new int[ageRangeS.length];
        for (int i = 0; i < ageRangeS.length; i++) {
            ageRange[i] = Integer.parseInt(ageRangeS[i]);
        }

        Map<String, String> params = new HashMap<>();
        params.put("@SCHEMA", schema);
        params.put("@QUESTIONNAIRE_TABLE", prop.getProperty("table.questionnaire"));
        params.put("@QUESTIONNAIRE_COLUMN_BASE", dictionary.getMainRecord().getName());
        params.put("@QUESTIONNAIRE_COLUMN_REGION", prop.getProperty("column.questionnaire.region"));
        params.put("@INDIVIDUAL_TABLE", prop.getProperty("table.individual"));
        params.put("@INDIVIDUAL_COLUMN_SEX", prop.getProperty("column.individual.sex"));
        params.put("@INDIVIDUAL_COLUMN_AGE", prop.getProperty("column.individual.age"));
        params.put("@INDIVIDUAL_COLUMN_RELIGION", prop.getProperty("column.individual.religion"));
        params.put("@INDIVIDUAL_VALUE_SEX_MALE", prop.getProperty("value.individual.sex.male"));
        params.put("@INDIVIDUAL_VALUE_SEX_FEMALE", prop.getProperty("value.individual.sex.female"));
        params.put("@VALUESET_REGION", prop.getProperty("valueset.region"));
        params.put("@VALUESET_SEX", prop.getProperty("valueset.sex"));
        params.put("@VALUESET_RELIGION", prop.getProperty("valueset.religion"));

        return MonitorWriter.write(schema, ea, eaName, ageRange, params, out);
    }

}
