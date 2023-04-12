-- Endre adresse til adresselinje1
alter table mottaker
    rename column adresse to adresselinje1;

-- Legge til adresselinje 2 og 3
alter table mottaker
    add adresselinje2 text;
alter table mottaker
    add adresselinje3 text;

-- Legge til adressetype
alter table mottaker
    add adressetype text;

alter table mottaker
    add landkode text;
