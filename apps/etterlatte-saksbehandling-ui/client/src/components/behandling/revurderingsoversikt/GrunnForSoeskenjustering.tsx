import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import {
  BarnepensjonSoeskenjusteringGrunn,
  hentUndertypeFraBehandling,
  RevurderingInfo,
  SOESKENJUSTERING_GRUNNER,
  SoeskenjusteringInfo,
  tekstSoeskenjustering,
} from '~shared/types/RevurderingInfo'
import React, { FormEvent, useState } from 'react'
import { BodyShort, Button, Heading, Select } from '@navikt/ds-react'
import { hentBehandlesFraStatus } from '~components/behandling/felles/utils'
import { isPending, isFailure, useApiCall, isSuccess } from '~shared/hooks/useApiCall'
import { lagreRevurderingInfo } from '~shared/api/revurdering'
import { ApiErrorAlert } from '~ErrorBoundary'
import { oppdaterRevurderingInfo } from '~store/reducers/BehandlingReducer'
import styled from 'styled-components'
import { Revurderingsbegrunnelse } from '~components/behandling/revurderingsoversikt/Revurderingsbegrunnelse'

export const GrunnForSoeskenjustering = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const soeskenjusteringInfo = hentUndertypeFraBehandling<SoeskenjusteringInfo>('SOESKENJUSTERING', behandling)
  const [valgtSoeskenjustering, setValgtSoeskenjustering] = useState<BarnepensjonSoeskenjusteringGrunn | undefined>(
    soeskenjusteringInfo?.grunnForSoeskenjustering
  )
  const [feilmelding, setFeilmelding] = useState<string | undefined>(undefined)
  const [begrunnelse, setBegrunnelse] = useState('')
  const [lagrestatus, lagre] = useApiCall(lagreRevurderingInfo)
  const redigerbar = hentBehandlesFraStatus(behandling.status)
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
          {isFailure(lagrestatus) ? <ApiErrorAlert>Kunne ikke lagre grunnen til søskenjustering</ApiErrorAlert> : null}
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
