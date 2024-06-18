-- Skru tilbake oppgaver som fikk feil status ved G-regulering

UPDATE oppgave
SET status  = 'ATTESTERING',
    merknad = (SELECT oppgavefoer ->> 'merknad'
               FROM oppgaveendringer
               WHERE oppgavefoer ->> 'status' = 'ATTESTERING'
                 AND oppgaveetter ->> 'status' = 'UNDER_BEHANDLING'
                 AND oppgaveid = '434586b1-ce99-439c-9e75-a14c3e3a12b8')
WHERE id = '434586b1-ce99-439c-9e75-a14c3e3a12b8';

INSERT INTO oppgaveendringer (id, oppgaveid, oppgavefoer, oppgaveetter, tidspunkt)
VALUES (gen_random_uuid(),
        '434586b1-ce99-439c-9e75-a14c3e3a12b8',
        (SELECT jsonb_set(oppgavefoer::jsonb, '{status}', '"UNDER_BEHANDLING"'::jsonb)
         FROM oppgaveendringer
         WHERE oppgaveid = '434586b1-ce99-439c-9e75-a14c3e3a12b8'
         ORDER BY tidspunkt DESC
         LIMIT 1),
        (SELECT jsonb_set(oppgaveetter::jsonb, '{status}', '"ATTESTERING"'::jsonb)
         FROM oppgaveendringer
         WHERE oppgaveid = '434586b1-ce99-439c-9e75-a14c3e3a12b8'
         ORDER BY tidspunkt DESC
         LIMIT 1),
        now());



UPDATE oppgave
SET status  = 'ATTESTERING',
    merknad = (SELECT oppgavefoer ->> 'merknad'
               FROM oppgaveendringer
               WHERE oppgavefoer ->> 'status' = 'ATTESTERING'
                 AND oppgaveetter ->> 'status' = 'UNDER_BEHANDLING'
                 AND oppgaveid = 'a9335637-1f41-47c7-8895-5523ab3dab42')
WHERE id = 'a9335637-1f41-47c7-8895-5523ab3dab42';


INSERT INTO oppgaveendringer (id, oppgaveid, oppgavefoer, oppgaveetter, tidspunkt)
VALUES (gen_random_uuid(),
        'a9335637-1f41-47c7-8895-5523ab3dab42',
        (SELECT jsonb_set(oppgavefoer::jsonb, '{status}', '"UNDER_BEHANDLING"'::jsonb)
         FROM oppgaveendringer
         WHERE oppgaveid = 'a9335637-1f41-47c7-8895-5523ab3dab42'
         ORDER BY tidspunkt DESC
         LIMIT 1),
        (SELECT jsonb_set(oppgaveetter::jsonb, '{status}', '"ATTESTERING"'::jsonb)
         FROM oppgaveendringer
         WHERE oppgaveid = 'a9335637-1f41-47c7-8895-5523ab3dab42'
         ORDER BY tidspunkt DESC
         LIMIT 1),
        now());

