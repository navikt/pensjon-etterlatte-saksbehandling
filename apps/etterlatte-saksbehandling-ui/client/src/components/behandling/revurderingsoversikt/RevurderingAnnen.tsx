import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import {
  hentUndertypeFraBehandling,
  RevurderingAarsakAnnen,
  RevurderingAarsakAnnenUtenBrev,
  RevurderingInfo,
} from '~shared/types/RevurderingInfo'
import { FormEvent, useState } from 'react'
import { BodyShort, Button, Heading, TextField, VStack } from '@navikt/ds-react'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreRevurderingInfo } from '~shared/api/revurdering'
import { oppdaterRevurderingInfo } from '~store/reducers/BehandlingReducer'
import styled from 'styled-components'
import { Revurderingsbegrunnelse } from '~components/behandling/revurderingsoversikt/Revurderingsbegrunnelse'

import { isPending, isSuccess } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useAppSelector } from '~store/Store'
import { Toast } from '~shared/alerts/Toast'

export const RevurderingAnnen = (props: { type: 'ANNEN' | 'ANNEN_UTEN_BREV'; behandling: IDetaljertBehandling }) => {
  const { type, behandling } = props
  const revurderingAnnenInfo = hentUndertypeFraBehandling<RevurderingAarsakAnnen | RevurderingAarsakAnnenUtenBrev>(
    type,
    behandling
  )
  const [revurderingsaarsak, setRevurderingsaarsak] = useState(revurderingAnnenInfo?.aarsak)
  const [begrunnelse, setBegrunnelse] = useState(behandling.revurderinginfo?.begrunnelse ?? '')
  const [feilmelding, setFeilmelding] = useState<string | null>(null)
  const [lagrestatus, lagre] = useApiCall(lagreRevurderingInfo)
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  const redigerbar = behandlingErRedigerbar(behandling.status) && innloggetSaksbehandler.skriveTilgang

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
        begrunnelse: begrunnelse,
        revurderingInfo,
      },
      () => oppdaterRevurderingInfo(revurderingInfo)
    )
  }

  return (
    <>
      {redigerbar ? (
        <SkjemaWrapper onSubmit={handlesubmit}>
          <AarsakWrapper>
            <Heading size="medium" level="3">
              Årsak til revurdering
            </Heading>
            <TextField
              value={revurderingsaarsak ?? ''}
              onChange={(e) => setRevurderingsaarsak(e.target.value)}
              error={feilmelding && !revurderingsaarsak && 'Årsak kan ikke være tom'}
              label=""
            />
          </AarsakWrapper>
          <Revurderingsbegrunnelse
            begrunnelse={begrunnelse}
            setBegrunnelse={setBegrunnelse}
            feilmelding={feilmelding}
            redigerbar={true}
          />
          <Button loading={isPending(lagrestatus)} variant="primary" size="small">
            Lagre
          </Button>
          {isSuccess(lagrestatus) && <Toast melding="Lagret" />}
          {isFailureHandler({
            apiResult: lagrestatus,
            errorMessage: 'Kunne ikke lagre',
          })}
        </SkjemaWrapper>
      ) : (
        <VStack gap="10" style={{ marginTop: '3rem', maxWidth: '40rem' }}>
          <VStack gap="2">
            <Heading size="medium" level="3">
              Årsak til revurdering
            </Heading>
            <BodyShort>{revurderingsaarsak}</BodyShort>
          </VStack>
          <VStack gap="2">
            <Heading size="medium" level="3">
              Begrunnelse
            </Heading>
            <BodyShort>{begrunnelse}</BodyShort>
          </VStack>
        </VStack>
      )}
    </>
  )
}

const SkjemaWrapper = styled.form`
  max-width: 40rem;
  margin-top: 3rem;

  & > *:not(:first-child) {
    margin-top: 1rem;
  }
`

const AarsakWrapper = styled.div`
  gap: 1rem;
  padding-right: 1rem;
  padding-bottom: 1rem;
`
