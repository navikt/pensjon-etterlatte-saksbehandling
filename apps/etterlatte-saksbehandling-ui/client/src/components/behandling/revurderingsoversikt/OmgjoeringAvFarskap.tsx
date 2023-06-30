import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { OmgjoeringAvFarskapInfo, RevurderingInfo } from '~shared/types/RevurderingInfo'
import React, { FormEvent, useState } from 'react'
import { BodyShort, Button, Heading, TextField } from '@navikt/ds-react'
import { hentBehandlesFraStatus } from '~components/behandling/felles/utils'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { lagreRevurderingInfo } from '~shared/api/revurdering'
import { ApiErrorAlert } from '~ErrorBoundary'
import { oppdaterRevurderingInfo } from '~store/reducers/BehandlingReducer'
import styled from 'styled-components'

function hentUndertypeFraBehandling(behandling?: IDetaljertBehandling): OmgjoeringAvFarskapInfo | null {
  const revurderinginfo = behandling?.revurderinginfo
  if (revurderinginfo?.type === 'OMGJOERING_AV_FARSKAP') {
    return revurderinginfo
  } else {
    return null
  }
}

export const OmgjoeringAvFarskap = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const omgjoeringAvFarskapInfo = hentUndertypeFraBehandling(behandling)
  const [fornavnNaavaerendeFar, setFornavnNaavaerendeFar] = useState(omgjoeringAvFarskapInfo?.naavaerendeFar?.fornavn)
  const [mellomnavnNaavaerendeFar, setMellomnavnNaavaerendeFar] = useState(
    omgjoeringAvFarskapInfo?.naavaerendeFar?.mellomnavn
  )
  const [etternavnNaavaerendeFar, setEtternavnNaavaerendeFar] = useState(
    omgjoeringAvFarskapInfo?.naavaerendeFar?.etternavn
  )
  const [fornavnForrigeFar, setFornavnForrigeFar] = useState(omgjoeringAvFarskapInfo?.naavaerendeFar?.fornavn)
  const [mellomnavnForrigeFar, setMellomnavnForrigeFar] = useState(omgjoeringAvFarskapInfo?.naavaerendeFar?.mellomnavn)
  const [etternavnForrigeFar, setEtternavnForrigeFar] = useState(omgjoeringAvFarskapInfo?.naavaerendeFar?.etternavn)
  const [feilmelding, setFeilmelding] = useState<string | undefined>(undefined)
  const [lagrestatus, lagre] = useApiCall(lagreRevurderingInfo)
  const redigerbar = hentBehandlesFraStatus(behandling.status)
  const handlesubmit = (e: FormEvent) => {
    e.stopPropagation()
    e.preventDefault()
    setFeilmelding(undefined)
    if (!fornavnNaavaerendeFar || !etternavnNaavaerendeFar || !fornavnForrigeFar || !etternavnForrigeFar) {
      setFeilmelding('Du må legge inn forrige og nåværende registert far')
      return
    }
    const revurderingInfo: RevurderingInfo = {
      type: 'OMGJOERING_AV_FARSKAP',
      naavaerendeFar: {
        fornavn: fornavnNaavaerendeFar,
        mellomnavn: mellomnavnNaavaerendeFar,
        etternavn: etternavnNaavaerendeFar,
      },
      forrigeFar: {
        fornavn: fornavnForrigeFar,
        mellomnavn: mellomnavnForrigeFar,
        etternavn: etternavnForrigeFar,
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
        Hvem var registrert som far før?
      </Heading>
      {redigerbar ? (
        <SkjemaWrapper onSubmit={handlesubmit}>
          <NavnWrapper>
            <TextField
              label={'Fornavn'}
              value={fornavnNaavaerendeFar}
              key={`naaevaerende-far-fornavn`}
              onChange={(e) => setFornavnNaavaerendeFar(e.target.value)}
            />
            <TextField
              label={'Mellomnavn'}
              value={mellomnavnNaavaerendeFar}
              key={`naaevaerende-far-mellomnavn`}
              onChange={(e) => setMellomnavnNaavaerendeFar(e.target.value)}
            />
            <TextField
              label={'Etternavn'}
              value={etternavnNaavaerendeFar}
              key={`naaevaerende-far-etternavn`}
              onChange={(e) => setEtternavnNaavaerendeFar(e.target.value)}
            />
          </NavnWrapper>
          <Heading size="medium" level="2">
            Hvem er registrert som far nå?
          </Heading>
          <NavnWrapper>
            <TextField
              label={'Fornavn'}
              value={fornavnForrigeFar}
              key={`forrige-far-fornavn`}
              onChange={(e) => setFornavnForrigeFar(e.target.value)}
            />
            <TextField
              label={'Mellomnavn'}
              value={mellomnavnForrigeFar}
              key={`forrige-far-mellomnavn`}
              onChange={(e) => setMellomnavnForrigeFar(e.target.value)}
            />
            <TextField
              label={'Etternavn'}
              value={etternavnForrigeFar}
              key={`forrige-far-etternavn`}
              onChange={(e) => setEtternavnForrigeFar(e.target.value)}
            />
          </NavnWrapper>
          <Button loading={isPending(lagrestatus)} variant="primary" size="small">
            Lagre
          </Button>
          {isSuccess(lagrestatus) ? <span>Lagret!</span> : null}
          {isFailure(lagrestatus) ? <ApiErrorAlert>Kunne ikke lagre navn på far</ApiErrorAlert> : null}
          {feilmelding ? <ApiErrorAlert>{feilmelding}</ApiErrorAlert> : null}
        </SkjemaWrapper>
      ) : (
        <BodyShort>
          Nåværende far:{' '}
          <strong>
            {!!fornavnNaavaerendeFar
              ? [fornavnNaavaerendeFar, mellomnavnNaavaerendeFar, etternavnNaavaerendeFar].join(' ')
              : 'Ikke angitt'}
          </strong>
          Forrige far:{' '}
          <strong>
            {!!fornavnForrigeFar
              ? [fornavnForrigeFar, mellomnavnForrigeFar, etternavnForrigeFar].join(' ')
              : 'Ikke angitt'}
          </strong>
        </BodyShort>
      )}
    </MarginTop>
  )
}

const SkjemaWrapper = styled.form`
  max-width: fit-content;
`

const MarginTop = styled.div`
  margin-top: 3em;
`

const NavnWrapper = styled.div`
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  gap: 1rem;
  padding-right: 1rem;
  padding-bottom: 2rem;
`
