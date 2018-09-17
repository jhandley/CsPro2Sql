CREATE OR REPLACE VIEW @SCHEMA.`r_religion` AS
    SELECT 
        `vs`.`VALUE` AS `RELIGION`, `i`.`INDIVIDUALS`
        FROM (SELECT @INDIVIDUAL_COLUMN_RELIGION, COUNT(0) AS `INDIVIDUALS` FROM @SCHEMA.@INDIVIDUAL_TABLE GROUP BY @INDIVIDUAL_COLUMN_RELIGION) `i`
		JOIN @SCHEMA.@VALUESET_RELIGION `vs` ON `i`.@INDIVIDUAL_COLUMN_RELIGION = `vs`.`ID`;