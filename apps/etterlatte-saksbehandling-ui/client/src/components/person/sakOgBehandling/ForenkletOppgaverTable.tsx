import React, { ReactNode, useEffect, useState } from 'react'
import {
  erOppgaveRedigerbar,
  OppgaveDTO,
  OppgaveSaksbehandler,
  Oppgavestatus,
  Oppgavetype,
} from '~shared/types/oppgave'
import { Alert, Box, HStack, Table } from '@navikt/ds-react'
import { OppgavetypeTag } from '~shared/tags/OppgavetypeTag'
import { Saksbehandler } from '~shared/types/saksbehandler'
import { VelgSaksbehandler } from '~components/oppgavebenk/tildeling/VelgSaksbehandler'
import { HandlingerForOppgave } from '~components/oppgavebenk/components/HandlingerForOppgave'
import { useApiCall } from '~shared/hooks/useApiCall'
import { saksbehandlereIEnhetApi } from '~shared/api/oppgaver'
import { OppgaveValg } from '~components/person/sakOgBehandling/SakOversikt'
import { useInnloggetSaksbehandler } from '~components/behandling/useInnloggetSaksbehandler'
import { finnOgOppdaterOppgave, sorterOppgaverEtterOpprettet } from '~components/oppgavebenk/utils/oppgaveHandlinger'
import { SakTypeTag } from '~shared/tags/SakTypeTag'
import { OppgavestatusTag } from '~shared/tags/OppgavestatusTag'
import { formaterDato } from '~utils/formatering/dato'
import { formaterEnumTilLesbarString } from '~utils/formatering/formatering'
import { OppgaveFrist } from '~components/oppgavebenk/frist/OppgaveFrist'

export const ForenkletOppgaverTable = ({
  oppgaver,
  oppgaveValg,
  refreshOppgaver,
}: {
  oppgaver: OppgaveDTO[]
  oppgaveValg: OppgaveValg
  refreshOppgaver: () => void
}): ReactNode => {
  const innloggetSaksbehandler = useInnloggetSaksbehandler()

  const filtrerOppgaverPaaOppgaveValg = (oppgaver: OppgaveDTO[]): OppgaveDTO[] => {
    switch (oppgaveValg) {
      case OppgaveValg.AKTIVE:
        return sorterOppgaverEtterOpprettet([...oppgaver].filter((oppgave) => erOppgaveRedigerbar(oppgave.status)))
      case OppgaveValg.FERDIGSTILTE:
        return sorterOppgaverEtterOpprettet([...oppgaver].filter((oppgave) => !erOppgaveRedigerbar(oppgave.status)))
      default:
        return oppgaver
    }
  }

  const [filtrerteOppgaver, setFiltrerteOppgaver] = useState<OppgaveDTO[]>(filtrerOppgaverPaaOppgaveValg(oppgaver))
  const [saksbehandlereIEnheter, setSaksbehandlereIEnheter] = useState<Array<Saksbehandler>>([])
  const [, saksbehandlereIEnheterFetch] = useApiCall(saksbehandlereIEnhetApi)

  const oppdaterSaksbehandlerTildeling = (oppgave: OppgaveDTO, saksbehandler: OppgaveSaksbehandler | null) =>
    setFiltrerteOppgaver(
      finnOgOppdaterOppgave(filtrerteOppgaver, oppgave.id, { status: Oppgavestatus.UNDER_BEHANDLING, saksbehandler })
    )

  const oppdaterStatus = (oppgaveId: string, status: Oppgavestatus) => {
    const oppdaterteOppgaver = finnOgOppdaterOppgave(filtrerteOppgaver, oppgaveId, { status })
    setFiltrerteOppgaver(filtrerOppgaverPaaOppgaveValg(oppdaterteOppgaver))
  }

  const oppdaterMerknad = (oppgaveId: string, merknad: string) => {
    const oppdaterteOppgaver = finnOgOppdaterOppgave(filtrerteOppgaver, oppgaveId, { merknad })
    setFiltrerteOppgaver(filtrerOppgaverPaaOppgaveValg(oppdaterteOppgaver))
  }

  useEffect(() => {
    setFiltrerteOppgaver(filtrerOppgaverPaaOppgaveValg(oppgaver))
  }, [oppgaveValg, oppgaver])

  useEffect(() => {
    if (!!innloggetSaksbehandler.enheter.length) {
      saksbehandlereIEnheterFetch({ enheter: innloggetSaksbehandler.enheter }, setSaksbehandlereIEnheter)
    }
  }, [])

  return !!filtrerteOppgaver?.length ? (
    <Table zebraStripes size="small">
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell scope="col">Reg.dato</Table.HeaderCell>
          <Table.HeaderCell scope="col">Frist</Table.HeaderCell>
          <Table.HeaderCell scope="col">Ytelse</Table.HeaderCell>
          <Table.HeaderCell scope="col">Oppgavetype</Table.HeaderCell>
          <Table.HeaderCell scope="col">Merknad</Table.HeaderCell>
          <Table.HeaderCell scope="col">Status</Table.HeaderCell>
          <Table.HeaderCell scope="col">Saksbehandler</Table.HeaderCell>
          <Table.HeaderCell scope="col">Handlinger</Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        {filtrerteOppgaver.map((oppgave: OppgaveDTO) => (
          <Table.Row key={oppgave.id}>
            <Table.DataCell>{formaterDato(oppgave.opprettet)}</Table.DataCell>
            <Table.DataCell>
              <OppgaveFrist oppgave={oppgave} oppdaterFrist={refreshOppgaver} />
            </Table.DataCell>
            <Table.DataCell>
              <HStack align="center">
                <SakTypeTag sakType={oppgave.sakType} kort />
              </HStack>
            </Table.DataCell>
            <Table.DataCell>
              <OppgavetypeTag oppgavetype={oppgave.type} />
            </Table.DataCell>
            <Table.DataCell>{oppgave.merknad}</Table.DataCell>
            <Table.DataCell>
              <OppgavestatusTag oppgavestatus={oppgave.status} />
            </Table.DataCell>
            <Table.DataCell>
              <VelgSaksbehandler
                saksbehandlereIEnhet={saksbehandlereIEnheter}
                oppgave={oppgave}
                oppdaterTildeling={oppdaterSaksbehandlerTildeling}
              />
            </Table.DataCell>
            <Table.DataCell>
              <Box minWidth="13rem">
                {oppgave.type !== Oppgavetype.VURDER_KONSEKVENS && (
                  <HandlingerForOppgave
                    oppgave={oppgave}
                    oppdaterStatus={oppdaterStatus}
                    oppdaterMerknad={oppdaterMerknad}
                  />
                )}
              </Box>
            </Table.DataCell>
          </Table.Row>
        ))}
      </Table.Body>
    </Table>
  ) : (
    <Alert variant="info" inline>
      Ingen {formaterEnumTilLesbarString(oppgaveValg).toLowerCase()} oppgaver på sak
    </Alert>
  )
}
