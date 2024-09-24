import { IBoddEllerArbeidetUtlandet, IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { LovtekstMedLenke } from '../LovtekstMedLenke'
import { Informasjon, Vurdering } from '../styled'
import { useState } from 'react'
import { BodyShort, Button, VStack } from '@navikt/ds-react'
import { TidligereFamiliepleierVurdering } from '~components/behandling/soeknadsoversikt/tidligereFamiliepleier/TidligereFamiliepleierVurdering'

const statusIkon = (boddEllerArbeidetUtlandet: IBoddEllerArbeidetUtlandet | null) => {
  if (boddEllerArbeidetUtlandet) {
    return 'success'
  }
  return 'warning'
}

export const TidligereFamiliepleier = ({
  behandling,
  redigerbar,
}: {
  behandling: IDetaljertBehandling
  redigerbar: boolean
}) => {
  const [vurdert, setVurdert] = useState<boolean>(!!behandling.boddEllerArbeidetUtlandet)

  return (
    <LovtekstMedLenke
      tittel="Tidligere familiepleier"
      hjemler={[]}
      status={statusIkon(behandling.boddEllerArbeidetUtlandet)}
    >
      <Informasjon>
        <VStack gap="6">
          <BodyShort>Her kommer det info om tidligere familiepleier</BodyShort>
        </VStack>
      </Informasjon>
      <Vurdering>
        {vurdert && (
          <TidligereFamiliepleierVurdering
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
