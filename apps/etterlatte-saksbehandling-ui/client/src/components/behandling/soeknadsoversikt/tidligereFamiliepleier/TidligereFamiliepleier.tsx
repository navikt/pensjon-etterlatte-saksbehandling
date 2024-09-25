import { IDetaljertBehandling, ITidligereFamiliepleier } from '~shared/types/IDetaljertBehandling'
import { LovtekstMedLenke } from '../LovtekstMedLenke'
import { Informasjon, Vurdering } from '../styled'
import { useState } from 'react'
import { BodyShort, Button, VStack } from '@navikt/ds-react'
import { TidligereFamiliepleierVurdering } from '~components/behandling/soeknadsoversikt/tidligereFamiliepleier/TidligereFamiliepleierVurdering'

const statusIkon = (tidligereFamiliepleier: ITidligereFamiliepleier | null) => {
  if (tidligereFamiliepleier) {
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
  const [vurdert, setVurdert] = useState<boolean>(!!behandling.tidligereFamiliepleier)

  return (
    <LovtekstMedLenke
      tittel="Tidligere familiepleier"
      hjemler={[]}
      status={statusIkon(behandling.tidligereFamiliepleier)}
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
