import React, { useState } from 'react'
import { IAktivitetspliktUnntak, tekstAktivitetspliktUnntakType } from '~shared/types/Aktivitetsplikt'
import { BodyShort, Box, Button, Detail, Heading, HStack, ReadMore, Table, VStack } from '@navikt/ds-react'
import { HandShakeHeartIcon, PencilIcon, TrashIcon } from '@navikt/aksel-icons'
import { AktivitetspliktUnntakTypeTag } from '~shared/tags/AktivitetspliktUnntakTypeTag'
import { formaterDato, formaterDatoMedFallback } from '~utils/formatering/dato'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'
import { UnntakAktivitetspliktOppgaveForm } from '~components/aktivitetsplikt/vurdering/unntak/UnntakAktivitetspliktOppgaveForm'
import { erOppgaveRedigerbar } from '~shared/types/oppgave'
import { useApiCall } from '~shared/hooks/useApiCall'
import { slettAktivitetspliktUnntak } from '~shared/api/aktivitetsplikt'
import { isFailure, isPending } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'

export function UnntakIOppgave() {
  const { vurdering, oppgave, oppdater } = useAktivitetspliktOppgaveVurdering()
  const [unntakForRedigering, setUnntakForRedigering] = useState<IAktivitetspliktUnntak | undefined>()
  const [slettUnntakStatus, slettSpesifiktUnntak, resetSlettStatus] = useApiCall(slettAktivitetspliktUnntak)
  const unntaker = vurdering.unntak

  const oppgaveErRedigerbar = erOppgaveRedigerbar(oppgave.status)

  function slettUnntak(unntak: IAktivitetspliktUnntak) {
    slettSpesifiktUnntak(
      {
        oppgaveId: oppgave.id,
        sakId: unntak.sakId,
        unntakId: unntak.id,
      },
      () => {
        setUnntakForRedigering(undefined)
        oppdater()
      }
    )
  }

  function oppdaterStateEtterRedigertUnntak() {
    resetSlettStatus()
    setUnntakForRedigering(undefined)
    oppdater()
  }

  return (
    <VStack gap="4">
      <HStack gap="4" align="center">
        <HandShakeHeartIcon fontSize="1.5rem" aria-hidden />
        <Heading size="small">Unntak</Heading>
      </HStack>

      <Box maxWidth="42.5rem">
        <ReadMore header="Dette menes med unntak">
          I oversikten over unntak ser du hvilke unntak som er satt på den gjenlevende. Det finnes både midlertidige og
          varige unntak
        </ReadMore>
      </Box>
      {isFailure(slettUnntakStatus) && (
        <ApiErrorAlert>Kunne ikke slette unntaket, på grunn av feil: {slettUnntakStatus.error.detail}</ApiErrorAlert>
      )}
      <Table size="small">
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell />
            <Table.HeaderCell scope="col">Unntak</Table.HeaderCell>
            <Table.HeaderCell scope="col">Type</Table.HeaderCell>
            <Table.HeaderCell scope="col">Fra og med</Table.HeaderCell>
            <Table.HeaderCell scope="col">Til og med</Table.HeaderCell>
            <Table.HeaderCell scope="col">Kilde</Table.HeaderCell>
            <Table.HeaderCell />
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {!!unntaker?.length ? (
            <>
              {unntaker.map((unntak) => (
                <Table.ExpandableRow
                  key={unntak.id}
                  content={
                    <UnntakAktivitetspliktOppgaveForm
                      onSuccess={oppdaterStateEtterRedigertUnntak}
                      onAvbryt={() => setUnntakForRedigering(undefined)}
                    />
                  }
                  open={unntakForRedigering?.id === unntak.id}
                >
                  <Table.DataCell>{tekstAktivitetspliktUnntakType[unntak.unntak]}</Table.DataCell>
                  <Table.DataCell>
                    <AktivitetspliktUnntakTypeTag unntak={unntak.unntak} />
                  </Table.DataCell>
                  <Table.DataCell>{formaterDatoMedFallback(unntak.fom, '-')}</Table.DataCell>
                  <Table.DataCell>{formaterDatoMedFallback(unntak.tom, '-')}</Table.DataCell>
                  <Table.DataCell>
                    <BodyShort>{unntak.endret.ident}</BodyShort>
                    <Detail>Saksbehandler: {formaterDato(unntak.endret.tidspunkt)}</Detail>
                  </Table.DataCell>
                  <Table.DataCell>
                    {oppgaveErRedigerbar && (
                      <HStack>
                        <Button size="xsmall" icon={<PencilIcon />} onClick={() => setUnntakForRedigering(unntak)}>
                          Rediger
                        </Button>
                        <Button
                          size="xsmall"
                          icon={<TrashIcon />}
                          onClick={() => slettUnntak(unntak)}
                          loading={isPending(slettUnntakStatus)}
                        >
                          Slett
                        </Button>
                      </HStack>
                    )}
                  </Table.DataCell>
                </Table.ExpandableRow>
              ))}
            </>
          ) : (
            <Table.Row>
              <Table.DataCell colSpan={7}>Ingen unntak</Table.DataCell>
            </Table.Row>
          )}
        </Table.Body>
      </Table>
    </VStack>
  )
}
