import { Alert, Heading, Table } from '@navikt/ds-react'
import { Grunnlagsendringshendelse, IBehandlingsammendrag, STATUS_IRRELEVANT } from '~components/person/typer'
import { FnrTilNavnMapContext } from '~components/person/uhaandtereHendelser/utils'
import { isFailure, isPending, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import React, { useEffect, useMemo, useState } from 'react'
import { hentPersonerISak } from '~shared/api/grunnlag'
import HistoriskeHendelser from '~components/person/uhaandtereHendelser/HistoriskeHendelser'
import { OpprettNyRevurdering } from '~components/person/OpprettNyRevurdering'
import VurderHendelseModal from '~components/person/VurderHendelseModal'
import UhaandtertHendelse from '~components/person/uhaandtereHendelser/UhaandtertHendelse'
import { IBehandlingsType } from '~shared/types/IDetaljertBehandling'
import { erFerdigBehandlet } from '~components/behandling/felles/utils'
import { hentGrunnlagsendringshendelserForPerson, hentStoettedeRevurderinger } from '~shared/api/behandling'
import Spinner from '~shared/Spinner'
import { ISak } from '~shared/types/sak'

type Props = {
  sak: ISak
  fnr: string
  behandlingliste: IBehandlingsammendrag[]
}

export default function RelevanteHendelser(props: Props) {
  const { sak, fnr, behandlingliste } = props

  const [visOpprettRevurderingsmodal, setVisOpprettRevurderingsmodal] = useState<boolean>(false)
  const [valgtHendelse, setValgtHendelse] = useState<Grunnlagsendringshendelse | undefined>(undefined)
  const startRevurdering = (hendelse: Grunnlagsendringshendelse) => {
    setValgtHendelse(hendelse)
    setVisOpprettRevurderingsmodal(true)
  }

  const [relevanteHendelser, setRelevanteHendelser] = useState<Grunnlagsendringshendelse[]>([])
  const [hendelserStatus, hentHendelser] = useApiCall(hentGrunnlagsendringshendelserForPerson)
  const [revurderingerStatus, hentRevurderinger] = useApiCall(hentStoettedeRevurderinger)
  const [personerISak, hentPersoner, resetPersoner] = useApiCall(hentPersonerISak)

  useEffect(() => {
    if (!sak.id) return resetPersoner
    void hentPersoner(sak.id)
    return resetPersoner
  }, [sak.id])

  useEffect(() => {
    if (sak.sakType && behandlingliste) hentRevurderinger({ sakType: sak.sakType })
  }, [sak.sakType])

  const navneMap = useMemo(() => {
    return isSuccess(personerISak) ? personerISak.data.personer : {}
  }, [personerISak])

  useEffect(() => {
    hentHendelser(fnr, (hendelser) => {
      const relevanteHendelser = hendelser[0].hendelser.filter((h) => h.status !== STATUS_IRRELEVANT)
      setRelevanteHendelser(relevanteHendelser)
    })
  }, [])

  const harAapenRevurdering =
    behandlingliste
      .filter((behandling) => behandling.behandlingType === IBehandlingsType.REVURDERING)
      .filter((behandling) => !erFerdigBehandlet(behandling.status)).length > 0

  return (
    <>
      <FnrTilNavnMapContext.Provider value={navneMap}>
        <Heading size="medium" spacing>
          Nye hendelser
        </Heading>

        {isFailure(hendelserStatus) && <Alert variant="error">{JSON.stringify(hendelserStatus.error)}</Alert>}
        {isPending(hendelserStatus) && <Spinner visible label="Laster hendelser ..." />}
        {isSuccess(hendelserStatus) &&
          (relevanteHendelser.length ? (
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
                      hendelse={hendelse}
                      harAapenRevurdering={harAapenRevurdering}
                      startRevurdering={startRevurdering}
                      revurderinger={isSuccess(revurderingerStatus) ? revurderingerStatus.data : []}
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
          ))}
      </FnrTilNavnMapContext.Provider>

      {isFailure(revurderingerStatus) && (
        <Alert variant="error">En feil skjedde under kallet for å hente støttede revurderinger</Alert>
      )}
      {isPending(revurderingerStatus) && <Spinner visible label="Sjekker om det kan opprettes ny behandling ..." />}
      {isSuccess(revurderingerStatus) && revurderingerStatus.data.length && (
        <>
          {valgtHendelse && (
            <VurderHendelseModal
              sakId={sak.id}
              valgtHendelse={valgtHendelse}
              open={visOpprettRevurderingsmodal}
              setOpen={setVisOpprettRevurderingsmodal}
              revurderinger={revurderingerStatus.data}
            />
          )}
          <OpprettNyRevurdering revurderinger={revurderingerStatus.data} sakId={sak.id} />
        </>
      )}

      {isSuccess(hendelserStatus) && <HistoriskeHendelser hendelser={hendelserStatus.data[0].hendelser} />}
    </>
  )
}
