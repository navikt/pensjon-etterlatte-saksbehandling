import { Button, ErrorSummary, Heading } from '@navikt/ds-react'
import { Content, ContentHeader, FlexRow } from '~shared/styled'
import { Border, HeadingWrapper, InnholdPadding } from '~components/behandling/soeknadsoversikt/styled'
import { useNavigate } from 'react-router-dom'
import React from 'react'
import { TilbakekrevingBehandling } from '~shared/types/Tilbakekreving'
import { TilbakekrevingVurderingOppsummering } from '~components/tilbakekreving/oppsummering/TilbakekrevingVurderingOppsummering'
import { useApiCall } from '~shared/hooks/useApiCall'
import { validerTilbakekreving } from '~shared/api/tilbakekreving'
import { isFailure } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import { addTilbakekreving } from '~store/reducers/TilbakekrevingReducer'
import { useAppDispatch } from '~store/Store'
import { ApiError } from '~shared/api/apiClient'
import { isPending } from '@reduxjs/toolkit'

export function TilbakekrevingOppsummering({ behandling }: { behandling: TilbakekrevingBehandling }) {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const [validerTilbakekrevingStatus, validerTilbakekrevingRequest] = useApiCall(validerTilbakekreving)

  const validerVurderingOgPerioder = () => {
    validerTilbakekrevingRequest(behandling.id, (lagretTilbakekreving) => {
      dispatch(addTilbakekreving(lagretTilbakekreving))
      navigate(`/tilbakekreving/${behandling?.id}/brev`)
    })
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
        {isFailure(validerTilbakekrevingStatus) && (
          <TilbakekrevingValideringsfeil error={validerTilbakekrevingStatus.error} />
        )}
      </InnholdPadding>
      <Border style={{ marginTop: '3em' }} />
      <FlexRow $spacing={true} justify="center">
        <Button variant="primary" onClick={validerVurderingOgPerioder} loading={isPending(validerTilbakekrevingStatus)}>
          Neste
        </Button>
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
