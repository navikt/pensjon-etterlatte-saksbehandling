-- Dersom videreført opphør settes til Nei, skal dato være null
alter table viderefoert_opphoer
    alter column dato drop not null;
