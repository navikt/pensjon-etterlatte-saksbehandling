-- Behandling_resultat ble korrigert til AVBRUTT for disse ETTEROPPGJOER_FORBEHANDLING-behandlingene i Postgres
-- (jf. V75), men BigQuery sin inkrementelle synk plukket aldri opp endringen, fordi verken id eller
-- tidspunkt_registrert på den eksisterende raden ble endret av oppdateringen.
--
-- Denne migreringen setter derfor inn en ny rad med samme behandling_id og teknisk_tid som den korrigerte
-- raden (slik at statistikk sin dedup-nøkkel (behandling_id, teknisk_tid) beholdes), men med en ny, høyere
-- id og et nytt tidspunkt_registrert (default NOW()), slik at den blir synlig for BigQuery-synken på nytt.
--
-- DISTINCT ON (s.behandling_id) ... ORDER BY s.behandling_id, s.id DESC sikrer at kun én rad - den siste,
-- allerede korrigerte raden - settes inn per behandling_id, selv om flere historiske rader skulle matche filteret.
INSERT INTO sak (behandling_id, sak_id, mottatt_tid, registrert_tid, ferdigbehandlet_tid, vedtak_tid,
                 behandling_type, behandling_status, behandling_resultat, resultat_begrunnelse, behandling_metode,
                 opprettet_av, ansvarlig_beslutter, aktor_id, dato_foerste_utbetaling, teknisk_tid, sak_ytelse,
                 vedtak_loepende_fom, vedtak_loepende_tom, saksbehandler, ansvarlig_enhet,
                 soeknad_format, sak_utland, beregning, sak_ytelsesgruppe, avdoede_foreldre, revurdering_aarsak,
                 avkorting, pesysid, kilde, relatert_til, paa_vent_aarsak, sak_utland_enhet)
SELECT DISTINCT ON (s.behandling_id)
    s.behandling_id, s.sak_id, s.mottatt_tid, s.registrert_tid, s.ferdigbehandlet_tid, s.vedtak_tid,
    s.behandling_type, s.behandling_status, s.behandling_resultat, s.resultat_begrunnelse, s.behandling_metode,
    s.opprettet_av, s.ansvarlig_beslutter, s.aktor_id, s.dato_foerste_utbetaling, s.teknisk_tid, s.sak_ytelse,
    s.vedtak_loepende_fom, s.vedtak_loepende_tom, s.saksbehandler, s.ansvarlig_enhet,
    s.soeknad_format, s.sak_utland, s.beregning, s.sak_ytelsesgruppe, s.avdoede_foreldre, s.revurdering_aarsak,
    s.avkorting, s.pesysid, s.kilde, s.relatert_til, s.paa_vent_aarsak, s.sak_utland_enhet
FROM sak s
WHERE s.behandling_id IN (
    '0448c584-b273-41e5-8210-cba47504cf2a', '0d5f17ef-8f2f-4482-9745-6db3be4ce884', '0e7b86a9-cec7-4589-9df5-e8da430c2694', '10171830-c0fe-4769-b435-33814cf7b39d',
    '13c7c0fb-2311-4c6d-9f17-f544cacb5367', '154a4c6a-89c2-49bf-8ceb-f1a30c28f1c0', '1624050b-e665-4965-a91c-31c914c953fc', '16e101e2-e3f9-4a34-a7a0-83ef726336aa',
    '17148f6f-d917-46b3-92f2-6ac2fbb93d16', '180655ed-29af-45f6-aac3-a71df4696c85', '257ff5b9-b338-47cb-9682-9e605d45e7df', '2912f317-93dd-454e-8ce5-e71613ef98bf',
    '2dc37478-1a37-4b43-84cf-dd02f1b63896', '2f53f689-8e96-419b-a622-4cfa15c85af4', '31aef78d-9bec-481d-aacb-d56314992f06', '35bf1edb-19ae-4f55-b3f4-3aef21b1bcc1',
    '3698ea73-89b5-4d27-8125-55e9cf53ab66', '36f40b8f-a500-4f00-9f98-b8e1eb5964b0', '393e4e40-6eb1-4daa-8f61-a4388d20bf26', '3a08c258-4db4-4d2e-a89e-4b13a1cc1ff7',
    '3a1a7dbb-6a9a-4241-b6d4-bd86c21a26cb', '3c74f111-376f-48a7-8796-f282a46072df', '3dc8d2ca-5e12-4f1f-bc10-98c83527042b', '41a67ae3-d6f8-4b66-951c-78c1b33e04d1',
    '45e80c4b-085c-47a4-8ccd-06989df928e1', '47b1d217-799a-4844-b063-e1557f03783a', '49b7d867-3544-4bc7-829b-20dc60c1d24a', '4c021e96-3200-4bb9-9eec-d440c6a11714',
    '4c3a7909-2213-4f83-8ccb-73889ba80b97', '4e4793c5-22ff-4a9b-bb9b-50f5e888b7da', '51197734-72e7-4a95-a7c6-c025570c2daf', '516bb3c4-f239-4ba3-bda4-c909a94f1d86',
    '52aa2f15-0627-4ea3-9d33-29b6f1406c71', '52f2a685-2ebc-4cf8-9701-04b587ef8aee', '56ccd64a-5454-41a2-9063-3cb1a6492eb1', '5a26d9c5-9af1-41d2-9379-345a12dced82',
    '5b86e2e6-0567-4533-bf49-b1d9bf1888f5', '5e8c88f3-8739-4041-95da-b596005c9f9b', '5e9b8200-ece1-4d7f-bf4a-a6c21caa3b34', '5fedeb6b-5448-4e9a-9389-81c584e18c35',
    '6181a369-d70a-4d2d-91f3-42ded23127e8', '6199e378-85e7-4c3f-9b35-ba2cbade3d12', '63e5e4da-d8d0-42e8-aeef-262a800d569b', '64af66df-e00b-429e-a7bb-c963e31fce6d',
    '6aad023c-ceb4-4cb1-b528-2b02f75f522f', '6b62340e-d1ba-45fa-8ad8-8505567e8339', '6e2e8264-ba73-4aad-b0d5-fa227351aff9', '74522710-f464-46d4-8893-4f058511e6be',
    '7583f058-5021-44e9-b02b-3faea07fc2fe', '789c6afe-9b84-447e-8f25-60956f61a321', '7cc66e53-fec1-4227-bebe-08d67adb9bb7', '7f941a90-cf5c-4943-9d99-a652dd025e69',
    '807648fb-1a04-4542-ac67-4dbfa740f162', '85af96dd-7850-4bb9-b4eb-da21d5c55fd3', '87e65489-e773-4399-b986-324f02ea067a', '8b620159-28b2-4a8c-8b7b-cd285484a8e4',
    '8ef9bee9-b848-4dcf-8115-778318473d14', '93f68bab-7840-42d8-a236-99a1730e6d49', '94850f2d-2862-40bb-af68-39f34b8b2e46', '95f24918-27a4-4603-9be1-4076bcc66971',
    '96981ea9-c650-43a0-ab2d-b51e90c18364', '997a1592-c8fd-4d1a-8fa0-0e62a9634df0', '9ab5983e-315c-4896-9026-de347c3f3182', 'a113bf5c-36ee-47d2-ba94-3b8ade859c22',
    'a17e1945-d7b6-435a-a1d3-b06db549c5cb', 'a238d05f-9483-4d9c-a604-4e062104bd1f', 'a4c15c27-2f32-45ff-ad80-cdcebf534cc6', 'a7a96077-b69b-4ebe-9e05-4d234e0e982c',
    'a9b38766-efd2-433c-8447-0e1bb8674f6a', 'aaa669c2-e3b0-4687-990c-74036ac4500e', 'abcfa2dd-aef7-4271-b596-3ef91ec9226c', 'ac664345-ac66-489b-8407-fbda7b3738d2',
    'ac807299-f8d3-4180-9f59-951e3f149296', 'ac9b14ae-74df-4da9-bad5-a8b052ae4bd3', 'aced5699-6f1f-476f-989e-a55863110d44', 'aedecb4a-1935-4d9d-a590-d3ab7aa6fdb8',
    'afb32f37-b398-40ef-b009-35e1fb900c8e', 'aff87ff4-ffdc-4647-bf04-172d1daa1fec', 'b2ffed54-b512-44a6-847e-adec1c0cf8bb', 'b55dbe2a-5ec6-4090-a565-d2879ae30d50',
    'b5e11d52-409f-4a57-981f-9dfe847e23d4', 'b9ce9774-e0fc-40c8-b3a9-6cdda9a75401', 'bab666af-9eb5-4ddc-aca0-a72cab05a80f', 'bb05b3b4-82f3-439b-9716-80088c9cc990',
    'c080204d-66ae-42d8-b3ac-e2ddbf6e03e1', 'c2c79c00-e5c2-40c4-8f99-4297acc221fa', 'c508f6de-f87a-4106-ad70-cd29a73e6b8d', 'c904af5c-3200-45e6-a4ac-60b008e4df86',
    'cc86a83b-6c39-401e-a744-20245940040c', 'cd8504f0-d2da-493c-8991-37a7487ee6cc', 'ce8fd341-7273-4520-a269-cd6004689cec', 'd959806a-e0d2-4212-938e-6b5b4ef93193',
    'db5461d8-3288-4181-9914-b76657c7d4fd', 'db85acda-3808-4933-a92e-1661bfd1990c', 'df433ce4-f25f-4e0c-8e02-9979c69dfb9b', 'e176f652-a562-4fd5-8546-0d8787f2dc39',
    'ea30ffe3-6734-4220-8ab7-cefa349e6a72', 'ed07357f-414e-440e-94ee-e51289555a0a', 'efd8c41c-aa0d-4853-9e50-3b500bc3bcf5', 'f16ecf51-5a8e-4f88-902d-f4db9eaa0948',
    'f2b3489f-dc6a-4982-8357-48a806ee6b85', 'f4236d6f-e3ee-4ffa-95da-db631e27a5be', 'f438bdb7-1b4e-44fb-a56a-70741d5ffca6', 'f8f7406a-8ce6-4a82-bcf2-16a36aa700c2',
    'f9b0171b-c9e0-44c3-837e-6b2cfa3597b1', 'fc74ea33-d6c4-42d2-af6f-5ffae585f7dd', 'fcdf3400-61c7-46f2-a9f2-27e55c169530', 'ff043f98-eb7f-48e1-bd24-7ba8673de518'
)
  AND s.behandling_type = 'ETTEROPPGJOER_FORBEHANDLING'
  AND s.behandling_status = 'AVBRUTT'
  AND s.behandling_resultat = 'AVBRUTT'
ORDER BY s.behandling_id, s.id DESC;
