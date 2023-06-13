import { isPendingOrInitial, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { hentBrevForSak } from '~shared/api/brev'
import { useEffect } from 'react'
import { useParams } from 'react-router-dom'
import { Button, Table } from '@navikt/ds-react'
import { BrevStatus, Mottaker } from '~shared/types/Brev'
import { FilePdfIcon } from '@navikt/aksel-icons'

const mapAdresse = (mottaker: Mottaker) => {
  const adr = mottaker.adresse
  const adresselinje = [adr?.adresselinje1, adr?.adresselinje2, adr?.adresselinje3].join(' ')
  const postlinje = [adr?.postnummer, adr?.poststed, adr?.land].join(' ')

  return `${adresselinje}, ${postlinje}`
}

const mapStatus = (status: BrevStatus) => status.charAt(0) + status.slice(1).toLowerCase()

export default function SendNyttBrev() {
  const { sakId } = useParams()

  const [brevListe, hentBrev] = useApiCall(hentBrevForSak)

  useEffect(() => {
    hentBrev(Number(sakId))
  }, [])

  return (
    <>
      {!isPendingOrInitial(brevListe) && (
        <Table>
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell>ID</Table.HeaderCell>
              <Table.HeaderCell>Status</Table.HeaderCell>
              <Table.HeaderCell>Navn</Table.HeaderCell>
              <Table.HeaderCell>Adresse</Table.HeaderCell>
              <Table.HeaderCell></Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {isSuccess(brevListe) &&
              brevListe.data.map((b) => (
                <Table.Row key={b.id}>
                  <Table.DataCell>{b.id}</Table.DataCell>
                  <Table.DataCell>{mapStatus(b.status)}</Table.DataCell>
                  <Table.DataCell>{b.mottaker.navn}</Table.DataCell>
                  <Table.DataCell>{mapAdresse(b.mottaker)}</Table.DataCell>
                  <Table.DataCell>
                    <Button variant={'secondary'} title={'Vis PDF'} icon={<FilePdfIcon />} />
                    <Button variant={'secondary'} title={'Vis PDF'} icon={<FilePdfIcon />} />
                  </Table.DataCell>
                </Table.Row>
              ))}
          </Table.Body>
        </Table>
      )}
    </>
  )
}
