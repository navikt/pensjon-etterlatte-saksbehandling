DO
$do$
    BEGIN
        IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname='vilkaar-api') THEN
            GRANT SELECT ON ALL TABLES IN SCHEMA public TO "vilkaar-api";
        END IF;
    END
$do$