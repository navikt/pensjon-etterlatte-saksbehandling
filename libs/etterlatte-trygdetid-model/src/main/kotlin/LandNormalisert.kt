package no.nav.etterlatte.libs.common.trygdetid.land

enum class LandNormalisert(
    val isoCode: String,
    val beskrivelse: String,
) {
    // Sorted by ISO 3166-1 alpha-3 code:
    ARUBA("ABW", "Aruba"),
    AFGHANISTAN("AFG", "Afghanistan"),
    ANGOLA("AGO", "Angola"),
    ANGUILLA("AIA", "Anguilla"),
    ALAND("ALA", "Åland"),
    ALBANIA("ALB", "Albania"),
    ANDORRA("AND", "Andorra"),
    NEDERLANDSKE_ANTILLER("ANT", "De nederlandske Antiller"),
    FORENTE_ARABISKE_EMIRATER("ARE", "De forente arabiske emirater"),
    ARGENTINA("ARG", "Argentina"),
    ARMENIA("ARM", "Armenia"),
    AMERIKANSK_SAMOA("ASM", "Amerikansk Samoa"),
    ANTARKTIS("ATA", "Antarktis"),
    DE_FRANSKE_TERRITORIENE_I_SOR("ATF", "De franske territoriene i sør"),
    ANTIGUA_OG_BARBUDA("ATG", "Antigua og Barbuda"),
    AUSTRALIA("AUS", "Australia"),
    OSTERRIKE("AUT", "Østerrike"),
    AZERBAJDZJAN("AZE", "Azerbajdzjan"),
    BURUNDI("BDI", "Burundi"),
    BELGIA("BEL", "Belgia"),
    BENIN("BEN", "Benin"),

    // a.k.a. Karibisk Nederland eller BES-øyene
    BONAIRE_SINT_EUSTATIUS_OG_SABA("BES", "Bonaire, Sint Eustatius og Saba"),
    BURKINA_FASO("BFA", "Burkina Faso"),
    BANGLADESH("BGD", "Bangladesh"),
    BULGARIA("BGR", "Bulgaria"),
    BAHRAIN("BHR", "Bahrain"),
    BAHAMAS("BHS", "Bahamas"),
    BOSNIA_HERCEGOVINA("BIH", "Bosnia-Hercegovina"),
    SAINT_BARTHELEMY("BLM", "Saint Barthelemy"),
    BELARUS("BLR", "Belarus"),
    BELIZE("BLZ", "Belize"),
    BERMUDA("BMU", "Bermuda"),
    BOLIVIA("BOL", "Bolivia"),
    BRASIL("BRA", "Brasil"),
    BARBADOS("BRB", "Barbados"),
    BRUNEI("BRN", "Brunei"),
    BHUTAN("BTN", "Bhutan"),
    BOUVETOYA("BVT", "Bouvetøya"),
    BOTSWANA("BWA", "Botswana"),
    SENTRALAFRIKANSKE_REPUBLIKK("CAF", "Den sentralafrikanske republikk"),
    CANADA("CAN", "Canada"),
    KOKOSOYENE("CCK", "Kokosøyene"),
    SVEITS("CHE", "Sveits"),
    CHILE("CHL", "Chile"),
    KINA("CHN", "Kina"),
    ELFENBENSKYSTEN("CIV", "Elfenbeinskysten"),
    KAMERUN("CMR", "Kamerun"),
    KONGO_DEMOKRATISKE_REPUBLIKK("COD", "Den demokratiske republikken Kongo"),
    KONGO_REPUBLIKKEN("COG", "Republikken Kongo"),
    COOKOYENE("COK", "Cookøyene"),
    COLOMBIA("COL", "Colombia"),
    KOMORENE("COM", "Komorene"),
    KAPP_VERDE("CPV", "Kapp Verde"),
    COSTA_RICA("CRI", "Costa Rica"),
    CUBA("CUB", "Cuba"),
    CURACAO("CUW", "Curacao"),
    CHRISTMASOYA("CXR", "Christmasøya"),
    CAYMANOYENE("CYM", "Caymanøyene"),
    KYPROS("CYP", "Kypros"),
    TSJEKKIA("CZE", "Tsjekkia"),
    TYSKLAND("DEU", "Tyskland"),
    DJIBOUTI("DJI", "Djibouti"),
    DOMINICA("DMA", "Dominica"),
    DANMARK("DNK", "Danmark"),
    DOMINIKANSKE_REPUBLIKK("DOM", "Den dominikanske republikk"),
    ALGERIE("DZA", "Algerie"),
    ECUADOR("ECU", "Ecuador"),
    EGYPT("EGY", "Egypt"),
    ERITREA("ERI", "Eritrea"),
    VEST_SAHARA("ESH", "Vest-Sahara"),
    SPANIA("ESP", "Spania"),
    ESTLAND("EST", "Estland"),
    ETIOPIA("ETH", "Etiopia"),
    FINLAND("FIN", "Finland"),
    FIJI("FJI", "Fiji"),
    FALKLANDSOYENE("FLK", "Falklandsøyene"),
    FRANKRIKE("FRA", "Frankrike"),
    FAROYENE("FRO", "Færøyene"),
    MIKRONESIAFODERASJONEN("FSM", "Mikronesiaføderasjonen"),
    GABON("GAB", "Gabon"),
    STORBRITANNIA("GBR", "Storbritannia"),
    GEORGIA("GEO", "Georgia"),
    GUERNSEY("GGY", "Guernsey"),
    GHANA("GHA", "Ghana"),
    GIBRALTAR("GIB", "Gibraltar"),
    GUINEA("GIN", "Guinea"),
    GUADELOUPE("GLP", "Guadeloupe"),
    GAMBIA("GMB", "Gambia"),
    GUINEA_BISSAU("GNB", "Guinea-Bissau"),
    EKVATORIAL_GUINEA("GNQ", "Ekvatorial-Guinea"),
    HELLAS("GRC", "Hellas"),
    GRENADA("GRD", "Grenada"),
    GRONLAND("GRL", "Grønland"),
    GUATEMALA("GTM", "Guatemala"),
    FRANSK_GUYANA("GUF", "Fransk Guyana"),
    GUAM("GUM", "Guam"),
    GUYANA("GUY", "Guyana"),
    HONGKONG("HKG", "Hongkong"),
    HEARD_OG_MCDONALDOYENE("HMD", "Heard- og McDonaldøyene"),
    HONDURAS("HND", "Honduras"),
    KROATIA("HRV", "Kroatia"),
    HAITI("HTI", "Haiti"),
    UNGARN("HUN", "Ungarn"),
    INDONESIA("IDN", "Indonesia"),
    ISLE_OF_MAN("IMN", "Isle of Man"),
    INDIA("IND", "India"),
    BRITISK_INDIAHAV("IOT", "Det britiske territoriet i Indiahavet"),
    IRLAND("IRL", "Irland"),
    IRAN("IRN", "Iran"),
    IRAK("IRQ", "Irak"),
    ISLAND("ISL", "Island"),
    ISRAEL("ISR", "Israel"),
    ITALIA("ITA", "Italia"),
    JAMAICA("JAM", "Jamaica"),
    JERSEY("JEY", "Jersey"),
    JORDAN("JOR", "Jordan"),
    JAPAN("JPN", "Japan"),
    KAZAKHSTAN("KAZ", "Kazakhstan"),
    KENYA("KEN", "Kenya"),
    KIRGISISTAN("KGZ", "Kirgisistan"),
    KAMBODSJA("KHM", "Kambodsja"),
    KIRIBATI("KIR", "Kiribati"),
    SAINT_KITTS_OG_NEVIS("KNA", "Saint Kitts og Nevis"),
    SOR_KOREA("KOR", "Sør-Korea"),
    KUWAIT("KWT", "Kuwait"),
    LAOS("LAO", "Laos"),
    LIBANON("LBN", "Libanon"),
    LIBERIA("LBR", "Liberia"),
    LIBYA("LBY", "Libya"),
    SAINT_LUCIA("LCA", "Saint Lucia"),
    LIECHTENSTEIN("LIE", "Liechtenstein"),
    SRI_LANKA("LKA", "Sri Lanka"),
    LESOTHO("LSO", "Lesotho"),
    LITAUEN("LTU", "Litauen"),
    LUXEMBOURG("LUX", "Luxembourg"),
    LATVIA("LVA", "Latvia"),
    MACAO("MAC", "Macao"),
    SAINT_MARTIN("MAF", "Saint Martin"),
    MAROKKO("MAR", "Marokko"),
    MONACO("MCO", "Monaco"),
    MOLDOVA("MDA", "Moldova"),
    MADAGASKAR("MDG", "Madagaskar"),
    MALDIVENE("MDV", "Maldivene"),
    MEXICO("MEX", "Mexico"),
    MARSHALLOYENE("MHL", "Marshalløyene"),
    NORD_MAKEDONIA("MKD", "Nord-Makedonia"),
    MALI("MLI", "Mali"),
    MALTA("MLT", "Malta"),
    MYANMAR("MMR", "Myanmar"),
    MONTENEGRO("MNE", "Montenegro"),
    MONGOLIA("MNG", "Mongolia"),
    NORD_MARIANENE("MNP", "Nord-Marianene"),
    MOSAMBIK("MOZ", "Mosambik"),
    MAURITANIA("MRT", "Mauritania"),
    MONSERRAT("MSR", "Monserrat"),
    MARTINIQUE("MTQ", "Martinique"),
    MAURITIUS("MUS", "Mauritius"),
    MALAWI("MWI", "Malawi"),
    MALAYSIA("MYS", "Malaysia"),
    MAYOTTE("MYT", "Mayotte"),
    NAMIBIA("NAM", "Namibia"),
    NY_KALEDONIA("NCL", "Ny-Kaledonia"),
    NIGER("NER", "Niger"),
    NORFOLKOYA("NFK", "Norfolkøya"),
    NIGERIA("NGA", "Nigeria"),
    NICARAGUA("NIC", "Nicaragua"),
    NIUE("NIU", "Niue"),
    NEDERLAND("NLD", "Nederland"),
    NORGE("NOR", "Norge"),
    NEPAL("NPL", "Nepal"),
    NAURU("NRU", "Nauru"),
    NEW_ZEALAND("NZL", "New Zealand"),
    OMAN("OMN", "Oman"),
    PAKISTAN("PAK", "Pakistan"),
    PANAMA("PAN", "Panama"),
    PITCAIRN("PCN", "Pitcairn"),
    PERU("PER", "Peru"),
    FILIPPINENE("PHL", "Filippinene"),
    PALAU("PLW", "Palau"),
    PAPUA_NY_GUINEA("PNG", "Papua Ny-Guinea"),
    POLEN("POL", "Polen"),
    PUERTO_RICO("PRI", "Puerto Rico"),
    NORD_KOREA("PRK", "Nord-Korea"),
    PORTUGAL("PRT", "Portugal"),
    PARAGUAY("PRY", "Paraguay"),
    DET_PALESTINSKE_OMRADET("PSE", "Det palestinske området"),
    FRANSK_POLYNESIA("PYF", "Fransk Polynesia"),
    QATAR("QAT", "Qatar"),
    QUEBEC("QEB", "Quebec"),
    REUNION("REU", "Réunion"),
    ROMANIA("ROU", "Romania"),
    RUSSLAND("RUS", "Russland"),
    RWANDA("RWA", "Rwanda"),
    SAUDI_ARABIA("SAU", "Saudi-Arabia"),
    SUDAN("SDN", "Sudan"),
    SENEGAL("SEN", "Senegal"),
    SINGAPORE("SGP", "Singapore"),
    SOR_GEORGIA_OG_SOR_SANDWICHOYENE("SGS", "Sør-Georgia og Sør-Sandwichøyene"),
    SAINT_HELENA("SHN", "Saint Helena, Ascension og Tristan da Cunha"),
    SVALBARD_OG_JAN_MAYEN("SJM", "Svalbard og Jan Mayen"),
    SALOMONOYENE("SLB", "Salomonøyene"),
    SIERRA_LEONE("SLE", "Sierra Leone"),
    EL_SALVADOR("SLV", "El Salvador"),
    SAN_MARINO("SMR", "San Marino"),
    SOMALIA("SOM", "Somalia"),
    SAINT_PIERRE_OG_MIQUELON("SPM", "Saint-Pierre og Miquelon"),
    SERBIA("SRB", "Serbia"),
    SOR_SUDAN("SSD", "Sør-Sudan"),
    SAO_TOME_OG_PRINCIPE("STP", "Sao Tome og Principe"),
    SURINAM("SUR", "Surinam"),
    SLOVAKIA("SVK", "Slovakia"),
    SLOVENIA("SVN", "Slovenia"),
    SVERIGE("SWE", "Sverige"),
    ESWATINI("SWZ", "Eswatini"), // formerly Swaziland
    SINT_MAARTEN("SXM", "Sint Maarten"),
    SEYCHELLENE("SYC", "Seychellene"),
    SYRIA("SYR", "Syria"),
    TURKS_CAICOSOYENE("TCA", "Turks/Caicosøyene"),
    TCHAD("TCD", "Tchad"),
    TOGO("TGO", "Togo"),
    THAILAND("THA", "Thailand"),
    TADZJIKISTAN("TJK", "Tadzjikistan"),
    TOKELAU("TKL", "Tokelau"),
    TURKMENISTAN("TKM", "Turkmenistan"),
    OST_TIMOR("TLS", "Øst-Timor"),
    TONGA("TON", "Tonga"),
    TRINIDAD_OG_TOBAGO("TTO", "Trinidad og Tobago"),
    TUNISIA("TUN", "Tunisia"),
    TYRKIA("TUR", "Tyrkia"),
    TUVALU("TUV", "Tuvalu"),
    TAIWAN("TWN", "Taiwan"),
    TANZANIA("TZA", "Tanzania"),
    UGANDA("UGA", "Uganda"),
    UKRAINA("UKR", "Ukraina"),
    MINDRE_STILLEHAVSOYER("UMI", "Mindre Stillehavsøyer"),
    URUGUAY("URY", "Uruguay"),
    USA("USA", "USA"),
    UZBEKISTAN("UZB", "Uzbekistan"),
    VATIKANSTATEN("VAT", "Vatikanstaten"),
    SAINT_VINCENT_OG_GRENADINENE("VCT", "Saint Vincent og Grenadinene"),
    VENEZUELA("VEN", "Venezuela"),
    JOMFRUOYENE_BRITISKE("VGB", "De britiske Jomfruøyer"),
    JOMFRUOYENE_AMERIKANSKE("VIR", "De amerikanske Jomfruøyer"),
    VIETNAM("VNM", "Vietnam"),
    VANUATU("VUT", "Vanuatu"),
    WAKOYA("WAK", "Wakøya"),
    WALLIS_OG_FUTUNAOYENE("WLF", "Wallis- og Futunaøyene"),
    SAMOA("WSM", "Samoa"),
    YEMEN("YEM", "Yemen"),
    SOR_AFRIKA("ZAF", "Sør-Afrika"),
    ZAMBIA("ZMB", "Zambia"),
    ZIMBABWE("ZWE", "Zimbabwe"),

    // Historical countries:
    TSJEKKOSLOVAKIA("CSK", "Tsjekkoslovakia"),
    OST_TYSKLAND("DDR", "Øst-Tyskland"),
    SERBIA_OG_MONTENEGRO("SCG", "Serbia og Montenegro"),
    SOVJETUNIONEN("SUN", "Sovjetunionen"),
    JUGOSLAVIA("YUG", "Jugoslavia"),

    // Special codes (not ISO 3166-1 alpha-3):
    SPANSKE_OMRADER_I_AFRIKA("P_349", "Spanske områder i Afrika"),
    SIKKIM("P_546", "Sikkim"),
    YEMEN_("P_556", "Yemen"),
    PANAMAKANALSONEN("P_669", "Panamakanalsonen"),
    UOPPGITT_UKJENT("P_UnkUnkUnk", "Uoppgitt/ukjent"),
    KOSOVO("XXK", "Kosovo"),
    STATSLOS("XXX", "Statløs"),
    ;

    companion object {
        private val mapAvLandkodeOgBeskrivelse = entries.associate { it.isoCode to it.beskrivelse }

        fun hentBeskrivelse(isoCode: String): String? = mapAvLandkodeOgBeskrivelse[isoCode]
    }
}