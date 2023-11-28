import { isPending, isSuccess, mapApiResult, Result, useApiCall } from '~shared/hooks/useApiCall'
import { hentBrevForSak, opprettBrevForSak } from '~shared/api/brev'
import { useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Table, Tag } from '@navikt/ds-react'
import { BrevStatus, IBrev, Mottaker } from '~shared/types/Brev'
import { DocPencilIcon, ExternalLinkIcon } from '@navikt/aksel-icons'
import Spinner from '~shared/Spinner'
import { Container, FlexRow } from '~shared/styled'
import BrevModal from '~components/person/brev/BrevModal'
import { ApiErrorAlert } from '~ErrorBoundary'
import { SakMedBehandlinger } from '~components/person/typer'

const mapAdresse = (mottaker: Mottaker) => {
  const adr = mottaker.adresse
  const adresselinje = [adr?.adresselinje1, adr?.adresselinje2, adr?.adresselinje3].join(' ')
  const postlinje = [adr?.postnummer, adr?.poststed, adr?.land].join(' ')

  return `${adresselinje}, ${postlinje}`
}

const mapStatus = (status: BrevStatus) => status.charAt(0) + status.slice(1).toLowerCase()

const tagColors = (status: BrevStatus) => {
  switch (status) {
    case BrevStatus.OPPRETTET:
    case BrevStatus.OPPDATERT:
      return 'neutral'
    case BrevStatus.FERDIGSTILT:
    case BrevStatus.JOURNALFOERT:
      return 'info'
    case BrevStatus.DISTRIBUERT:
      return 'success'
    case BrevStatus.SLETTET:
      return 'neutral'
  }
}

const brevKanEndres = (status: BrevStatus) => [BrevStatus.OPPRETTET, BrevStatus.OPPDATERT].includes(status)

const handlingKnapp = (brev: IBrev) => {
  if (brev.behandlingId && brevKanEndres(brev.status)) {
    return (
      <Button
        as="a"
        href={`/behandling/${brev.behandlingId}/brev`}
        variant="secondary"
        title="Gå til behandling"
        icon={<ExternalLinkIcon />}
      />
    )
  } else if (!brev.behandlingId && brev.status != BrevStatus.DISTRIBUERT) {
    return (
      <Button
        as="a"
        href={`/person/${brev.soekerFnr}/sak/${brev.sakId}/brev/${brev.id}`}
        variant="secondary"
        title="Rediger"
        icon={<DocPencilIcon />}
      />
    )
  }
  return <BrevModal brev={brev} />
}

export default function BrevOversikt({ sakStatus }: { sakStatus: Result<SakMedBehandlinger> }) {
  const navigate = useNavigate()

  const [brevListe, hentBrev] = useApiCall(hentBrevForSak)
  const [nyttBrevStatus, opprettBrev] = useApiCall(opprettBrevForSak)

  useEffect(() => {
    if (isSuccess(sakStatus)) {
      hentBrev(Number(sakStatus.data.sak.id))
    }
  }, [sakStatus])

  const opprettNyttBrevOgRedirect = () => {
    if (isSuccess(sakStatus)) {
      opprettBrev({ sakId: Number(sakStatus.data.sak.id), tittel: 'en liten tittel' }, (brev) => {
        navigate(`/person/${brev.soekerFnr}/sak/${brev.sakId}/brev/${brev.id}`)
      })
    } else {
      throw Error('SakID mangler!')
    }
  }

  return (
    <Container>
      {mapApiResult(
        brevListe,
        <Spinner visible label="Henter brev for sak ..." />,
        () => (
          <ApiErrorAlert>Feil ved henting av brev...</ApiErrorAlert>
        ),
        (brevListe) => (
          <Table>
            <Table.Header>
              <Table.Row>
                <Table.HeaderCell>ID</Table.HeaderCell>
                <Table.HeaderCell>Status</Table.HeaderCell>
                <Table.HeaderCell>Type</Table.HeaderCell>
                <Table.HeaderCell>Navn</Table.HeaderCell>
                <Table.HeaderCell>Adresse</Table.HeaderCell>
                <Table.HeaderCell></Table.HeaderCell>
              </Table.Row>
            </Table.Header>
            <Table.Body>
              {!brevListe.length && (
                <Table.Row>
                  <Table.DataCell colSpan={6}>Ingen brev funnet</Table.DataCell>
                </Table.Row>
              )}
              {brevListe.map((b) => (
                <Table.Row key={b.id}>
                  <Table.DataCell>{b.id}</Table.DataCell>
                  <Table.DataCell>
                    <Tag variant={tagColors(b.status)} size="medium">
                      {mapStatus(b.status)}
                    </Tag>
                  </Table.DataCell>
                  <Table.DataCell>{b.behandlingId ? 'Vedtaksbrev' : 'Annet'}</Table.DataCell>
                  <Table.DataCell>{b.mottaker.navn}</Table.DataCell>
                  <Table.DataCell>{mapAdresse(b.mottaker)}</Table.DataCell>
                  <Table.DataCell>{handlingKnapp(b)}</Table.DataCell>
                </Table.Row>
              ))}
            </Table.Body>
          </Table>
        )
      )}

      <br />

      <FlexRow>
        <Button
          variant="primary"
          icon={<DocPencilIcon />}
          iconPosition="right"
          onClick={opprettNyttBrevOgRedirect}
          loading={isPending(nyttBrevStatus)}
        >
          Nytt brev
        </Button>
      </FlexRow>
    </Container>
  )
}
