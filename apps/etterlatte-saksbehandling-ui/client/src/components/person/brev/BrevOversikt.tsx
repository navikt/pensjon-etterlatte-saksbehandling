import { useApiCall } from '~shared/hooks/useApiCall'
import { hentBrevForSak, opprettBrevForSak, slettBrev } from '~shared/api/brev'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { BodyShort, Box, Button, HStack, Modal, Table } from '@navikt/ds-react'
import { BrevStatus, formaterBrevtype, IBrev, Mottaker } from '~shared/types/Brev'
import { DocPencilIcon, ExternalLinkIcon, TrashIcon } from '@navikt/aksel-icons'
import Spinner from '~shared/Spinner'
import BrevModal from '~components/person/brev/BrevModal'
import { ApiErrorAlert, ApiWarningAlert } from '~ErrorBoundary'
import { SakMedBehandlinger } from '~components/person/typer'
import { isFailure, isPending, isSuccess, mapApiResult, mapSuccess, Result } from '~shared/api/apiUtils'
import { LastOppBrev } from '~components/person/brev/LastOppBrev'
import { NyttBrevModal } from '~components/person/brev/NyttBrevModal'
import { useFeatureEnabledMedDefault } from '~shared/hooks/useFeatureToggle'
import BrevStatusTag from '~components/person/brev/BrevStatusTag'

const mapAdresse = (mottaker: Mottaker) => {
  const adr = mottaker.adresse
  const adresselinje = [adr?.adresselinje1, adr?.adresselinje2, adr?.adresselinje3].join(' ')
  const postlinje = [adr?.postnummer, adr?.poststed, adr?.land].join(' ')

  return `${adresselinje}, ${postlinje}`
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

  const kanOppretteBrevMedGittType = useFeatureEnabledMedDefault('kan-opprette-brev-med-data-for-spesifikk-type', false)

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
      <Box padding="8">
        {sakResult.error.status === 404 ? (
          <ApiWarningAlert>Kan ikke opprette brev: {sakResult.error.detail}</ApiWarningAlert>
        ) : (
          <ApiErrorAlert>{sakResult.error.detail || 'Feil ved henting av brev'}</ApiErrorAlert>
        )}
      </Box>
    )
  }

  return (
    <Box padding="8">
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
                    <BrevStatusTag status={b.status} />
                  </Table.DataCell>
                  <Table.DataCell>{formaterBrevtype(b.brevtype)}</Table.DataCell>
                  <Table.DataCell>{b.mottaker.navn}</Table.DataCell>
                  <Table.DataCell>{mapAdresse(b.mottaker)}</Table.DataCell>
                  <Table.DataCell>
                    <HStack gap="4">
                      {handlingKnapp(b)}
                      {kanSletteBrev(b) && <SlettBrev brevId={b.id} sakId={b.sakId} />}
                    </HStack>
                  </Table.DataCell>
                </Table.Row>
              ))}
            </Table.Body>
          </Table>
        )
      )}

      <br />

      <div>
        {mapSuccess(sakResult, (sak) => (
          <HStack gap="4">
            {kanOppretteBrevMedGittType ? (
              <NyttBrevModal sakId={sak.sak.id} sakType={sak.sak.sakType} />
            ) : (
              <Button
                variant="primary"
                icon={<DocPencilIcon />}
                iconPosition="right"
                onClick={opprettNyttBrevOgRedirect}
                loading={isPending(nyttBrevStatus)}
              >
                Nytt brev
              </Button>
            )}
            <LastOppBrev sak={sak.sak} />
          </HStack>
        ))}
      </div>
    </Box>
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

          <HStack gap="4" justify="center">
            <Button variant="secondary" onClick={() => setIsOpen(false)} disabled={isPending(slettBrevStatus)}>
              Nei, avbryt
            </Button>
            <Button variant="danger" onClick={slett} loading={isPending(slettBrevStatus)}>
              Ja, slett
            </Button>
          </HStack>
        </Modal.Body>
      </Modal>
    </>
  )
}
