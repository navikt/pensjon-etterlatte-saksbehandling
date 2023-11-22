import { Info, Overskrift, Tekst, Wrapper } from '~components/behandling/attestering/styled'
import { Generellbehandling, KravpakkeUtland } from '~shared/types/Generellbehandling'
import { genbehandlingTypeTilLesbartNavn } from '~components/person/behandlingsslistemappere'

export const AttestertVisning = (props: {
  utlandsBehandling: Generellbehandling & { innhold: KravpakkeUtland | null }
}) => {
  const { utlandsBehandling } = props
  //TODO: hente attestertdato, saksbehandler og attestant fra hendelser?
  return (
    <Wrapper innvilget={true}>
      <Overskrift>{genbehandlingTypeTilLesbartNavn(utlandsBehandling.type)}</Overskrift>
      <div className="flex">
        <div>
          <Info>Attestant</Info>
          <Tekst></Tekst>
        </div>
        <div>
          <Info>Saksbehandler</Info>
          <Tekst></Tekst>
        </div>
      </div>
      <div className="flex">
        <div>
          <Info>Attestert dato</Info>
          <Tekst></Tekst>
        </div>
      </div>
    </Wrapper>
  )
}
