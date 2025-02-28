import { IDetaljertBehandling, ViderefoertOpphoer } from '~shared/types/IDetaljertBehandling'
import { SoeknadVurdering } from '../SoeknadVurdering'
import { useEffect, useState } from 'react'
import { Alert, BodyShort, Box, Button, VStack } from '@navikt/ds-react'
import { ViderefoereOpphoerVurdering } from '~components/behandling/soeknadsoversikt/viderefoere-opphoer/ViderefoereOpphoerVurdering'
import { ViderefoereOpphoerVisning } from '~components/behandling/soeknadsoversikt/viderefoere-opphoer/ViderefoereOpphoerVisning'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentVilkaartyper } from '~shared/api/vilkaarsvurdering'
import { isSuccess } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

const statusIkon = (viderefoertOpphoer: ViderefoertOpphoer | null) =>
  viderefoertOpphoer === null ? 'warning' : 'success'

export const ViderefoereOpphoer = ({
  behandling,
  redigerbar,
}: {
  behandling: IDetaljertBehandling
  redigerbar: boolean
}) => {
  const [visVurdering, setVisVurdering] = useState(false)
  const [hentVilkaartyperStatus, hentVilkaartyperRequest] = useApiCall(hentVilkaartyper)
  const viderefoertOpphoer = behandling.viderefoertOpphoer
  const virkningstidspunkt = behandling.virkningstidspunkt ? new Date(behandling.virkningstidspunkt.dato) : null

  useEffect(() => {
    if (virkningstidspunkt != null) {
      hentVilkaartyperRequest(behandling.id)
    }
  }, [behandling.id, behandling.virkningstidspunkt])

  return (
    <SoeknadVurdering tittel="Opphør fra og med" hjemler={[]} status={statusIkon(viderefoertOpphoer)}>
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
        {visVurdering ? (
          virkningstidspunkt && isSuccess(hentVilkaartyperStatus) ? (
            <ViderefoereOpphoerVurdering
              virkningstidspunkt={virkningstidspunkt}
              viderefoertOpphoer={viderefoertOpphoer}
              vilkaarTyper={hentVilkaartyperStatus.data}
              setVisVurdering={(visVurdering) => setVisVurdering(visVurdering)}
              behandlingId={behandling.id}
            />
          ) : (
            <Alert variant="warning">Virkningstidspunkt må være satt for å sette opphør fra og med</Alert>
          )
        ) : viderefoertOpphoer ? (
          <ViderefoereOpphoerVisning
            viderefoertOpphoer={viderefoertOpphoer}
            behandlingId={behandling.id}
            setVisVurdering={setVisVurdering}
          />
        ) : (
          redigerbar && (
            <Button variant="secondary" onClick={() => setVisVurdering(true)}>
              Legg til vurdering
            </Button>
          )
        )}
        {isFailureHandler({
          apiResult: hentVilkaartyperStatus,
          errorMessage: 'Kunne ikke hente vilkår',
        })}
      </Box>
    </SoeknadVurdering>
  )
}
