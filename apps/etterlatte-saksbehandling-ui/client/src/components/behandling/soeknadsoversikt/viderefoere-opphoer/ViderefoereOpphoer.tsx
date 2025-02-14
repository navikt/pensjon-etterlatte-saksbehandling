import { IDetaljertBehandling, ViderefoertOpphoer } from '~shared/types/IDetaljertBehandling'
import { SoeknadVurdering } from '../SoeknadVurdering'
import { useState } from 'react'
import { BodyShort, Box, Button, VStack } from '@navikt/ds-react'
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
    <SoeknadVurdering tittel="Opphør fra og med" hjemler={[]} status={statusIkon(behandling.viderefoertOpphoer)}>
      <VStack gap="4" marginBlock="3" marginInline="0" maxWidth="41rem">
        <BodyShort>
          Er opphørsdato tidligere enn dagens dato, eller skal saken opphøre i nær fremtid fordi vilkårene ikke lenger
          er oppfylt?
        </BodyShort>
        <BodyShort>
          Hvis brukers pensjon skal opphøre før behandlingsdatoen, så velg den måneden pensjonen skal opphøre fra. Legg
          også inn opphørstidspunkt dersom pensjonen skal opphøre f.eks. ved aldersovergang i så nær fremtid at den ikke
          blir behandlet av det automatiske opphøret.
        </BodyShort>
      </VStack>
      <Box paddingInline="3 0" minWidth="18.75rem" width="10rem" borderWidth="0 0 0 2" borderColor="border-subtle">
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
      </Box>
    </SoeknadVurdering>
  )
}
