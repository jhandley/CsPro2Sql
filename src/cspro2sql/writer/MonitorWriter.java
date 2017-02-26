package cspro2sql.writer;

import cspro2sql.sql.TemplateManager;
import java.io.IOException;
import java.io.PrintStream;

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
 * @version 0.9.1
 */
public class MonitorWriter {

    private static final String[] TEMPLATES = new String[]{
        "r_questionnaire_info",
        "r_individual_info",
        "r_religion",
        "r_sex_by_age",
        "r_sex_by_region"
    };

    public static boolean write(String schema, TemplateManager tm, PrintStream out) {
        out.println("USE " + schema + ";");
        out.println();

        String[] ea = tm.getEa();
        String[] eaName = tm.getEaName();
        String[] eaDescription = tm.getEaDescription();
        int[] ageRange = tm.getAgeRange();

        try {
            tm.printTemplate("c_user", out);
            tm.printTemplate("cspro2sql_report", out);
        } catch (IOException ex) {
            return false;
        }

        try {
            for (String template : TEMPLATES) {
                if (tm.printTemplate(template, out)) {
                    printMaterialized(schema, template, out);
                }
            }
        } catch (IOException ex) {
            return false;
        }

        if (ea != null) {
            out.println("CREATE OR REPLACE VIEW " + schema + ".`r_regional_area` AS");
            String groupBy = ea[0];
            String name = eaName[0];
            out.print("  SELECT '" + name + "' name, COUNT(0) value FROM (SELECT COUNT(0) FROM " + schema + "." + tm.getParam("@QUESTIONNAIRE_TABLE") + " GROUP BY " + groupBy + ") a0");
            for (int i = 1; i < ea.length; i++) {
                name = eaName[i];
                groupBy += "," + ea[i];
                out.println(" UNION");
                out.print("  SELECT '" + name + "', COUNT(0) FROM (SELECT COUNT(0) FROM " + schema + "." + tm.getParam("@QUESTIONNAIRE_TABLE") + " GROUP BY " + groupBy + ") a" + i);
            }
            out.println();
            out.println(";");
            printMaterialized(schema, "r_regional_area", out);
        }

        if (ageRange != null && tm.getParam("@INDIVIDUAL_TABLE") != null && tm.getParam("@INDIVIDUAL_COLUMN_SEX") != null
                && tm.getParam("@INDIVIDUAL_VALUE_SEX_MALE") != null && tm.getParam("@INDIVIDUAL_VALUE_SEX_FEMALE") != null
                && tm.getParam("@INDIVIDUAL_COLUMN_AGE") != null) {
            out.println("CREATE OR REPLACE VIEW " + schema + ".`r_sex_by_age_group` AS");
            out.print("  SELECT '" + ageRange[0] + " to " + (ageRange[1] - 1) + "' as 'range', a.male, b.female FROM "
                    + "(SELECT COUNT(0) male FROM " + schema + "." + tm.getParam("@INDIVIDUAL_TABLE") + " WHERE " + tm.getParam("@INDIVIDUAL_COLUMN_SEX") + " = " + tm.getParam("@INDIVIDUAL_VALUE_SEX_MALE") + " AND " + tm.getParam("@INDIVIDUAL_COLUMN_AGE") + " >= " + ageRange[0] + " AND " + tm.getParam("@INDIVIDUAL_COLUMN_AGE") + " < " + ageRange[1] + ") a,"
                    + "(SELECT COUNT(0) female FROM " + schema + "." + tm.getParam("@INDIVIDUAL_TABLE") + " WHERE " + tm.getParam("@INDIVIDUAL_COLUMN_SEX") + " = " + tm.getParam("@INDIVIDUAL_VALUE_SEX_FEMALE") + " AND " + tm.getParam("@INDIVIDUAL_COLUMN_AGE") + " >= " + ageRange[0] + " AND " + tm.getParam("@INDIVIDUAL_COLUMN_AGE") + " < " + ageRange[1] + ") b");
            for (int i = 1; i < ageRange.length - 1; i++) {
                out.println(" UNION");
                out.print("  SELECT '" + ageRange[i] + " to " + (ageRange[i + 1] - 1) + "' as 'range', a.male, b.female FROM "
                        + "(SELECT COUNT(0) male FROM " + schema + "." + tm.getParam("@INDIVIDUAL_TABLE") + " WHERE " + tm.getParam("@INDIVIDUAL_COLUMN_SEX") + " = " + tm.getParam("@INDIVIDUAL_VALUE_SEX_MALE") + " AND " + tm.getParam("@INDIVIDUAL_COLUMN_AGE") + " >= " + ageRange[i] + " AND " + tm.getParam("@INDIVIDUAL_COLUMN_AGE") + " < " + ageRange[i + 1] + ") a,"
                        + "(SELECT COUNT(0) female FROM " + schema + "." + tm.getParam("@INDIVIDUAL_TABLE") + " WHERE " + tm.getParam("@INDIVIDUAL_COLUMN_SEX") + " = " + tm.getParam("@INDIVIDUAL_VALUE_SEX_FEMALE") + " AND " + tm.getParam("@INDIVIDUAL_COLUMN_AGE") + " >= " + ageRange[i] + " AND " + tm.getParam("@INDIVIDUAL_COLUMN_AGE") + " < " + ageRange[i + 1] + ") b");
            }
            out.println();
            out.println(";");
            printMaterialized(schema, "r_sex_by_age_group", out);
        }

        if (ea != null) {
            out.println("CREATE OR REPLACE VIEW " + schema + ".`r_household_by_ea` AS");
            out.print("  SELECT concat(");
            out.print("'" + eaName[0] + "'");
            for (int i = 1; i < eaName.length; i++) {
                out.print(",'#','" + eaName[i] + "'");
            }
            out.println(") as name, null as household");
            out.println("  UNION");
            out.print("  SELECT concat(");
            if (eaDescription[0] == null || eaDescription[0].isEmpty()) {
                out.print("h." + ea[0]);
            } else {
                out.print("vs0.value");
            }
            for (int i = 1; i < ea.length; i++) {
                if (eaDescription[i] == null || eaDescription[i].isEmpty()) {
                    out.print(",'#',h." + ea[i]);
                } else {
                    out.print(",'#',vs" + i + ".value");
                }
            }
            out.println(") as name, COUNT(0) AS `household`");
            out.println("  FROM " + schema + "." + tm.getParam("@QUESTIONNAIRE_TABLE") + " `h`");
            for (int i = 0; i < ea.length; i++) {
                if (eaDescription[i] != null && !eaDescription[i].isEmpty()) {
                    out.println("    JOIN " + schema + "." + eaDescription[i] + " vs" + i + " ON `h`.`" + ea[i] + "` = vs" + i + ".`ID`");
                }
            }
            out.print("  GROUP BY ");
            out.print("`h`." + ea[0]);
            for (int i = 1; i < ea.length; i++) {
                out.print(", `h`." + ea[i]);
            }
            out.println(";");
            printMaterialized(schema, "r_household_by_ea", out);
        }

        return true;
    }

    private static void printMaterialized(String schema, String name, PrintStream out) {
        printMaterialized(schema, name, false, out);
    }

    private static void printMaterialized(String schema, String name, boolean noId, PrintStream out) {
        out.println("DROP TABLE IF EXISTS " + schema + ".m" + name + ";");
        if (noId) {
            out.println("CREATE TABLE " + schema + ".m" + name + " AS SELECT " + name + ".* FROM " + schema + "." + name + ";");
        } else {
            out.println("SELECT @ID := 0;");
            out.println("CREATE TABLE " + schema + ".m" + name + " (PRIMARY KEY (ID)) AS SELECT @ID := @ID + 1 ID, " + name + ".* FROM " + schema + "." + name + ";");
        }
        out.println("INSERT INTO " + schema + ".`cspro2sql_report` VALUES ('" + name + "');");
        out.println();
    }

}
