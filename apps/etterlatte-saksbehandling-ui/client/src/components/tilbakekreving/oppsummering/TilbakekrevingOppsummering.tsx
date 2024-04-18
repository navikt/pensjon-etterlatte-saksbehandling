import { Button, ErrorSummary, Heading } from '@navikt/ds-react'
import { Content, ContentHeader, FlexRow } from '~shared/styled'
import { Border, HeadingWrapper, InnholdPadding } from '~components/behandling/soeknadsoversikt/styled'
import { useNavigate } from 'react-router-dom'
import React, { useEffect, useState } from 'react'
import { TilbakekrevingBehandling } from '~shared/types/Tilbakekreving'
import { TilbakekrevingVurderingOppsummering } from '~components/tilbakekreving/oppsummering/TilbakekrevingVurderingOppsummering'
import { useApiCall } from '~shared/hooks/useApiCall'
import { fattVedtak, validerTilbakekreving } from '~shared/api/tilbakekreving'
import { isPending, mapResult } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { addTilbakekreving } from '~store/reducers/TilbakekrevingReducer'
import { useAppDispatch } from '~store/Store'
import { ApiError } from '~shared/api/apiClient'
import { SendTilAttesteringModal } from '~components/behandling/handlinger/SendTilAttesteringModal'
import { TilbakekrevingSkalSendeBrev } from './TilbakekrevingSkalSendeBrev'
import Spinner from '~shared/Spinner'

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
  const [gyldigTilbakekreving, setGyldigTilbakekreving] = useState(false)

  const gaaTilBrev = () => {
    navigate(`/tilbakekreving/${behandling?.id}/brev`)
  }

  useEffect(() => {
    valider()
  }, [])

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
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading level="1" size="large">
            Oppsummering
          </Heading>
        </HeadingWrapper>
      </ContentHeader>
      <InnholdPadding>
        <TilbakekrevingVurderingOppsummering behandling={behandling} />
        <TilbakekrevingSkalSendeBrev behandling={behandling} redigerbar={redigerbar} />
        {mapResult(validerTilbakekrevingStatus, {
          pending: <Spinner label="Sjekker om tilbakekreving er fylt ut" visible={true} />,
          error: (error) => <TilbakekrevingValideringsfeil error={error} />,
        })}
      </InnholdPadding>
      <Border style={{ marginTop: '3em' }} />
      <FlexRow $spacing={true} justify="center">
        {behandling.sendeBrev && gyldigTilbakekreving ? (
          <Button variant="primary" onClick={gaaTilBrev} loading={isPending(validerTilbakekrevingStatus)}>
            Neste
          </Button>
        ) : (
          <>
            {redigerbar && gyldigTilbakekreving && (
              <SendTilAttesteringModal
                behandlingId={behandling.id}
                fattVedtakApi={fattVedtak}
                validerKanSendeTilAttestering={() => gyldigTilbakekreving}
              />
            )}
          </>
        )}
      </FlexRow>
    </Content>
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
