ALTER TABLE avkorting_aarsoppgjoer ADD COLUMN innvilga_maaneder TEXT;

update avkorting_aarsoppgjoer set innvilga_maaneder = grunnlag.relevante_maaneder
from (select * from avkortingsgrunnlag) grunnlag
where avkorting_aarsoppgjoer.id = grunnlag.aarsoppgjoer_id;

ALTER TABLE avkorting_aarsoppgjoer  ALTER COLUMN innvilga_maaneder SET NOT NULL;
