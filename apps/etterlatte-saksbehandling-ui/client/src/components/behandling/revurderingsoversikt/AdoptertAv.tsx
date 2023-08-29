import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { AdopsjonInfo, hentUndertypeFraBehandling, Navn, RevurderingInfo } from '~shared/types/RevurderingInfo'
import React, { FormEvent, useState } from 'react'
import { BodyShort, Button, Heading } from '@navikt/ds-react'
import { hentBehandlesFraStatus } from '~components/behandling/felles/utils'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { lagreRevurderingInfo } from '~shared/api/revurdering'
import { ApiErrorAlert } from '~ErrorBoundary'
import { oppdaterRevurderingInfo } from '~store/reducers/BehandlingReducer'
import styled from 'styled-components'
import { NavnInput, standardnavn } from '~components/behandling/revurderingsoversikt/NavnInput'
import { Revurderingsbegrunnelse } from '~components/behandling/revurderingsoversikt/Revurderingsbegrunnelse'

function formaterNavn(navn: Navn) {
  return [navn.fornavn, navn.mellomnavn, navn.etternavn].join(' ')
}

export const AdoptertAv = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const adopsjonInfo = hentUndertypeFraBehandling<AdopsjonInfo>('ADOPSJON', behandling)
  const [navn1, setNavn1] = useState(adopsjonInfo?.adoptertAv1)
  const [navn2, setNavn2] = useState(adopsjonInfo?.adoptertAv2)
  const [feilmelding, setFeilmelding] = useState<string | undefined>(undefined)
  const [begrunnelse, setBegrunnelse] = useState(behandling.revurderinginfo?.begrunnelse ?? '')
  const [lagrestatus, lagre] = useApiCall(lagreRevurderingInfo)
  const redigerbar = hentBehandlesFraStatus(behandling.status)
  const handlesubmit = (e: FormEvent) => {
    e.stopPropagation()
    e.preventDefault()
    setFeilmelding(undefined)
    if (!navn1 || !navn1.fornavn || !navn1.etternavn) {
      setFeilmelding('Du må velge hvem som adopterer')
      return
    }
    if (!begrunnelse) {
      setFeilmelding('Begrunnelse mangler')
      return
    }
    const revurderingInfo: RevurderingInfo = {
      type: 'ADOPSJON',
      adoptertAv1: navn1,
      adoptertAv2: navn2,
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
        Hvem er hen adoptert av?
      </Heading>
      {redigerbar ? (
        <SkjemaWrapper onSubmit={handlesubmit}>
          <Heading size="small" level="3">
            Den første som adopterer
          </Heading>
          <NavnInput navn={navn1 || standardnavn()} update={(n: Navn) => setNavn1(n)} />
          <Heading size="small" level="3">
            Den andre som adopterer, hvis det er to
          </Heading>
          <NavnInput navn={navn2 || standardnavn()} update={(n: Navn) => setNavn2(n)} />
          <Revurderingsbegrunnelse begrunnelse={begrunnelse} setBegrunnelse={setBegrunnelse} redigerbar={true} />
          <Button loading={isPending(lagrestatus)} variant="primary" size="small">
            Lagre
          </Button>
          {isSuccess(lagrestatus) ? <span>Lagret!</span> : null}
          {isFailure(lagrestatus) ? <ApiErrorAlert>Kunne ikke lagre adoptert av</ApiErrorAlert> : null}
          {feilmelding ? <ApiErrorAlert>{feilmelding}</ApiErrorAlert> : null}
        </SkjemaWrapper>
      ) : (
        <BodyShort>
          Adoptert av: <strong>{!!navn1 ? formaterNavn(navn1) : 'Ikke angitt'}</strong>
          {adopsjonInfo?.adoptertAv2 ? <strong> og {formaterNavn(adopsjonInfo?.adoptertAv2)}</strong> : ''}
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
