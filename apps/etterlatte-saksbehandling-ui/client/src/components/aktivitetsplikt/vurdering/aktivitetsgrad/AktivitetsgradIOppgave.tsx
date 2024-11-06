import { IAktivitetspliktAktivitetsgrad, tekstAktivitetspliktVurderingType } from '~shared/types/Aktivitetsplikt'
import { BodyShort, Box, Button, Detail, Heading, HStack, ReadMore, Table, VStack } from '@navikt/ds-react'
import { ClockDashedIcon, PencilIcon, TrashIcon } from '@navikt/aksel-icons'
import { formaterDato, formaterDatoMedFallback } from '~utils/formatering/dato'
import React, { useState } from 'react'
import { VurderingAktivitetsgradForm } from './VurderingAktivitetsgradForm'
import { useApiCall } from '~shared/hooks/useApiCall'
import { slettAktivitetspliktVurdering } from '~shared/api/aktivitetsplikt'
import { useAktivitetspliktOppgaveVurdering } from '~components/aktivitetsplikt/OppgaveVurderingRoute'
import { isFailure, isPending } from '~shared/api/apiUtils'
import { ApiErrorAlert } from '~ErrorBoundary'

export function AktivitetsgradIOppgave(props: { doedsdato?: Date }) {
  const { oppgave, vurdering, oppdater } = useAktivitetspliktOppgaveVurdering()
  const [aktivitetForRedigering, setAktivitetForRedigering] = useState<IAktivitetspliktAktivitetsgrad | undefined>()
  const [slettStatus, slettSpesifikkAktivitet] = useApiCall(slettAktivitetspliktVurdering)

  const aktiviteter = vurdering.aktivitet

  function oppdaterTilstandLagretVurdering() {
    setAktivitetForRedigering(undefined)
    oppdater()
  }

  function slettAktivitetsgradIOppgave(aktivitet: IAktivitetspliktAktivitetsgrad) {
    slettSpesifikkAktivitet(
      {
        sakId: aktivitet.sakId,
        oppgaveId: oppgave.id,
        vurderingId: aktivitet.id,
      },
      () => {
        setAktivitetForRedigering(undefined)
        oppdater()
      }
    )
  }

  return (
    <VStack gap="4">
      <HStack gap="4" align="center">
        <ClockDashedIcon fontSize="1.5rem" aria-hidden />
        <Heading size="small">Aktivitetsgrad</Heading>
      </HStack>

      <Box maxWidth="42.5rem">
        <ReadMore header="Dette menes med aktivitetsgrad">
          I oversikten over aktivitetsgrad kan du se hvilken aktivitetsgrad brukeren har hatt. For å motta
          omstillingsstønad stilles det ingen krav til aktivitet de første seks månedene etter dødsfall. Etter seks
          måneder forventes det at du er i minst 50 % aktivitet, og etter ett år og fremover forventes det 100 %
          aktivitet. Vær oppmerksom på at det finnes unntak.
        </ReadMore>
      </Box>

      {isFailure(slettStatus) && (
        <ApiErrorAlert>
          Kunne ikke slette aktivitetsvurderingen, på grunn av feil: {slettStatus.error.detail}
        </ApiErrorAlert>
      )}

      <Table size="small">
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell />
            <Table.HeaderCell scope="col">Aktivitetsgrad</Table.HeaderCell>
            <Table.HeaderCell scope="col">Fra og med</Table.HeaderCell>
            <Table.HeaderCell scope="col">Til og med</Table.HeaderCell>
            <Table.HeaderCell scope="col">Kilde</Table.HeaderCell>
            <Table.HeaderCell />
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {!!aktiviteter?.length ? (
            <>
              {aktiviteter.map((aktivitet) => (
                <Table.ExpandableRow
                  key={aktivitet.id}
                  open={aktivitetForRedigering?.id === aktivitet.id}
                  content={
                    <VurderingAktivitetsgradForm
                      aktivitet={aktivitet}
                      doedsdato={props.doedsdato}
                      onAvbryt={() => setAktivitetForRedigering(undefined)}
                      onSuccess={oppdaterTilstandLagretVurdering}
                    />
                  }
                >
                  <Table.DataCell>{tekstAktivitetspliktVurderingType[aktivitet.aktivitetsgrad]}</Table.DataCell>
                  <Table.DataCell>{formaterDatoMedFallback(aktivitet.fom, '-')}</Table.DataCell>
                  <Table.DataCell>{formaterDatoMedFallback(aktivitet.tom, '-')}</Table.DataCell>
                  <Table.DataCell>
                    <BodyShort>{aktivitet.endret.ident}</BodyShort>
                    <Detail>Saksbehandler: {formaterDato(aktivitet.endret.tidspunkt)}</Detail>
                  </Table.DataCell>
                  <Table.DataCell>
                    <HStack gap="4">
                      <Button
                        size="xsmall"
                        variant="secondary"
                        onClick={() => setAktivitetForRedigering(aktivitet)}
                        icon={<PencilIcon />}
                      >
                        Rediger
                      </Button>
                      <Button
                        size="xsmall"
                        variant="secondary"
                        icon={<TrashIcon />}
                        loading={isPending(slettStatus)}
                        onClick={() => slettAktivitetsgradIOppgave(aktivitet)}
                      >
                        Slett
                      </Button>
                    </HStack>
                  </Table.DataCell>
                </Table.ExpandableRow>
              ))}
            </>
          ) : (
            <Table.Row>
              <Table.DataCell colSpan={6}>Ingen aktivitetsgrad</Table.DataCell>
            </Table.Row>
          )}
        </Table.Body>
      </Table>
    </VStack>
  )
}
