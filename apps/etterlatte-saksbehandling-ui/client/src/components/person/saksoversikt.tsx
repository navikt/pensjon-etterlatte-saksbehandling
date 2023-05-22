import React, { useEffect, useState } from 'react'
import { Behandlingsliste } from './behandlingsliste'
import styled from 'styled-components'
import { Grunnlagsendringshendelse, GrunnlagsendringsListe, IBehandlingListe, IBehandlingsammendrag } from './typer'
import { ManueltOpphoerModal } from './ManueltOpphoerModal'
import { ToKolonner } from '../toKolonner/ToKolonner'
import {
  hentBehandlingerForPerson,
  hentGrunnlagsendringshendelserForPerson,
  hentStoettedeRevurderinger,
} from '~shared/api/behandling'
import { Container } from '~shared/styled'
import Spinner from '~shared/Spinner'
import {
  erFerdigBehandlet,
  harIngenUavbrutteManuelleOpphoer,
  kunIverksatteBehandlinger,
} from '~components/behandling/felles/utils'
import { IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import RelevanteHendelser from '~components/person/uhaandtereHendelser/RelevanteHendelser'
import { isFailure, useApiCall } from '~shared/hooks/useApiCall'
import { hentPersonerISak } from '~shared/api/grunnlag'
import { ApiErrorAlert } from '~ErrorBoundary'
import { Revurderingsaarsak } from '~shared/types/Revurderingsaarsak'

export const Saksoversikt = ({ fnr }: { fnr: string | undefined }) => {
  const [behandlingliste, setBehandlingliste] = useState<IBehandlingsammendrag[]>([])
  const [grunnlagshendelser, setGrunnlagshendelser] = useState<Grunnlagsendringshendelse[] | undefined>([])
  const [lastetBehandlingliste, setLastetBehandlingliste] = useState<boolean>(false)
  const [lastetGrunnlagshendelser, setLastetGrunnlagshendelser] = useState<boolean>(false)
  const [behandlinglisteError, setBehandlinglisteError] = useState<IBehandlingsammendrag[]>()
  const [grunnlagshendelserError, setGrunnlagshendelserError] = useState<Grunnlagsendringshendelse[]>()
  const [sakId, setSakId] = useState<number | undefined>()
  const [personerISak, hentPersoner, resetPersoner] = useApiCall(hentPersonerISak)

  const [hentStoettedeRevurderingerStatus, hentStoettedeRevurderingerFc] = useApiCall(hentStoettedeRevurderinger)
  const [revurderinger, setStoettedeRevurderinger] = useState<Array<Revurderingsaarsak> | undefined>(undefined)
  useEffect(() => {
    hentStoettedeRevurderingerFc(
      {},
      (ans) => setStoettedeRevurderinger(ans),
      () => {}
    )
  }, [])

  useEffect(() => {
    if (!sakId) return resetPersoner
    void hentPersoner(sakId)
    return resetPersoner
  }, [sakId])

  useEffect(() => {
    const getBehandlingsListeAsync = async (fnr: string) => {
      const response = await hentBehandlingerForPerson(fnr)

      if (response.status === 'ok') {
        const responseData: IBehandlingListe[] = response?.data

        if (responseData && responseData.length > 0) {
          const sortedData = responseData[0].behandlinger.sort((a, b) =>
            new Date(b.behandlingOpprettet!) > new Date(a.behandlingOpprettet!) ? 1 : -1
          )
          setBehandlingliste(sortedData)
          setSakId(sortedData[0]?.sak)
        }
      } else {
        setBehandlinglisteError(response?.error)
      }

      setLastetBehandlingliste(true)
    }

    const getGrunnlagshendelserAsync = async (fnr: string) => {
      const response = await hentGrunnlagsendringshendelserForPerson(fnr)

      if (response.status === 'ok') {
        const responseData: GrunnlagsendringsListe[] = response?.data

        if (responseData && responseData.length > 0) {
          setGrunnlagshendelser(responseData[0].hendelser)
        }
      } else {
        setGrunnlagshendelserError(response?.error)
      }

      setLastetGrunnlagshendelser(true)
    }

    if (fnr) {
      getBehandlingsListeAsync(fnr)
      getGrunnlagshendelserAsync(fnr)
    }
  }, [])

  if (
    (behandlinglisteError && behandlingliste === undefined) ||
    (grunnlagshendelserError && grunnlagshendelser === undefined)
  ) {
    return (
      <Container>
        {behandlinglisteError && <div>{JSON.stringify(behandlinglisteError)}</div>}
        {grunnlagshendelserError && <div>{JSON.stringify(grunnlagshendelserError)}</div>}
      </Container>
    )
  }

  const iverksatteBehandlinger = kunIverksatteBehandlinger(behandlingliste)
  const kanOppretteManueltOpphoer =
    iverksatteBehandlinger.length > 0 && harIngenUavbrutteManuelleOpphoer(behandlingliste)

  const harAapenRevurdering =
    behandlingliste
      .filter((behandling) => behandling.behandlingType === IBehandlingsType.REVURDERING)
      .filter((behandling) => !erFerdigBehandlet(behandling.status)).length > 0

  const hendelser = grunnlagshendelser ?? []

  return (
    <>
      <Spinner visible={!lastetBehandlingliste || !lastetGrunnlagshendelser} label={'Laster'} />
      {lastetBehandlingliste && lastetGrunnlagshendelser && (
        <SaksoversiktWrapper>
          <ToKolonner>
            {{
              left: (
                <>
                  {sakId !== undefined ? (
                    <EkstraHandlinger>
                      {kanOppretteManueltOpphoer && (
                        <ManueltOpphoerModal sakId={sakId} iverksatteBehandlinger={iverksatteBehandlinger} />
                      )}
                    </EkstraHandlinger>
                  ) : null}
                  {isFailure(hentStoettedeRevurderingerStatus) && (
                    <ApiErrorAlert>En feil skjedde under kallet for å hente støttede revurderinger</ApiErrorAlert>
                  )}
                  {sakId !== undefined && revurderinger ? (
                    <RelevanteHendelser
                      hendelser={hendelser}
                      revurderinger={revurderinger}
                      disabled={harAapenRevurdering}
                      grunnlag={personerISak}
                      sakId={sakId}
                    />
                  ) : null}
                  <div className="behandlinger">
                    <h2>Behandlinger</h2>
                    {behandlingliste !== undefined && <Behandlingsliste behandlinger={behandlingliste} />}
                  </div>
                </>
              ),
              right: null,
            }}
          </ToKolonner>
        </SaksoversiktWrapper>
      )}
    </>
  )
}

const EkstraHandlinger = styled.div`
  display: flex;
  flex-direction: row-reverse;
  gap: 0.5em;
`

export const SaksoversiktWrapper = styled.div`
  min-width: 40em;
  max-width: 100%;

  margin: 3em 1em;

  .behandlinger {
    margin-top: 5em;
  }

  h1 {
    margin-bottom: 1em;
    &::first-letter {
      text-transform: capitalize;
    }
  }

  .button {
    margin-top: 4em;
    padding-left: 2em;
    padding-right: 2em;
  }
`

export const HeadingWrapper = styled.div`
  display: inline-flex;
  margin-top: 3em;

  .details {
    padding: 0.6em;
  }
`
