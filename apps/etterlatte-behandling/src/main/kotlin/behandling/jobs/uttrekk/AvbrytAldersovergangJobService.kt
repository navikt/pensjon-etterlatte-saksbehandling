package no.nav.etterlatte.behandling.jobs.uttrekk

import no.nav.etterlatte.Context
import no.nav.etterlatte.Kontekst
import no.nav.etterlatte.behandling.BehandlingService
import no.nav.etterlatte.funksjonsbrytere.FeatureToggle
import no.nav.etterlatte.funksjonsbrytere.FeatureToggleService
import no.nav.etterlatte.inTransaction
import no.nav.etterlatte.libs.common.behandling.AarsakTilAvbrytelse
import no.nav.etterlatte.libs.common.behandling.BehandlingStatus
import no.nav.etterlatte.libs.common.behandling.Revurderingaarsak
import no.nav.etterlatte.libs.ktor.token.Fagsaksystem
import no.nav.etterlatte.libs.ktor.token.HardkodaSystembruker
import no.nav.etterlatte.oppgave.OppgaveDao
import org.slf4j.LoggerFactory
import java.util.UUID

private enum class AvbrytAldersovergangToggles(
    private val key: String,
) : FeatureToggle {
    AVBRYT_ALDEROVERGANG_BEHANDLINGER("avbryt-alderovergang-behandlinger"),
    ;

    override fun key(): String = key
}

class AvbrytAldersovergangJobService(
    private val behandlingService: BehandlingService,
    private val oppgaveDao: OppgaveDao,
    private val featureToggleService: FeatureToggleService,
) {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun setupKontekstAndRun(context: Context) {
        Kontekst.set(context)
        run()
    }

    private fun run() {
        if (featureToggleService.isEnabled(AvbrytAldersovergangToggles.AVBRYT_ALDEROVERGANG_BEHANDLINGER, false)) {
            // oppgaveId, behandlingId
            val behandlingerSomSkalAvbrytes =
                listOf(
                    // Ferdig Pair("0f02837f-e5a7-4718-9ca4-a6a48cd3e8be", "6e324deb-e0ef-40e5-afbc-5f024341af14"),
                    Pair("bfc75ca4-99ab-49ed-8a93-6f9d0230f5f2", "0ed42567-3d3f-442d-9e54-bc73166f01cf"),
                    Pair("5869931e-2c8d-4537-ad45-409fefb48b7d", "5aa02465-9693-4c08-afd5-000e2ff50638"),
                    Pair("c0fa152e-59e7-4d2c-a0ba-fb81f8b27377", "2c911391-1217-44d5-b446-1badc2de92f6"),
                    Pair("239fac63-0ba2-4158-82ee-426070e635f3", "ec922fdf-2ea5-4992-b292-58cceee218e1"),
                    Pair("dc7e3350-2c12-4f4c-a778-0bbde4bc7ce3", "98458952-fa38-405d-9636-37e441d289db"),
                    Pair("57b30c6c-fced-4bff-ac78-c00aaaf70844", "76f9bfb9-d47f-41ba-a33e-c2727ebc0105"),
                    Pair("b6dadef0-fc4d-440f-920a-5fa368eca8ca", "e1761872-3348-46ba-a0f4-5e9f69b4253c"),
                    Pair("a79a1a27-50cf-4be6-aea3-6b53ae5494d2", "c547e46f-542f-4eda-a256-72685b537563"),
                    Pair("71934fb2-4a08-4b5b-926c-4de23f1d554a", "c1353e6c-8fa1-47ae-86ed-1bd7ad328c5a"),
                    Pair("31af240c-71cf-45b3-afbe-ae1f5de377f8", "64d044df-4b3a-4b75-9289-fa60eb81bf97"),
                    Pair("39382f80-7d80-4aa1-af2b-a5282b3a472a", "b79840fe-aefc-426a-83c8-de61638e784c"),
                    Pair("bfcce754-b31e-47b3-9320-d917e48a3a92", "7aee7716-f983-46c2-884c-8be502db0d30"),
                    Pair("fc24b5f8-86d7-4d5d-b75f-ab8367c4e527", "9f60519b-28ce-432f-86f8-c9ddf4747bfe"),
                    Pair("7b08e694-ab04-4ccc-8503-3141e01f4307", "6f8ed6f4-b27d-4407-99b7-4b5acf049e34"),
                    Pair("d40d7caa-2ce6-46c0-ba99-016180b1db22", "32b2085a-518b-4129-9e54-f95ad007fad0"),
                    Pair("8906fb74-7363-41a9-949d-910778a0a69c", "79218ce0-b836-4aa4-b435-cd2cb0871838"),
                    Pair("c473d039-5151-4353-867b-005e19875731", "36742877-24a3-4ccf-a9b7-91820989d356"),
                    Pair("803c8958-365f-4118-8c07-b64ba623a401", "86b6363b-44e9-410f-a92b-91c0ff7af9f8"),
                    Pair("2c0f63b9-c986-442f-b2d7-4ffa2c20d123", "b95a0de2-c06d-4cc7-af4c-b6f0d9000077"),
                    Pair("cfb971a4-e067-40b8-8fb1-339092a898f6", "a4ba993e-aafe-48c2-930c-e5b4b3eec658"),
                    Pair("c34c964e-aeba-4080-baee-5ff890203d17", "70b657fa-385e-4460-95df-deb129802be7"),
                    Pair("5ff713d2-cb65-4eaa-ade7-93969c724aa3", "2021bd44-dcc6-4079-bac0-4261641f944a"),
                    Pair("e4a25cb1-f7fe-4891-90cb-8cd1d91651af", "01a1cada-242e-46b8-867e-deaaa709e9d4"),
                    Pair("cafe3d55-7d41-4a85-ae06-c7d5d21c12b6", "b3f0e2c7-e520-4eab-bdae-8c1d9c906bdd"),
                    Pair("5ee46e43-f756-4b9a-a7ca-d2dc1f2cf750", "f045c663-2f55-4198-8a66-cb29f27c17e6"),
                    Pair("f0ccfc9c-86a2-4566-8189-fc69b33c2cf7", "9689ca0b-9870-479b-b78a-60df17df8e83"),
                    Pair("0182f67a-51c5-4945-b672-9b226ba32c61", "70a3da91-ab38-489d-8a38-38cc2d3ef648"),
                    Pair("6acb4e50-1d78-40f2-a565-9e2f7e703301", "fac2242e-5fe0-4ec7-a457-26a0a0cff453"),
                    Pair("7fd73f9f-4035-4c02-b2d5-297d5c6ee575", "893548b2-ff18-4527-a18e-0176d76ef178"),
                    Pair("a3d34407-3f8e-4a2b-a5b2-f359008832ad", "c24c4d02-dd2e-4ad4-8daf-5413b941f6a1"),
                    Pair("dcf987c5-4935-48d0-a342-1c0b2af2c3de", "b4b5a761-4b40-4461-b676-51a9e9ae46f5"),
                    Pair("ee24e5fd-1546-4b74-8e35-4d03450d10bf", "c05919e1-93e3-4485-9dc5-dfb016ecbaf3"),
                    Pair("b90c1b0d-2916-4ee6-b9db-5d6a06339964", "85a88842-68d7-41ed-9b12-9eb108883795"),
                    Pair("10c5577e-e7bd-49b1-8a8e-b463594e7099", "11d8de33-816f-4b68-bbe3-e1edf6b6e177"),
                    Pair("8dffc704-a13a-4df6-be96-b0a4dfd38d94", "089f9ae9-b17f-4e30-be87-c95c462c583a"),
                    Pair("53a56e20-f55d-47fb-8f91-965ca10c7cee", "cf16d9f0-c351-488e-9e8a-5729e86c5841"),
                    Pair("7211b6ee-3931-4ab5-9df5-e38b6b0ed7e4", "38078f03-a68c-4355-a2f0-2031cd65f6c9"),
                    Pair("2c43f5dd-2119-4c2f-8b73-38b129e51cec", "01c3ec7c-efdd-4853-8042-60407269b493"),
                    Pair("d0a89642-2fdc-48c4-9b3d-0ec38c7ded04", "01540764-cafb-40c6-b468-9b46eea2f2cf"),
                    Pair("4920b3db-10e3-4bb0-9a41-a6310f709be4", "9ce27417-9d78-4828-a7e0-60fca648457c"),
                    Pair("18677018-0e14-4b2d-8585-60ba87819f82", "828fdfe5-2d58-4653-9c19-1e17bdfe6d38"),
                    Pair("ffd62bd2-8bd8-4c08-90d7-e1bdb81333b5", "571af8f6-fa9a-46a4-9ae6-54d0299cedfc"),
                    Pair("c3969a9d-fe9f-4e22-a5dc-789ce26ac845", "533480c6-bb67-4daf-9bc1-508a99789c19"),
                    Pair("210ab50b-e83a-4f73-bbd5-fa2b1bb81022", "55e6b15a-e4e2-4130-bef8-5feefa338b08"),
                    Pair("cad3ac86-78fb-4cf3-98d8-92114a7fac09", "2b622290-65cb-47b6-8532-1c95d9c25c34"),
                    Pair("56772240-bb4d-4bb2-b580-687108b36db9", "597bd2c5-124f-4783-9496-2f73870e05a0"),
                    Pair("ed072050-ae4a-4d57-962c-883dd67d5562", "d4a51951-3ff4-44fd-8206-f80a0aecf4c5"),
                    Pair("40e826c4-198a-4000-9cd0-ddba5f946935", "4b67264a-75e4-47fb-ae9f-9e799331b017"),
                    Pair("bb463807-57de-4f71-9782-be8033453af6", "59355671-6ff4-4149-a390-97b5ae5a005b"),
                    Pair("35f91587-de87-40bc-b125-73f457a7f1b5", "8a498b76-9e05-4b02-bbbd-aa65d42ff62b"),
                    Pair("78de4e21-03fc-46bf-81e1-3b6b078ef0f6", "7b764880-b74e-4418-859b-c7ab02fe5af2"),
                    Pair("fd2bbd85-5516-4ad9-9742-f009428f3bec", "cd095bf4-a39b-4271-8140-ffb12d0b86b4"),
                    Pair("ab84a498-f09b-4d27-a236-2e871d80f412", "4f6df846-4827-42d1-ba76-58562479ca9a"),
                    Pair("a7eceb82-2367-4996-aa46-742575aa0178", "c5fecb37-9155-409f-80df-162560f2b40d"),
                    Pair("943a3603-e201-4627-a563-475fe94e075d", "f20bc172-19de-4ab0-a728-69b467dc0daa"),
                    Pair("5a1348b7-2b6a-49bf-9c87-850451a0e066", "9002b87d-ae00-45fa-8083-6cbc944c4da1"),
                    Pair("19ad24f3-0749-4ba6-9918-9124cff0a44f", "aad9c06c-ef8b-46f0-b4fe-9559a3b509a2"),
                    Pair("66c82f0f-94af-494c-9957-296e2f4721d6", "ab1950b4-fd64-41c7-b814-acdde545433d"),
                    Pair("13ff82bb-29bc-49c5-8f25-d5f3c8d4728d", "8316ad8e-c2b0-4ef4-a8f6-9fad4e0d741c"),
                    Pair("7e02bb6e-fc88-4a04-82b8-01c6a36948a9", "8c013e6d-8f91-48a2-ada4-44321ef0fd26"),
                    Pair("e7edfacb-d639-47f9-a80d-f6b17c5786e1", "09528f38-025d-4f74-852e-aef4ea6eeb38"),
                    Pair("e9455b10-c82c-4c45-a8aa-32bea4617f8e", "89185d00-77b8-4980-8cba-52a848a0ab87"),
                    Pair("459272c4-e9ee-4ce3-a531-fd339a2b3601", "7d80ceff-fa9e-4a78-a731-d32cd1c15c0c"),
                    Pair("be0df7d4-9267-4b0f-bcd9-0e5f7992339f", "bce21d67-8ce2-4426-aeb1-efad57341c73"),
                    Pair("4ce90e08-f8bd-40e9-8770-0dfbd03a6c5e", "87f45cef-fe83-4379-8c66-16559b5394bb"),
                    Pair("3649224a-7c83-4de8-9fef-b7222445723b", "57fdcf3c-ec9e-405e-8808-c7c4f206d5cb"),
                    Pair("23dce8a3-661b-4c78-8005-bc8de172f421", "b91bfaf9-427f-4ce8-89cd-ec27677cce32"),
                    Pair("9db205c8-634d-4489-929c-b940f133001c", "ecb454ff-8989-4bb1-9ea5-873711a3f1cc"),
                    Pair("e8857c15-1736-415d-bf03-e3a3ed89f9b1", "ce9d6975-4b73-448a-b4cb-e7b772523f9c"),
                    Pair("f05ceaac-e450-4a8a-a5e7-7d939a851b9a", "ac77488d-797a-4798-b77a-ae9d094a452a"),
                    Pair("3691eec1-2d61-42bf-a114-95fdfee0ae2e", "abe790f0-bb00-4490-9089-33a5a1a94508"),
                    Pair("bc27e102-07e1-4ed1-bb41-d04c42ef66fc", "b601bf17-c022-4f60-bada-ae6ffa849fee"),
                    Pair("6678c673-e35b-4e3d-9177-68fd94259715", "d3350a8f-5ba8-495f-b269-d3a12ac96b12"),
                    Pair("50476541-1390-4cd4-8e3e-65d64f2d659a", "c7c6ba44-8e11-4915-a618-fb4f77522c82"),
                    Pair("6e0d2757-e77f-4dd3-895a-50337ad6b49a", "d499f133-6e3a-447a-a768-d24067e53879"),
                    Pair("13ecee30-99f9-44c4-a824-c981c9dc5b61", "2571dd2e-9f15-4d9a-a97e-5383ace87489"),
                    Pair("518c8b3d-a0d3-4a82-b813-d8958f637c2a", "010e8dbf-212f-476b-ae17-efb30c1feb89"),
                    Pair("120ab698-e0d4-4ab9-9b7f-6340f088be7c", "c1f90c68-27aa-406c-a3cb-8a528ea71e05"),
                    Pair("2e2c5b88-55c8-45cf-a82a-70e3da9cf715", "0b2449a7-3c43-4e3c-b515-f4b716ba34ac"),
                    Pair("f5c8ad5f-acd3-48c0-aca7-16930f29b732", "03732a54-d57f-4473-b679-5ff7527280b4"),
                    Pair("23c83d21-28ca-4f3c-8fe2-a6eadcec8c8d", "6cc4f1af-13aa-4e5a-8d19-b4bede2332fa"),
                    Pair("575cd109-251e-46de-8ba1-b73a4ccf785e", "61b6ff54-8b82-4ec7-a2a5-3281ce7db4f5"),
                    Pair("76f61150-9816-415a-b18a-5fba867d8da1", "518ce6f3-85ba-4fc3-9058-a7a58357897a"),
                    Pair("87e988f3-38b2-4e3c-8da5-96523572c62a", "9f189d8c-e607-4399-8d10-f9505c570c9f"),
                    Pair("d553f870-dda9-4a19-a188-6769c9be7088", "a315503f-8873-4c1f-b275-14cab2ccf451"),
                    Pair("231934ce-3137-4b67-b54a-c816533911ee", "5f342e6d-93a6-41e0-9a8f-319989dda582"),
                    Pair("e4860598-b1c1-451a-9dcf-84a279a5a477", "ca347ddd-2e34-4504-bb85-f6eafdd97327"),
                    Pair("2411f225-dcff-481b-988a-8cb9edefb9f1", "7d5e089e-b539-4eec-bc2a-15dc57c6f70a"),
                    Pair("4a2f8193-a912-4137-b972-844f61f2c2bd", "91f047eb-d80c-4799-86d1-6e176cb99e23"),
                    Pair("7813590c-bf72-47aa-86b9-e78f8fe99642", "4221760a-dd75-431c-8d9a-326c4d40012f"),
                    Pair("4053fa64-7440-4e6c-a6c0-2781cd05c6e8", "6fb43f2f-df46-49ab-b771-3e310e6de1e9"),
                    Pair("ede51af2-54d1-4a58-8634-c57a93554d72", "c9d3dea6-c017-44a8-a918-0086a2e0687f"),
                    Pair("06121f13-274b-41e0-9b42-ef46bc6fe724", "80614df3-93b9-4ba8-8c70-e03565dac778"),
                    Pair("89de5e60-f25c-4bb9-87ae-4ee60cb4f3b8", "2a673020-480a-42d9-b04b-5a4586b4aaec"),
                    Pair("299af259-8a3a-4bfe-8ed0-89e4b8c779fb", "f6509903-67f8-40a0-a901-33760a8cb33c"),
                    Pair("42b0252d-8c6f-49c1-be6a-e0aacded5cb0", "ea8dd8dc-6f62-475f-a3eb-cc0c55398edc"),
                    Pair("c8656b05-56a8-4087-b204-4d279b851802", "e6f500c2-c67b-4d48-a992-2ecb63ca1512"),
                    Pair("ce19fb97-5835-4653-b75b-39b31ff79d90", "3781fd2c-11fa-43de-8c5b-cdfceff5cd11"),
                    Pair("3b33d0f3-a203-4ce0-b779-3201e3c9e673", "76ae1fda-e762-43eb-b470-057e5428deea"),
                    Pair("8a7d099b-4c4b-4558-9066-3e9941407a79", "6fa7bf22-be20-485e-8001-dbdf1347516f"),
                    Pair("928875ac-8152-4f89-b076-e7b5535e349e", "79e3f121-a4f2-4bd5-839d-b677aebdd902"),
                    Pair("e085af8f-ea4d-4c5d-bcc8-735938ce746a", "4e56ba2c-1b65-4e03-bde6-9cfbdde8ffd7"),
                    Pair("215e8a9a-c72d-4c95-b357-d5b802a31249", "d387076d-81c3-4e63-b0ad-f66f0b6c23cf"),
                    Pair("31c3e11d-67c8-4cab-b55c-fe79e7f26ea2", "a6740de2-95b6-4e85-8f5e-84191344c62d"),
                    Pair("8ed54556-a71e-44e6-a131-930c05690603", "40122e5d-406d-4091-ae79-b7b07b12239c"),
                    Pair("bce77b83-af08-41f3-94fd-325aac4632b7", "7d4fa012-7a76-44c0-b9d7-75f3c97991ed"),
                    Pair("a22335f7-0205-4719-a91f-f5bec1a17f0b", "59836619-c2bd-4d68-b3af-4fe74313d809"),
                    Pair("5259cbbc-96d7-4587-8975-ca75d9e127cc", "b0c60ce2-aed8-456f-bc16-9f8cbf3cc27d"),
                    Pair("a3933564-6bc5-4d13-b308-c16c3c2827f4", "1bd0c3ee-9882-4f9a-8f8c-61b3b4d18a0c"),
                    Pair("6d71ce9a-92c1-4648-87f9-c0f8ab13ac73", "c031aa38-6994-46f2-b7b4-3769fdb8a78f"),
                    Pair("241fc0f3-affb-477b-8496-71903e83521d", "6bab211d-0ba5-4fde-8561-290e8094e41a"),
                    Pair("f1800a44-57cc-4c64-964c-aa7dec32089e", "2991db00-94cb-4350-9fcf-7f1b808be41e"),
                    Pair("c7e2cc98-3e36-406c-ad0c-a2414b72f32f", "1d728aa6-ee37-4410-bf39-0ec64368e172"),
                    Pair("77c2ae89-c08e-4cd7-aa6f-323898d06d14", "081f160d-fbaa-46c0-9414-8aceb43d52b2"),
                    Pair("e3a01e7b-25d8-4146-8cdb-cbc53ca4ac0a", "2866f861-b571-4c5a-991b-8684fb27c0a0"),
                    Pair("945fab2a-6668-462a-862d-a156d226f987", "0ba9dee1-bdcd-4a74-a485-039731c03924"),
                    Pair("7c25ccf3-192c-4e8f-aa9f-9a472119615b", "dbacdee2-be84-465b-9d56-703e5d37c1c4"),
                    Pair("9307e61f-db5d-4d35-8b8d-a7b92195f1fe", "28b71cf9-3f92-4804-ac20-7d6a08934454"),
                    Pair("0ebac5d4-9213-4052-bd38-e941491d2ce7", "c64a1d36-f932-4f25-be99-498b8a96e0e3"),
                    Pair("5790beb4-15ee-4160-9cef-04591c6e0f17", "5bc2d546-65aa-4662-93a9-ec8292432515"),
                    Pair("10e37e5e-da17-4af3-9f51-98e8eecb0e26", "f149e573-2758-4eda-bc3d-d101f47ae0ca"),
                    Pair("45c396c0-55d9-4e2e-a814-74c187a6559e", "080d7d41-be72-4082-bcc6-3a4d06019378"),
                    Pair("a455465a-5f73-4735-aefa-59897efa854a", "62e7bbb2-e47a-4ff5-a942-1601af4a8021"),
                    Pair("a64fd7a6-73c1-40ad-bd26-3e8aeb984cd3", "9b67d491-5469-4b21-9cbe-d9dc55a36001"),
                    Pair("8ffa81b5-e726-4d66-bf3a-69ab2be8e244", "6a8c067c-64a8-49d2-b93e-e0c974174cd9"),
                    Pair("682b209d-0d76-478e-b757-0982fdcc02bf", "694908d6-b80d-4c53-9bc9-6e4623bc12cf"),
                    Pair("cf1172c0-cd9f-4bb9-b48c-3668642a21d7", "7493d041-4c00-474e-9a3a-229f2dcfbea1"),
                    Pair("8fc33d56-704d-42df-8ccf-2b2871e816e7", "45158b69-1423-4cfd-b507-23eb553b31b1"),
                    Pair("54a72f51-47bd-45a7-9a30-2f6218e831f0", "908f7828-f190-404f-906d-490594cff581"),
                    Pair("e18abf31-bff9-421e-b23c-263707282d9c", "cfa64d11-35ff-43a7-81e4-87dfa9366de5"),
                    Pair("1c83ef88-cb01-41e5-9b3a-941894b14831", "62cb816c-173a-4664-a5cc-ca8900f50296"),
                    Pair("68c41605-6675-4fdd-b180-785b4304d6b1", "9ae958d5-f6c1-42ae-b19d-bdcae133eb16"),
                    Pair("3de0cc10-81f7-4dce-b491-c7c9d3e0ede9", "5ed48824-9446-4e28-b329-8d41c9133b32"),
                    Pair("0c9ba19d-fae1-4756-a38a-3a1402a0ca27", "66f3b284-18d2-4b20-b6ba-d71cef8afe92"),
                    Pair("8d7d877f-7e4a-499f-a8d7-6c2637536457", "38a85929-fec4-402e-bce0-858b0c77653d"),
                    Pair("1167cb4c-51c9-4a9e-8f2b-9dc454bd913a", "9bc13d2a-8499-4549-b332-9c754ebf25f3"),
                    Pair("aa1cc9d4-c8fd-4f18-9f67-c32c5428ce35", "2d586171-794e-474e-8fcb-b2b5abc91094"),
                    Pair("3462af99-d269-4974-ac29-4c9530f86f83", "63b22d18-01eb-4d15-acff-713f4d815c04"),
                    Pair("3a6ac50f-704a-4bcb-8554-51bbec2011be", "86385c55-3115-44d0-a115-7b9415e21199"),
                    Pair("52a42f08-7f46-4bce-9f3c-07e642591ac7", "0ad13f93-eca1-4d37-9fd9-979fc728cb86"),
                    Pair("ff881674-869f-469b-9bb6-50047bee4491", "cb5a4b03-8ec0-4706-ae03-241cd4a6a858"),
                    Pair("33b426f0-d79a-42ad-a743-ca8da194143d", "535ceaa9-aad9-4881-924e-53d7858946a1"),
                    Pair("cf62061c-e33e-4b5e-a8e7-1c32d6cdf141", "c22444fc-1a8a-4a18-948c-3c985e389d13"),
                    Pair("7e1c62a9-1fff-4377-b25b-fe888f6ec1f3", "6778688c-71f7-455c-b563-3b2a63e1afbf"),
                    Pair("59c9767b-ccdd-41d0-9802-6b592d43d058", "3b8a36a4-a28e-4931-adf0-b45055a97766"),
                    Pair("2f0a9f78-94ac-4b35-9e5f-af0ea4724ce4", "550f825d-50e5-47ac-8743-737c51d59041"),
                    Pair("cb3304bd-13c2-465c-aa55-4dc53b4c5b9c", "a83c88bf-4c01-45b3-93a3-c223bddb42ad"),
                    Pair("f4cef619-ed90-4a0d-8175-21eefb59ac9f", "a5b7a449-a649-46ba-936d-b7ea0bcd655b"),
                    Pair("72b7caf1-98c8-4291-8e7b-0585bd118c05", "b10270d2-0ff2-41bd-a61a-ac08ec89642b"),
                    Pair("8d79e364-8b3c-44b6-a56d-d23d598bd0aa", "529500b3-1619-4ae0-a6ef-cbbd090ca973"),
                    Pair("09d75d83-f7a3-4b38-a054-aa72f50b7287", "9a7e50ce-3249-4807-80ea-5b06a398595a"),
                    Pair("41cd6f43-14a9-444b-9099-e05838fb89b7", "d31f34dc-0f14-44bd-ab5d-0cd8a017d233"),
                    Pair("1d20a3d5-b610-44e2-b7a9-a13d043625b6", "33418876-9d01-41aa-8a4a-0bfe678f89ec"),
                    Pair("4c5ddf4b-832b-499d-b7fb-0cb0ec78bdd5", "6af7f1ae-debb-47e7-a26e-0a1598e512ad"),
                    Pair("d763359f-c2b1-477b-9ddb-81216f8ecfbd", "e41320a9-0032-42f7-815d-bd79576f0b72"),
                    Pair("999be8b2-df97-4cf8-8aaa-c454c311a2bd", "2857575b-a209-4f83-acc4-8c8e4f5315b7"),
                    Pair("554bfeb1-bb65-46a0-bab1-489f87e9655c", "8a9fa05f-d0b8-4763-8cc2-fed6bdeac9df"),
                    Pair("9370735e-974b-4ad1-a9f5-60cd1088e7a6", "cc243d09-fc05-4b1c-a794-42c75fee7a1d"),
                    Pair("219ff5fd-70c4-459d-a5b2-92c28e0924c2", "c1ea60b9-76bf-457b-8874-b535dfa250a3"),
                    Pair("201fed98-0c0d-4758-86b2-ab1edffd58ea", "4850bcdf-27d6-461c-af91-1f38e8af78d8"),
                    Pair("6ad0d245-9675-45f7-a446-18cb9ce17692", "e189eb9f-0078-4910-87c4-26fe63536213"),
                    Pair("7065124c-4c5f-4f74-a30e-0e59ad4cb69d", "b38d5e9f-6228-407b-b4a8-eaf16d728845"),
                    Pair("e7ae24f3-f9d1-478e-91ef-3cb9ef05dba5", "957b11b8-688f-4c94-bdeb-5317365794b8"),
                    Pair("1c268d5f-8cac-4d08-9015-7b75ad47e632", "3f60bf8a-bbcd-4d0d-9ea8-c31323298c21"),
                    Pair("87a1d8a0-55e3-4226-a87e-274c4563f3ef", "b9299577-ef6f-434c-805c-342077d47392"),
                    Pair("7044fdbc-b845-4a55-967c-ea77e2678ca2", "0d994f3a-dd3a-4889-8f9a-8c73a2ae81f6"),
                    Pair("99c6dd48-42e7-4ac0-8d5b-9662e487afb0", "c4079690-c0f2-4685-a89e-feb969574ab8"),
                    Pair("6c561a0e-cb1a-49e5-b2a9-5b70ace9ac73", "c4ec14bb-89db-431e-99e5-1ef1834ed099"),
                    Pair("c2972377-b1dc-44ad-ba24-25b8e95f1bd9", "0873e8d8-610a-435b-8ec2-d008db8e2784"),
                    Pair("3b481e5a-fadb-4665-b1c2-55e8734668b5", "e6ed243f-0763-4cfc-b4d2-a468ece70118"),
                    Pair("8f119996-94f2-472e-b7ad-3c6f04b0cd4d", "a025d9e0-33e1-4fc9-95a9-76db10dfeb28"),
                    Pair("d3f5cccf-2e46-4758-9dff-64ddb4abd719", "e8506356-b600-4934-8ae7-c2ef7e83af15"),
                    Pair("5fc73e86-14ca-4c41-874f-9bdfd6aa340b", "f79963a2-c6fc-47ef-9d45-200cc527a299"),
                    Pair("4ce79f8f-8c73-45aa-ba85-8306adc689b6", "7a08448d-9004-4024-a9ac-c1a93240a74a"),
                    Pair("9e87c7cf-28fd-4d1a-8329-b0799082f75e", "e77b07b1-f2af-42f5-915b-161be431458d"),
                    Pair("43ea775d-6e02-4854-949d-9935a7ae4081", "8ee24c88-9a29-4af1-85e4-d2827403b98e"),
                )

            behandlingerSomSkalAvbrytes
                .map { Pair(UUID.fromString(it.first), UUID.fromString(it.second)) }
                .forEach { (oppgaveId, behandlingId) ->
                    try {
                        inTransaction {
                            logger.info("Fikser referanse på oppgave $oppgaveId til å peke på behandling $behandlingId")
                            val oppgave = oppgaveDao.hentOppgave(oppgaveId)

                            oppgaveDao.settNySaksbehandler(oppgaveId, Fagsaksystem.EY.navn)
                            oppgaveDao.oppdaterReferanseOgMerknad(
                                oppgaveId,
                                behandlingId.toString(),
                                oppgave?.merknad ?: "",
                            )

                            logger.info("Avbryter behandling $behandlingId med tilhørende oppgave $oppgaveId")
                            val behandling = behandlingService.hentBehandling(behandlingId)
                            if (behandling != null &&
                                behandling.status == BehandlingStatus.OPPRETTET &&
                                behandling.revurderingsaarsak() == Revurderingaarsak.ALDERSOVERGANG
                            ) {
                                behandlingService.avbrytBehandling(
                                    behandlingId,
                                    HardkodaSystembruker.uttrekk,
                                    AarsakTilAvbrytelse.ANNET,
                                    "Aldersovergangsjobb feilet - avbryter behandling for å kjøre jobb på nytt",
                                )

                                logger.info("Behandling $behandlingId ble avbrutt")
                            } else {
                                logger.info("Behandling $behandlingId hadde feil status eller tilstand: ${behandling?.status}")
                            }
                        }
                    } catch (ex: Exception) {
                        logger.error("Feilet ved avbrytelse av behandling $behandlingId ifm feilet aldersovergangsjobb", ex)
                    }
                }

            logger.info("Fullført avbrytelse av behandlinger for aldersovergang")
        }
    }
}
