import { IBoddEllerArbeidetUtlandet, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { LovtekstMedLenke } from '../LovtekstMedLenke'
import { Beskrivelse, VurderingsContainerWrapper } from '../styled'
import { useState } from 'react'
import { LeggTilVurderingButton } from '../LeggTilVurderingButton'
import { BoddEllerArbeidetUtlandetVurdering } from './BoddEllerArbeidetUtlandetVurdering'
import { BodyShort, VStack } from '@navikt/ds-react'

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
    <LovtekstMedLenke tittel="Utlandsopphold" hjemler={[]} status={statusIkon(behandling.boddEllerArbeidetUtlandet)}>
      <Beskrivelse>
        <VStack gap="8">
          <BodyShort>
            Hvis avdøde har bodd og/eller arbeidet i utlandet, kan dette ha innvirkning på rettighetene til og
            størrelsen på etterlatteytelser.
          </BodyShort>
          <BodyShort>
            Har avdøde utlandsopphold må det hukes av for de alternativ som gjelder. Velg aktuelle alternativ, og noter
            gjeldende land for de ulike alternativene i begrunnelsesfeltet.
          </BodyShort>
          <BodyShort>
            Vurdere avdødes trygdeavtale: huk av kun hvis det er bosatt utland-sak. Dette vurderes først i
            sluttbehandling, etter opplysninger fra utland er mottatt, i bosatt Norge-saker.
          </BodyShort>
          <BodyShort>
            Det skal sendes kravpakke: huk av kun hvis det er bosatt Norge-sak og det skal sendes krav til involverte
            avtaleland det ikke er sendt krav til tidligere i annen sak til avdød. Se spørsmålstegn for mer informasjon.
          </BodyShort>
        </VStack>
      </Beskrivelse>
      <VurderingsContainerWrapper>
        {vurdert ? (
          <BoddEllerArbeidetUtlandetVurdering
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
