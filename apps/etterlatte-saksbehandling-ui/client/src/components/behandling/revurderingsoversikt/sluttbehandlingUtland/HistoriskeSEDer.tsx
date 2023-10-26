import {
  RevurderingInfo,
  RevurderinginfoMedIdOgOpprettet,
  SluttbehandlingUtlandInfo,
} from '~shared/types/RevurderingInfo'
import React, { useState } from 'react'
import { Revurderingaarsak } from '~shared/types/Revurderingaarsak'
import { Table } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { List } from '@navikt/ds-react'
import { ILand } from '~shared/api/trygdetid'
import { oversettIsokodeTilLandnavn } from '~components/behandling/revurderingsoversikt/sluttbehandlingUtland/SluttbehandlingUtland'

function erSluttbehandling(revurderingsinfo: RevurderingInfo): revurderingsinfo is SluttbehandlingUtlandInfo {
  return revurderingsinfo.type === Revurderingaarsak.SLUTTBEHANDLING_UTLAND
}

export default function HistoriskeSEDer({
  revurderingsinfoliste,
  landListe,
}: {
  revurderingsinfoliste: RevurderinginfoMedIdOgOpprettet[]
  landListe: ILand[]
}) {
  return (
    <List>
      {revurderingsinfoliste.map((revinfo) => {
        if (erSluttbehandling(revinfo.revurderingsinfo)) {
          return (
            <Sluttbehandling
              key={revinfo.id}
              id={revinfo.id}
              opprettetDato={revinfo.opprettetDato}
              sluttbehandlingInfo={revinfo.revurderingsinfo}
              landListe={landListe}
            />
          )
        }
      })}
    </List>
  )
}

function Sluttbehandling({
  sluttbehandlingInfo,
  id,
  opprettetDato,
  landListe,
}: {
  sluttbehandlingInfo: SluttbehandlingUtlandInfo
  id: string
  opprettetDato: string
  landListe: ILand[]
}) {
  const [open, setOpen] = useState<boolean>(false)
  return (
    <List.Item>
      <button onClick={() => setOpen((open) => !open)}>
        Toggle Revurdering. Opprettet {formaterStringDato(opprettetDato)} {id}
      </button>
      {open ? (
        <Table>
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell />
              <Table.HeaderCell scope="col">Land</Table.HeaderCell>
              <Table.HeaderCell scope="col">Dokumenter</Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {sluttbehandlingInfo.landMedDokumenter.map((landMedDokument, i) => {
              return (
                <Table.ExpandableRow key={i} content={null} defaultOpen={true}>
                  <Table.DataCell scope="row">
                    {oversettIsokodeTilLandnavn(landListe, landMedDokument.landIsoKode)}
                  </Table.DataCell>
                  <Table.DataCell>
                    {landMedDokument.dokumenter
                      .filter((e) => !!e.dokumenttype)
                      .map((e) => e.dokumenttype)
                      .join(', ')}
                  </Table.DataCell>
                </Table.ExpandableRow>
              )
            })}
          </Table.Body>
        </Table>
      ) : null}
    </List.Item>
  )
}
