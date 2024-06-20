-- Setter opphoer fra og med på alle behandling som har opphørsvetdak som ble opprettet før feltet ble innført
UPDATE behandling
SET opphoer_fom='"' || subquery.virk || '"'
FROM (
select id, virkningstidspunkt::jsonb->>'dato' as virk from behandling
-- Ider hentet ut med samme spørring som i V28__populer_opphoer_fom.sql i vedtaksvurdering
where id IN (
             'd0306b37-f10b-4295-aa89-f9f99c32204a', 'faae6730-286d-4fc1-8169-48e3797a835c', 'f6da8397-f164-45f9-865f-111d75642b05', 'c7d7e1b4-5f14-469e-95d4-4fa4ee769982', '5d8a5edb-6f02-4e1c-9118-206959af5f26', 'f42bc3b9-7d94-4387-bc40-1eebaee134f3', '8510d236-0c05-453b-a06e-ae3cbd21223d', '7ea520c5-779a-4c7c-81b1-ef6ad8328b8c', 'e450e85b-c4bf-41f7-b8a9-2287005dafd4', '224f505a-7fd8-4dd6-bc9d-264cb58c67a4', '7eb0ba8f-bf51-460f-90b7-bfbadb639146', 'c6e59c5b-6f8c-4f38-a34a-7e10b8e31338', 'd6793dfa-c486-4ad9-a520-cb64804700e0', '80a311f4-1d68-4c5c-8033-cafd89e47bf3', 'd8c94a88-7b38-4921-8350-618086f09fe0', '080549c3-dfb9-4c91-b1d2-16c503345bcc', 'cea81d06-22eb-4498-abaa-70f8b145bf43', 'c0fb024f-f36e-419b-b0b2-1b86cefb776f', '7c9c26b1-50ce-41f3-ab0e-ba703ac7454f', '94b3c264-0a35-4d20-a832-5c0cb31d1a58', '225bb1ac-571e-4bc5-a320-6bd9440dadc6', '5fcddbb7-9705-46ae-b96d-346efa93b746', 'ac6a4732-672b-45a3-a289-41386dc52c77', '99ca49ca-d33a-49d8-a51b-61ec50a62142', '4e962f12-9c69-4218-aba6-07e8821962f3', '562197b4-4e04-4ee1-8438-b99cc262f558', 'afbc82b8-c137-433a-8e36-96288a1b3c05', '8371a9b7-7d63-43d4-8cc9-9df96babb30c', 'adfda7b8-b299-4136-a521-d44864c84e96', '1629035a-ab5d-4b61-908e-b7efd4e44f59', '7f6bef66-7685-4fe7-8bd0-5fb219a704e5', '4bda320b-158a-4cb7-ba0e-b9418cb4cf27', 'b0aac73a-d554-4fd2-aa63-0bb91b476ee0', 'b2e7ea56-1267-407e-bd93-3121f5a05a9e', '1ebff7a5-9a8f-48c7-beee-4fe7673022f5', '420dd44f-ce07-4eb0-994e-c0eb1dc7de04', '0dc2add5-b687-4489-9874-94ab06b47dfa', 'e2401406-9df6-42e0-a0d9-46f559986d77', '335aa1db-21b3-48db-812c-7d59ddd8137c', 'ed59a5bf-bc28-4124-ab7a-4609a606b6cd', '76456e38-e884-49f2-8972-b2cbb8d68556', '92518afc-ba4b-4b99-b498-e71d631b1dca', '1c89ada4-fb4e-48b2-a0b0-f985369b7bff', 'd83c253c-974a-420b-b423-623710b1a66b', 'aad62520-5b18-4e13-9ede-bae9b25c7196', '47e299c4-c79a-47b7-8557-7fcae61f58f6', '5da9a95e-5cf9-460f-a8f0-9ec864c9dd77', '37db20ec-1c80-4971-9f19-2ec421976a7a', 'a60c8cf5-d480-49d7-aaaf-eda982c805c6', 'e596db5f-0939-4c79-9052-e5e0e02ac158', 'f81f6195-6962-40fc-87f9-41799eebf42b', '76c41efa-0ea9-4671-8074-6a8e7bf5203c', '052524ca-36bc-4122-ab18-c37a5af52b99', 'dd844531-1ade-4f60-b353-0afea636d0b5', '126154e7-e438-4b53-aa09-51a7f8605e23', '5295846d-ade0-4ddd-bb3c-81a94783ea32', 'a1a376b6-ff31-4c38-b0bb-1054b45ea466', '23e97a28-ddc7-460e-b9d2-4366279475be', '421600aa-b3f4-4bdf-9cfc-1feea14799da', '9358f9d6-1e62-4ecb-9d28-42246e9fa84b', 'fdecfd18-23b6-44bc-a994-9ff490f26a97', '062f3a5a-9e17-48a1-9bf1-e76965a9ad41', '21f0ffe5-ed49-4cf9-8fa5-3bb447617a7e', '2f36fd8f-9c97-4227-812f-8c81944952f6', 'b7422996-81dd-466a-a75f-be72872bbc57', '564c6664-8faa-4035-9fba-20be94662542', '45fd4851-1afd-4057-aa85-70f4671ea9b8', '543b0818-b661-436b-9c33-90f83a29901f', 'c37ffadb-ee45-4cd2-bf8c-63620476ca11', '49415c58-aab0-43c3-b674-b6e46acfe699', '1bc16874-325a-49ff-ba6e-dd06a64d0887', '85897d28-07bb-4768-aa33-aa13889bf76e', '33e5eb03-6d00-4cd9-8d6f-f64bbd362e65', '99562a48-c4f7-486b-ba88-83d4377bdf89', '2c22de8a-0454-4429-87d6-3491903150c4', 'a76a426b-26a4-459b-8eaf-ca1624211715', '5958d327-82bc-4a19-a472-e7f23d971446', 'bfaeb8ea-af2f-41d4-8e4c-aeaedb3ed268', 'c7d9b743-d6cc-4758-8d7e-ff0b65ea81c1', '94cced5f-5010-45f4-933d-e74900a970e6', 'df78502b-95d5-4933-95da-e911e5bb0807', 'b2f921bc-8815-49f6-b9a4-c05019978c21', 'b6bee4fb-36bd-45fd-a392-d7a8b2475d92', 'd68767db-4efc-42c2-b60c-f3f73b304ede', '901f31a8-aa19-4b5a-bab8-787800c66232', '048b2481-fed6-41f7-a207-deb2820b9e63', 'd6528dc4-ca60-4a90-a7d4-eda677ab0584', '1fcc69f1-23f8-454b-a456-f1240c995c7d', '394abf8f-be2e-474c-ab90-43bcf7befec3', '55971bc5-5a8f-4b79-97a3-aba6c5e65819', '2f25998b-6846-48bd-8342-dd49e21d5b6c', '3eae7b22-a5e3-4a43-bf9a-ea9a9d4b48b4', '481a3760-ec35-430b-ad22-afe5e14a0f87', '4ba06d82-f455-4f51-80f7-4c1c4b2ce490', 'fa1507a0-5cff-40c2-b272-873a7c09d935', 'd43a3cf3-399a-4bab-a504-db27e9d8db30', 'c3fcf535-d441-4e9b-9716-984d49ce8f70', 'f44e6177-433f-4d5e-b651-9b90bc7c47ab', '52121b60-4874-4cee-8b28-71889a84ac0e', '0d228d90-3641-400f-bdc6-5ed3d94cd5e7', 'f3d3b331-1b84-4842-8e23-d6d0ecb590ad', '2cb48dfa-c589-4ddd-b5e5-3b023bab89e8', 'b91b633f-7cc4-4c2b-b5af-c038061c4096', '96bdc47d-716a-4455-a617-7c8f5bc705ce', 'e48b2e41-8b0f-4f86-8599-9cf144ca6812', '5a482abe-9792-48d4-9c6a-960fed487b7b', '5684dac6-d383-4a7a-971d-1f024f889611', 'edbb5355-a9fb-4eda-ab6d-2fb657a482a4', '483b95ac-a63f-4263-88f5-5d0d0ac58c06', 'a29aacf2-2643-43d3-8be5-e7464c0f6ae0', 'bfb7db86-2ce2-4765-8715-aad5912901d4', 'd1f11806-282a-4842-80ca-60bfca6b4d88', 'ef5c7d8b-5d0b-4288-8e0b-d400a66a4235', '3080272c-99ad-4fd7-841d-dfc41831519d', '5051aa67-5593-4999-8d25-4fe0063db539', 'fffe6cec-561e-451c-a997-4a95c7780fed', '59adf4ca-e7d1-4c2b-9b1e-e1ccddb5b607', 'a948a623-7e8d-4553-b3f1-829f0b6eb7f7', '86c28f95-5ab9-4c3d-a42c-b4d892c9bb6c', 'fabd399e-23e9-4759-bbc9-377ff509d06f', 'fd04d515-b61d-41e6-8dc2-5ae94c9bf6a9', '2672a424-d40e-451f-8853-92c2328619b4', '465d52d4-c388-49f4-be4b-6b1374e5575e', '02e85b2f-80a9-4f4e-ba8f-35d0b7fad98e', 'ff881382-8381-4951-8703-bdef34f300af', 'e03abb02-964a-480b-ae76-44c0df03811d', 'c08c3104-c0e5-4139-8332-5f3152deecff', '8aaed1c8-dcf0-4b79-bc3d-bf594b7f13fe', '506eb70a-16a1-4245-af0a-5b5769f7711e', '399dbb59-a0a6-4ae7-b3c9-98a8fc73501a', '56db192c-4d88-421a-bd03-5bc9b8e35c6a', '21df8adc-2141-4970-b8aa-8f7ecf19cbe0', '48dab99b-61e3-4ee8-8edf-ab5dfd0e12f5', 'f5c32b58-4b22-4f75-9b48-cecf7afdb4a4', '83eea723-ffae-4043-b5e1-b31b63dcbbe7', '86e2f101-edba-4586-a93f-421a36932f27', '0ec1b3b8-d1c8-4e74-8ae4-7dfe2ecc430c', '9c7bf3e8-6fb0-4134-968f-89be61a63295', '9037c3bb-6d12-4bf9-95cd-941349d09f10', '7698ec8d-5a49-4755-b50f-9c460a34b97d', 'dbd47c79-d0e2-4b62-8b2b-adb417c444d4', '74f15aba-1c27-4ea1-8ba2-506574207c2d', '6c44eb46-b4da-47bb-abbe-39f2a536da40', '4883df35-ef18-4331-a672-1d800b719a0f', 'a073e08b-f994-4cc9-b169-c6c38b83df7d', 'f963616b-c64f-40a0-b663-4a2591900a92', 'bb68133f-89ec-48fe-aecf-ffc8d19f520a', 'a8ee2a41-b40d-41c3-894a-f4b06398711b', '30de1339-46d6-4ba5-b041-40568626a470', '97fdfd1b-2384-4a19-90a4-5cec83cb53ac', '40bbc8c6-12e2-4009-9086-f1fe1204c89a', '1a92bebf-f216-43f4-913e-1c9caaacf4e8', '47c4924a-5933-4ba2-92ca-c088ae2b040a', 'a22b1209-e9c1-494f-a08d-3eef2aa5878c', '0b40e2bf-c8e7-437b-b88d-11a4e6d316a7', '6bc8c0f6-0d80-46df-8903-dae5dee012d9', '2ef97426-5641-4d79-bf5b-b07176b488e5', '5ccf3509-582c-4810-9e02-26c0d9a64cef', '04fe3bd9-61f2-4aa9-9386-3d3a37055268', 'f1956d41-7ff7-460b-b000-f13e9d416c43', 'd05bd40d-0d7a-478a-8e4a-69c8f02ecc06', 'ae97b2bf-ae9a-4f26-a9b9-96cc8358d985', '91c1add5-99d7-40a9-b793-0b3632648aba', 'ffa5b3be-dc46-41b7-9663-71e630340e36', '11e0b65b-30f5-4c0c-96c8-840d913a32ce', '54b88254-b912-48cb-84d5-46ab17d51d55', '4f979cea-4430-4d56-b271-4e3ed91191a8', '48cf72c8-ae5e-4109-82d9-00a6a25da8f7', '7e9fd0aa-1fe7-45f7-90c9-23345af7e8dc', '9ad95d4c-5d38-42b0-879d-e8a2292bfaaa', 'c419be05-cd28-448b-a692-52439a7de77a', 'ee5b1849-4aba-4f3a-868d-9784e3a7a8da', '5ea4ff96-fd3e-4c3b-b780-d634b7bda478', 'd898eca4-1d8c-48e8-bfed-be914d604924', 'a6152628-4423-4e3f-800a-f1f3d7d11507', '1aa7ae3d-f4a3-4457-a61b-dadad88fb3d7', '97199406-f3eb-4fa6-b0d4-2c6ce62de7b0', '39e14459-38d3-4a5a-9815-6f3b5a76dcf9', 'f960aa0a-a3ed-462c-9b13-daa6495d5878', 'b5b5b835-1016-415a-a8b1-d4f5a352123b', 'f5435300-0bb4-48f9-bee0-747a8b9421b8', 'e95314e5-d5dc-4bc0-b987-fd27ab9207ff', '3a5954a1-8ab0-4cae-ae70-c829da76725f', '0d66429a-a168-4ea2-9002-7db1c4ccbdcb', 'ed59d156-2889-4a11-9bbc-91eb365c1c18', 'c9255898-633a-416d-9f6f-9e5984f4730d', '487bc28e-27cd-401e-8778-d904ed902c68', 'c9e49637-036e-4749-9804-bbc5463a1c9a', '6998e0ed-9e42-479b-9b20-2e901db02786', 'f5f4b01c-2d94-464a-b27a-8813d2e8d1af', 'c92e209f-8889-4f27-ba7d-961a0f0b601f', 'da52202d-a88a-40a8-af59-6f32cc068480', '2c19ee96-ee58-4509-961e-45c067068df2', 'ed45f078-a34a-4139-a6f6-53dad2a6b1e3', '874b93e6-65a2-401c-8511-50aed518ab27', 'e1a8c3dc-0b99-45b2-a884-1746b5ca4369', 'd963b528-092c-402a-8195-53bdc15232de', '97fd28db-4574-432e-9a4f-b6ca02140420', '215a5daf-ef5f-4dee-a8b9-7ef591a70816', '7a6b7cce-ef82-4039-bbeb-8856f5e6076c', 'c1f8c5ef-7b0d-4220-bdd9-2443dad86299', '19bd40c3-720b-44b4-9291-413e65e5423d', '88c7ddc7-21b5-4eeb-b816-d43df6e92b48', '5b39b816-c9e1-446b-a541-47bb85c9dcb5', 'a357ffa9-9dc3-4e42-b608-881c7f5791e7', '6c057fc6-e003-4620-815f-1d36facc55a5', '7af3bc06-8a24-4609-8206-6d685ca7df49', '2f9aebb7-9c3e-46aa-aeae-c4ef37bc85af', '2e64b335-2184-4b08-97c2-f467686a2aa2', 'cf465bd2-d905-4460-9372-d5a087c7ab9a', 'cdabf726-c670-4614-a080-599dff012756', 'cd0fea96-b32c-4166-9c0c-2e2379bda834', 'd711c081-44a6-4569-b5bb-c44bf19ebf12', '8d61cb22-87b1-4bd0-9c00-305d4f0513c7', '6aef16b9-71dd-4b89-86f8-d9876a4f288e', '3214c4aa-78cd-46ce-88da-82cfd2b85db9', '106a6064-0f5e-4696-b5f8-c3883d5b9b4c', 'b32d77dc-cdb2-48f3-aa3c-8652ddbc9060', 'dbcb7092-84d5-4405-9abc-35c16b3a3bc9', 'dfe7fac6-aee5-4cb8-a6e8-327ae3881743', 'f9860c4d-1b9e-4057-8bba-9abbbfc05531', 'bb6f5b14-cde8-44db-9e21-65b99d0e5d3d', 'ddebdc55-13bd-4bc1-b41e-d9d9136067b6', '614b307b-af65-4b82-bd66-aa13b053352f', '8c9adcac-5522-4dbe-b6eb-ec82f8b25180', '83c9e6a1-9299-4af3-8185-b239bae1b97c', '387f7df8-a52d-4ce1-b695-60d89327c062', '5c09f5ad-e776-4a90-8453-3e38f585f2fd', '475f86ab-862e-4bcb-9aad-f39e7c69ac5d', '6bb9af30-b22f-4a54-a4ae-361beef4122f', '453de25a-1e50-4896-b0d3-900f8b4a1eec', '9e065c9a-074f-42ae-a679-653afc157cf8', '868771e6-b4b7-4c8d-8f97-03cfcca52627', 'ee79f881-97cc-40ac-abb9-2077e7b898e1', '7adc33ad-bb98-4e71-9815-96dd40178f74', 'cda12422-5fb5-46f6-b87f-8e38f65e70f1', '8c3f978e-568d-423c-bc36-21044a7a91a1', 'ede6643b-d8eb-4a58-9f2b-a213a94e72a9', '7f65b2f7-bfa5-44ca-9216-7f23fa1a0653', 'fae13df8-5969-4503-87a0-fd7d04f523b4', '9b92efed-28c3-4b43-bf8b-c2b4d9554602', '9f34feda-4991-46fa-b71a-862bd2dae51d', '51471218-72fd-4692-a41c-ddcd585ce61f', 'e0270cef-933a-4581-afb8-0326bbb15d71', '350f78df-73a2-4233-94c9-4d899af1ac78', '5cf0b9ee-218a-4087-b64c-608f31d2ef59', '10fa6631-d4bb-4cdf-91f2-211f6a01988b', '44ffbf08-d404-4d99-bbc7-99b973dc0144', '6fe217a0-a509-4925-a4f1-8920d98e0dc4', '0633716e-8776-41bb-b0d0-0698dd54f27d', 'ec450dd8-857d-4367-9560-eb78db19c1aa', '05f1c795-dd8e-4ab6-b9aa-a0c7ddb5e122', '74667696-a585-4307-901c-1e6b39013167', 'f76db8ee-64db-47df-b856-0eb296c4963d', '15318685-0ef6-43ad-9a41-a8ee853c3b8b', 'dfa52ed3-ad45-4653-8a9a-405b58837e29', 'cd7213d1-fa1d-45ac-940e-e356ec912151', 'fd43fea2-9307-4c57-9c26-eafb283c7d58', 'fb95cc32-f5fe-4b3e-8a98-d5d3fdf2ee36', 'cf4204c6-e193-4b01-9e32-7b8e26adb5ed', '694ac6d3-ab3c-4a31-9110-6028cc2a4559', '899889be-92c9-467f-a59f-396da449d573', 'ea6c48b8-5665-4d96-8d06-c920f155988d', '824eec0b-a0e7-4f91-a769-8e95218eeb78', '50a72c46-2351-4a30-925f-ad90e0d0f055', '5d7fb68a-e967-4742-97b9-cc61985c7858', '2ee5b635-5e48-409c-9019-b1fd0fde6fc1', 'bf963d5f-b094-4119-94bd-cc30c481a536', '46dfd291-e183-4e09-bd6a-fd81f6b78c3a', 'eba05d42-8561-408d-b99b-de37136575e3', '3411bccc-7cb6-4fe6-a8dd-d248f69b6a5c', '2d1f6095-dc86-432e-a6e6-935637f53c9b', '0cd33215-54fb-4c5e-a00d-bef5b01a4695', '1c845414-4afd-461e-8a4b-562b12a40d22', '40a5d289-472e-4f0a-9c14-a83cbf5909ee', 'b2974789-e4ef-4053-b6d3-4382aa246549', '7f2eb00d-c0c4-4f60-b8f5-5d6e942796d3', '5678aedd-e47f-4b20-81ec-bfec595f5326', 'ee8266ce-a6a6-4223-a08f-6b08d36ed382', 'ecd72615-9396-4de2-979c-7e942c61074d', 'a359db9b-f084-4d59-889a-8bb090692b2a', '750a0c7e-33b3-4200-8e2e-3ab6a9b40847', '0d4d0380-545b-4af4-81c5-310aecc63f2d', '2546af00-dc18-497d-9b62-c933f8a334cb', 'e3687c98-3af9-402f-8d64-f466b9d05629', '3a3512d4-972a-4960-a3ac-3862873c4b0e', '764cd7e0-baeb-441c-ab74-52a297944129', 'e8eaf364-b84f-4660-82d4-af0206cc8784', '0df30acc-0b69-4c58-b4e3-275c7da0307d', '7f643a84-f480-4c74-8013-9390979df9bd', 'ce271090-031b-4b27-b3d4-d3e06f447b06', 'e146bdcc-3db0-4dd4-b54a-502c5ce17b67', '42f89bb1-41ea-4b03-ac53-fc3b1b5faae3', 'b5d8c44a-9580-453e-ba84-1c27d897ab98', '63ecfc6b-da6a-4308-9e69-9c75b2cbe739', '99ff032f-d4ea-4be7-b3bc-4ab83074d5b9', '581c6253-db14-41d6-9c57-03f196f1b562', 'e5e92802-27ec-4bc2-870c-8496e05d78a7', '76e7c6a5-0eea-46bf-b392-0393c64fe3cc', '667ab342-53e7-4afc-9210-3dcd511c1ad0', '3d1fc370-8bd1-44f2-a8c3-d0f696d3b73a', '414b9412-0187-4e79-a9f1-e3f99df01e16', '47fcf364-dc0b-4fe1-a586-355b0e6ae4c4', '18977f09-4743-483d-9c20-5fb313826c78', '2c623baa-a7df-433c-9924-e0fc3db985e3', 'd78295de-da5e-4c4a-a009-879f99ca8e3a', '61fcc0fa-7639-4b4e-9f6d-bfc03c1875d8', '979272b9-574c-418a-ab42-d82327dc1cde', '4d9c28b7-3bc1-4fd1-8d0f-06d3f16e1d98', '1eb45fa9-d8fe-4c04-857a-8a376d7db37c', 'bed8ced8-c726-4089-ab11-6a3d6e93888f', '29aeb35f-7d30-4444-9275-fde643ef3ceb', '834137a8-51a7-46d7-b583-de2205059ad0', 'd0f0739c-1de8-484a-a5c0-2a23f165af0a', '72ca97b5-df6b-4a09-97d6-73e796eb43e2', '9b869867-1cf8-413d-86e4-85344c2fea19', 'b04394d9-e519-4d96-8d84-1a789050e940', '4bb2a508-588e-4a0f-9ecc-d081d70602d2', '2954bc19-2399-46fa-b38e-1951218f544a', '2a86f2c5-ccc5-4715-b794-69d69b217de6', '09ee80af-65e1-4d37-a4a8-2ed5bebe296f', 'bed962e9-ccf4-4d54-a115-259dedca766e', 'f43d286d-9611-4ddd-a56d-ccfc73ef7daa', '57637d5d-e123-45f4-826a-d1573dd1a992', '51a8dc99-03ab-45da-910f-07d82047671e', '8aef9fea-b2da-4f5a-bf55-edc664f735e0', 'e928db18-1171-417c-bf43-05013818d92b', 'bc36e96d-3410-470b-b770-cfbf8f748e19', 'cc281481-d533-4a9d-ad95-5f44d530b506', '62023e32-d304-4cb4-8819-e468d746c2d9', 'c5c81556-173f-4fec-b1ab-b9e8d3363ba2', 'a88d81be-53a8-49b3-83d0-6331439a0ec5', '405021bc-73ec-452e-af95-6fc02d46d432', '1de701a5-67ba-4c38-a721-b5ab725b12e1', 'd89c76ae-ef1a-4273-b9fe-3437c96ee1bc', '1f131ecb-fca4-4e42-9bd2-f0dc78d96ec4', '24e6343e-f098-4b6c-bd77-8ddb0d6cd2a3', '21cfd288-9cda-4dec-8399-c07cf1b116de', '534e434c-0c62-42bd-b9a7-e3e031bc9195', 'fa278c41-d37f-4671-8400-5419035a7172', '048249d9-d88d-435d-86d1-7d042aadde38', 'bf6a980e-4d09-499a-afb3-086507b93ad9', '960d8b35-8415-49cd-ab8f-e6078ff88f67', '40a88a37-6d25-4d06-968f-77a07bac33a3', '8f63873e-5faa-446c-b23e-908694f9b2cb', 'f032f111-e5df-40c2-9166-9d4aeadb1b14', 'c016e930-ff2e-44f5-b95d-859560bdf6c9', '89ad357d-e94a-4dd3-a8d3-495c4e870aa1', 'b51736e2-4147-4b35-bed2-17ebafabe318', 'bca447b9-087d-4936-bdec-c6f6fee083e3', 'cdeb9a02-bf32-43e1-b193-fef6d8d09786', '62140234-66cb-4203-ac85-9189e47d3a96', '1b3dc403-d88f-4d1c-887c-1b14b6d2cbcb', '0db342f2-0d5e-402d-b0b7-dca83e7227f6', '284d3dde-390d-4b4f-9780-93f7de5a77b0', '5ca391f7-64ce-4f78-aa24-c1fe8144c471', 'e9428779-0975-4ac6-894c-07d73e7d37f8', '641242a5-663a-4e1f-8d8b-90bd601bd986', 'b27ae984-4601-4743-b14e-02d21450c99f', '3431ec18-fccd-464c-a254-085c55b9a813', '9e57579e-621e-47db-a1b1-bd52d3900ba8', 'b57b9991-6db0-4ecd-9568-70af3f302cc7', '98541428-0c63-4cca-9383-fe6ff2eea827', '2a1b62e8-6446-4503-8d20-7c177ba70472', 'bbedea0e-b5b0-4fad-b7d5-fa458e4cdced', 'd03f9693-333d-4d02-84d1-00adc5d94f41', '6275a717-7472-4b4e-938c-d69aee183e43', '1d827d4f-a789-43b5-bbcf-7d80981be882', '8e35e4f2-6ee6-49eb-ae6b-57017784eccb', 'ab0f8984-b2d9-4bc9-9527-4364f5b589f6', 'de449858-839f-4c23-a3f7-35040a793627', 'c360fe05-bee7-49f1-9933-093f1aa16ed5', '8dc9f947-7348-431b-976c-4cd17efefa66', 'd357f29b-b97c-4d9c-9aeb-dbae000d5bd6', '2b0547bd-4e24-41e5-ac75-df2076e492d4', 'b4d64f52-4331-4d8b-83f4-f2048d3bc0a4', 'fabdab55-72a2-43ad-9537-b7c0e70d51ca', '21b47c3b-63ac-41da-813f-d1397cab208e', 'dd47364a-3b61-4ada-bc4b-ed5188d38edc', '05f558a3-0cd6-4ada-a93b-ff3499b24f7e', '70348063-fff7-4824-a9b4-9f7c1426a8cd', 'cba6224c-f073-4b52-a5c3-c93ed5712884', '364699dc-2d0b-4c90-b923-c5c83ca6a01c', 'c2cb3381-41c6-457c-b6b1-d7cdca277121', '4f5c7f80-5e62-410f-bac8-808cd7c62b1a', '5581b534-78e0-47cc-9bc4-a8e029a5e0e8', '5110716e-241a-4260-a2a9-5711b3b38791', '7e46a01a-5505-4db2-9408-eaf67fcad805', 'cae7932c-7b04-417a-b4f1-9e3872b35e9f', 'd7832383-eb29-4bd2-9181-54f566f22c76', 'a7f20218-f13a-4100-92bd-3e86a8667cba', 'f0571849-24ca-4bfe-971d-2890a794b227', 'b688e8ac-51b2-4e5d-bd19-4b49478c4bad', 'ca5a5705-960e-463a-a487-092be8c0b890', '486a1241-b7de-4e4e-a7af-c61fdaed1b99', '261c88fe-3113-45da-a87d-7677324fd907', 'a07850a6-f443-4acf-aca1-e37ed139760b', '79593a74-d485-407d-bc3a-1dec219dc7c9', 'd275c6ec-65ef-431d-afa3-f913db1dc812', '4c40b52f-37a0-4c5b-aa62-6cffae3e5feb', '10f4164e-37a4-406b-a384-038c4af41200', '65774775-02a2-45fd-8d4d-98ff5114cb5b', '979ff111-6e01-4910-add7-7063a3bbfef8', '6c9a7c16-f74d-40ef-ab3e-9c58907f2422', '04bc5a72-71ff-4e71-b9fd-47db17bbb1d7', '5349689e-91e9-410e-bf01-2f65947d40a6', '9badf399-0f8e-47b5-81f0-58f6a19f9d56', '9b1294f5-54dc-4ae5-a7dd-e56ebbc7d834', '20dd71e4-041b-4238-8958-65059b02daf6', '85ec9102-bb86-4af8-abfd-52e25205f152', '8e49c50d-2bde-4dd1-a0ff-41daaf55b159', '81d00cb6-3dcd-4f77-96ba-3a9847a69dbd', '0c84dc96-9844-4438-8a50-959ecd98f278', '7b47d025-ffb1-4572-a4f5-b456e3a9f10a', '503f4ab8-3fc0-42cd-b0ce-c690b3b5ed68', '83d99c75-b30d-41a0-ad51-f44d51757062', 'ffe64468-b8fb-4e6c-b801-c883b490d96a', '4dc48a30-eea6-4447-aab2-e7572b1a15da', '99f4ba90-9c57-4ded-a688-d40948f358b5', 'e13d156e-511a-43ad-b8d8-5ad1bd835031', '659425b9-4a32-45da-acf6-bbaf5667a70f', '1dee5f25-cbeb-4096-8a47-1fe0112e59a4', '838bb82b-a758-437b-812b-523e04765616', '65421d2b-6d47-4ec0-b0a1-c60749181259', 'f4f334be-159a-4c0b-86a9-9494272e7884', '0458112e-6c2c-4c02-aa45-08399d988387', 'e0b63d74-31f0-42bc-8ca5-6df7ce2ce188', 'bb07ead8-0cf6-44ca-a166-ee4cd557b709', '46ff5e99-6f88-46ab-b5a2-518394ab4414', '2f9ecf65-9261-440e-8058-2862707f97c6', '32f1dc96-79da-4e7d-9a2f-d067ba2f23d2', 'c9a952a0-aa9f-4202-92eb-e0098d5efa92', '1507d643-9430-4d76-866a-61b689e76e47', '31070b9c-4901-4165-a662-4c3ea2b27cc3', '7989131e-a1fe-408e-9a8a-c9486a0f5763', '36a516be-8a03-42bc-b749-a952e66075f0', '196a0f80-065e-403e-bfe7-1629d36cda38', '13371e43-3005-4a07-b0d0-8cd3368b32db', 'f6c94819-5840-4cc7-8a86-317d1096a37d', 'f9e9f62e-0445-4934-a03c-7f89bf8e4531', '9f758a40-112f-41b9-abdf-fbf40683b340', '626da120-4d38-407d-8315-ed59d4be00b0', 'ed058ea6-a30b-4a56-8680-e08090aeb429', 'f3bf20f3-34cc-4c70-8dc2-1a5b1a0427ee', '863411bc-dc06-4a4f-a52b-496be787af76', '6a8c2779-7c7a-4b11-bfac-c7459550bf45', '4175ba6d-ecd5-48d3-ae5c-edfed0dba8a4', '15691a18-5cf4-4b60-a0e5-18fbfd11a82c', '9e87c569-e0cd-4bdd-a1cb-fa98d4ceb671', 'ea5650b8-e82a-4a1b-bcb1-f06f7dcc0631', '910450a1-f337-415a-b0ee-b0d2eed915c4', 'eec734bc-91d2-4602-98ae-06256d90543b', '502c771c-bfab-41fd-8a94-81cd92910ec6', '7ab2c151-3dfd-4986-9d90-e3ecd058b418', '363cff8a-0093-44aa-9e8d-05ec62e3f7c2', 'ff62cbba-8975-46bd-b384-3de89f30a2ba', '6a95e76c-1a86-4b35-a136-1d4114e779b7', 'f4834351-b431-4024-a05f-d595446b2c46', '24499c92-e966-4dea-b147-aa9c8515b56a', '68db419d-8f1c-41f6-89fe-9dd021d8ef1f', 'a3b02c2a-3ddf-4b9e-b79f-a35324f84da5', '1c4a8c86-bfee-418e-9b12-e4d3aeb1c05e', '562cf1da-6a26-43bc-ac60-1bbe04b5e8b7', '19c103f3-dd87-476c-b30d-ecb3ed2c4f36', 'b759a1dd-651a-4a60-a2ae-b18a4529da57', '1bcc2640-304d-4b4a-b04d-df95b79f00bb', 'a955e64b-875f-4aae-9337-96f3363843f1', '0b2cec0e-eb13-437c-b8f0-2716ec447af9', 'fb12e3ee-84f9-4a15-9888-9516461cd6e7', 'b44de0ab-89f9-49f6-a43f-de6cd548fade', 'cfba8872-6749-4fb6-ac8d-4e28d7878748', 'c1d0c13e-83e0-4078-bb7e-9a261cebfdc2', 'cb6339b7-a929-4247-97ce-0775458c9c8a', '0bc8db1e-fe5a-4ecd-8428-31a8fcc9ce7f', 'c55c6c77-cea8-47a8-beea-7ee393a67afa', '9a82b702-e66a-4935-9960-fe18a8f97ceb', '95e0ab3c-b811-4f0e-836e-032414e33269', 'be7cc44b-430f-4c3b-97fa-4375a5f71e9c', 'f905066e-1568-4589-a9b4-07634ba507c9', 'd426f92c-bb52-4ac1-ba96-0dbfb07eb9a9', '12d3c802-d1c2-4b9f-ba86-23bfba3fd996', '565f67b5-7fa3-4428-aaba-ea6878d436a0', '5f4f0744-48de-4094-8693-9f37a23e0de3', '28542aff-f030-4913-b81e-8d6700eb716b', '9a093fdb-700d-4be1-938f-f1eadd24925b', 'b0d06611-5bbf-4c33-ac6a-7d75bdd8380e', '663b6e31-e1da-436b-b656-c1f75e46f9ca', 'ce611411-410f-4415-9d24-3307529cb6ea', '076f9100-2a6c-4f49-b5f4-478f823d228f', 'f3e6155d-1d74-4965-aaf2-e498ccfb471e', 'bd4861e0-e216-4aa2-a157-3371ebf927a3', '97e86d01-ece9-4b27-a938-cd9177eca9f7', 'ceee8323-a5fb-4d98-bf3b-f0dbf03e01e2', 'caf26423-1ddd-4001-918c-e01b15ce4bb5', 'abf29894-d0f3-4d9b-ba5a-5324ef13f922', 'ccdeb9aa-7d96-4930-b23e-9cc4027c8143', '73a25201-b5ed-4dfa-b225-cab2422f1e7a', 'b96d942d-0744-4939-8857-91663dd6407a', '2e0b219a-af1d-47a5-8e29-e211d8c85486', 'f38f156c-0a2a-4f46-8b9d-17ae4f920ce1',
             '6a8d6146-e5f8-4f51-8089-159786d791e5', '0cee04d4-c6c5-4fd9-8629-bd4619436587', '98ef4754-518a-4745-b7ba-02c19c9a6ecf', '931946ad-6b44-4e95-ac39-144281c2c376', '023eb235-da9e-4291-9708-93d49c89aed6', '16224510-255c-4a01-81d0-9b124fa6bc30', 'b16732b3-bc66-4eef-b670-ae4d8a1d5ecc', '6010b845-9350-4d4d-8642-ca0feeebfbbb', '76a202ad-49c9-4370-9232-97945daf6faa', 'c6178276-c79a-418e-90de-c518cec09076', '1315d694-19be-45f5-badf-5e8d782e07ec', '7435882c-3cf9-473d-b4c9-1648137b59de', '2cd6ea7d-f0af-40e5-8f11-860c8882a7b0', 'b25c25ba-547c-4cbf-b10c-33959171dec6', '88b33f58-f8f2-462b-bb01-50f0a6af4c6c', '7faa0078-0453-44e5-8114-8b1866cceef9', 'e73b218e-e9df-4333-9857-5e1edcebf8b1', 'b6105e6e-44ba-40df-b6da-00c07113823d', 'fe2ae36e-12c0-4d24-a8a2-25c224de5b68', 'a98e4199-93b8-4c3e-bb45-7eaa61271ee4', 'e4bfd800-e5a5-4998-af57-7d349765bc8f', 'a7ff1e62-6ef8-42fa-a868-42d69472807c', 'f3e51b6c-d1ff-4d4c-900a-c1e090268d7f', 'e823c7e9-f38d-4432-bf3d-f35281beb1d0', '2ec49536-e258-4a81-837f-bc7851672d25', '511ccc77-031e-4cf0-9588-d854615f8c6f', 'cd8bc135-2993-4538-8d40-ec62fb272767', '2a361f3c-e26d-4b6c-a945-e11254329fae', 'cd81aebe-1c48-43c2-b4a6-ba0f18054e6c', '213846ff-6461-403e-b817-2f56624b3016', '6c7fa049-5cb5-4426-8686-da59ef9258dd', 'a6d499ce-d09f-4098-9bc3-393123ae141d', '4a8b3112-f8e7-4602-8d71-ea96b620e5a9', '23fa2746-fdcb-4d25-aabe-cfc51fcb86a7', '5e393278-520c-4621-97aa-16d0ed7b665c', '55f74ce5-caf7-4bb7-879e-20af0b5d14a4', '2b209767-a6fd-4dbc-914d-0f3d5d1d6349', '3a0fcedc-dab8-46cb-ade3-ba954d18d37e', 'fa4063be-f08b-43d9-9390-b2f4e02de220', '7991a8e6-55e0-47c4-a7aa-32f01a6ef2d0', '086e2dc5-18cc-490a-93e7-224886951261', 'bed7a38c-f1c9-475a-90dc-cba8923c8b15', '9dc586ae-d7d2-4893-8d6f-9a89c7bcab86', '4089cdbd-595a-436e-bc55-fb152f6e6ccf', '0d967d6f-db37-4620-b448-cdc1b421c398', '0f7876ce-8b48-425d-92e8-7528730f785c', '9d865008-8385-4509-9296-1323812f4909', 'd4942832-85a7-4d97-a7f7-5d996700d58c', '3ca6f91e-dc18-4576-b59a-a0e793f57655', '71e44d1c-499f-408e-a5fb-947a31eab179', '62dd2382-a93a-4d45-8687-3df448dd673c', 'a4d38850-fac1-4c36-9087-f299634dbeaa', 'bc5fbfe6-ae57-408e-9791-134c5b9ef0d8', '3aef4b2e-3a9c-421d-8ef1-0afed8d050e0', 'e94a70f4-bfde-4c2b-b008-f264e4b95352', '29e1d565-8b7f-4c94-b84a-a691cc2a7bd8', '8a188b2a-c415-4cf4-a2d6-6469999db62d', '31f48571-4785-4a7e-ae54-af0620604ecc', 'acf3457f-ad5b-453e-a4ad-3c15ae792aa2', '2d44f827-443c-4dbc-a6d5-e214f8e919ef', '447e3029-6d79-45d0-a761-c7a949997314', '6476a4b8-7aea-4eec-a324-f88cb28cb26d', '6dc1207d-3e86-4363-8000-078c7228aa4c', 'ae8c3c55-7a75-4ce4-ba2c-77a16f665269', '383f872c-ffb9-4dfc-9899-b47a2e1e85aa', '20af4f1e-69b5-43c9-bde5-7d83c329931a', '44645cc2-7ab3-4309-90cc-9bd69624e019', 'b7043680-adf8-4fa4-81b4-b51799dddbdf', '5696a03a-f924-49a7-aec8-316151eca5dd', '56e6aaea-34b4-4b31-862a-0ebc973aaac5', '17f9c756-3246-4753-bf45-799affe82191', 'f0b4d06e-5b70-473f-858b-21fe635b04f1', '83f74407-a3e0-44ef-92f0-33bcbaa022e7', '5d0ce6a9-02c9-4c72-951e-d8a9d90f9c09', 'a2c8b3b7-94e7-45cc-9ecc-a61f5b3b3d09', '27e15fb7-14a4-483e-8f54-2155e743c5eb', 'c9eac335-b00b-4cfc-8166-0422bb073ee7', 'fd8b84fa-e049-4fde-9939-d75dcbb2f58a', 'cd186f0a-a6aa-443e-b2fb-ac03eb5064d8', '2f14cf52-bb69-4282-be2a-758969d04beb', '114e24a4-f1cc-476b-a7ee-68ec4d54c1ec', '9491a93b-108b-4e46-af2d-808bf25d6963', '09c2f464-bf73-495a-af4d-a394af69f750', '48e89f3d-db88-4915-a9f6-6d0afcc7acfd', '475ef9f3-0602-4a83-a363-756f25b62a7c', '26a62405-6afe-4d83-a9ae-28b3bd7fb00d', 'eed2bdb5-0833-4520-90ea-065c34a147f1', '0ecada39-7cd6-4e1b-99c3-267698c00f85', 'f88a3836-5fca-44c7-9407-27ce5e08058d', 'd3478655-7eb8-443e-a7f6-23342b7b88b1', '2d4a2f6b-88e2-4593-87df-bc8fd19be68a', '0338068c-b15c-4567-a4d3-ffa885d18fad', '6f5017ce-fff1-4049-a16b-8654e7c4fc03', '37b75e0c-c722-41e1-b717-829837a24075', '1e932e16-1fe2-4cf1-b137-a360fe2d2083', '516b081b-ba66-40bf-802b-292073d7cdd8', '1bf23ee7-a81e-4904-8462-7ec51848f73f', '332af96e-efd7-4b84-88e0-29117160b74c', '446fda9f-42d1-4bba-a45a-43fe78280d5b', '52173adb-4041-4582-aefb-7a37120fdbe1', '4d7e7921-605c-4b79-9185-c89f93321553', '22159960-c919-4706-92b8-807f4ff2e7f5', '2993f113-75d6-4f75-9247-142651231504', '38aa472c-2eb7-44f6-aeba-e987d4410ea5', '54409993-6e34-44ea-acd4-2df433ca1a2d', 'c86aecf5-9765-47bd-9b08-23a2ec418c04', '841370b7-8f5e-44d3-8139-23f379a0533b', 'f2b439b6-3dac-4ad2-9b8a-f2f23c7d885a', 'eb009c82-dbaa-4b94-b873-fab1fd5b30c2', 'cc940033-f023-4a9a-b014-114860171360', 'f3672577-1f90-482d-8c06-ac4a8cafefff', '72048d56-e87f-41ed-ba2d-7be826c94146', '1c903206-07ba-44d0-b71f-0b8c220d7055', '1b56ed20-6dc9-4709-a6e8-5471fb06e823', '2487b9da-f7e5-40a4-ba42-2373c1579b60', 'bba2e710-dbff-46a0-8949-9a2c2ad45929', 'a843c299-25a3-4335-bf9b-b61efe99903c', '69480e80-62ec-4cb1-b942-9d592d5bcc66', '531e12ec-b0ba-493a-8513-6f1995621b4c', 'ae89e18d-6412-47f6-a71f-e4e595161693', '008aeb14-b137-42b6-aa0c-0d41b05f3010', '985b060a-b9a1-4e60-9f93-edaa53e90345', 'a90abad8-b124-4f61-b342-e4574e7ed758', '28065a7f-5f28-437d-8ae0-5a4d605cb373', 'e8d158f6-c24a-432e-9801-e1482e7a18ce', '0ab5275c-5a5a-4787-a121-504e95d6e0e1', 'f8575ad4-17ca-46af-bf0b-03265e8aff62', 'b99e61dc-f08e-40af-9037-ae66f939455c', '7adede23-4753-49a3-bef1-cf5b76bc29d9', 'c54a312f-f8c6-469c-ad5d-78f3e30778fe', 'd3b474a0-02e5-4a49-a1cb-d413ec087aac', 'adfb6aac-09a1-447a-bef4-afc7c051eb4f', 'b63a12c1-58a7-47f7-acb6-dc4de17300aa', 'bf2f9ed5-3955-407b-b558-aaa9a0bd8e18', '263d2f29-0be7-4261-885d-73e63f72ad53', 'c6b9c88f-ab36-4772-afc9-9cf286ecef5f', 'f424d7e5-2ab9-4118-a13c-ea1a61d30a91', 'c896aae5-f518-4f1c-b9ec-657a1a99c5e2', '927227c5-aee0-44dd-be56-251024667fb3', '0229748d-1724-4d88-a8a1-29c723dbc7c6', 'd7d2958c-5fca-4982-98b9-8fbd4864051c', '81de3206-4982-4cad-9e75-9f750de571d9', 'd1a3243c-2a18-45f6-a5e3-daeb0f01f1b0', 'aa289f90-366b-4019-9f52-5a1710b6176d', 'e1f974ac-bca2-4fa4-bc34-078f588455cd', 'ba40d0fe-1b4f-4b0d-a637-27e94761935f', 'b29caeb0-a3e3-482b-8840-b3215d593599', '9f1d88fb-5cc3-4629-90c4-84ab1e43fef8', 'ef90f8d8-fcce-47de-ab7c-a1a86a3f9ff9', '42dceafe-80fc-485b-bf61-346593ba3da6', '44faa8f7-bdc6-4e97-b295-b77c3ced1dbc', '8cd92f37-1ddc-4057-9267-bab1595d081b', '5c7afc2a-3f46-4dd0-8f8f-d6aad2272649', 'cf13e3d4-7c5d-4425-a3d2-0c869294fc17', '93a7688d-8b65-4ce3-b616-edf11d8c8b6f', '9b4ee37c-bb4f-45f2-8994-29ee20306a53', 'eed861f3-e803-477f-8d71-6dbad1855cd4', '718b1384-1fe7-47b9-b6a8-eee55500fe34', 'd9bf890a-3cb5-4a01-bbdf-30638720effa', 'da3a3d3f-c7c3-4c38-adb7-a26423769aa7', '09eeca64-14e9-4d5d-a043-c1d23446e8ea', '4e0e1c2f-417d-4af4-b8e6-11a596028c03', 'a7b4653b-2b73-4106-80be-5d053c49241e', '214dc2b4-b800-4c37-ad56-3d91a28c2e3d', '7f0babf2-2d90-48fd-a891-282b3ba298d7', '75b2bf60-2a50-473c-8503-1f35fbb9c011', '0cf36c23-edb4-4ee0-999a-9896ef39dca7', '997d773d-9b62-4365-8245-31f43dee3117', '1d1be962-5711-4ce7-aada-c0426ff55528', '1eadf9b5-fe0e-4680-8c02-822a0d266b37', 'fa0c2d9c-ce24-4501-b2cc-926461f85a79', 'c8ec2faa-4a4b-4821-adcb-59f246adc6be', '4f63c222-6b26-43e4-9f00-3f7ba2a3737c', 'e6dcacc4-b61b-4c37-bb18-f54792785553', 'f686b36d-edaa-4178-b260-47b1081a50d8', 'ca8bafe9-b531-4015-bfd6-e0f301138a5a', 'b616730b-6ed1-480f-91be-22b0d278f6fb', '511ad47e-f5e4-4a3f-9ebc-b04c2897627f', 'd9b9d1f5-64eb-4ae7-97d2-f72c5e6c9581', '58b18dcf-7dde-417f-90bb-a7bc6db2b046', '0e9e30e6-ab26-4042-9ede-c8f8a4921471', '217250a1-e0ee-4bac-b15e-dfd433affa2c', 'ebf070c9-d5bf-48c4-8a98-2e2d422eb332', '8944ce81-2bf9-406b-8fd3-a5d2b34a22d1', 'b887dc96-2697-4bf8-a640-5b263c6f7ecc', 'dd9b7a83-b957-4ad9-b4a3-3cc9ec648470', '48422279-b0f1-47fe-9a62-a8d15767c4f3', 'c190adae-0805-4f6b-9bb0-f1faea21404c', '89fee5db-29d8-40b2-9273-0d754a1df69c', '47a06b74-121d-4476-870d-b95034d3584c', 'e3268943-c08d-4437-ab58-befcdd945787', '2dbd8afa-6d20-4d05-af53-be6ef037e0f3', '95b03099-2248-4762-8ae1-c3e21563c9cf', 'cbeb412c-c9e2-4c5c-923e-5073a503c3b6', '50f91d42-6d0a-422b-822e-b7fe8420f214', '3e13ec24-4af6-4a7c-aa29-b2804a06b915', '97be68cc-e5a0-41ce-b265-529548e563a8', '100dae19-780b-4521-9b21-45b84e817ebd', '89a3cfc9-a277-4964-8d2b-9594cc4f0b7c', '027cd073-7429-4d65-82d6-ad5cf5815eb0', '02f5ad85-0269-4f7f-b355-a957fd38677a', 'a54f5d33-9377-44b9-995c-46f2ad6b795e', '21a58b94-fc18-4423-8a03-abd7eb0703c9', '01c0152a-eb1f-4e5d-85ab-201758ca66e1', '22b35028-2749-43b5-9edc-8b0a562f43fb', '00cb4513-0611-4f68-a9ff-9350c8bdb360', '01414e36-3118-4605-88cb-6fe3fe6b21de', 'd047b331-63c1-4ba1-b0d3-58b0a331a287', '4416a4c8-82f1-489b-9e54-494a58facbf3', '0bdb0dba-a96d-47e8-8fc9-193a2b574212', '9d7aa4ea-3205-409f-90fb-f86aed963f5b', 'ca086f13-e3e2-4c05-a556-59ee95d0ecca', '21ad776e-eb83-4582-8ef6-3a1b5550b038', '878ee325-c44b-420c-b63a-ec6c6355db3c', '9f76f512-814f-4d53-8f5a-e43e33f1d274', '387707cf-2abb-493e-983e-9828e811c56b', 'c85fda83-79e3-4642-a5df-a264c5fa3647', '562bdedc-f7e6-4951-a30d-5b973035df33', '11f4074a-ba8d-43bf-947e-8258db96ff09', '72038f32-5d30-4c33-9096-22642f6cac53', '2f11c2a4-a3cd-4e1d-b0ef-f7bb91a985f3', '0671f4ad-f75b-4631-915c-0da89ee518fb', 'd4d1de14-8108-4848-a349-db4bb4f62bec', 'c349df9f-3a7f-47f6-86c3-cf563e0dfdc8', 'ae059249-2e26-4175-bf44-301c1f9350f5', 'ad66ceae-cf77-421c-b62d-1c23cfa67424', 'e390ba03-f8aa-444b-8235-18f3e0e4df12', '153b1567-f727-47e5-8a75-2d80f27a8dc6', '8f226b09-31ae-499f-b168-ab80534cfa46', '269d8650-c321-40d2-990f-35af04d30d23', 'ec330ff5-0d91-4492-a5c0-ceda5cd50831', '7ed88bf5-f857-472a-bc11-580914748e49', '86636346-d15a-47e4-8451-b7ff604231ff', '985481d2-fa4d-41b7-bc9b-88fbda0aa420', 'bfa16b12-1395-49ab-b333-b7b916881c0b', '66b829cf-4f98-48a1-b437-1623fce42314', '446f9c72-55b7-4686-97a0-b197a8f11512', 'eda99d92-f40e-45f3-9fd9-2056a45dff38', 'af98da35-d16e-4847-820f-ea8d60a90cac', 'e6135037-c1c4-426b-86a5-9f29c7be3737', '9b574fca-da99-4ea1-a956-fdc9034e0950', 'f821fc0a-c3b8-4958-bc06-2e3c89057e28', '93547274-0381-40d0-b115-222cbccc8772', 'f980d856-b7da-4010-8f51-afba38816006', '59db4275-167f-4121-a901-c7d6ba092335', 'fbaae6ad-15c5-4e8e-8432-ca3ceee2d9c7', 'b927d7e0-b8d6-4593-81e2-92e630ff7a3f', 'd85e065c-2685-43c4-95b8-7d1d60f40262', '20affe59-71d1-47e8-b0c9-726072d31692', '443a4233-15ca-480e-96bc-e588c3ee50be', '22b080a5-a16d-427b-b4c6-f0b3d325b24c', '253b5734-b825-465b-b3bf-df6551784550', 'd988f3c8-3973-41b7-a137-1e56d6f5df4c', '4943e995-c222-4380-9b42-55be5f3fc691', 'a6798ad1-9e9d-42f0-ac00-a7ba8b8ad371', '5a6318d4-8986-4741-aa40-72d38f30b65e', 'ec63b783-4636-4567-b56e-92c7e42bf94a', '99ee0285-2268-45f9-9c30-73c1431365a0', '16e39ea5-7d30-4d81-8581-dd4c023f568d', '5fa1c7c5-a3e9-4bea-a638-a82ecc6d73e5'
    )
and opphoer_fom is null
) AS subquery
WHERE behandling.id = subquery.id;