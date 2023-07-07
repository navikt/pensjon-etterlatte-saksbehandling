import { Revurderingsaarsak } from '~shared/types/Revurderingsaarsak'
import { useBehandling } from '~components/behandling/useBehandling'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { formaterStringDato } from '~utils/formattering'
import { mapApiResult, useApiCall } from '~shared/hooks/useApiCall'
import { useEffect } from 'react'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { hentFoersteVirk } from '~shared/api/behandling'

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

export const GrunnlagForVirkningstidspunkt = () => {
  const behandling = useBehandling()
  switch (behandling?.revurderingsaarsak) {
    case Revurderingsaarsak.DOEDSFALL:
      return <SoekerDoedsdatoGrunnlag />
    case Revurderingsaarsak.OMGJOERING_AV_FARSKAP:
      return <OmgjoeringFarskapGrunnlag />
  }
  return null
}
