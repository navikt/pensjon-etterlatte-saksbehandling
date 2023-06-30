import { Revurderingsaarsak } from '~shared/types/Revurderingsaarsak'
import { useBehandling } from '~components/behandling/useBehandling'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { formaterStringDato } from '~utils/formattering'

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

export const GrunnlagForVirkningstidspunkt = () => {
  const behandling = useBehandling()
  if (behandling?.revurderingsaarsak === Revurderingsaarsak.DOEDSFALL) {
    return <SoekerDoedsdatoGrunnlag />
  }
  return null
}
