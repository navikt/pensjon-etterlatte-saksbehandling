import { Button } from '@navikt/ds-react'
import React from 'react'
import { ILand } from '~shared/api/trygdetid'
import { Table } from '@navikt/ds-react'
import LandRad from '~components/behandling/revurderingsoversikt/sluttbehandlingUtland/LandRad'
import { TrashIcon } from '@navikt/aksel-icons'

export interface MottattDokument {
  dokumenttype?: string
  dato?: string
  kommentar?: string
}

export interface LandMedDokumenter {
  landIsoKode?: string
  dokumenter: MottattDokument[]
}

export default function SEDLand({
  landListe,
  landMedDokumenter,
  setLandMedDokumenter,
}: {
  landListe: ILand[]
  landMedDokumenter: LandMedDokumenter[]
  setLandMedDokumenter: React.Dispatch<React.SetStateAction<LandMedDokumenter[]>>
}) {
  return (
    <>
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell />
            <Table.HeaderCell scope="col">Land</Table.HeaderCell>
            <Table.HeaderCell scope="col">Dokumenter</Table.HeaderCell>
            <Table.HeaderCell scope="col"></Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {landMedDokumenter.map((landogdoc, i) => {
            const oppdaterLandMedDokumenter = (oppdatertLandMedDokumenter: LandMedDokumenter) => {
              setLandMedDokumenter((landMedDokumenter) => {
                const oppdaterteLandMedDokumenter = landMedDokumenter.map((landMedDokumenter, idx) =>
                  idx === i ? oppdatertLandMedDokumenter : landMedDokumenter
                )
                return oppdaterteLandMedDokumenter
              })
            }
            const fjernLand = () => {
              setLandMedDokumenter((landMedDokumenter) => {
                const nyLandlisteMedDokumenter = landMedDokumenter.filter((_, idx) => idx !== i)
                return nyLandlisteMedDokumenter
              })
            }
            const landrad = (
              <LandRad
                landMedDokumenter={landogdoc}
                oppdaterLandMedDokumenter={oppdaterLandMedDokumenter}
                landListe={landListe}
              />
            )
            return (
              <Table.ExpandableRow key={i} content={landrad} defaultOpen={true}>
                <Table.DataCell scope="row">{landogdoc.landIsoKode}</Table.DataCell>
                <Table.DataCell>
                  {landogdoc.dokumenter
                    .filter((e) => !!e.dokumenttype)
                    .map((e) => e.dokumenttype)
                    .join(', ')}
                </Table.DataCell>
                <Table.DataCell scope="row">
                  <Button
                    variant="tertiary"
                    icon={<TrashIcon />}
                    onClick={() => fjernLand()}
                    style={{ marginLeft: '20rem' }}
                  >
                    Slett SED
                  </Button>
                </Table.DataCell>
              </Table.ExpandableRow>
            )
          })}
        </Table.Body>
      </Table>
      <Button
        onClick={() => {
          setLandMedDokumenter((landMedDokumenter) => {
            return landMedDokumenter.concat({ dokumenter: [] })
          })
        }}
        variant="tertiary"
      >
        Legg til land
      </Button>
    </>
  )
}
