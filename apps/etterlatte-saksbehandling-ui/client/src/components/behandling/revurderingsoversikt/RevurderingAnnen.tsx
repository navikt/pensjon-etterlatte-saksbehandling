import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { hentUndertypeFraBehandling, RevurderingAarsakAnnen, RevurderingInfo } from '~shared/types/RevurderingInfo'
import { FormEvent, useState } from 'react'
import { BodyShort, Button, Heading, TextField } from '@navikt/ds-react'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreRevurderingInfo } from '~shared/api/revurdering'
import { ApiErrorAlert } from '~ErrorBoundary'
import { oppdaterRevurderingInfo } from '~store/reducers/BehandlingReducer'
import styled from 'styled-components'
import { Revurderingsbegrunnelse } from '~components/behandling/revurderingsoversikt/Revurderingsbegrunnelse'

import { isPending, isSuccess } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useAppSelector } from '~store/Store'

export const RevurderingAnnen = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const revurderingAnnenInfo = hentUndertypeFraBehandling<RevurderingAarsakAnnen>('ANNEN', behandling)
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
      type: 'ANNEN',
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
              Årsak til revurdering (Annen)
            </Heading>
            <TextField
              value={revurderingsaarsak ?? ''}
              onChange={(e) => setRevurderingsaarsak(e.target.value)}
              error={!revurderingsaarsak && 'Årsak kan ikke være tom'}
              label=""
            />
          </AarsakWrapper>
          <Revurderingsbegrunnelse begrunnelse={begrunnelse} setBegrunnelse={setBegrunnelse} redigerbar={true} />
          <Button loading={isPending(lagrestatus)} variant="primary" size="small">
            Lagre
          </Button>
          {isSuccess(lagrestatus) && <span>Lagret!</span>}
          {isFailureHandler({
            apiResult: lagrestatus,
            errorMessage: 'Kunne ikke lagre',
          })}
          {feilmelding && <ApiErrorAlert>{feilmelding}</ApiErrorAlert>}
        </SkjemaWrapper>
      ) : (
        <BodyShort>
          <AarsakWrapper>
            <Heading size="medium" level="3">
              Årsak til revurdering (Annen)
            </Heading>
            {revurderingsaarsak}
          </AarsakWrapper>
          <Revurderingsbegrunnelse begrunnelse={begrunnelse} setBegrunnelse={setBegrunnelse} redigerbar={false} />
        </BodyShort>
      )}
    </>
  )
}

const SkjemaWrapper = styled.form`
  max-width: fit-content;

  & > *:not(:first-child) {
    margin-top: 1rem;
  }
`

const AarsakWrapper = styled.div`
  gap: 1rem;
  padding-right: 1rem;
  margin-top: 0rem;
  padding-bottom: 1rem;
`
