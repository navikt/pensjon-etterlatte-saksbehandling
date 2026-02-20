import { IDetaljertBehandling, ViderefoertOpphoer } from '~shared/types/IDetaljertBehandling'
import { SoeknadVurdering } from '../SoeknadVurdering'
import { useEffect, useState } from 'react'
import { Alert, BodyShort, Box, Button, VStack } from '@navikt/ds-react'
import { ViderefoereOpphoerSkjema } from '~components/behandling/soeknadsoversikt/viderefoereOpphoer/ViderefoereOpphoerSkjema'
import { ViderefoereOpphoerVisning } from '~components/behandling/soeknadsoversikt/viderefoereOpphoer/ViderefoereOpphoerVisning'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentVilkaartyper } from '~shared/api/vilkaarsvurdering'
import { mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'

const statusIkon = (viderefoertOpphoer: ViderefoertOpphoer | null) =>
  viderefoertOpphoer === null ? 'warning' : 'success'

export const ViderefoereOpphoer = ({
  behandling,
  redigerbar,
}: {
  behandling: IDetaljertBehandling
  redigerbar: boolean
}) => {
  const [visSkjema, setVisSkjema] = useState(false)
  const [hentVilkaartyperResult, hentVilkaartyperRequest] = useApiCall(hentVilkaartyper)
  const viderefoertOpphoer = behandling.viderefoertOpphoer
  const virkningstidspunkt = !!behandling.virkningstidspunkt && new Date(behandling.virkningstidspunkt.dato)

  useEffect(() => {
    if (virkningstidspunkt) {
      hentVilkaartyperRequest(behandling.id)
    }
  }, [behandling.id, behandling.virkningstidspunkt])

  return (
    <SoeknadVurdering tittel="Opphør fra og med" hjemler={[]} status={statusIkon(viderefoertOpphoer)}>
      <VStack gap="4" marginBlock="3" marginInline="0" maxWidth="41rem">
        <BodyShort>
          Er opphørsdato tidligere enn dagens dato, eller skal saken opphøre innen 3 måneder fra saken er attestert?
        </BodyShort>
        <BodyShort>
          Hvis brukers pensjon skal opphøre før behandlingsdatoen, så velg den måneden pensjonen skal opphøre fra. Legg
          også inn opphørstidspunkt dersom pensjonen skal opphøre f.eks. ved aldersovergang i så nær fremtid at den ikke
          blir behandlet av det automatiske opphøret.
        </BodyShort>
      </VStack>
      <Box paddingInline="3 0" minWidth="18.75rem" width="10rem" borderWidth="0 0 0 2" borderColor="border-subtle">
        {mapResult(hentVilkaartyperResult, {
          initial: <Alert variant="warning">Virkningstidspunkt må være satt for å sette opphør fra og med</Alert>,
          pending: <Spinner label="Henter vilkårstyper" visible />,
          error: () => <Alert variant="error">Kunne ikke hente vilkårstyper</Alert>,
          success: (vilkaarTyper) =>
            visSkjema ? (
              virkningstidspunkt && (
                <ViderefoereOpphoerSkjema
                  virkningstidspunkt={virkningstidspunkt}
                  viderefoertOpphoer={viderefoertOpphoer}
                  vilkaarTyper={vilkaarTyper}
                  setVisSkjema={(visSkjema) => setVisSkjema(visSkjema)}
                  behandlingId={behandling.id}
                />
              )
            ) : viderefoertOpphoer ? (
              <ViderefoereOpphoerVisning
                viderefoertOpphoer={viderefoertOpphoer}
                behandlingId={behandling.id}
                vilkaarTyper={vilkaarTyper}
                setVisSkjema={setVisSkjema}
                redigerbar={redigerbar}
              />
            ) : (
              redigerbar && (
                <Button variant="secondary" onClick={() => setVisSkjema(true)}>
                  Legg til vurdering
                </Button>
              )
            ),
        })}
      </Box>
    </SoeknadVurdering>
  )
}
