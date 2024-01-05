import { useBehandling } from '~components/behandling/useBehandling'
import { Info } from '~components/behandling/soeknadsoversikt/Info'
import { formaterStringDato } from '~utils/formattering'
import { ReactNode } from 'react'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { formaterGrunnlagKilde } from '~components/behandling/soeknadsoversikt/utils'

export const GrunnlagForVirkningstidspunkt = () => {
  const behandling = useBehandling()
  return (
    <>
      {doedsdatoer()}
      {behandling?.soeknadMottattDato && (
        <Info key="soeknadMottatt" label="Søknad mottatt" tekst={formaterStringDato(behandling.soeknadMottattDato)} />
      )}
    </>
  )
}

function doedsdatoer(): ReactNode[] {
  const personopplysninger = usePersonopplysninger()
  const avdoede = personopplysninger?.avdoede || []
  const nodes = avdoede.map((avdod, index) => (
    <Info
      key={index}
      label="Dødsdato"
      tekst={avdod?.opplysning.doedsdato ? formaterStringDato(avdod?.opplysning.doedsdato) : 'Ikke registrert'}
      undertekst={formaterGrunnlagKilde(avdod?.kilde)}
    />
  ))
  if (!avdoede.find((it) => it.opplysning.doedsdato)) {
    nodes.push(<Info key="manglerDoedsdato" label="Dødsdato" tekst="Ikke registrert" />)
  }
  return nodes
}
