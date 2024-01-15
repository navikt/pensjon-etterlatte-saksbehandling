import React, { useState } from 'react'
import { BodyShort, Heading, Label, Link, Modal, Table } from '@navikt/ds-react'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentSamordningsdata } from '~shared/api/vedtaksvurdering'
import { Samordningsvedtak } from '~components/vedtak/typer'
import { isPending } from '~shared/api/apiUtils'
import Spinner from '~shared/Spinner'
import { isFailureHandler } from '~shared/api/IsFailureHandler'
import styled from 'styled-components'
import { formaterStringDato } from '~utils/formattering'

const InfoGrid = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr 1fr 1fr 1fr;
  gap: 1rem;
  margin-bottom: 2rem;
`

export const SamordningModal = ({ behandlingId }: { behandlingId: string }) => {
  const [open, setOpen] = useState(false)
  const [samordningsdata, setSamordningsdata] = useState<Samordningsvedtak[]>()
  const [hentet, hent] = useApiCall(hentSamordningsdata)

  const aapneModal = () => {
    setOpen(true)
    hent(behandlingId, (result) => setSamordningsdata(result))
  }

  return (
    <>
      <Link onClick={() => aapneModal()}>Til samordning</Link>
      <Modal
        open={open}
        aria-labelledby="modal-heading"
        width="500"
        closeOnBackdropClick={true}
        onClose={() => setOpen(false)}
      >
        <Modal.Header>
          <Heading size="medium">Samordning for behandling {behandlingId}</Heading>
        </Modal.Header>
        <Modal.Body>
          <Spinner visible={isPending(hentet)} label="Henter data" />

          {isFailureHandler({
            apiResult: hentet,
            errorMessage: 'Kunne ikke hente data',
          })}

          {!isPending(hentet) &&
            samordningsdata &&
            samordningsdata.map((vedtak) => (
              <div key={vedtak.samordningVedtakId}>
                <InfoGrid>
                  <div>
                    <Label>VedtakID</Label>
                    <BodyShort>{vedtak.vedtakId}</BodyShort>
                  </div>
                  <div>
                    <Label>SamordningID</Label>
                    <BodyShort>{vedtak.samordningVedtakId}</BodyShort>
                  </div>
                  <div>
                    <Label>Status</Label>
                    <BodyShort>{vedtak.vedtakstatusKode}</BodyShort>
                  </div>
                  <div>
                    <Label>Etterbetaling</Label>
                    <BodyShort>{vedtak.etterbetaling ? 'Ja' : 'Nei'}</BodyShort>
                  </div>
                  <div>
                    <Label>Utvidet samordningsfrist</Label>
                    <BodyShort>{vedtak.utvidetSamordningsfrist ? 'Ja' : 'Nei'}</BodyShort>
                  </div>
                </InfoGrid>

                <Heading size="small" id="modal-heading">
                  Samordningsmeldinger
                </Heading>

                <Table>
                  <Table.Header>
                    <Table.Row>
                      <Table.HeaderCell>ID</Table.HeaderCell>
                      <Table.HeaderCell>Status</Table.HeaderCell>
                      <Table.HeaderCell>Sendt dato</Table.HeaderCell>
                      <Table.HeaderCell>Mottatt dato</Table.HeaderCell>
                      <Table.HeaderCell>Purret dato</Table.HeaderCell>
                      <Table.HeaderCell>Refusjonskrav</Table.HeaderCell>
                      <Table.HeaderCell>TSS</Table.HeaderCell>
                    </Table.Row>
                  </Table.Header>
                  <Table.Body>
                    {!vedtak.samordningsmeldinger.length && (
                      <Table.Row>
                        <Table.DataCell colSpan={7}>Ingen samordningsmeldinger funnet</Table.DataCell>
                      </Table.Row>
                    )}
                    {vedtak.samordningsmeldinger.map((mld) => (
                      <Table.Row key={mld.samId}>
                        <Table.DataCell>{mld.samId}</Table.DataCell>
                        <Table.DataCell>{mld.meldingstatusKode}</Table.DataCell>
                        <Table.DataCell>{mld.sendtDato}</Table.DataCell>
                        <Table.DataCell>{mld.svartDato && formaterStringDato(mld.svartDato)}</Table.DataCell>
                        <Table.DataCell>{mld.purretDato && formaterStringDato(mld.purretDato)}</Table.DataCell>
                        <Table.DataCell>{mld.refusjonskrav}</Table.DataCell>
                        <Table.DataCell>{mld.tssEksternId}</Table.DataCell>
                      </Table.Row>
                    ))}
                  </Table.Body>
                </Table>
              </div>
            ))}
        </Modal.Body>
      </Modal>
    </>
  )
}
