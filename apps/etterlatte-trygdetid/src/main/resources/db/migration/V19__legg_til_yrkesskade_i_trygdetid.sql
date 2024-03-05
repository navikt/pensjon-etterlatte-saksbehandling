ALTER TABLE trygdetid ADD COLUMN yrkesskade boolean NOT NULL default false;

UPDATE trygdetid SET yrkesskade=true WHERE trygdetid_regelresultat ILIKE '%yrkesskade%';
