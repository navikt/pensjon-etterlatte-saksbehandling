import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'
import { useBehandling } from '~components/behandling/useBehandling'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { formaterDato, formaterKanskjeStringDato } from '~utils/formatering/dato'
import { useApiCall } from '~shared/hooks/useApiCall'
import { useEffect } from 'react'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { hentFoersteVirk } from '~shared/api/behandling'
import { Alert, Box, Label, VStack } from '@navikt/ds-react'
import { formaterNavn, IPdlPerson } from '~shared/types/Person'
import { getHistoriskForeldreansvar } from '~shared/api/grunnlag'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'

import { mapApiResult } from '~shared/api/apiUtils'
import { SakType } from '~shared/types/sak'
import { formaterGrunnlagKilde } from '~components/behandling/soeknadsoversikt/utils'
import { addYears, isBefore } from 'date-fns'

const SoekerDoedsdatoGrunnlag = () => {
  const soeker = usePersonopplysninger()?.soeker?.opplysning
  const soekerDoedsdato = soeker?.doedsdato

  return (
    <Info
      label="Dødsdato mottaker av ytelsen"
      tekst={soekerDoedsdato ? formaterDato(soekerDoedsdato) : 'Ingen dødsdato registrert'}
    />
  )
}

const FoersteVirkGrunnlag = () => {
  const behandling = useBehandling()
  const [foersteVirk, hentVirk] = useApiCall(hentFoersteVirk)
  useEffect(() => {
    if (behandling?.sakId) {
      hentVirk({ sakId: behandling?.sakId })
    }
  }, [behandling?.sakId])

  return mapApiResult(
    foersteVirk,
    <Spinner label="Henter første virkningstidspunkt" />,
    () => <ApiErrorAlert>Kunne ikke hente første virkningstidspunkt</ApiErrorAlert>,
    (foersteVirk) => (
      <>
        <Info tekst={formaterDato(foersteVirk.foersteIverksatteVirkISak)} label="Første virkningstidspunkt i sak" />

        {behandling?.virkningstidspunkt &&
          isBefore(new Date(behandling.virkningstidspunkt.dato), new Date(foersteVirk.foersteIverksatteVirkISak)) && (
            <Alert variant="warning">
              <strong>OBS:</strong> Nytt virkningstidspunkt er <i>før</i> første virkningstidspunkt i sak!
            </Alert>
          )}
      </>
    )
  )
}

function isNotUndefined<T>(v: T | undefined): v is T {
  return v !== undefined
}

const AdopsjonGrunnlag = () => {
  const behandling = useBehandling()
  const personopplysninger = usePersonopplysninger()
  const avdoede = personopplysninger?.avdoede?.find((po) => po)
  const gjenlevende = personopplysninger?.gjenlevende?.find((po) => po)
  const [foreldreansvar, hentForeldreansvar] = useApiCall(getHistoriskForeldreansvar)

  const foreldre = [avdoede, gjenlevende]
    .filter(isNotUndefined)
    .map((person) => person.opplysning)
    .reduce(
      (personer, nestePerson) => ({ ...personer, [nestePerson.foedselsnummer]: nestePerson }),
      {} as Record<string, IPdlPerson>
    )

  useEffect(() => {
    if (!behandling?.sakId || !behandling.id) {
      return
    }
    hentForeldreansvar({ sakId: behandling.sakId, behandlingId: behandling.id })
  }, [behandling?.sakId, behandling?.id])

  return mapApiResult(
    foreldreansvar,
    <Spinner label="Henter historikk for foreldreansvar" />,
    () => <ApiErrorAlert>Kunne ikke hente foreldreansvar</ApiErrorAlert>,
    (foreldreansvar) => (
      <div>
        <Label size="small" as="p">
          Foreldreansvar
        </Label>
        {foreldreansvar.opplysning.ansvarligeForeldre.map((forelderPeriode) => {
          const forelder: IPdlPerson | undefined = foreldre[forelderPeriode.forelder]
          return (
            <VStack key={`${forelderPeriode.forelder}-${forelderPeriode.fraDato}`}>
              <Box paddingInline="space-0 space-2">
                {formaterKanskjeStringDato(forelderPeriode.fraDato)} -{' '}
                {formaterKanskjeStringDato(forelderPeriode.tilDato)}
              </Box>
              <span>
                {forelder !== undefined
                  ? `${formaterNavn(forelder)} (${forelder.foedselsnummer})`
                  : forelderPeriode.forelder}
              </span>
            </VStack>
          )
        })}
      </div>
    )
  )
}

const BrukerFyller20AarGrunnlag = () => {
  const soeker = usePersonopplysninger()?.soeker
  const foedselsdato = soeker?.opplysning?.foedselsdato
  return (
    foedselsdato && (
      <Info
        label="Bruker 20 år (21 år)"
        tekst={formaterDato(addYears(foedselsdato, 20)) + ' (' + formaterDato(addYears(foedselsdato, 21)) + ')'}
        undertekst={formaterGrunnlagKilde(soeker?.kilde)}
      />
    )
  )
}

const BrukerFyller67AarGrunnlag = () => {
  const soeker = usePersonopplysninger()?.soeker
  const foedselsdato = soeker?.opplysning?.foedselsdato
  return (
    foedselsdato && (
      <Info
        label="Bruker 67 år"
        tekst={formaterDato(addYears(foedselsdato, 67))}
        undertekst={formaterGrunnlagKilde(soeker?.kilde)}
      />
    )
  )
}

export const GrunnlagForVirkningstidspunkt = () => {
  const behandling = useBehandling()
  switch (behandling?.revurderingsaarsak) {
    case Revurderingaarsak.DOEDSFALL:
      return <SoekerDoedsdatoGrunnlag />
    case Revurderingaarsak.OMGJOERING_AV_FARSKAP:
    case Revurderingaarsak.YRKESSKADE:
      return <FoersteVirkGrunnlag />
    case Revurderingaarsak.ADOPSJON:
      return <AdopsjonGrunnlag />
    case Revurderingaarsak.ALDERSOVERGANG:
      return (
        (behandling.sakType === SakType.BARNEPENSJON && <BrukerFyller20AarGrunnlag />) || <BrukerFyller67AarGrunnlag />
      )
  }
  return null
}
