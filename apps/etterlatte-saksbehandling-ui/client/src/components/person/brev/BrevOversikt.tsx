import { useApiCall } from '~shared/hooks/useApiCall'
import { hentBrevForSak } from '~shared/api/brev'
import { useEffect } from 'react'
import { BodyShort, Box, Button, HStack, Table } from '@navikt/ds-react'
import { BrevStatus, formaterBrevtype, IBrev, Mottaker } from '~shared/types/Brev'
import { DocPencilIcon, ExternalLinkIcon } from '@navikt/aksel-icons'
import Spinner from '~shared/Spinner'
import BrevModal from '~components/person/brev/BrevModal'
import { ApiErrorAlert, ApiWarningAlert } from '~ErrorBoundary'
import { SakMedBehandlinger } from '~components/person/typer'
import { isFailure, isSuccess, mapApiResult, mapSuccess, Result } from '~shared/api/apiUtils'
import { LastOppBrev } from '~components/person/brev/LastOppBrev'
import { NyttBrevModal } from '~components/person/brev/NyttBrevModal'
import BrevStatusTag from '~components/person/brev/BrevStatusTag'
import { formaterDatoMedKlokkeslett } from '~utils/formatering/dato'
import { SlettBrev } from '~components/person/brev/handlinger/SlettBrev'
import { BrevUtgaar } from '~components/person/brev/handlinger/BrevUtgaar'

const mapAdresse = (mottaker: Mottaker) => {
  const adr = mottaker.adresse
  const adresselinje = [adr?.adresselinje1, adr?.adresselinje2, adr?.adresselinje3].join(' ')
  const postlinje = [adr?.postnummer, adr?.poststed, adr?.land].join(' ')

  return `${adresselinje}, ${postlinje}`
}

const kanEndres = (status: BrevStatus) => status !== BrevStatus.DISTRIBUERT

const handlingKnapp = (brev: IBrev) => {
  if (kanEndres(brev.status)) {
    const href = brev.behandlingId
      ? `/behandling/${brev.behandlingId}/brev`
      : `/person/sak/${brev.sakId}/brev/${brev.id}`

    return (
      <Button as="a" href={href} variant="secondary" title="Rediger" icon={<DocPencilIcon />} size="small">
        Rediger
      </Button>
    )
  }

  return (
    <>
      <Button
        as="a"
        href={`/api/brev/${brev.id}/pdf?sakId=${brev.sakId}`}
        target="_blank"
        variant="secondary"
        icon={<ExternalLinkIcon />}
        size="small"
      >
        Åpne
      </Button>

      <BrevModal brev={brev} />
    </>
  )
}

export default function BrevOversikt({ sakResult }: { sakResult: Result<SakMedBehandlinger> }) {
  const [brevListe, hentBrev] = useApiCall(hentBrevForSak)

  useEffect(() => {
    if (isSuccess(sakResult)) {
      hentBrev(Number(sakResult.data.sak.id))
    }
  }, [sakResult])

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
        <Spinner label="Henter brev for sak ..." />,
        () => (
          <ApiErrorAlert>Feil ved henting av brev...</ApiErrorAlert>
        ),
        (brevListe) => (
          <Table>
            <Table.Header>
              <Table.Row>
                <Table.HeaderCell>ID</Table.HeaderCell>
                <Table.HeaderCell>Status</Table.HeaderCell>
                <Table.HeaderCell>Distribuert</Table.HeaderCell>
                <Table.HeaderCell>Type</Table.HeaderCell>
                <Table.HeaderCell>Mottaker</Table.HeaderCell>
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
                  <Table.DataCell>
                    {b.status === BrevStatus.DISTRIBUERT ? formaterDatoMedKlokkeslett(b.statusEndret) : '-'}
                  </Table.DataCell>
                  <Table.DataCell>{formaterBrevtype(b.brevtype)}</Table.DataCell>
                  <Table.DataCell>
                    {b.mottakere.map((mottaker) => (
                      <BodyShort key={mottaker.id}>
                        {mottaker.navn}
                        <br />
                        {mapAdresse(mottaker)}
                      </BodyShort>
                    ))}
                  </Table.DataCell>
                  <Table.DataCell>
                    <HStack gap="4" justify="end" align="center">
                      <SlettBrev brev={b} />
                      <BrevUtgaar brev={b} />
                      {handlingKnapp(b)}
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
            <NyttBrevModal sakId={sak.sak.id} sakType={sak.sak.sakType} />
            <LastOppBrev sak={sak.sak} />
          </HStack>
        ))}
      </div>
    </Box>
  )
}
