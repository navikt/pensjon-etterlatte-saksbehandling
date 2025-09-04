update behandling
set status = 'IVERKSATT'
where sak_id in (23574, 24006, 22642)
  and id in ('0d7da2f7-c990-43d0-9567-56d59ff0d1e9',
             'a618152d-65ef-4406-b64e-d562b8c08356',
             '3422597f-1794-4f2c-b98e-74eff527af37')
  and status = 'SAMORDNET';


-- Legger også inn en hendelse
insert
into behandlinghendelse (hendelse, opprettet, inntruffet, vedtakid, behandlingid, sakid, ident, identtype,
                         kommentar, valgtbegrunnelse)
VALUES ('VEDTAK:IVERKSATT', (select opprettet + interval '1 minute'
                             from behandlinghendelse
                             where hendelse = 'VEDTAK:SAMORDNET'
                               and sakid = 23574
                               and behandlingid = '0d7da2f7-c990-43d0-9567-56d59ff0d1e9'),
        (select opprettet + interval '1 minute'
         from behandlinghendelse
         where hendelse = 'VEDTAK:SAMORDNET'
           and sakid = 23574
           and behandlingid = '0d7da2f7-c990-43d0-9567-56d59ff0d1e9'), 60104,
        '0d7da2f7-c990-43d0-9567-56d59ff0d1e9', 23574, null, null, 'Manuelt registrert hendelse', null);
-- Legger også inn en hendelse
insert
into behandlinghendelse (hendelse, opprettet, inntruffet, vedtakid, behandlingid, sakid, ident, identtype,
                         kommentar, valgtbegrunnelse)
VALUES ('VEDTAK:IVERKSATT', (select opprettet + interval '1 minute'
                             from behandlinghendelse
                             where hendelse = 'VEDTAK:SAMORDNET'
                               and sakid = 22642
                               and behandlingid = '3422597f-1794-4f2c-b98e-74eff527af37'),
        (select opprettet + interval '1 minute'
         from behandlinghendelse
         where hendelse = 'VEDTAK:SAMORDNET'
           and sakid = 22642
           and behandlingid = '3422597f-1794-4f2c-b98e-74eff527af37'), 59374,
        '3422597f-1794-4f2c-b98e-74eff527af37', 22642, null, null, 'Manuelt registrert hendelse', null);
insert
into behandlinghendelse (hendelse, opprettet, inntruffet, vedtakid, behandlingid, sakid, ident, identtype,
                         kommentar, valgtbegrunnelse)
VALUES ('VEDTAK:IVERKSATT', (select opprettet + interval '1 minute'
                             from behandlinghendelse
                             where hendelse = 'VEDTAK:SAMORDNET'
                               and sakid = 24006
                               and behandlingid = 'a618152d-65ef-4406-b64e-d562b8c08356'),
        (select opprettet + interval '1 minute'
         from behandlinghendelse
         where hendelse = 'VEDTAK:SAMORDNET'
           and sakid = 24006
           and behandlingid = 'a618152d-65ef-4406-b64e-d562b8c08356'), 60208,
        'a618152d-65ef-4406-b64e-d562b8c08356', 24006, null, null, 'Manuelt registrert hendelse', null);


-- 60104	23574	0d7da2f7-c990-43d0-9567-56d59ff0d1e9	SAMORDNET
-- 59374	22642	3422597f-1794-4f2c-b98e-74eff527af37	SAMORDNET
-- 60208	24006	a618152d-65ef-4406-b64e-d562b8c08356	SAMORDNET
