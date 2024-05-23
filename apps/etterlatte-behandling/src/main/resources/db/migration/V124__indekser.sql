CREATE INDEX on oppgaveendringer (oppgaveid);

ALTER TABLE sjekkliste_item RENAME COLUMN sjekkliste TO sjekkliste_id;

CREATE INDEX on sjekkliste_item (sjekkliste_id);
