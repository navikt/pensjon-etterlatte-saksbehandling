import { addEtteroppgjoer } from '~store/reducers/EtteroppgjoerReducer'
import { useAppDispatch } from '~store/Store'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentEtteroppgjoer } from '~shared/api/etteroppgjoer'
import React, { useContext, useEffect } from 'react'
import { mapResult } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { BodyShort, Box, Button, Heading, HStack, Radio, VStack } from '@navikt/ds-react'
import { formaterDato } from '~utils/formatering/dato'
import { Inntektsopplysninger } from '~components/etteroppgjoer/components/inntektsopplysninger/Inntektsopplysninger'
import { FastsettFaktiskInntekt } from '~components/etteroppgjoer/components/fastsettFaktiskInntekt/FastsettFaktiskInntekt'
import { ResultatAvForbehandling } from '~components/etteroppgjoer/components/resultatAvForbehandling/ResultatAvForbehandling'
import { BehandlingRouteContext } from '~components/behandling/BehandlingRoutes'
import AvbrytBehandling from '~components/behandling/handlinger/AvbrytBehandling'
import { useForm } from 'react-hook-form'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { SammendragAvSkjemaFeil } from '~shared/components/SammendragAvSkjemaFeil'

interface EtteroppgjoerRevurderingOversiktSkjema {
  skalKunneRedigereFastsattInntekt: string
}

export const EtteroppgjoerRevurderingOversikt = ({ behandling }: { behandling: IDetaljertBehandling }) => {
  const etteroppgjoerId = behandling.relatertBehandlingId
  const dispatch = useAppDispatch()
  const [etteroppgjoerResult, hentEtteroppgjoerRequest] = useApiCall(hentEtteroppgjoer)
  const { next } = useContext(BehandlingRouteContext)

  const {
    control,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<EtteroppgjoerRevurderingOversiktSkjema>()

  const paaSubmit = (data: EtteroppgjoerRevurderingOversiktSkjema) => {
    // TODO: sjekke her om fastsatt inntekt er faktisk redigert
    // TODO: hvis den ikke er redigert, legg til error. Kan man legge til custom valideringsregler, hvor RHF automagisk legger til error i lista over errors?

    if (!data) {
      // TODO: denne sjekken er bare for å få linteren til å holde kjeft, full fiks kommer i neste PR...
    } else {
      next()
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
            <FastsettFaktiskInntekt erRedigerbar />
          ) : (
            <FastsettFaktiskInntekt erRedigerbar={false} />
          )}

          <ResultatAvForbehandling />

          <HStack width="100%" justify="center">
            <SammendragAvSkjemaFeil errors={errors} />
          </HStack>

          <Box borderWidth="1 0 0 0" borderColor="border-subtle" paddingBlock="8 16">
            <HStack width="100%" justify="center">
              <VStack gap="4">
                <Button type="submit" variant="primary">
                  {watch('skalKunneRedigereFastsattInntekt') === 'JA' ? 'Rediger' : 'Gå videre'}
                </Button>
                <AvbrytBehandling />
              </VStack>
            </HStack>
          </Box>
        </VStack>
      </form>
    ),
  })
}
