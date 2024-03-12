ALTER TABLE trygdetid ADD COLUMN yrkesskade boolean NOT NULL default false, ADD COLUMN beregnet_samlet_trygdetid_norge BIGINT;

UPDATE trygdetid SET yrkesskade=true WHERE trygdetid_regelresultat ILIKE '%yrkesskade%';
