-- Periods stored in string form - toString()/parse()

ALTER TABLE trygdetid
    ADD COLUMN faktisk_trygdetid_norge_total TEXT,
    ADD COLUMN faktisk_trygdetid_norge_antall_maaneder BIGINT,
    ADD COLUMN faktisk_trygdetid_teoretisk_total TEXT,
    ADD COLUMN faktisk_trygdetid_teoretisk_antall_maaneder BIGINT,
    ADD COLUMN fremtidig_trygdetid_norge_total TEXT,
    ADD COLUMN fremtidig_trygdetid_norge_antall_maaneder BIGINT,
    ADD COLUMN fremtidig_trygdetid_norge_opptjeningstid_maaneder BIGINT,
    ADD COLUMN fremtidig_trygdetid_norge_mindre_enn_fire_femtedeler BOOLEAN,
    ADD COLUMN fremtidig_trygdetid_teoretisk_total TEXT,
    ADD COLUMN fremtidig_trygdetid_teoretisk_antall_maaneder BIGINT,
    ADD COLUMN fremtidig_trygdetid_teoretisk_opptjeningstid_maaneder BIGINT,
    ADD COLUMN fremtidig_trygdetid_teoretisk_mindre_enn_fire_femtedeler BOOLEAN,
    ADD COLUMN samlet_trygdetid_norge BIGINT,
    ADD COLUMN samlet_trygdetid_teoretisk BIGINT,
    ADD COLUMN prorata_broek_teller BIGINT,
    ADD COLUMN prorata_broek_nevner BIGINT,
    ADD COLUMN trygdetid_tidspunkt TIMESTAMP,
    ADD COLUMN trygdetid_regelresultat TEXT;

UPDATE trygdetid SET
    samlet_trygdetid_norge = trygdetid_total,
    trygdetid_tidspunkt = trygdetid_total_tidspunkt,
    trygdetid_regelresultat = trygdetid_total_regelresultat;

-- Also remove to unused fields - trygdetid_nasjonal, trygdetid_fremtidig

ALTER TABLE trygdetid
    DROP COLUMN trygdetid_nasjonal,
    DROP COLUMN trygdetid_fremtidig,
    DROP COLUMN trygdetid_total_tidspunkt,
    DROP COLUMN trygdetid_total_regelresultat;
