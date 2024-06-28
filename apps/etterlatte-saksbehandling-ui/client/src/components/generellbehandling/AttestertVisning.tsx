import { Info, Overskrift, Tekst, Wrapper } from '~components/behandling/attestering/styled'
import { Generellbehandling, KravpakkeUtland } from '~shared/types/Generellbehandling'
import { genbehandlingTypeTilLesbartNavn } from '~components/person/sakOgBehandling/behandlingsslistemappere'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'
import { formaterKanskjeStringDatoMedFallback } from '~utils/formatering/dato'

export const AttestertVisning = (props: {
  utlandsBehandling: Generellbehandling & { innhold: KravpakkeUtland | null }
}) => {
  const { utlandsBehandling } = props

  return (
    <Wrapper $innvilget={true}>
      <Overskrift>{genbehandlingTypeTilLesbartNavn(utlandsBehandling.type)}</Overskrift>
      <div className="flex">
        <div>
          <Info>Attestant</Info>
          <Tekst>{utlandsBehandling.attestant?.attestant}</Tekst>
        </div>
        <div>
          <Info>Saksbehandler</Info>
          <Tekst>{utlandsBehandling.behandler?.saksbehandler}</Tekst>
        </div>
      </div>
      <div className="flex">
        <div>
          <Info>Attestert dato</Info>
          <Tekst>
            {formaterKanskjeStringDatoMedFallback('Ikke registrert', utlandsBehandling.attestant?.tidspunkt)}
          </Tekst>
        </div>
        <div>
          <Info>Behandlet dato</Info>
          <Tekst>
            {formaterKanskjeStringDatoMedFallback('Ikke registrert', utlandsBehandling.behandler?.tidspunkt)}
          </Tekst>
        </div>
      </div>
      <KopierbarVerdi value={utlandsBehandling.sakId.toString()} />
    </Wrapper>
  )
}
