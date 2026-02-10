import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import {
  hentUndertypeFraBehandling,
  RevurderingAarsakAnnen,
  RevurderingAarsakAnnenUtenBrev,
  RevurderingInfo,
} from '~shared/types/RevurderingInfo'
import { FormEvent, useState } from 'react'
import { BodyLong, BodyShort, Button, Heading, Textarea, TextField, VStack } from '@navikt/ds-react'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreRevurderingInfo } from '~shared/api/revurdering'
import { oppdaterRevurderingInfo } from '~store/reducers/BehandlingReducer'
import styled from 'styled-components'

import { isPending, mapResult } from '~shared/api/apiUtils'
import { useAppDispatch } from '~store/Store'
import { Toast } from '~shared/alerts/Toast'
import { useInnloggetSaksbehandler } from '../useInnloggetSaksbehandler'
import { ApiErrorAlert } from '~ErrorBoundary'

export const RevurderingAnnen = (props: { type: 'ANNEN' | 'ANNEN_UTEN_BREV'; behandling: IDetaljertBehandling }) => {
  const { type, behandling } = props
  const dispatch = useAppDispatch()
  const revurderingAnnenInfo = hentUndertypeFraBehandling<RevurderingAarsakAnnen | RevurderingAarsakAnnenUtenBrev>(
    type,
    behandling
  )
  const [revurderingsaarsak, setRevurderingsaarsak] = useState(revurderingAnnenInfo?.aarsak)
  const [begrunnelse, setBegrunnelse] = useState(behandling.revurderinginfo?.begrunnelse ?? '')
  const [feilmelding, setFeilmelding] = useState<string | null>(null)
  const [lagrestatus, lagre] = useApiCall(lagreRevurderingInfo)
  const innloggetSaksbehandler = useInnloggetSaksbehandler()
  const redigerbar = behandlingErRedigerbar(
    behandling.status,
    behandling.sakEnhetId,
    innloggetSaksbehandler.skriveEnheter
  )

  const handlesubmit = (e: FormEvent) => {
    e.stopPropagation()
    e.preventDefault()
    setFeilmelding(null)

    if (!revurderingsaarsak) {
      setFeilmelding('Årsak mangler')
      return
    }
    if (!begrunnelse) {
      setFeilmelding('Begrunnelse mangler')
      return
    }

    const revurderingInfo: RevurderingInfo = {
      type: type,
      aarsak: revurderingsaarsak,
    }

    lagre(
      {
        behandlingId: behandling.id,
        revurderingInfo,
        begrunnelse,
      },
      () => {
        dispatch(oppdaterRevurderingInfo({ revurderingInfo, begrunnelse }))
      }
    )
  }

  return (
    <>
      <SkjemaWrapper onSubmit={handlesubmit}>
        <VStack gap="space-8">
          <VStack gap="space-2">
            <Heading size="medium" level="3">
              Årsak til revurdering
            </Heading>
            {redigerbar ? (
              <TextField
                value={revurderingsaarsak ?? ''}
                onChange={(e) => setRevurderingsaarsak(e.target.value)}
                error={feilmelding && !revurderingsaarsak && 'Årsak kan ikke være tom'}
                label=""
              />
            ) : (
              <BodyShort>{revurderingsaarsak}</BodyShort>
            )}
          </VStack>
          <VStack gap="space-2">
            <Heading size="medium" level="3">
              Begrunnelse
            </Heading>
            {redigerbar ? (
              <VStack gap="space-4">
                <Textarea
                  value={begrunnelse}
                  onChange={(e) => {
                    setBegrunnelse(e.target.value)
                  }}
                  placeholder="Begrunnelse"
                  error={feilmelding && !begrunnelse && 'Begrunnelse kan ikke være tom'}
                  label=""
                />
                <Button loading={isPending(lagrestatus)} variant="primary" size="small" style={{ maxWidth: '5em' }}>
                  Lagre
                </Button>
                {mapResult(lagrestatus, {
                  success: () => <Toast melding="Lagret" />,
                  error: (error) => <ApiErrorAlert>Kunne ikke lagre: {error.detail}</ApiErrorAlert>,
                })}
              </VStack>
            ) : (
              <BodyLong>{begrunnelse}</BodyLong>
            )}
          </VStack>
        </VStack>
      </SkjemaWrapper>
    </>
  )
}

const SkjemaWrapper = styled.form`
  max-width: 40rem;
  margin-top: 3rem;
`
