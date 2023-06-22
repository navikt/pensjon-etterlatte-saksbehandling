import { isFailure, isPending, isPendingOrInitial, isSuccess, useApiCall } from '~shared/hooks/useApiCall'
import { hentBrevForSak, opprettBrevForSak } from '~shared/api/brev'
import { useEffect } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Alert, Button, Table, Tag } from '@navikt/ds-react'
import { BrevStatus, IBrev, Mottaker } from '~shared/types/Brev'
import { DocPencilIcon, ExternalLinkIcon } from '@navikt/aksel-icons'
import Spinner from '~shared/Spinner'
import { StatusBar, StatusBarTheme } from '~shared/statusbar/Statusbar'
import { getPerson } from '~shared/api/grunnlag'
import { Container } from '~shared/styled'
import BrevModal from '~components/person/brev/BrevModal'

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
        as={'a'}
        href={`/behandling/${brev.behandlingId}/brev`}
        variant={'secondary'}
        title={'Gå til behandling'}
        icon={<ExternalLinkIcon />}
      />
    )
  } else if (!brev.behandlingId && brev.status != BrevStatus.DISTRIBUERT) {
    return (
      <Button
        as={'a'}
        href={`/person/${brev.soekerFnr}/sak/${brev.sakId}/brev/${brev.id}`}
        variant={'secondary'}
        title={'Rediger'}
        icon={<DocPencilIcon />}
      />
    )
  }
  return <BrevModal brev={brev} />
}

export default function BrevOversikt() {
  const navigate = useNavigate()
  const { fnr, sakId } = useParams()
  const [personStatus, hentPerson] = useApiCall(getPerson)

  const [brevListe, hentBrev] = useApiCall(hentBrevForSak)
  const [nyttBrevStatus, opprettBrev] = useApiCall(opprettBrevForSak)

  useEffect(() => {
    hentBrev(Number(sakId))
    hentPerson(fnr!!)
  }, [])

  const opprettNyttBrevOgRedirect = () => {
    opprettBrev(Number(sakId), (brev) => {
      navigate(`/person/${brev.soekerFnr}/sak/${brev.sakId}/brev/${brev.id}`)
    })
  }

  return (
    <>
      {isSuccess(personStatus) && <StatusBar theme={StatusBarTheme.gray} personInfo={personStatus.data} />}

      <Container>
        {isPendingOrInitial(brevListe) ? (
          <Spinner visible label={'Henter brev for sak ...'} />
        ) : (
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
              {isFailure(brevListe) && <Alert variant={'error'}>Feil ved henting av brev...</Alert>}
              {isSuccess(brevListe) &&
                brevListe.data.map((b) => (
                  <Table.Row key={b.id}>
                    <Table.DataCell>{b.id}</Table.DataCell>
                    <Table.DataCell>
                      <Tag variant={tagColors(b.status)} size={'medium'}>
                        {mapStatus(b.status)}
                      </Tag>
                    </Table.DataCell>
                    <Table.DataCell>
                      {b.behandlingId ? (
                        <>
                          Vedtaksbrev{' '}
                          <Button
                            variant={'tertiary'}
                            as={'a'}
                            href={`/behandling/${b.behandlingId}/brev`}
                            icon={<ExternalLinkIcon />}
                            size={'small'}
                            title={'Gå til behandling'}
                          />
                        </>
                      ) : (
                        'Annet'
                      )}
                    </Table.DataCell>
                    <Table.DataCell>{b.mottaker.navn}</Table.DataCell>
                    <Table.DataCell>{mapAdresse(b.mottaker)}</Table.DataCell>
                    <Table.DataCell>{handlingKnapp(b)}</Table.DataCell>
                  </Table.Row>
                ))}
            </Table.Body>
          </Table>
        )}
      </Container>

      <Container>
        <Button
          variant={'primary'}
          icon={<DocPencilIcon />}
          iconPosition={'right'}
          onClick={opprettNyttBrevOgRedirect}
          loading={isPending(nyttBrevStatus)}
        >
          Nytt brev
        </Button>
      </Container>
    </>
  )
}
