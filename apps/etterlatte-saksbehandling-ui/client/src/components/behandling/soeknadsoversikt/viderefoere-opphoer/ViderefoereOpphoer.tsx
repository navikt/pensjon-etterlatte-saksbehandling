import { IDetaljertBehandling, ViderefoertOpphoer } from '~shared/types/IDetaljertBehandling'
import { LovtekstMedLenke } from '../LovtekstMedLenke'
import { Informasjon, Vurdering } from '../styled'
import { useState } from 'react'
import { Button } from '@navikt/ds-react'
import { ViderefoereOpphoerVurdering } from '~components/behandling/soeknadsoversikt/viderefoere-opphoer/ViderefoereOpphoerVurdering'

const statusIkon = (viderefoertOpphoer: ViderefoertOpphoer | null) => {
  if (viderefoertOpphoer === null) {
    return 'warning'
  }
  return 'success'
}

export const ViderefoereOpphoer = ({
  behandling,
  redigerbar,
}: {
  behandling: IDetaljertBehandling
  redigerbar: boolean
}) => {
  const [vurdert, setVurdert] = useState(behandling.viderefoertOpphoer !== null)

  return (
    <LovtekstMedLenke tittel="Opphør f.o.m." hjemler={[]} status={statusIkon(behandling.viderefoertOpphoer)}>
      <Informasjon>Kommer det opphør nært forestående, fordi et av vilkårene snart ikke lenger er oppfylt?</Informasjon>
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
