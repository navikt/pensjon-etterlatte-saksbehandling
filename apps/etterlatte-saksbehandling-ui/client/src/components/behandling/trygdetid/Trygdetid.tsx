import { Content, ContentHeader } from '~shared/styled'
import { HeadingWrapper } from '~components/behandling/soeknadsoversikt/styled'
import { Button, Heading, TextField } from '@navikt/ds-react'
import { Innhold } from '~components/behandling/trygdetid/styled'
import { hentBehandlesFraStatus } from '~components/behandling/felles/utils'
import { BehandlingHandlingKnapper } from '~components/behandling/handlinger/BehandlingHandlingKnapper'
import { NesteOgTilbake } from '~components/behandling/handlinger/NesteOgTilbake'
import { useBehandlingRoutes } from '~components/behandling/BehandlingRoutes'
import { handlinger } from '~components/behandling/handlinger/typer'
import { isFailure, isPending, useApiCall } from '~shared/hooks/useApiCall'
import { hentTrygdetid, ITrygdetid, ITrygdetidGrunnlag, lagreTrygdetidgrunnlag } from '~shared/api/trygdetid'
import React, { FormEvent, useEffect, useState } from 'react'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import styled from 'styled-components'
import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'

export const Trygdetid = (props: { behandling: IDetaljertBehandling }) => {
  const { behandling } = props
  const behandles = hentBehandlesFraStatus(behandling.status)
  const { next } = useBehandlingRoutes()

  const [trygdetidStatus, fetchTrygdetid] = useApiCall(hentTrygdetid)
  const [trygdetid, setTrygdetid] = useState<ITrygdetid>()

  const [nyttTrygdetidgrunnlag, setNyttTrygdetidgrunnlag] = useState<ITrygdetidGrunnlag>()
  const [trygdetidgrunnlagStatus, fetchLagretrygdetidgrunnlag] = useApiCall(lagreTrygdetidgrunnlag)
  const onSubmit = (e: FormEvent) => {
    e.preventDefault()
    fetchLagretrygdetidgrunnlag(nyttTrygdetidgrunnlag!)
  }

  useEffect(() => {
    fetchTrygdetid(null, (trygdetid: ITrygdetid) => {
      setTrygdetid(trygdetid)
    })
  }, [])

  return (
    <Content>
      <ContentHeader>
        <HeadingWrapper>
          <Heading spacing size="large" level="1">
            Trygdetid
          </Heading>
        </HeadingWrapper>
      </ContentHeader>

      {trygdetid && (
        <Innhold>
          <OppsummeringListe>
            {trygdetid.grunnlag.map((grunnlag) => (
              <li key={grunnlag.bosted}>
                {grunnlag.bosted} fra {grunnlag.periodeTil} til {grunnlag.periodeTil}
              </li>
            ))}
          </OppsummeringListe>
          <form onSubmit={onSubmit}>
            <FormWrapper>
              <TextField
                label="Nytt trygdegrunnlag"
                size="small"
                type="text"
                value={nyttTrygdetidgrunnlag?.bosted}
                onChange={(e) =>
                  setNyttTrygdetidgrunnlag({
                    bosted: e.target.value,
                    periodeTil: '2023-03-10T00:00:00.000+01:00',
                    periodeFra: '2023-03-10T00:00:00.000+01:00',
                  })
                }
              />
            </FormWrapper>
            <FormKnapper>
              <Button loading={isPending(trygdetidgrunnlagStatus)} type="submit">
                Lagre
              </Button>
            </FormKnapper>
          </form>
        </Innhold>
      )}

      {isPending(trygdetidStatus) && <Spinner visible={true} label={'Henter trygdetid'} />}
      {isFailure(trygdetidStatus) && <ApiErrorAlert>En feil har oppstått</ApiErrorAlert>}
      {isFailure(trygdetidgrunnlagStatus) && <ApiErrorAlert>En feil har oppstått</ApiErrorAlert>}

      {behandles ? (
        <BehandlingHandlingKnapper>
          <Button variant="primary" size="medium" className="button" onClick={next}>
            {handlinger.START.navn}
          </Button>
        </BehandlingHandlingKnapper>
      ) : (
        <NesteOgTilbake />
      )}
    </Content>
  )
}
const FormKnapper = styled.div`
  margin-top: 1rem;
  display: flex;
  gap: 1rem;
`
const FormWrapper = styled.div`
  max-width: 20rem;
  display: flex;
  gap: 1rem;
  flex-direction: column;
`
const OppsummeringListe = styled.ul`
  margin: 0 0 2em 2em;
`
