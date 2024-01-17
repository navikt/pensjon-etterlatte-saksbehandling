-- Oppdaterer behandlinger i produksjon som ble avslått før status AVSLAG fantes på behandling

update behandling
set status = 'AVSLAG'
where id in (
    'a73c9ddb-3863-48f0-bcb2-f72ba82c0ab0',
    'bc196a69-6430-45b5-9575-abd293385c48',
    '1eeef019-5924-4810-a54e-8c9bac89ec36',
    'b14d8d50-da10-4f36-b8c0-dd4d8f824e7f',
    'e2689390-dced-4b89-84df-69f8a33582ea'
);

update behandlinghendelse
set hendelse = 'VEDTAK:AVSLAG'
where behandlingid in (
    'a73c9ddb-3863-48f0-bcb2-f72ba82c0ab0',
    'bc196a69-6430-45b5-9575-abd293385c48',
    '1eeef019-5924-4810-a54e-8c9bac89ec36',
    'b14d8d50-da10-4f36-b8c0-dd4d8f824e7f',
    'e2689390-dced-4b89-84df-69f8a33582ea'
) and hendelse in ('VEDTAK:ATTESTERT');