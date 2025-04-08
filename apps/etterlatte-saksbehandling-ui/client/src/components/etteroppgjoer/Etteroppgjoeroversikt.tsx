import { addEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { useAppDispatch } from '~store/Store'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentEtteroppgjoer } from '~shared/api/etteroppgjoer'
import React, { useEffect } from 'react'
import { mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { NesteOgTilbake } from '~components/behandling/handlinger/NesteOgTilbake'
import { BodyShort, Box, Heading, HStack, VStack } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { Inntektsopplysninger } from '~components/etteroppgjoer/oversiktOverEtteroppgjoer/inntektsopplysninger/Inntektsopplysninger'
import { FastsettFaktiskInntekt } from '~components/etteroppgjoer/oversiktOverEtteroppgjoer/fastsettFaktiskInntekt/FastsettFaktiskInntekt'
import { ResultatAvForbehandling } from '~components/etteroppgjoer/oversiktOverEtteroppgjoer/resultatAvForbehandling/ResultatAvForbehandling'

export const Etteroppgjoeroversikt = ({ behandling }: { behandling: IDetaljertBehandling }) => {
  const etteroppgjoerId = behandling.relatertBehandlingId
  const dispatch = useAppDispatch()
  const [etteroppgjoerResult, hentEtteroppgjoerRequest] = useApiCall(hentEtteroppgjoer)

  useEffect(() => {
    if (!etteroppgjoerId) return
    hentEtteroppgjoerRequest(etteroppgjoerId, (etteroppgjoer) => {
      dispatch(addEtteroppgjoer(etteroppgjoer))
    })
  }, [etteroppgjoerId])

  return mapResult(etteroppgjoerResult, {
    pending: <Spinner label="Henter etteroppgjørbehandling" />,
    error: (error) => <ApiErrorAlert>Kunne ikke hente forbehandlingen for etteroppgjør: {error.detail}</ApiErrorAlert>,
    success: (etteroppgjoer) => (
      <VStack gap="10" paddingInline="16" paddingBlock="16 4">
        <Heading size="xlarge" level="1">
          Etteroppgjør for {etteroppgjoer.behandling.aar}
        </Heading>
        <BodyShort>
          <b>Skatteoppgjør mottatt:</b> {formaterDato(etteroppgjoer.behandling.opprettet)}
        </BodyShort>
        <Inntektsopplysninger />
        <FastsettFaktiskInntekt forbehandlingId={etteroppgjoer.behandling.id} />
        <ResultatAvForbehandling />

        <Box borderWidth="1 0 0 0" borderColor="border-subtle" paddingBlock="8 16">
          <HStack width="100%" justify="center">
            <NesteOgTilbake />
          </HStack>
        </Box>
      </VStack>
    ),
  })
}
