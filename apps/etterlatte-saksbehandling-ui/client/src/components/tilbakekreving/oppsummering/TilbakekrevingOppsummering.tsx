import { BodyLong, Button, Heading, VStack } from '@navikt/ds-react'
import { Content, ContentHeader, FlexRow } from '~shared/styled'
import { Border, HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
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

export function TilbakekrevingOppsummering({ behandling }: { behandling: TilbakekrevingBehandling }) {
  const dispatch = useAppDispatch()
  const navigate = useNavigate()
  const [validerTilbakekrevingStatus, validerTilbakekrevingRequest] = useApiCall(validerTilbakekreving)

  const valider = () => {
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
      <TilbakekrevingVurderingOppsummering behandling={behandling} />
      {isFailure(validerTilbakekrevingStatus) && (
        <TilbakekrevingValideringsfeil error={validerTilbakekrevingStatus.error} />
      )}
      <Border style={{ marginTop: '3em' }} />
      <FlexRow $spacing={true} justify="center">
        <Button variant="primary" onClick={valider}>
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
      <ApiErrorAlert>
        <VStack gap="4">
          <BodyLong style={{ fontWeight: 'bold' }}>{error.detail}</BodyLong>
          {ugyldigeFelterVurdering.length > 0 && (
            <div>
              <BodyLong>Manglende felter i vurdering:</BodyLong>
              <ul>
                {ugyldigeFelterVurdering.map((felt, index) => (
                  <li key={index} style={{ marginLeft: '1.4rem' }}>
                    {felt}
                  </li>
                ))}
              </ul>
            </div>
          )}
          {ugyldigeFelterPerioder.length > 0 && (
            <div>
              <BodyLong>Manglende felter i utbetalinger:</BodyLong>
              <ul>
                {ugyldigeFelterPerioder.map((felt, index) => (
                  <li key={index} style={{ marginLeft: '1.4rem' }}>
                    {felt}
                  </li>
                ))}
              </ul>
            </div>
          )}
        </VStack>
      </ApiErrorAlert>
    )
  } else {
    return <ApiErrorAlert>{error.detail}</ApiErrorAlert>
  }
}
