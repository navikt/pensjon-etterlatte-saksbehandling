import { addEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { useAppDispatch } from '~store/Store'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentEtteroppgjoer } from '~shared/api/etteroppgjoer'
import React, { useContext, useEffect, useState } from 'react'
import { mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { BodyShort, Box, Button, Heading, HStack, Radio, RadioGroup, VStack } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { Inntektsopplysninger } from '~components/etteroppgjoer/components/inntektsopplysninger/Inntektsopplysninger'
import { FastsettFaktiskInntekt } from '~components/etteroppgjoer/components/fastsettFaktiskInntekt/FastsettFaktiskInntekt'
import { ResultatAvForbehandling } from '~components/etteroppgjoer/components/resultatAvForbehandling/ResultatAvForbehandling'
import { BehandlingRouteContext } from '~components/behandling/BehandlingRoutes'
import AvbrytBehandling from '~components/behandling/handlinger/AvbrytBehandling'

export const EtteroppgjoerRevurderingOversikt = ({ behandling }: { behandling: IDetaljertBehandling }) => {
  const etteroppgjoerId = behandling.relatertBehandlingId
  const dispatch = useAppDispatch()
  const [etteroppgjoerResult, hentEtteroppgjoerRequest] = useApiCall(hentEtteroppgjoer)
  const { next } = useContext(BehandlingRouteContext)

  const [skalKunneRedigereFastsattInntekt, setSkalKunneRedigereFastsattInntekt] = useState<string>('')

  const paaNeste = () => {
    /* TODO: legg til validering og feilmelding for radio kanpper */
    /* TODO: hvis de velger "JA" så må vi sjekke mot orginal forbehandling om SB faktisk har endret på fastsatt faktisk inntekt, hvis ikke blokker disse fra å gå videre og vis feilmelding */

    next()
  }

  useEffect(() => {
    if (!etteroppgjoerId) return
    hentEtteroppgjoerRequest(etteroppgjoerId, (etteroppgjoer) => {
      dispatch(addEtteroppgjoer(etteroppgjoer))
    })
  }, [etteroppgjoerId])

  return mapResult(etteroppgjoerResult, {
    pending: <Spinner label="Henter forbehandling" />,
    error: (error) => <ApiErrorAlert>Kunne ikke hente forbehandling for etteroppgjør: {error.detail}</ApiErrorAlert>,
    success: (etteroppgjoer) => (
      <VStack gap="10" paddingInline="16" paddingBlock="16 4">
        <Heading size="xlarge" level="1">
          Etteroppgjør for {etteroppgjoer.behandling.aar}
        </Heading>
        <BodyShort>
          <b>Skatteoppgjør mottatt:</b> {formaterDato(etteroppgjoer.behandling.opprettet)}
        </BodyShort>
        <Inntektsopplysninger />
        <RadioGroup
          legend="Har du fått ny informasjon fra bruker eller oppdaget feil i forbehandlingen?"
          onChange={(val) => setSkalKunneRedigereFastsattInntekt(val)}
        >
          <Radio value="JA">Ja</Radio>
          <Radio value="NEI">Nei</Radio>
        </RadioGroup>
        {skalKunneRedigereFastsattInntekt === 'JA' ? (
          <FastsettFaktiskInntekt erRedigerbar />
        ) : (
          <FastsettFaktiskInntekt erRedigerbar={false} />
        )}
        <ResultatAvForbehandling />
        <Box borderWidth="1 0 0 0" borderColor="border-subtle" paddingBlock="8 16">
          <HStack width="100%" justify="center">
            <VStack gap="4">
              <Button type="button" variant="primary" onClick={paaNeste}>
                {skalKunneRedigereFastsattInntekt === 'JA' ? 'Rediger' : 'Gå videre'}
              </Button>
              <AvbrytBehandling />
            </VStack>
          </HStack>
        </Box>
      </VStack>
    ),
  })
}
