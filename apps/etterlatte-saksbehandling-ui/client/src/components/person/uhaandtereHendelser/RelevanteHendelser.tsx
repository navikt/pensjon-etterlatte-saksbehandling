import { Alert, Heading, Table } from '@navikt/ds-react'
import { Grunnlagsendringshendelse, IBehandlingsammendrag, STATUS_IRRELEVANT } from '~components/person/typer'
import { FnrTilNavnMapContext } from '~components/person/uhaandtereHendelser/utils'
import { useApiCall } from '~shared/hooks/useApiCall'
import React, { useEffect, useMemo, useState } from 'react'
import { hentPersonerISak } from '~shared/api/grunnlag'
import HistoriskeHendelser from '~components/person/uhaandtereHendelser/HistoriskeHendelser'
import { OpprettNyRevurdering } from '~components/person/OpprettNyRevurdering'
import VurderHendelseModal from '~components/person/VurderHendelseModal'
import UhaandtertHendelse from '~components/person/uhaandtereHendelser/UhaandtertHendelse'
import { IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import { behandlingErIverksattEllerSamordnet, erFerdigBehandlet } from '~components/behandling/felles/utils'
import { hentGrunnlagsendringshendelserForSak } from '~shared/api/behandling'
import Spinner from '~shared/Spinner'
import { ISak } from '~shared/types/sak'
import { hentStoettedeRevurderinger } from '~shared/api/revurdering'

import { isSuccess, mapApiResult } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'
import styled from 'styled-components'

type Props = {
  sak: ISak
  behandlingliste: IBehandlingsammendrag[]
}

export default function RelevanteHendelser(props: Props) {
  const { sak, behandlingliste } = props

  const [visOpprettRevurderingsmodal, setVisOpprettRevurderingsmodal] = useState<boolean>(false)
  const [valgtHendelse, setValgtHendelse] = useState<Grunnlagsendringshendelse | undefined>(undefined)
  const startRevurdering = (hendelse: Grunnlagsendringshendelse) => {
    setValgtHendelse(hendelse)
    setVisOpprettRevurderingsmodal(true)
  }

  const [relevanteHendelser, setRelevanteHendelser] = useState<Grunnlagsendringshendelse[]>([])
  const [hendelserStatus, hentHendelser] = useApiCall(hentGrunnlagsendringshendelserForSak)
  const [muligeRevurderingAarsakerStatus, hentMuligeRevurderingeraarsaker] = useApiCall(hentStoettedeRevurderinger)
  const [personerISak, hentPersoner, resetPersoner] = useApiCall(hentPersonerISak)

  useEffect(() => {
    if (!sak.id) return resetPersoner
    void hentPersoner(sak.id)
    return resetPersoner
  }, [sak.id])

  useEffect(() => {
    if (sak.sakType && behandlingliste) hentMuligeRevurderingeraarsaker({ sakType: sak.sakType })
  }, [sak.sakType])

  const navneMap = useMemo(() => {
    return isSuccess(personerISak) ? personerISak.data.personer : {}
  }, [personerISak])

  useEffect(() => {
    hentHendelser(sak.id, (grunnlagsendrlingsListe) => {
      const relevanteHendelser = grunnlagsendrlingsListe.hendelser.filter((h) => h.status !== STATUS_IRRELEVANT)
      setRelevanteHendelser(relevanteHendelser)
    })
  }, [])

  const harAapenRevurdering =
    behandlingliste
      .filter((behandling) => behandling.behandlingType === IBehandlingsType.REVURDERING)
      .filter((behandling) => !erFerdigBehandlet(behandling.status)).length > 0

  const revurderingKanOpprettes =
    behandlingliste.filter((behandling) => behandlingErIverksattEllerSamordnet(behandling.status)).length > 0

  return (
    <>
      <FnrTilNavnMapContext.Provider value={navneMap}>
        <Heading size="medium" spacing>
          Nye hendelser
        </Heading>
        {mapApiResult(
          hendelserStatus,
          <Spinner visible label="Laster hendelser ..." />,
          (apierror) => (
            <ApiErrorAlert>{JSON.stringify(apierror)}</ApiErrorAlert>
          ),
          () =>
            relevanteHendelser.length ? (
              <>
                <Table style={{ marginBottom: '3rem' }}>
                  <Table.Header>
                    <Table.Row>
                      <Table.HeaderCell />
                      <Table.HeaderCell>Hendelse</Table.HeaderCell>
                      <Table.HeaderCell>Dato</Table.HeaderCell>
                      <Table.HeaderCell></Table.HeaderCell>
                    </Table.Row>
                  </Table.Header>
                  <Table.Body>
                    {relevanteHendelser.map((hendelse) => (
                      <UhaandtertHendelse
                        key={hendelse.id}
                        sakType={sak.sakType}
                        hendelse={hendelse}
                        harAapenRevurdering={harAapenRevurdering}
                        startRevurdering={startRevurdering}
                        revurderingKanOpprettes={revurderingKanOpprettes}
                        revurderinger={
                          isSuccess(muligeRevurderingAarsakerStatus) ? muligeRevurderingAarsakerStatus.data : []
                        }
                      />
                    ))}
                  </Table.Body>
                </Table>

                <Alert variant="warning">
                  Ny hendelse som kan kreve revurdering. Vurder om det har konsekvens for ytelsen.
                </Alert>
              </>
            ) : (
              <Alert variant="info" inline>
                Ingen nye hendelser
              </Alert>
            )
        )}
      </FnrTilNavnMapContext.Provider>

      {mapApiResult(
        muligeRevurderingAarsakerStatus,
        <Spinner visible label="Sjekker om det kan opprettes ny behandling ..." />,
        () => (
          <ApiErrorAlert>En feil skjedde under kallet for å hente støttede revurderinger</ApiErrorAlert>
        ),
        (muligeRevurderingAarsakerStatus) =>
          muligeRevurderingAarsakerStatus.length ? (
            <>
              {valgtHendelse && (
                <VurderHendelseModal
                  sakId={sak.id}
                  valgtHendelse={valgtHendelse}
                  open={visOpprettRevurderingsmodal}
                  setOpen={setVisOpprettRevurderingsmodal}
                  revurderinger={muligeRevurderingAarsakerStatus}
                />
              )}
              {revurderingKanOpprettes && (
                <OpprettRevurderingSpacer>
                  <OpprettNyRevurdering revurderinger={muligeRevurderingAarsakerStatus} sakId={sak.id} />
                </OpprettRevurderingSpacer>
              )}
            </>
          ) : (
            <></>
          )
      )}

      {isSuccess(hendelserStatus) && <HistoriskeHendelser hendelser={hendelserStatus.data.hendelser} />}
    </>
  )
}

const OpprettRevurderingSpacer = styled.div`
  margin-top: 3rem;
`
