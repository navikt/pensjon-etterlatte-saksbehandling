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
  const [vurdert, setVurdert] = useState(behandling.opphoerFom !== null)

  return (
    <LovtekstMedLenke tittel="Opphør f.o.m." hjemler={[]} status={statusIkon(behandling.opphoerFom)}>
      <Informasjon>Skal det opphøre?</Informasjon>
      <Vurdering>
        {vurdert && (
          <ViderefoereOpphoerVurdering
            behandling={behandling}
            viderefoertOpphoer={behandling.opphoerFom}
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
