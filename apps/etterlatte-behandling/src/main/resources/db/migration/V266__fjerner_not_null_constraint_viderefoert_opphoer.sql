-- Dersom videreført opphør settes til Nei, skal dato være null
alter table viderefoert_opphoer
    alter column dato drop not null;

-- Endrer fra string-verdien "null" til faktisk null
update viderefoert_opphoer set dato = null where dato = 'null';
