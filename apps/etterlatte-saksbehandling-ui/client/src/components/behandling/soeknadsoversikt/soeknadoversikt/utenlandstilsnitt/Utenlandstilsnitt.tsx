import { IDetaljertBehandling, IUtenlandstilsnitt } from '~shared/types/IDetaljertBehandling'
import { Soeknadsvurdering } from '../SoeknadsVurdering'
import { Beskrivelse, VurderingsContainerWrapper } from '../../styled'
import { useState } from 'react'
import { LeggTilVurderingButton } from '../LeggTilVurderingButton'
import { UtenlandstilsnittVurdering } from './UtenlandstilsnittVurdering'

const statusIkon = (utenlandstilsnitt: IUtenlandstilsnitt | undefined) => {
  if (utenlandstilsnitt == undefined) {
    return 'warning'
  }
  return 'success'
}

export const Utenlandstilsnitt = ({
  behandling,
  redigerbar,
}: {
  behandling: IDetaljertBehandling
  redigerbar: boolean
}) => {
  const [vurder, setVurder] = useState(behandling.utenlandstilsnitt !== null)

  return (
    <Soeknadsvurdering tittel="Utlandstilknytning" hjemler={[]} status={statusIkon(behandling.utenlandstilsnitt)}>
      <Beskrivelse>
        Svar for om saken skal behandles som følge av utlandstilknytning basert på om avdøde har bodd/arbeidet i
        EØS/avtale-land eller ikke, og om gjenlevende bor i Norge eller utlandet. Om søker bor i utlandet er det en
        bosatt utland-sak, om avdøde har bodd/arbeidet i EØS/avtale-land og gjenlevende bor i Norge er det en
        utlandstilsnitt-sak. I andre tilfeller er det en nasjonal sak.
      </Beskrivelse>
      <VurderingsContainerWrapper>
        {vurder ? (
          <UtenlandstilsnittVurdering
            utenlandstilsnitt={behandling.utenlandstilsnitt}
            redigerbar={redigerbar}
            setVurder={(visVurderingKnapp: boolean) => setVurder(visVurderingKnapp)}
            behandlingId={behandling.id}
          />
        ) : (
          <LeggTilVurderingButton onClick={() => setVurder(true)}>Legg til vurdering</LeggTilVurderingButton>
        )}
      </VurderingsContainerWrapper>
    </Soeknadsvurdering>
  )
}
