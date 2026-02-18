import { Alert, BodyShort, Button, HStack, Link, Modal, Table, VStack } from '@navikt/ds-react'
import { IBehandlingsammendrag, SakMedBehandlinger } from '../typer'
import React, { useEffect, useState } from 'react'
import { useApiCall } from '~shared/hooks/useApiCall'
import Spinner from '~shared/Spinner'
import { hentGenerelleBehandlingForSak, opprettGenerellBehandling } from '~shared/api/generellbehandling'
import { Generellbehandling } from '~shared/types/Generellbehandling'
import {
  behandlingStatusTilLesbartnavn,
  genbehandlingTypeTilLesbartNavn,
  generellBehandlingsStatusTilLesbartNavn,
  mapAarsak,
} from '~components/person/sakOgBehandling/behandlingsslistemappere'
import { VedtakKolonner } from '~components/person/VedtakKoloner'

import { isPending, isSuccess, mapSuccess } from '~shared/api/apiUtils'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import { UtlandstilknytningType } from '~shared/types/IDetaljertBehandling'
import { EessiPensjonLenke } from '~components/behandling/soeknadsoversikt/bosattUtland/EessiPensjonLenke'
import { formaterDato } from '~utils/formatering/dato'
import { formaterBehandlingstype } from '~utils/formatering/formatering'
import { EtteroppgjoerOmgjoerRevurderingModal } from '~components/oppgavebenk/oppgaveModal/etteroppgjoer/EtteroppgjoerOmgjoerRevurderingModal'

type alleBehandlingsTyper = IBehandlingsammendrag | Generellbehandling

const kravpakkeKanGjenopprettes = (behandlinger: Generellbehandling[]): boolean => {
  const kravpakkeBehandlinger = behandlinger.filter(
    (behandling) => !isVanligBehandling(behandling) && behandling.type === 'KRAVPAKKE_UTLAND'
  )
  return (
    kravpakkeBehandlinger.length > 0 && kravpakkeBehandlinger.every((behandling) => behandling.status === 'AVBRUTT')
  )
}

function isVanligBehandling(behandling: alleBehandlingsTyper): behandling is IBehandlingsammendrag {
  return (behandling as IBehandlingsammendrag).soeknadMottattDato !== undefined
}

function hentDato(behandling: alleBehandlingsTyper): string {
  if (isVanligBehandling(behandling)) {
    return behandling.behandlingOpprettet
  } else {
    return behandling.opprettet
  }
}

export const Behandlingsliste = ({ sakOgBehandlinger }: { sakOgBehandlinger: SakMedBehandlinger }) => {
  const [generellbehandlingStatus, hentGenerellbehandlinger] = useApiCall(hentGenerelleBehandlingForSak)

  const { sak, behandlinger } = sakOgBehandlinger

  useEffect(() => {
    hentGenerellbehandlinger(sak.id)
  }, [sak.id])

  let allebehandlinger: alleBehandlingsTyper[] = []
  allebehandlinger = allebehandlinger.concat(behandlinger)
  if (isSuccess(generellbehandlingStatus)) {
    allebehandlinger = allebehandlinger.concat(generellbehandlingStatus.data)
  }

  allebehandlinger.sort((a, b) => (new Date(hentDato(a)) < new Date(hentDato(b)) ? 1 : -1))

  return (
    <VStack gap="4">
      {!!allebehandlinger?.length ? (
        <Table zebraStripes>
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell>Reg. dato</Table.HeaderCell>
              <Table.HeaderCell>Behandlingstype</Table.HeaderCell>
              <Table.HeaderCell>Årsak</Table.HeaderCell>
              <Table.HeaderCell>Status</Table.HeaderCell>
              <Table.HeaderCell>Virkningstidspunkt</Table.HeaderCell>
              <Table.HeaderCell>Vedtaksdato</Table.HeaderCell>
              <Table.HeaderCell>Resultat</Table.HeaderCell>
              <Table.HeaderCell></Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {allebehandlinger.map((behandling) => {
              if (isVanligBehandling(behandling)) {
                return (
                  <Table.Row key={behandling.id}>
                    <Table.DataCell>{formaterDato(behandling.behandlingOpprettet)}</Table.DataCell>
                    <Table.DataCell>
                      <HStack gap="2" align="center">
                        {formaterBehandlingstype(behandling.behandlingType)}
                        {(sak.utlandstilknytning?.type === UtlandstilknytningType.UTLANDSTILSNITT ||
                          sak.utlandstilknytning?.type === UtlandstilknytningType.BOSATT_UTLAND) && (
                          <EessiPensjonLenke sakId={sak.id} behandlingId={behandling.id} sakType={sak.sakType} />
                        )}
                      </HStack>
                    </Table.DataCell>
                    <Table.DataCell>{mapAarsak(behandling.aarsak)}</Table.DataCell>
                    <Table.DataCell>{behandlingStatusTilLesbartnavn(behandling.status)}</Table.DataCell>
                    <Table.DataCell>
                      {behandling.virkningstidspunkt ? formaterDato(behandling.virkningstidspunkt!!.dato) : ''}
                    </Table.DataCell>
                    <VedtakKolonner behandlingId={behandling.id} />
                    <Table.DataCell>
                      <HStack gap="4">
                        <Link href={`/behandling/${behandling.id}/`}>Se behandling</Link>
                        {behandling.behandlingType === 'REVURDERING' &&
                          behandling.aarsak === 'ETTEROPPGJOER' &&
                          behandling.status === 'AVBRUTT' && (
                            <EtteroppgjoerOmgjoerRevurderingModal sakId={behandling.sak} behandlingId={behandling.id} />
                          )}
                      </HStack>
                    </Table.DataCell>
                  </Table.Row>
                )
              } else {
                return (
                  <Table.Row key={behandling.id}>
                    <Table.DataCell>{formaterDato(behandling.opprettet)}</Table.DataCell>
                    <Table.DataCell>{genbehandlingTypeTilLesbartNavn(behandling.type)}</Table.DataCell>
                    <Table.DataCell>-</Table.DataCell>
                    <Table.DataCell>{generellBehandlingsStatusTilLesbartNavn(behandling.status)}</Table.DataCell>
                    <Table.DataCell>-</Table.DataCell>
                    <Table.DataCell>-</Table.DataCell>
                    <Table.DataCell>-</Table.DataCell>
                    <Table.DataCell>
                      <Link href={`/generellbehandling/${behandling.id}`}>Se behandling</Link>
                    </Table.DataCell>
                  </Table.Row>
                )
              }
            })}
          </Table.Body>
        </Table>
      ) : (
        <Alert variant="info" inline>
          Ingen behandlinger på sak
        </Alert>
      )}

      <Spinner visible={isPending(generellbehandlingStatus)} label="Henter generelle behandlinger" />
      {mapSuccess(generellbehandlingStatus, (generelleBehandlinger) => {
        return kravpakkeKanGjenopprettes(generelleBehandlinger) && <GjenopprettKravpakkeBehandling sakId={sak.id} />
      })}

      {isFailureHandler({
        apiResult: generellbehandlingStatus,
        errorMessage: 'Vi klarte ikke å hente generelle behandligner',
      })}
    </VStack>
  )
}

function GjenopprettKravpakkeBehandling(props: { sakId: number }) {
  const [modalOpen, setModalOpen] = useState(false)
  const [opprettKravpakkeResult, opprettKravpakkeFetch, resetKravpakke] = useApiCall(opprettGenerellBehandling)

  function avbryt() {
    if (isSuccess(opprettKravpakkeResult)) {
      window.location.reload()
    } else {
      setModalOpen(false)
      resetKravpakke()
    }
  }

  function opprettKravpakkeBehandling() {
    opprettKravpakkeFetch({
      sakId: props.sakId,
      type: 'KRAVPAKKE_UTLAND',
    })
  }

  return (
    <>
      <div>
        <Button variant="secondary" onClick={() => setModalOpen(true)}>
          Gjenopprett kravpakkebehandling
        </Button>
      </div>
      <Modal open={modalOpen} onClose={avbryt} header={{ heading: 'Gjenopprett kravpakke' }}>
        <Modal.Body>
          <VStack gap="4">
            <BodyShort>En avbrutt kravpakkebehandling kan opprettes på nytt.</BodyShort>
            {isFailureHandler({
              apiResult: opprettKravpakkeResult,
              errorMessage: 'Kunne ikke opprette kravpakkebehandling',
            })}
            {mapSuccess(opprettKravpakkeResult, () => (
              <Alert variant="success">Kravpakke opprettet!</Alert>
            ))}
          </VStack>
        </Modal.Body>
        <Modal.Footer>
          {isSuccess(opprettKravpakkeResult) ? (
            <>
              <Button as="a" href={`/generellbehandling/${opprettKravpakkeResult.data.id}`}>
                Gå til behandling
              </Button>
              <Button variant="secondary" onClick={avbryt}>
                Lukk
              </Button>
            </>
          ) : (
            <>
              <Button
                variant="primary"
                onClick={opprettKravpakkeBehandling}
                loading={isPending(opprettKravpakkeResult)}
              >
                Opprett kravpakkebehandling
              </Button>
              <Button variant="secondary" onClick={avbryt} disabled={isPending(opprettKravpakkeResult)}>
                Avbryt
              </Button>
            </>
          )}
        </Modal.Footer>
      </Modal>
    </>
  )
}
