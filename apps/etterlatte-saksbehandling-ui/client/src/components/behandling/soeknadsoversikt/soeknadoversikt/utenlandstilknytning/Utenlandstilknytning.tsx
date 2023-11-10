import { IDetaljertBehandling, IUtenlandstilknytning } from '~shared/types/IDetaljertBehandling'
import { LovtekstMedLenke } from '../LovtekstMedLenke'
import { Beskrivelse, VurderingsContainerWrapper } from '../../styled'
import { useState } from 'react'
import { LeggTilVurderingButton } from '../LeggTilVurderingButton'
import { UtenlandstilknytningVurdering } from './UtenlandstilknytningVurdering'

const statusIkon = (utenlandstilsnitt: IUtenlandstilknytning | null) => {
  if (utenlandstilsnitt === null) {
    return 'warning'
  }
  return 'success'
}

export const Utenlandstilknytning = ({
  behandling,
  redigerbar,
}: {
  behandling: IDetaljertBehandling
  redigerbar: boolean
}) => {
  const [vurdert, setVurdert] = useState(behandling.utenlandstilknytning !== null)

  return (
    <LovtekstMedLenke tittel="Utlandstilknytning" hjemler={[]} status={statusIkon(behandling.utenlandstilknytning)}>
      <Beskrivelse>
        Svar for om saken skal behandles som følge av utlandstilknytning basert på om avdøde har bodd/arbeidet i
        EØS/avtale-land eller ikke, og om gjenlevende bor i Norge eller utlandet. Om søker bor i utlandet er det en
        bosatt utland-sak, om avdøde har bodd/arbeidet i EØS/avtale-land og gjenlevende bor i Norge er det en
        utlandstilsnitt-sak. I andre tilfeller er det en nasjonal sak.
      </Beskrivelse>
      <VurderingsContainerWrapper>
        {vurdert ? (
          <UtenlandstilknytningVurdering
            utenlandstilknytning={behandling.utenlandstilknytning}
            redigerbar={redigerbar}
            setVurdert={(visVurderingKnapp: boolean) => setVurdert(visVurderingKnapp)}
            sakId={behandling.sakId}
          />
        ) : (
          <LeggTilVurderingButton onClick={() => setVurdert(true)}>Legg til vurdering</LeggTilVurderingButton>
        )}
      </VurderingsContainerWrapper>
    </LovtekstMedLenke>
  )
}
