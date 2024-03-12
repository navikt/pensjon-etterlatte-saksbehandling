import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import {
  BarnepensjonSoeskenjusteringGrunn,
  hentUndertypeFraBehandling,
  RevurderingInfo,
  RevurderingMedBegrunnelse,
  SOESKENJUSTERING_GRUNNER,
  SoeskenjusteringInfo,
  tekstSoeskenjustering,
} from '~shared/types/RevurderingInfo'
import React, { FormEvent, useState } from 'react'
import { BodyShort, Button, Heading, Select } from '@navikt/ds-react'
import { behandlingErRedigerbar } from '~components/behandling/felles/utils'
import { useApiCall } from '~shared/hooks/useApiCall'
import { lagreRevurderingInfo } from '~shared/api/revurdering'
import { ApiErrorAlert } from '~ErrorBoundary'
import { oppdaterRevurderingInfo } from '~store/reducers/BehandlingReducer'
import styled from 'styled-components'
import { Revurderingsbegrunnelse } from '~components/behandling/revurderingsoversikt/Revurderingsbegrunnelse'

import { isPending, isSuccess } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { useAppDispatch, useAppSelector } from '~store/Store'

export const GrunnForSoeskenjustering = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const dispatch = useAppDispatch()
  const soeskenjusteringInfo = hentUndertypeFraBehandling<SoeskenjusteringInfo>('SOESKENJUSTERING', behandling)
  const [valgtSoeskenjustering, setValgtSoeskenjustering] = useState<BarnepensjonSoeskenjusteringGrunn | undefined>(
    soeskenjusteringInfo?.grunnForSoeskenjustering
  )
  const [feilmelding, setFeilmelding] = useState<string | undefined>(undefined)
  const [begrunnelse, setBegrunnelse] = useState(behandling.revurderinginfo?.begrunnelse ?? '')
  const [lagrestatus, lagre] = useApiCall(lagreRevurderingInfo)
  const innloggetSaksbehandler = useAppSelector((state) => state.saksbehandlerReducer.innloggetSaksbehandler)
  const redigerbar = behandlingErRedigerbar(behandling.status) && innloggetSaksbehandler.skriveTilgang
  const harEndretInfo =
    soeskenjusteringInfo?.grunnForSoeskenjustering !== null &&
    valgtSoeskenjustering !== soeskenjusteringInfo?.grunnForSoeskenjustering
  const handlesubmit = (e: FormEvent) => {
    e.stopPropagation()
    e.preventDefault()
    setFeilmelding(undefined)
    if (valgtSoeskenjustering === undefined) {
      setFeilmelding('Du må velge hvorfor du gjennomfører en søskenjustering')
      return
    }
    const revurderingInfo: RevurderingInfo = {
      type: 'SOESKENJUSTERING',
      grunnForSoeskenjustering: valgtSoeskenjustering,
    }

    const revurderingMedBegrunnelse: RevurderingMedBegrunnelse = {
      revurderingInfo: revurderingInfo,
      begrunnelse: begrunnelse,
    }

    lagre(
      {
        behandlingId: behandling.id,
        begrunnelse: begrunnelse,
        revurderingInfo,
      },
      () => dispatch(oppdaterRevurderingInfo(revurderingMedBegrunnelse))
    )
  }

  return (
    <MarginTop>
      <Heading size="medium" level="2">
        Velg brevmal for søskenjusteringen
      </Heading>
      {redigerbar ? (
        <SkjemaWrapper onSubmit={handlesubmit}>
          <Select
            label="Hvorfor søskenjusteres det?"
            onChange={(e) => setValgtSoeskenjustering(e.target.value as BarnepensjonSoeskenjusteringGrunn)}
            defaultValue={valgtSoeskenjustering}
          >
            <option value={undefined}>Velg grunn for søksenjustering</option>
            {SOESKENJUSTERING_GRUNNER.map((grunn) => (
              <option key={grunn} value={grunn}>
                {tekstSoeskenjustering[grunn]}
              </option>
            ))}
          </Select>
          <Revurderingsbegrunnelse begrunnelse={begrunnelse} setBegrunnelse={setBegrunnelse} redigerbar={true} />
          <Button loading={isPending(lagrestatus)} variant="primary" size="small">
            Lagre
          </Button>
          {isSuccess(lagrestatus) && !harEndretInfo ? <span>Lagret!</span> : null}
          {isFailureHandler({
            apiResult: lagrestatus,
            errorMessage: 'Kunne ikke lagre grunnen til søskenjustering',
          })}
          {feilmelding ? <ApiErrorAlert>{feilmelding}</ApiErrorAlert> : null}
        </SkjemaWrapper>
      ) : (
        <BodyShort>
          Grunn for søskenjustering:{' '}
          <strong>{valgtSoeskenjustering ? tekstSoeskenjustering[valgtSoeskenjustering] : 'Ikke angitt'}</strong>
          <Revurderingsbegrunnelse begrunnelse={begrunnelse} setBegrunnelse={setBegrunnelse} redigerbar={false} />
        </BodyShort>
      )}
    </MarginTop>
  )
}

const SkjemaWrapper = styled.form`
  max-width: fit-content;
  & > *:not(:first-child) {
    margin-top: 1rem;
  }
`

const MarginTop = styled.div`
  margin-top: 3em;
`
