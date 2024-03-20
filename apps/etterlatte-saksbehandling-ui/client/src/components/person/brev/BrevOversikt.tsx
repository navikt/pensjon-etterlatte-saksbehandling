import { useApiCall } from '~shared/hooks/useApiCall'
import { hentBrevForSak, opprettBrevForSak, slettBrev } from '~shared/api/brev'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { BodyShort, Button, Modal, Table, Tag } from '@navikt/ds-react'
import { BrevStatus, formaterBrevtype, IBrev, Mottaker } from '~shared/types/Brev'
import { DocPencilIcon, ExternalLinkIcon, TrashIcon } from '@navikt/aksel-icons'
import Spinner from '~shared/Spinner'
import { Container, FlexRow } from '~shared/styled'
import BrevModal from '~components/person/brev/BrevModal'
import { ApiErrorAlert, ApiWarningAlert } from '~ErrorBoundary'
import { SakMedBehandlinger } from '~components/person/typer'

import { isFailure, isPending, isSuccess, mapApiResult, mapSuccess, Result } from '~shared/api/apiUtils'
import { LastOppBrev } from '~components/person/brev/LastOppBrev'

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

const kanSletteBrev = (brev: IBrev) => brevKanEndres(brev.status) && !brev.behandlingId

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

export default function BrevOversikt({ sakResult }: { sakResult: Result<SakMedBehandlinger> }) {
  const navigate = useNavigate()

  const [brevListe, hentBrev] = useApiCall(hentBrevForSak)
  const [nyttBrevStatus, opprettBrev] = useApiCall(opprettBrevForSak)

  useEffect(() => {
    if (isSuccess(sakResult)) {
      hentBrev(Number(sakResult.data.sak.id))
    }
  }, [sakResult])

  const opprettNyttBrevOgRedirect = () => {
    if (isSuccess(sakResult)) {
      opprettBrev(Number(sakResult.data.sak.id), (brev) => {
        navigate(`/person/${brev.soekerFnr}/sak/${brev.sakId}/brev/${brev.id}`)
      })
    } else {
      throw Error('SakID mangler!')
    }
  }

  if (isFailure(sakResult)) {
    return (
      <Container>
        {sakResult.error.status === 404 ? (
          <ApiWarningAlert>Kan ikke opprette brev: {sakResult.error.detail}</ApiWarningAlert>
        ) : (
          <ApiErrorAlert>{sakResult.error.detail || 'Feil ved henting av brev'}</ApiErrorAlert>
        )}
      </Container>
    )
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
                  <Table.DataCell>{formaterBrevtype(b.brevtype)}</Table.DataCell>
                  <Table.DataCell>{b.mottaker.navn}</Table.DataCell>
                  <Table.DataCell>{mapAdresse(b.mottaker)}</Table.DataCell>
                  <Table.DataCell>
                    <FlexRow>
                      {handlingKnapp(b)}
                      {kanSletteBrev(b) && <SlettBrev brevId={b.id} sakId={b.sakId} />}
                    </FlexRow>
                  </Table.DataCell>
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

        {mapSuccess(sakResult, (sak) => (
          <LastOppBrev sak={sak.sak} />
        ))}
      </FlexRow>
    </Container>
  )
}

const SlettBrev = ({ brevId, sakId }: { brevId: number; sakId: number }) => {
  const [isOpen, setIsOpen] = useState(false)
  const [slettBrevStatus, apiSlettBrev] = useApiCall(slettBrev)

  const slett = () => {
    apiSlettBrev({ brevId, sakId }, () => {
      window.location.reload()
    })
  }

  return (
    <>
      <Button variant="danger" icon={<TrashIcon />} onClick={() => setIsOpen(true)} />

      <Modal open={isOpen} onClose={() => setIsOpen(false)} aria-label="Slett brev">
        <Modal.Body>
          <BodyShort spacing>Er du sikker på at du vil slette brevet? Handlingen kan ikke angres.</BodyShort>

          <FlexRow justify="center">
            <Button variant="secondary" onClick={() => setIsOpen(false)} disabled={isPending(slettBrevStatus)}>
              Nei, avbryt
            </Button>
            <Button variant="danger" onClick={slett} loading={isPending(slettBrevStatus)}>
              Ja, slett
            </Button>
          </FlexRow>
        </Modal.Body>
      </Modal>
    </>
  )
}
