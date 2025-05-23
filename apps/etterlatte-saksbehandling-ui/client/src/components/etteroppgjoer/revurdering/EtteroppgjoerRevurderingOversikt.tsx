import { addEtteroppgjoer, useEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { useAppDispatch } from '~store/Store'
import { useApiCall } from '~shared/hooks/useApiCall'
import {
  hentEtteroppgjoerForbehandling,
  lagreEndringErTilUgunstForBruker,
  lagreHarMottattNyInformasjon,
} from '~shared/api/etteroppgjoer'
import React, { useContext, useEffect, useState } from 'react'
import { isPending, mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { Alert, BodyShort, Box, Button, Heading, HStack, Radio, Textarea, VStack } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { Inntektsopplysninger } from '~components/etteroppgjoer/components/inntektsopplysninger/Inntektsopplysninger'
import { FastsettFaktiskInntekt } from '~components/etteroppgjoer/components/fastsettFaktiskInntekt/FastsettFaktiskInntekt'
import { ResultatAvForbehandling } from '~components/etteroppgjoer/components/resultatAvForbehandling/ResultatAvForbehandling'
import { BehandlingRouteContext } from '~components/behandling/BehandlingRoutes'
import AvbrytBehandling from '~components/behandling/handlinger/AvbrytBehandling'
import { useForm } from 'react-hook-form'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { EtteroppgjoerRevurderingResultat } from '~components/etteroppgjoer/revurdering/EtteroppgjoerRevurderingResultat'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { EtteroppgjoerForbehandling } from '~shared/types/EtteroppgjoerForbehandling'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'

const etteroppgjoerRevurderingOversiktSkjemaDefaultValues = (
  etteroppgjoerForbehandling: EtteroppgjoerForbehandling
): EtteroppgjoerRevurderingOversiktSkjema => {
  let etteroppgjoerRevurderingOversiktSkjemaValues: EtteroppgjoerRevurderingOversiktSkjema = {
    harMottattNyInformasjon: '',
    endringErTilUgunstForBruker: '',
    beskrivelseAvUgunst: '',
  }

  if (etteroppgjoerForbehandling) {
    if (etteroppgjoerForbehandling.behandling) {
      if (etteroppgjoerForbehandling.behandling.harMottattNyInformasjon === true) {
        etteroppgjoerRevurderingOversiktSkjemaValues = {
          ...etteroppgjoerRevurderingOversiktSkjemaValues,
          harMottattNyInformasjon: 'JA',
        }
      } else if (etteroppgjoerForbehandling.behandling.harMottattNyInformasjon === false) {
        etteroppgjoerRevurderingOversiktSkjemaValues = {
          ...etteroppgjoerRevurderingOversiktSkjemaValues,
          harMottattNyInformasjon: 'NEI',
        }
      }

      if (etteroppgjoerForbehandling.behandling.endringErTilUgunstForBruker === true) {
        etteroppgjoerRevurderingOversiktSkjemaValues = {
          ...etteroppgjoerRevurderingOversiktSkjemaValues,
          endringErTilUgunstForBruker: 'JA',
        }
      } else if (etteroppgjoerForbehandling.behandling.endringErTilUgunstForBruker === false) {
        etteroppgjoerRevurderingOversiktSkjemaValues = {
          ...etteroppgjoerRevurderingOversiktSkjemaValues,
          endringErTilUgunstForBruker: 'NEI',
        }
      }

      if (!!etteroppgjoerForbehandling.behandling.beskrivelseAvUgunst) {
        etteroppgjoerRevurderingOversiktSkjemaValues = {
          ...etteroppgjoerRevurderingOversiktSkjemaValues,
          beskrivelseAvUgunst: etteroppgjoerForbehandling.behandling.beskrivelseAvUgunst,
        }
      }
    }
  }

  return etteroppgjoerRevurderingOversiktSkjemaValues
}

interface EtteroppgjoerRevurderingOversiktSkjema {
  harMottattNyInformasjon: 'JA' | 'NEI' | ''
  // TODO: disse 2 feltene skal kun vises hvis svarfristen har gått ut på varselbrev etter forbehandling
  endringErTilUgunstForBruker: 'JA' | 'NEI' | ''
  beskrivelseAvUgunst: string
}

export const EtteroppgjoerRevurderingOversikt = ({ behandling }: { behandling: IDetaljertBehandling }) => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const erRedigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )

  const etteroppgjoerForbehandlingId = behandling.relatertBehandlingId

  const dispatch = useAppDispatch()

  const etteroppgjoer = useEtteroppgjoer()

  const [etteroppgjoerResult, hentEtteroppgjoerRequest] = useApiCall(hentEtteroppgjoerForbehandling)
  const [harMottattNyInformasjonResult, harMottattNyInformasjonRequest] = useApiCall(lagreHarMottattNyInformasjon)
  const [endringErTilUgunstForBrukerResult, endringErTilUgunstForBrukerRequest] = useApiCall(
    lagreEndringErTilUgunstForBruker
  )

  const { next } = useContext(BehandlingRouteContext)

  const [faktiskInntektSkjemaErAapen, setFaktiskInntektSkjemaErAapen] = useState<boolean>(false)
  const [fastsettInntektSkjemaErSkittent, setFastsettInntektSkjemaErSkittent] = useState<boolean>(false)
  const [fastsettInntektSkjemaErSkittentFeilmelding, setFastsettInntektSkjemaErSkittentFeilmelding] =
    useState<string>('')

  // TODO: bytte om til form context, dette begynner å bli riiiimelig messy
  const {
    control,
    register,
    handleSubmit,
    watch,
    setValue,
    getValues,
    formState: { errors },
  } = useForm<EtteroppgjoerRevurderingOversiktSkjema>({
    defaultValues: etteroppgjoerRevurderingOversiktSkjemaDefaultValues(etteroppgjoer),
  })

  const paaSubmit = (data: EtteroppgjoerRevurderingOversiktSkjema) => {
    if (data.harMottattNyInformasjon === 'JA' && fastsettInntektSkjemaErSkittent) {
      harMottattNyInformasjonRequest(
        { forbehandlingId: etteroppgjoerForbehandlingId!, harMottattNyInformasjon: true },
        () => {
          endringErTilUgunstForBrukerRequest(
            {
              forbehandlingId: etteroppgjoerForbehandlingId!,
              endringErTilUgunstForBruker: data.endringErTilUgunstForBruker === 'JA',
              beskrivelseAvUgunst: data.beskrivelseAvUgunst,
            },
            () => {
              setFastsettInntektSkjemaErSkittentFeilmelding('')
              next()
            }
          )
        }
      )
    } else if (data.harMottattNyInformasjon === 'JA' && !fastsettInntektSkjemaErSkittent) {
      harMottattNyInformasjonRequest(
        { forbehandlingId: etteroppgjoerForbehandlingId!, harMottattNyInformasjon: true },
        () => {
          setFastsettInntektSkjemaErSkittentFeilmelding('Du må gjøre en endring i fastsatt inntekt')
        }
      )
      // Saksbehandler har trykket "Nei", da kan man gå videre
    } else {
      harMottattNyInformasjonRequest(
        { forbehandlingId: etteroppgjoerForbehandlingId!, harMottattNyInformasjon: false },
        () => {
          setFastsettInntektSkjemaErSkittentFeilmelding('')
          next()
        }
      )
    }
  }

  useEffect(() => {
    if (!etteroppgjoerForbehandlingId) return
    hentEtteroppgjoerRequest(etteroppgjoerForbehandlingId, (etteroppgjoer) => {
      dispatch(addEtteroppgjoer(etteroppgjoer))
    })
  }, [etteroppgjoerForbehandlingId])

  useEffect(() => {
    setValue(
      'harMottattNyInformasjon',
      etteroppgjoerRevurderingOversiktSkjemaDefaultValues(etteroppgjoer).harMottattNyInformasjon
    )
    setValue(
      'endringErTilUgunstForBruker',
      etteroppgjoerRevurderingOversiktSkjemaDefaultValues(etteroppgjoer).endringErTilUgunstForBruker
    )
    setValue(
      'beskrivelseAvUgunst',
      etteroppgjoerRevurderingOversiktSkjemaDefaultValues(etteroppgjoer).beskrivelseAvUgunst
    )
  }, [etteroppgjoer])

  useEffect(() => {
    if (fastsettInntektSkjemaErSkittent) {
      harMottattNyInformasjonRequest(
        { forbehandlingId: etteroppgjoerForbehandlingId!, harMottattNyInformasjon: true },
        () => {
          endringErTilUgunstForBrukerRequest({
            forbehandlingId: etteroppgjoerForbehandlingId!,
            endringErTilUgunstForBruker: getValues('endringErTilUgunstForBruker') === 'JA',
            beskrivelseAvUgunst: getValues('beskrivelseAvUgunst'),
          })
        }
      )
    }
  }, [fastsettInntektSkjemaErSkittent])

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

          <ControlledRadioGruppe
            name="harMottattNyInformasjon"
            control={control}
            legend="Har du fått ny informasjon fra bruker eller oppdaget feil i forbehandlingen?"
            errorVedTomInput="Du må ta stilling til om bruker gitt ny informasjon"
            readOnly={!erRedigerbar}
            radios={
              <>
                <Radio value="JA">Ja</Radio>
                <Radio value="NEI">Nei</Radio>
              </>
            }
          />

          {watch('harMottattNyInformasjon') === 'JA' ? (
            <>
              <ControlledRadioGruppe
                name="endringErTilUgunstForBruker"
                control={control}
                legend="Er endringen til ugunst for bruker?"
                errorVedTomInput="Du må ta stilling til om endringen er til ugunst for bruker"
                readOnly={!erRedigerbar}
                radios={
                  <>
                    <Radio value="JA">Ja</Radio>
                    <Radio value="NEI">Nei</Radio>
                  </>
                }
              />
              {watch('endringErTilUgunstForBruker') === 'JA' && (
                <Box maxWidth="30rem">
                  <Textarea
                    {...register('beskrivelseAvUgunst', {
                      required: {
                        value: true,
                        message: 'Du må beskrive hvorfor endringen kommer til ugunst',
                      },
                    })}
                    label="Beskriv hvorfor det er til ugunst"
                    error={errors.beskrivelseAvUgunst?.message}
                  />
                </Box>
              )}

              <FastsettFaktiskInntekt
                erRedigerbar={erRedigerbar}
                faktiskInntektSkjemaErAapen={faktiskInntektSkjemaErAapen}
                setFaktiskInntektSkjemaErAapen={setFaktiskInntektSkjemaErAapen}
                setFastsettInntektSkjemaErSkittent={setFastsettInntektSkjemaErSkittent}
              />
            </>
          ) : (
            <FastsettFaktiskInntekt
              erRedigerbar={false}
              faktiskInntektSkjemaErAapen={faktiskInntektSkjemaErAapen}
              setFaktiskInntektSkjemaErAapen={setFaktiskInntektSkjemaErAapen}
            />
          )}

          <ResultatAvForbehandling />

          <EtteroppgjoerRevurderingResultat />

          {fastsettInntektSkjemaErSkittentFeilmelding && (
            <HStack width="100%" justify="center">
              <Alert variant="error">{fastsettInntektSkjemaErSkittentFeilmelding}</Alert>
            </HStack>
          )}

          {isFailureHandler({
            apiResult: harMottattNyInformasjonResult,
            errorMessage: 'Kunne ikke lagre om bruker har gitt ny informasjon',
          })}

          {isFailureHandler({
            apiResult: endringErTilUgunstForBrukerResult,
            errorMessage: 'Kunne ikke lagre om endring er til ugunst for bruker',
          })}

          <Box borderWidth="1 0 0 0" borderColor="border-subtle" paddingBlock="8 16">
            <HStack width="100%" justify="center">
              <VStack gap="4" align="center">
                <div>
                  <Button
                    type="submit"
                    variant="primary"
                    loading={isPending(harMottattNyInformasjonResult) || isPending(endringErTilUgunstForBrukerResult)}
                  >
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
