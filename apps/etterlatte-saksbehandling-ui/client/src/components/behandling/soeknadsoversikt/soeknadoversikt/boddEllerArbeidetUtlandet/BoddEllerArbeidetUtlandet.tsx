import { IBoddEllerArbeidetUtlandet, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { LovtekstMedLenke } from '../LovtekstMedLenke'
import { Beskrivelse, VurderingsContainerWrapper } from '../../styled'
import { useState } from 'react'
import { LeggTilVurderingButton } from '../LeggTilVurderingButton'
import { BoddEllerArbeidetUtlandetVurdering } from './BoddEllerArbeidetUtlandetVurdering'

const statusIkon = (boddEllerArbeidetUtlandet: IBoddEllerArbeidetUtlandet | null) => {
  if (boddEllerArbeidetUtlandet) {
    return 'success'
  }
  return 'warning'
}

export const BoddEllerArbeidetUtlandet = ({
  behandling,
  redigerbar,
}: {
  behandling: IDetaljertBehandling
  redigerbar: boolean
}) => {
  const [vurdert, setVurdert] = useState<boolean>(!!behandling.boddEllerArbeidetUtlandet)

  return (
    <LovtekstMedLenke tittel="Utenlandsopphold" hjemler={[]} status={statusIkon(behandling.boddEllerArbeidetUtlandet)}>
      <Beskrivelse>Har avdøde bodd eller arbeidet i utlandet?</Beskrivelse>
      <VurderingsContainerWrapper>
        {vurdert ? (
          <BoddEllerArbeidetUtlandetVurdering
            boddEllerArbeidetUtlandet={behandling.boddEllerArbeidetUtlandet}
            redigerbar={redigerbar}
            setVurdert={(visVurderingKnapp: boolean) => setVurdert(visVurderingKnapp)}
            behandlingId={behandling.id}
          />
        ) : (
          <LeggTilVurderingButton onClick={() => setVurdert(true)}>Legg til vurdering</LeggTilVurderingButton>
        )}
      </VurderingsContainerWrapper>
    </LovtekstMedLenke>
  )
}
