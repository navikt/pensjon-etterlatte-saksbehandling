-- Persongalleri har blitt lagret med mellomrom i fnr som fører til feil flere steder
UPDATE grunnlagshendelse SET opplysning=replace(opplysning, ' ', '')
WHERE opplysning LIKE '% %'
AND opplysning_type = 'PERSONGALLERI_V1';
