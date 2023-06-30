import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { Navn, OmgjoeringAvFarskapInfo, RevurderingInfo } from '~shared/types/RevurderingInfo'
import React, { FormEvent, useState } from 'react'
import { BodyShort, Button, Heading } from '@navikt/ds-react'
import { hentBehandlesFraStatus } from '~components/behandling/felles/utils'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { lagreRevurderingInfo } from '~shared/api/revurdering'
import { ApiErrorAlert } from '~ErrorBoundary'
import { oppdaterRevurderingInfo } from '~store/reducers/BehandlingReducer'
import styled from 'styled-components'
import { NavnInput } from '~components/behandling/revurderingsoversikt/NavnInput'

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
  const [naavaerendeFar, setNaavaerendeFar] = useState(omgjoeringAvFarskapInfo?.naavaerendeFar)
  const [forrigeFar, setForrigeFar] = useState(omgjoeringAvFarskapInfo?.forrigeFar)
  const [feilmelding, setFeilmelding] = useState<string | undefined>(undefined)
  const [lagrestatus, lagre] = useApiCall(lagreRevurderingInfo)
  const redigerbar = hentBehandlesFraStatus(behandling.status)
  const handlesubmit = (e: FormEvent) => {
    e.stopPropagation()
    e.preventDefault()
    setFeilmelding(undefined)
    if (
      !naavaerendeFar ||
      !forrigeFar ||
      !naavaerendeFar.fornavn ||
      !naavaerendeFar.etternavn ||
      !forrigeFar.fornavn ||
      !forrigeFar.etternavn
    ) {
      setFeilmelding('Du må legge inn forrige og nåværende registert far')
      return
    }
    const revurderingInfo: RevurderingInfo = {
      type: 'OMGJOERING_AV_FARSKAP',
      naavaerendeFar: naavaerendeFar,
      forrigeFar: forrigeFar,
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
          <NavnInput navn={naavaerendeFar} update={(n: Navn) => setNaavaerendeFar(n)} />
          <Heading size="medium" level="2">
            Hvem er registrert som far nå?
          </Heading>
          <NavnInput navn={forrigeFar} update={(n: Navn) => setForrigeFar(n)} />
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
            {!!naavaerendeFar
              ? [naavaerendeFar.fornavn, naavaerendeFar.mellomnavn, naavaerendeFar.etternavn].join(' ')
              : 'Ikke angitt'}
          </strong>
          Forrige far:{' '}
          <strong>
            {!!forrigeFar ? [forrigeFar.fornavn, forrigeFar.mellomnavn, forrigeFar.etternavn].join(' ') : 'Ikke angitt'}
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
