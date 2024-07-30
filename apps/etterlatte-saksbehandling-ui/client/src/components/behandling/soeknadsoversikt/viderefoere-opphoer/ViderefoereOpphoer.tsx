import { IDetaljertBehandling, ViderefoertOpphoer } from '~shared/types/IDetaljertBehandling'
import { LovtekstMedLenke } from '../LovtekstMedLenke'
import { Informasjon, Vurdering } from '../styled'
import { useState } from 'react'
import { BodyShort, Button } from '@navikt/ds-react'
import { ViderefoereOpphoerVurdering } from '~components/behandling/soeknadsoversikt/viderefoere-opphoer/ViderefoereOpphoerVurdering'

const statusIkon = (viderefoertOpphoer: ViderefoertOpphoer | null) =>
  viderefoertOpphoer === null ? 'warning' : 'success'

export const ViderefoereOpphoer = ({
  behandling,
  redigerbar,
}: {
  behandling: IDetaljertBehandling
  redigerbar: boolean
}) => {
  const [vurdert, setVurdert] = useState(behandling.viderefoertOpphoer !== null)

  return (
    <LovtekstMedLenke tittel="Opphør fra og med" hjemler={[]} status={statusIkon(behandling.viderefoertOpphoer)}>
      <Informasjon>
        <BodyShort>
          Er opphørsdato tidligere enn dagens dato, eller skal saken opphøre i nær fremtid fordi vilkårene ikke lenger
          er oppfylt?
        </BodyShort>
        <BodyShort>
          Hvis brukers pensjon skal opphøre før behandlingsdatoen, så velg den måneden pensjonen skal opphøre fra. Legg
          også inn opphørstidspunkt dersom pensjonen skal opphøre f.eks. ved aldersovergang i så nær fremtid at den ikke
          blir behandlet av det automatiske opphøret.
        </BodyShort>
      </Informasjon>
      <Vurdering>
        {vurdert && (
          <ViderefoereOpphoerVurdering
            virkningstidspunkt={behandling.virkningstidspunkt ? new Date(behandling.virkningstidspunkt.dato) : null}
            viderefoertOpphoer={behandling.viderefoertOpphoer}
            redigerbar={redigerbar}
            setVurdert={(visVurderingKnapp: boolean) => setVurdert(visVurderingKnapp)}
            behandlingId={behandling.id}
          />
        )}
        {!vurdert && redigerbar && (
          <Button variant="secondary" onClick={() => setVurdert(true)}>
            Legg til vurdering
          </Button>
        )}
      </Vurdering>
    </LovtekstMedLenke>
  )
}
