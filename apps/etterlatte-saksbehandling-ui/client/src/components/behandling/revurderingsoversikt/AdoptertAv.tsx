import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { AdopsjonInfo, Navn, RevurderingInfo } from '~shared/types/RevurderingInfo'
import React, { FormEvent, useState } from 'react'
import { BodyShort, Button, Heading } from '@navikt/ds-react'
import { hentBehandlesFraStatus } from '~components/behandling/felles/utils'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { lagreRevurderingInfo } from '~shared/api/revurdering'
import { ApiErrorAlert } from '~ErrorBoundary'
import { oppdaterRevurderingInfo } from '~store/reducers/BehandlingReducer'
import styled from 'styled-components'
import { NavnInput } from '~components/behandling/revurderingsoversikt/NavnInput'

function hentUndertypeFraBehandling(behandling?: IDetaljertBehandling): AdopsjonInfo | null {
  const revurderinginfo = behandling?.revurderinginfo
  if (revurderinginfo?.type === 'ADOPSJON') {
    return revurderinginfo
  } else {
    return null
  }
}

export const AdoptertAv = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const adopsjonInfo = hentUndertypeFraBehandling(behandling)
  const [navn, setNavn] = useState(adopsjonInfo?.adoptertAv)
  const [feilmelding, setFeilmelding] = useState<string | undefined>(undefined)
  const [lagrestatus, lagre] = useApiCall(lagreRevurderingInfo)
  const redigerbar = hentBehandlesFraStatus(behandling.status)
  const handlesubmit = (e: FormEvent) => {
    e.stopPropagation()
    e.preventDefault()
    setFeilmelding(undefined)
    if (!navn || !navn.fornavn || !navn.etternavn) {
      setFeilmelding('Du må velge hvem som adopterer')
      return
    }
    const revurderingInfo: RevurderingInfo = {
      type: 'ADOPSJON',
      adoptertAv: navn,
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
          <NavnInput navn={adopsjonInfo?.adoptertAv} update={(n: Navn) => setNavn(n)} />
          <Button loading={isPending(lagrestatus)} variant="primary" size="small">
            Lagre
          </Button>
          {isSuccess(lagrestatus) ? <span>Lagret!</span> : null}
          {isFailure(lagrestatus) ? <ApiErrorAlert>Kunne ikke lagre adoptert av</ApiErrorAlert> : null}
          {feilmelding ? <ApiErrorAlert>{feilmelding}</ApiErrorAlert> : null}
        </SkjemaWrapper>
      ) : (
        <BodyShort>
          Adoptert av:{' '}
          <strong>{!!navn ? [navn.fornavn, navn.mellomnavn, navn.etternavn].join(' ') : 'Ikke angitt'}</strong>
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
