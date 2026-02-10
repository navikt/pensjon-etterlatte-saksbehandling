import {
  Alert,
  BodyLong,
  BodyShort,
  Box,
  Button,
  ErrorSummary,
  Heading,
  HStack,
  Label,
  Radio,
  VStack,
} from '@navikt/ds-react'
import { useNavigate } from 'react-router-dom'
import React, { useEffect, useState } from 'react'
import { harPerioderMedOverstyringAvNettoBrutto, TilbakekrevingBehandling } from '~shared/types/Tilbakekreving'
import { TilbakekrevingVurderingOppsummering } from '~components/tilbakekreving/oppsummering/TilbakekrevingVurderingOppsummering'
import { useApiCall } from '~shared/hooks/useApiCall'
import { fattVedtak, lagreSkalSendeBrev, validerTilbakekreving } from '~shared/api/tilbakekreving'
import { isInitial, isPending, mapResult } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { addTilbakekreving } from '~store/reducers/TilbakekrevingReducer'
import { useAppDispatch } from '~store/Store'
import { ApiError } from '~shared/api/apiClient'
import { SendTilAttesteringModal } from '~components/behandling/handlinger/SendTilAttesteringModal'
import Spinner from '~shared/Spinner'
import { ControlledRadioGruppe } from '~shared/components/radioGruppe/ControlledRadioGruppe'
import { RadioGroupLegend } from '~components/tilbakekreving/vurdering/TilbakekrevingVurderingSkjema'
import { Toast } from '~shared/alerts/Toast'
import { useForm } from 'react-hook-form'
import { FixedAlert } from '~shared/alerts/FixedAlert'

interface ISkalSendeBrev {
  skalSendeBrev: boolean
}

export function TilbakekrevingOppsummering({
  behandling,
  redigerbar,
}: {
  behandling: TilbakekrevingBehandling
  redigerbar: boolean
}) {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const [validerTilbakekrevingStatus, validerTilbakekrevingRequest] = useApiCall(validerTilbakekreving)
  const [gyldigTilbakekreving, setGyldigTilbakekreving] = useState(!redigerbar)

  const [lagreSkalSendeBrevStatus, lagreSkalSendeBrevRequest] = useApiCall(lagreSkalSendeBrev)
  const { reset, handleSubmit, control, formState, getValues } = useForm<ISkalSendeBrev>({
    defaultValues: {
      skalSendeBrev: behandling.sendeBrev,
    },
  })

  const lagreOppsummering = (skalSendeBrev: ISkalSendeBrev) => {
    lagreSkalSendeBrevRequest(
      { tilbakekrevingId: behandling.id, skalSendeBrev: skalSendeBrev.skalSendeBrev },
      (lagretTilbakekreving) => {
        dispatch(addTilbakekreving(lagretTilbakekreving))
        reset({ skalSendeBrev: lagretTilbakekreving.sendeBrev })
      }
    )
  }

  const gaaTilBrev = () => {
    navigate(`/tilbakekreving/${behandling?.id}/brev`)
  }

  useEffect(() => {
    if (redigerbar && isInitial(validerTilbakekrevingStatus)) {
      valider()
    }
  }, [redigerbar])

  useEffect(() => {
    if (formState.isDirty && Object.keys(formState.dirtyFields).length) {
      reset(getValues())
      handleSubmit(lagreOppsummering)()
    }
  }, [formState])

  const valider = () => {
    validerTilbakekrevingRequest(
      behandling.id,
      (lagretTilbakekreving) => {
        dispatch(addTilbakekreving(lagretTilbakekreving))
        setGyldigTilbakekreving(true)
      },
      () => {
        setGyldigTilbakekreving(false)
      }
    )
  }

  return (
    <>
      <Box paddingInline="space-16" paddingBlock="space-16 space-4">
        <Heading level="1" size="large">
          Oppsummering
        </Heading>
      </Box>
      <Box paddingBlock="space-8" paddingInline="space-16 space-8">
        <TilbakekrevingVurderingOppsummering behandling={behandling} />
        {harPerioderMedOverstyringAvNettoBrutto(behandling) && (
          <Box marginBlock="space-8 space-0" maxWidth="45rem">
            <Alert variant="info">
              <VStack gap="space-4">
                <BodyLong>
                  Det er lagt inn overstyring av netto tilbakekrevingsbeløp til brutto tilbakekreving av kun
                  nettobeløpet. Det må lages en oppgave til NØP for å få postert skattebeløpet manuelt etter
                  overstyringen.
                </BodyLong>
                <Label>Merknad</Label>
                <BodyShort>Barnepensjon - se vedtak om netto tilbakekreving i Gjenny [dato]</BodyShort>
                <BodyShort>Skatt kr [skattebeløp] må posteres manuelt.</BodyShort>

                <Label>Tema</Label>
                <BodyShort>Økonomi</BodyShort>
                <Label>Gjelder</Label>
                <BodyShort>Tilbakekreving</BodyShort>
                <Label>Prioritet</Label>
                <BodyShort>Høy</BodyShort>

                <BodyShort>Oppgaven legges til 4819 (Nav økonomi pensjon)</BodyShort>
              </VStack>
            </Alert>
          </Box>
        )}

        <div style={{ marginTop: '3rem' }}>
          <VStack gap="space-6">
            <ControlledRadioGruppe
              name="skalSendeBrev"
              control={control}
              legend={<RadioGroupLegend label="Skal det sendes vedtak for tilbakekrevingen?" />}
              size="small"
              readOnly={!redigerbar}
              radios={
                <>
                  <Radio value={true}>Ja</Radio>
                  <Radio value={false}>Nei</Radio>
                </>
              }
            />
            {mapResult(lagreSkalSendeBrevStatus, {
              success: () => <Toast melding="Brevvalg lagret" position="bottom-center" />,
              error: (error) => (
                <FixedAlert
                  variant="error"
                  melding={`Kunne ikke lagre brevvalg: ${error.detail}`}
                  position="bottom-center"
                />
              ),
            })}
          </VStack>
        </div>

        {mapResult(validerTilbakekrevingStatus, {
          pending: <Spinner label="Sjekker om tilbakekreving er fylt ut" />,
          error: (error) => <TilbakekrevingValideringsfeil error={error} />,
        })}
      </Box>

      <Box paddingBlock="space-12 space-0" borderWidth="1 0 0 0">
        <HStack justify="center">
          {!isPending(lagreSkalSendeBrevStatus) && (
            <>
              {behandling.sendeBrev && gyldigTilbakekreving && (
                <Button variant="primary" onClick={gaaTilBrev} loading={isPending(validerTilbakekrevingStatus)}>
                  Gå til brev
                </Button>
              )}
              {!behandling.sendeBrev && gyldigTilbakekreving && redigerbar && (
                <SendTilAttesteringModal
                  behandlingId={behandling.id}
                  fattVedtakApi={fattVedtak}
                  validerKanSendeTilAttestering={() => gyldigTilbakekreving}
                />
              )}
            </>
          )}
          <Spinner visible={isPending(lagreSkalSendeBrevStatus)} label="Lagrer brevvalg" />
        </HStack>
      </Box>
    </>
  )
}

function TilbakekrevingValideringsfeil({ error }: { error: ApiError }) {
  if (error.meta && error.meta['ugyldigeFelterVurdering'] && error.meta['ugyldigeFelterPerioder']) {
    const ugyldigeFelterVurdering = error.meta!['ugyldigeFelterVurdering'] as Array<string>
    const ugyldigeFelterPerioder = error.meta!['ugyldigeFelterPerioder'] as Array<string>

    return (
      <>
        {ugyldigeFelterVurdering.length > 0 && (
          <ErrorSummary
            style={{ marginTop: '5rem', maxWidth: '50rem' }}
            heading="Et eller flere felter mangler gyldig verdi i Vurdering:"
          >
            {ugyldigeFelterVurdering.map((feilmelding, i) => (
              <ErrorSummary.Item key={i}>{feilmelding}</ErrorSummary.Item>
            ))}
          </ErrorSummary>
        )}
        {ugyldigeFelterPerioder.length > 0 && (
          <ErrorSummary
            style={{ marginTop: '2rem', maxWidth: '50rem' }}
            heading="Et eller flere felter mangler gyldig verdi i Utbetalinger:"
          >
            {ugyldigeFelterPerioder.map((feilmelding, i) => (
              <ErrorSummary.Item key={i}>{feilmelding}</ErrorSummary.Item>
            ))}
          </ErrorSummary>
        )}
      </>
    )
  } else {
    return <ApiErrorAlert>{error.detail}</ApiErrorAlert>
  }
}
