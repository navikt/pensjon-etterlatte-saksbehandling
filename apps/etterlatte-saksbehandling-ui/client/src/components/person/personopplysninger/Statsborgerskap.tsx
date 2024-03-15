import React, { ReactNode, useEffect, useState } from 'react'
import { Personopplysning } from '~components/person/personopplysninger/Personopplysning'
import { PassportIcon } from '@navikt/aksel-icons'
import { Statsborgerskap as PdlStatsborgerskap } from '~shared/types/Person'
import { Table } from '@navikt/ds-react'
import { formaterStringDato } from '~utils/formattering'
import { hentAlleLand, ILand, sorterLand } from '~shared/api/trygdetid'
import { useApiCall } from '~shared/hooks/useApiCall'

export const Statsborgerskap = ({
  statsborgerskap,
  pdlStatsborgerskap,
  bosattLand,
}: {
  statsborgerskap?: string
  pdlStatsborgerskap?: PdlStatsborgerskap[]
  bosattLand?: string
}): ReactNode => {
  const [landListe, setLandListe] = useState<ILand[]>([])

  const [, fetchAlleLand] = useApiCall(hentAlleLand)

  const finnLandSomTekst = (isoLandKode: string): string | undefined => {
    return landListe.find((val) => val.isoLandkode === isoLandKode)?.beskrivelse.tekst
  }

  useEffect(() => {
    fetchAlleLand(null, (landListe: ILand[]) => {
      setLandListe(sorterLand(landListe))
    })
  }, [])

  return (
    <Personopplysning heading="Statsborgerskap" icon={<PassportIcon height="2rem" width="2rem" />}>
      <Table>
        <Table.Header>
          <Table.Row>
            <Table.ColumnHeader scope="col">Land</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Fra</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Til</Table.ColumnHeader>
            <Table.ColumnHeader scope="col">Personstatus</Table.ColumnHeader>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {pdlStatsborgerskap && pdlStatsborgerskap.length >= 0 ? (
            pdlStatsborgerskap.map((borgerskap: PdlStatsborgerskap, index: number) => (
              <Table.Row key={index}>
                <Table.DataCell>{finnLandSomTekst(borgerskap.land)}</Table.DataCell>
                <Table.DataCell>
                  {!!borgerskap.gyldigFraOgMed ? formaterStringDato(borgerskap.gyldigFraOgMed) : ''}
                </Table.DataCell>
                <Table.DataCell>
                  {!!borgerskap.gyldigTilOgMed ? formaterStringDato(borgerskap.gyldigTilOgMed) : ''}
                </Table.DataCell>
                <Table.DataCell>{finnLandSomTekst(borgerskap.land) === bosattLand && 'Bosatt'}</Table.DataCell>
              </Table.Row>
            ))
          ) : (
            <Table.Row>
              <Table.DataCell>{!!statsborgerskap && finnLandSomTekst(statsborgerskap)}</Table.DataCell>
              <Table.DataCell>-</Table.DataCell>
              <Table.DataCell>-</Table.DataCell>
              <Table.DataCell>-</Table.DataCell>
            </Table.Row>
          )}
        </Table.Body>
      </Table>
    </Personopplysning>
  )
}
