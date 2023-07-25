import { Revurderingsaarsak } from '~shared/types/Revurderingsaarsak'
import { useBehandling } from '~components/behandling/useBehandling'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { formaterKanskjeStringDato, formaterStringDato } from '~utils/formattering'
import { mapApiResult, useApiCall } from '~shared/hooks/useApiCall'
import { useEffect } from 'react'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { hentFoersteVirk } from '~shared/api/behandling'
import { Label } from '@navikt/ds-react'
import { HistorikkElement } from '~components/behandling/soeknadsoversikt/styled'
import { formaterNavn, IPdlPerson } from '~shared/types/Person'
import { getHistoriskForeldreansvar } from '~shared/api/grunnlag'

const SoekerDoedsdatoGrunnlag = () => {
  const behandling = useBehandling()
  const soekerDoedsdato = behandling?.søker?.doedsdato

  return (
    <Info
      label="Dødsdato mottaker av ytelsen"
      tekst={soekerDoedsdato ? formaterStringDato(soekerDoedsdato) : 'Ingen dødsdato registrert'}
    />
  )
}

const OmgjoeringFarskapGrunnlag = () => {
  const behandling = useBehandling()
  const [foersteVirk, hentVirk] = useApiCall(hentFoersteVirk)
  useEffect(() => {
    if (behandling?.sakId) {
      hentVirk({ sakId: behandling?.sakId })
    }
  }, [behandling?.sakId])

  return mapApiResult(
    foersteVirk,
    <Spinner visible={true} label="Henter første virkningstidspunkt" />,
    () => <ApiErrorAlert>Kunne ikke hente første virkningstidspunkt</ApiErrorAlert>,
    (foersteVirk) => (
      <Info tekst={formaterStringDato(foersteVirk.foersteIverksatteVirkISak)} label="Første virkningstidspunkt i sak" />
    )
  )
}

function isNotUndefined<T>(v: T | undefined): v is T {
  return v !== undefined
}

const AdopsjonGrunnlag = () => {
  const behandling = useBehandling()
  const [foreldreansvar, hentForeldreansvar] = useApiCall(getHistoriskForeldreansvar)

  const foreldre = [behandling?.familieforhold?.avdoede, behandling?.familieforhold?.gjenlevende]
    .filter(isNotUndefined)
    .map((person) => person.opplysning)
    .reduce(
      (personer, nestePerson) => ({ ...personer, [nestePerson.foedselsnummer]: nestePerson }),
      {} as Record<string, IPdlPerson>
    )

  useEffect(() => {
    if (!behandling?.sakId) {
      return
    }
    hentForeldreansvar({ sakId: behandling.sakId })
  }, [behandling?.sakId])

  return mapApiResult(
    foreldreansvar,
    <Spinner visible={true} label="Henter historikk for foreldreansvar" />,
    () => <ApiErrorAlert>Kunne ikke hente foreldreansvar</ApiErrorAlert>,
    (foreldreansvar) => (
      <div>
        <Label size="small" as="p">
          Foreldreansvar
        </Label>
        {foreldreansvar.opplysning.ansvarligeForeldre.map((forelderPeriode) => {
          const forelder: IPdlPerson | undefined = foreldre[forelderPeriode.forelder]
          return (
            <HistorikkElement key={`${forelderPeriode.forelder}-${forelderPeriode.fraDato}`}>
              <span className="date">
                {formaterKanskjeStringDato(forelderPeriode.fraDato)} -{' '}
                {formaterKanskjeStringDato(forelderPeriode.tilDato)}
              </span>
              <span>
                {forelder !== undefined
                  ? `${formaterNavn(forelder)} (${forelder.foedselsnummer})`
                  : forelderPeriode.forelder}
              </span>
            </HistorikkElement>
          )
        })}
      </div>
    )
  )
}

export const GrunnlagForVirkningstidspunkt = () => {
  const behandling = useBehandling()
  switch (behandling?.revurderingsaarsak) {
    case Revurderingsaarsak.DOEDSFALL:
      return <SoekerDoedsdatoGrunnlag />
    case Revurderingsaarsak.OMGJOERING_AV_FARSKAP:
      return <OmgjoeringFarskapGrunnlag />
    case Revurderingsaarsak.ADOPSJON:
      return <AdopsjonGrunnlag />
  }
  return null
}
