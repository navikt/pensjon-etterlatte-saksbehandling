import { KommerBarnetTilGodeVurdering } from './KommerBarnetTilGodeVurdering'
import { IKommerBarnetTilgode } from '~shared/types/IDetaljertBehandling'
import { IPdlPerson } from '~shared/types/Person'
import { formaterGrunnlagKilde, svarTilStatusIcon } from '../utils'
import { SoeknadVurdering } from '../SoeknadVurdering'
import { Info } from '../Info'
import { Personopplysning } from '~shared/types/grunnlag'
import { IAdresse } from '~shared/types/IAdresse'
import { Box, HStack } from '@navikt/ds-react'
import { Foreldreansvar } from '~components/behandling/soeknadsoversikt/gyldigFramsattSoeknad/barnepensjon/Foreldreansvar'

interface AdresseProps {
  label: string
  adresse: IAdresse
  kilde?: string
}

const AdresseKort = (props: AdresseProps) => {
  const adresse = `${props.adresse.adresseLinje1}, ${props.adresse.postnr} ${props.adresse.poststed || ''}`
  return <Info label={props.label} tekst={adresse} undertekst={props.kilde} />
}

interface Props {
  kommerBarnetTilgode: IKommerBarnetTilgode | null
  soeker: Personopplysning | undefined
  gjenlevendeForelder: Personopplysning | undefined
  redigerbar: boolean
  behandlingId: string
  innsender: IPdlPerson | undefined
  avdoed: string[]
}

export const OversiktKommerBarnetTilgode = ({
  kommerBarnetTilgode,
  redigerbar,
  soeker,
  gjenlevendeForelder,
  behandlingId,
  innsender,
  avdoed,
}: Props) => {
  const soekerOpplysning = soeker?.opplysning
  const bostedsadresse = soekerOpplysning?.bostedsadresse?.find((adresse) => adresse.aktiv)
  const foreldersadresse = gjenlevendeForelder?.opplysning?.bostedsadresse?.find((adresse) => adresse.aktiv)
  const innsenderAdresse = innsender?.bostedsadresse?.find((adresse) => adresse.aktiv)
  const innsenderFnr = innsender?.foedselsnummer
  const innsenderHarForeldreAnsvar = innsenderFnr
    ? soekerOpplysning?.familieRelasjon?.ansvarligeForeldre?.includes(innsenderFnr)
    : false

  const gjenlevendeHarForeldreAnsvar = gjenlevendeForelder?.opplysning.foedselsnummer
    ? soekerOpplysning?.familieRelasjon?.ansvarligeForeldre?.includes(gjenlevendeForelder.opplysning.foedselsnummer)
    : false
  const innsenderErGjenlevende = innsenderFnr === gjenlevendeForelder?.opplysning?.foedselsnummer

  const skalViseGjenlevendeForelderAdresse = gjenlevendeHarForeldreAnsvar && foreldersadresse
  const skalViseInnsenderAdresse = !innsenderErGjenlevende && innsenderHarForeldreAnsvar && innsenderAdresse

  return (
    <SoeknadVurdering
      tittel="Vurdering - kommer pensjonen barnet til gode?"
      hjemler={[
        { lenke: 'https://lovdata.no/lov/1997-02-28-19/§18-1', tittel: 'Folketrygdloven § 18-1' },
        { lenke: 'https://lovdata.no/lov/1997-02-28-19/§22-1', tittel: 'Folketrygdloven § 22-1' },
        { lenke: 'https://lovdata.no/lov/2010-03-26-9/§9', tittel: 'Vergemålsloven § 9, § 16 og § 19' },
        { lenke: 'https://lovdata.no/lov/1981-04-08-7/§30', tittel: 'Barneloven § 30 og § 38' },
      ]}
      status={svarTilStatusIcon(kommerBarnetTilgode?.svar)}
    >
      <div>
        <Box marginBlock="space-2" marginInline="space-0" maxWidth="41rem">
          Undersøk om boforholdet er avklart og det er sannsynlig at pensjonen kommer barnet til gode.
        </Box>

        <HStack gap="space-4">
          {bostedsadresse && (
            <AdresseKort label="Barnets adresse" adresse={bostedsadresse} kilde={bostedsadresse?.kilde} />
          )}
          {skalViseGjenlevendeForelderAdresse && (
            <AdresseKort
              label="Gjenlevende forelders adresse"
              adresse={foreldersadresse}
              kilde={formaterGrunnlagKilde(gjenlevendeForelder?.kilde)}
            />
          )}

          {skalViseInnsenderAdresse && (
            <AdresseKort label="Innsenders adresse" adresse={innsenderAdresse} kilde={innsenderAdresse?.kilde} />
          )}

          {!skalViseInnsenderAdresse && !skalViseGjenlevendeForelderAdresse && (
            <Foreldreansvar
              harKildePesys={false}
              soekerGrunnlag={soeker}
              gjenlevendeGrunnlag={gjenlevendeForelder}
              innsender={innsenderFnr}
              avdoed={avdoed}
            />
          )}
        </HStack>
      </div>

      <Box
        paddingInline="space-2 space-0"
        minWidth="18.75rem"
        width="10rem"
        borderWidth="0 0 0 2"
        borderColor="neutral-subtle"
      >
        <KommerBarnetTilGodeVurdering
          kommerBarnetTilgode={kommerBarnetTilgode}
          redigerbar={redigerbar}
          behandlingId={behandlingId}
        />
      </Box>
    </SoeknadVurdering>
  )
}
