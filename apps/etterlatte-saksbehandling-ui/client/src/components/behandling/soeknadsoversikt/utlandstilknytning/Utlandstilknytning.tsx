import { IDetaljertBehandling, IUtlandstilknytning } from '~shared/types/IDetaljertBehandling'
import { LovtekstMedLenke } from '../LovtekstMedLenke'
import { Informasjon, Vurdering } from '../styled'
import { useState } from 'react'
import { UtlandstilknytningVurdering } from './UtlandstilknytningVurdering'
import { Button } from '@navikt/ds-react'

const statusIkon = (utlandstilknytning: IUtlandstilknytning | null) => {
  if (utlandstilknytning === null) {
    return 'warning'
  }
  return 'success'
}

export const Utlandstilknytning = ({
  behandling,
  redigerbar,
}: {
  behandling: IDetaljertBehandling
  redigerbar: boolean
}) => {
  const [vurdert, setVurdert] = useState(behandling.utlandstilknytning !== null)

  return (
    <LovtekstMedLenke tittel="Utlandstilknytning" hjemler={[]} status={statusIkon(behandling.utlandstilknytning)}>
      <Informasjon>
        Svar for om saken skal behandles som følge av utlandstilknytning basert på om avdøde har bodd/arbeidet i
        EØS/avtale-land eller ikke, og om gjenlevende bor i Norge eller utlandet. Om søker bor i utlandet er det en
        bosatt utland-sak, om avdøde har bodd/arbeidet i EØS/avtale-land og gjenlevende bor i Norge er det en
        utlandstilsnitt-sak. I andre tilfeller er det en nasjonal sak.
      </Informasjon>
      <Vurdering>
        {vurdert && (
          <UtlandstilknytningVurdering
            utlandstilknytning={behandling.utlandstilknytning}
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
