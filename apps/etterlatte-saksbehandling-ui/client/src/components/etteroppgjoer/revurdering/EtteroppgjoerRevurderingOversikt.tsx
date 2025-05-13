import { addEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { useAppDispatch } from '~store/Store'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentEtteroppgjoerForbehandling, lagreHarMottattNyInformasjon } from '~shared/api/etteroppgjoer'
import React, { useContext, useEffect, useState } from 'react'
import { isPending, mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { Alert, BodyShort, Box, Button, Heading, HStack, Radio, VStack } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { Inntektsopplysninger } from '~components/etteroppgjoer/components/inntektsopplysninger/Inntektsopplysninger'
import { FastsettFaktiskInntekt } from '~components/etteroppgjoer/components/fastsettFaktiskInntekt/FastsettFaktiskInntekt'
import { ResultatAvForbehandling } from '~components/etteroppgjoer/components/resultatAvForbehandling/ResultatAvForbehandling'
import { BehandlingRouteContext } from '~components/behandling/BehandlingRoutes'
import AvbrytBehandling from '~components/behandling/handlinger/AvbrytBehandling'
import { useForm } from 'react-hook-form'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { isFailureHandler } from '~shared/api/IsFailureHandler'

interface EtteroppgjoerRevurderingOversiktSkjema {
  skalKunneRedigereFastsattInntekt: string
  fastsettInntektSkjemaErSkittent: boolean
}

export const EtteroppgjoerRevurderingOversikt = ({ behandling }: { behandling: IDetaljertBehandling }) => {
  const etteroppgjoerId = behandling.relatertBehandlingId
  const dispatch = useAppDispatch()

  const [etteroppgjoerResult, hentEtteroppgjoerRequest] = useApiCall(hentEtteroppgjoerForbehandling)
  const [harMottattNyInformasjonResult, harMottattNyInformasjonRequest] = useApiCall(lagreHarMottattNyInformasjon)

  const { next } = useContext(BehandlingRouteContext)

  const [fastsettInntektSkjemaErSkittent, setFastsettInntektSkjemaErSkittent] = useState<boolean>(false)
  const [fastsettInntektSkjemaErSkittentFeilmelding, setFastsettInntektSkjemaErSkittentFeilmelding] =
    useState<string>('')

  // TODO: utlede default value her
  const { control, handleSubmit, watch } = useForm<EtteroppgjoerRevurderingOversiktSkjema>({
    defaultValues: { skalKunneRedigereFastsattInntekt: '' },
  })

  const paaSubmit = (data: EtteroppgjoerRevurderingOversiktSkjema) => {
    if (data.skalKunneRedigereFastsattInntekt === 'JA' && fastsettInntektSkjemaErSkittent) {
      harMottattNyInformasjonRequest({ forbehandlingId: etteroppgjoerId!, harMottattNyInformasjon: true }, () => {
        setFastsettInntektSkjemaErSkittentFeilmelding('')
        next()
      })
    } else if (data.skalKunneRedigereFastsattInntekt === 'JA' && !fastsettInntektSkjemaErSkittent) {
      harMottattNyInformasjonRequest({ forbehandlingId: etteroppgjoerId!, harMottattNyInformasjon: true }, () => {
        setFastsettInntektSkjemaErSkittentFeilmelding('Du må gjøre en endring i fastsatt inntekt')
      })
      // Saksbehandler har trykket "Nei", da kan man gå videre
    } else {
      harMottattNyInformasjonRequest({ forbehandlingId: etteroppgjoerId!, harMottattNyInformasjon: false }, () => {
        setFastsettInntektSkjemaErSkittentFeilmelding('')
        next()
      })
    }
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
      <form onSubmit={handleSubmit(paaSubmit)}>
        <VStack gap="10" paddingInline="16" paddingBlock="16 4">
          <Heading size="xlarge" level="1">
            Etteroppgjør for {etteroppgjoer.behandling.aar}
          </Heading>
          <BodyShort>
            <b>Skatteoppgjør mottatt:</b> {formaterDato(etteroppgjoer.behandling.opprettet)}
          </BodyShort>
          <Inntektsopplysninger />
          {/* TODO: lagret resultatet her noen sted */}
          <ControlledRadioGruppe
            name="skalKunneRedigereFastsattInntekt"
            control={control}
            legend="Har du fått ny informasjon fra bruker eller oppdaget feil i forbehandlingen?"
            radios={
              <>
                <Radio value="JA">Ja</Radio>
                <Radio value="NEI">Nei</Radio>
              </>
            }
            errorVedTomInput="Du må ta stilling til om bruker gitt ny informasjon"
          />

          {watch('skalKunneRedigereFastsattInntekt') === 'JA' ? (
            <FastsettFaktiskInntekt
              erRedigerbar
              setFastsettInntektSkjemaErSkittent={setFastsettInntektSkjemaErSkittent}
            />
          ) : (
            <FastsettFaktiskInntekt erRedigerbar={false} />
          )}

          <ResultatAvForbehandling />

          {fastsettInntektSkjemaErSkittentFeilmelding && (
            <HStack width="100%" justify="center">
              <Alert variant="error">{fastsettInntektSkjemaErSkittentFeilmelding}</Alert>
            </HStack>
          )}

          {isFailureHandler({
            apiResult: harMottattNyInformasjonResult,
            errorMessage: 'Kunne ikke lagre om bruker har gitt ny informasjon',
          })}

          <Box borderWidth="1 0 0 0" borderColor="border-subtle" paddingBlock="8 16">
            <HStack width="100%" justify="center">
              <VStack gap="4" align="center">
                <div>
                  <Button type="submit" variant="primary" loading={isPending(harMottattNyInformasjonResult)}>
                    Neste steg
                  </Button>
                </div>
                <AvbrytBehandling />
              </VStack>
            </HStack>
          </Box>
        </VStack>
      </form>
    ),
  })
}
