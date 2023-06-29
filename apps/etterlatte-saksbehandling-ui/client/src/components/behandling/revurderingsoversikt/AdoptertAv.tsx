import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { RevurderingInfo } from '~shared/types/RevurderingInfo'
import React, { FormEvent, useState } from 'react'
import { BodyShort, Button, Heading, TextField } from '@navikt/ds-react'
import { hentBehandlesFraStatus } from '~components/behandling/felles/utils'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { lagreRevurderingInfo } from '~shared/api/revurdering'
import { ApiErrorAlert } from '~ErrorBoundary'
import { oppdaterRevurderingInfo } from '~store/reducers/BehandlingReducer'
import styled from 'styled-components'

export const AdoptertAv = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const [fornavn, setFornavn] = useState<string>('')
  const [mellomnavn, setMellomnavn] = useState<string>('')
  const [etternavn, setEtternavn] = useState<string>('')
  const [feilmelding, setFeilmelding] = useState<string | undefined>(undefined)
  const [lagrestatus, lagre] = useApiCall(lagreRevurderingInfo)
  const redigerbar = hentBehandlesFraStatus(behandling.status)
  const handlesubmit = (e: FormEvent) => {
    e.stopPropagation()
    e.preventDefault()
    setFeilmelding(undefined)
    if (!fornavn || !etternavn) {
      setFeilmelding('Du må velge hvem som adopterer')
      return
    }
    const revurderingInfo: RevurderingInfo = {
      type: 'ADOPSJON',
      adoptertAv: {
        fornavn: fornavn,
        mellomnavn: mellomnavn,
        etternavn: etternavn,
      },
    }
    lagre(
      {
        behandlingId: behandling.id,
        revurderingInfo,
      },
      () => oppdaterRevurderingInfo(revurderingInfo)
    )
  }

  return (
    <MarginTop>
      <Heading size="medium" level="2">
        Hvem er hen adoptert av?
      </Heading>
      {redigerbar ? (
        <SkjemaWrapper onSubmit={handlesubmit}>
          <TextField
            label={'Fornavn'}
            value={fornavn}
            key={`adoptertav-fornavn`}
            onChange={(e) => setFornavn(e.target.value)}
          />
          <TextField
            label={'Mellomnavn'}
            value={mellomnavn}
            key={`adoptertav-mellomnavn`}
            onChange={(e) => setMellomnavn(e.target.value)}
          />
          <TextField
            label={'Etternavn'}
            value={etternavn}
            key={`adoptertav-etternavn`}
            onChange={(e) => setEtternavn(e.target.value)}
          />
          <Button loading={isPending(lagrestatus)} variant="primary" size="small">
            Lagre
          </Button>
          {isSuccess(lagrestatus) ? <span>Lagret!</span> : null}
          {isFailure(lagrestatus) ? <ApiErrorAlert>Kunne ikke lagre adoptert av</ApiErrorAlert> : null}
          {feilmelding ? <ApiErrorAlert>{feilmelding}</ApiErrorAlert> : null}
        </SkjemaWrapper>
      ) : (
        <BodyShort>
          Adoptert av: <strong>{!!fornavn ? [fornavn, mellomnavn, etternavn].join(' ') : 'Ikke angitt'}</strong>
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
