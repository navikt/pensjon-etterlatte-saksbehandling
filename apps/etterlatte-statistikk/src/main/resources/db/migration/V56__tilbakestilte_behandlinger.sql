CREATE TABLE tilbakestilte_behandlinger
(
    behandling_id UUID,
    ryddet        BOOL DEFAULT FALSE
);

-- BehandlingId=er som er kun kjent av statistikk
INSERT INTO tilbakestilte_behandlinger (behandling_id)
VALUES ('cfb92e15-53b7-4dfe-961c-e22fa30de43e'),
       ('bed8bac9-344c-4691-bb83-9816f50a2c8f'),
       ('370b00fb-012d-461b-9e2a-8a238bfa38a4'),
       ('3aff1147-732f-427b-a06e-9229b1e27c5a'),
       ('e963cab0-422c-4981-8fdf-ee82fa4ae844'),
       ('12840b5d-0fed-4523-b098-ab0da9046e34'),
       ('030bc00b-8859-44eb-8d13-7703bddd4bff'),
       ('862dee07-b5f0-4036-88f4-c3dda5db1a89'),
       ('66e5e4d2-8bbc-4217-a041-b60953076a1e'),
       ('f8a599aa-858f-4ad2-a969-67d7f601788a'),
       ('a647e407-6553-470f-896b-a34a32180dc5'),
       ('68d64afd-f66e-4cac-a85b-5e04aa30a374'),
       ('73630bd6-687b-40e6-88f4-d79164a7ebc8'),
       ('7bb9eb98-1037-4e65-a7a4-846d28f6e782'),
       ('4a9ee48a-5b34-4754-9392-cd10b574af07'),
       ('768e6667-d91a-4d9c-ab1c-462b5f7d00ad'),
       ('7d08b0c1-dfae-416e-8a74-a6b50ce973d3'),
       ('12e09151-c523-48cc-8f56-b6abbfe35801'),
       ('f179eeae-afe9-4dad-8b6b-a9855518cdf6'),
       ('9b6cfebe-6886-498c-8a02-0c3dc418f260'),
       ('b55b5ade-7569-44e5-ae67-42c5a6891277'),
       ('4434ee40-cd7e-42b4-99e9-8d19a25cdd94'),
       ('8a9f5673-8357-4c7a-8abc-3b7e3b97079c'),
       ('20c99970-8e74-4b0c-a17c-0a50e0dae34a'),
       ('664ecf4e-2040-48f3-8cae-7fbb72f3e186'),
       ('46967197-a3e1-47b9-ad51-ecb34f5c7100'),
       ('cb2db92a-749e-4404-85a3-5732c363894a'),
       ('5d320287-fa80-48d7-b56e-d5276a45f39e'),
       ('0d42131a-04d9-4af0-b065-78b909348faa'),
       ('eba2dbfa-f64e-45c7-bee6-af129a45c7fd'),
       ('9ef210a6-06c8-4752-b8af-7a06ee7fbfc3'),
       ('ea476e80-1155-4149-a7a4-3982d00f5e60'),
       ('ca0ceb53-67f0-4708-9bb9-89d931172299'),
       ('23384ca2-a272-4376-b3bd-090115d7a79d'),
       ('181e0c44-8111-49b8-b7cc-1f12c6af9e94'),
       ('0af608a4-2d25-4d06-bb75-45087dc0dac7'),
       ('fc245b2c-c0aa-408c-914f-481d67892824'),
       ('76c6cda4-5c97-43a6-90ba-237a5efd4714'),
       ('cc242066-f31e-45f0-9267-76f2a5883dce'),
       ('4c8b5b64-1c40-447b-b30f-65d66fecc398'),
       ('98e4d62a-1710-4ac9-bc7e-7bba49b024b7'),
       ('9a398f25-ba97-4ae7-9ae2-ce5123e164bc'),
       ('06fdfe41-b6ef-478d-94a3-ec9953f0c96f'),
       ('44040aa9-998d-4004-a2c3-c375188173ef'),
       ('5a3bfcef-2ee9-4a9c-99aa-02db6f96d698'),
       ('d9f93f5a-2447-4dff-be71-a624d6e516a6'),
       ('82abaf33-178b-4fde-8942-7dee5a9a48cc'),
       ('9aea9cf7-0594-4f53-9655-d25989411b9d'),
       ('32575ac2-0700-4b88-9a02-a7881d7f14fe'),
       ('c713186e-98f4-41da-9e6f-feb217f46670'),
       ('6c897e62-2b0b-4eb9-b0c5-41a14d607aac'),
       ('6b0f61c2-2f46-4210-920d-07da783cbfc1'),
       ('f03542ea-3c41-42ef-9589-3b9f9b197449'),
       ('fdf25f89-6c69-4a5b-ab0d-8187ec1d6728'),
       ('510bd6a1-1b52-480e-b490-bae9165f0f17'),
       ('de7c759c-0987-4b90-a1f6-294c6687f3e7'),
       ('21ca9a81-e502-4bed-96ad-58623edc7583'),
       ('1da75d47-654b-4e1c-9645-bd344619fe71'),
       ('59853173-925e-4c91-83c7-c0bf39ff4a8f'),
       ('50493c7f-4203-459a-ba1c-e35ae908aefc'),
       ('aeef6ded-0d0d-4aab-9811-25050cc9831f'),
       ('4bb264fa-aba4-4468-b993-ae93fffcc052'),
       ('f780ff80-4984-491d-a933-e2fa36a25726'),
       ('13978f50-e3b6-47a3-a15f-7ac2af09e2cb'),
       ('837a5107-ff28-42ca-b7f2-4a47b84cc2e2'),
       ('5e2996cf-32b7-4212-9ed2-298f83d47e08'),
       ('941297ff-fc02-4db2-b974-8585ddb7463c'),
       ('535a04a7-d6d4-46f0-863c-730134344b6d'),
       ('590c5487-b338-4d65-919f-dc446d922214'),
       ('9abb2fe7-654f-45e8-8868-47319f2e553f'),
       ('ad29e31b-6cd4-4d66-8fab-156aa25a356b'),
       ('8d7f53cb-9670-45d5-939d-18b4f7a12f23'),
       ('ee0e5d5e-5685-420b-8010-eae56d6ead84'),
       ('a67a7f57-d028-4ef6-b1f7-df0a1898e7a1'),
       ('f3fb1573-ebe0-4a83-ba5c-45319591ddba'),
       ('7969229f-04d7-4885-8889-041b12953af6');
