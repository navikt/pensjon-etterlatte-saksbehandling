import { isSuccess, mapResult, Result } from '~shared/api/apiUtils'
import { SakMedBehandlinger } from '~components/person/typer'
import React, { ReactNode, useEffect, useState } from 'react'
import { BodyShort, Box, Heading, Table, VStack } from '@navikt/ds-react'
import {
  IAktivitetspliktVurderingNy,
  tekstAktivitetspliktUnntakType,
  tekstAktivitetspliktVurderingType,
} from '~shared/types/Aktivitetsplikt'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentAktivitspliktVurderingForSak } from '~shared/api/aktivitetsplikt'
import { formaterDato, formaterDatoMedFallback } from '~utils/formatering/dato'
import Spinner from '~shared/Spinner'

export const Aktivitet = ({ sakResult }: { sakResult: Result<SakMedBehandlinger> }): ReactNode => {
  const [hentet, hent] = useApiCall(hentAktivitspliktVurderingForSak)
  const [vurdering, setVurdering] = useState<IAktivitetspliktVurderingNy>()

  useEffect(() => {
    if (isSuccess(sakResult)) {
      hent({ sakId: sakResult.data.sak.id }, (aktivitetspliktVurdering) => {
        setVurdering(aktivitetspliktVurdering)
      })
    }
  }, [])

  return (
    <Box padding="8" maxWidth="70rem">
      <Heading size="medium" spacing>
        Aktivitet
      </Heading>

      {mapResult(hentet, {
        pending: <Spinner label="Henter vurderinger..." />,
        error: (error) => <BodyShort>{error.detail || 'Kunne ikke hente vurderinger'}</BodyShort>,
        success: () => (
          <>
            {vurdering ? (
              <VStack gap="4">
                <VStack gap="4">
                  <div>
                    <Heading size="small">Vurderinger</Heading>
                    <BodyShort>Det er registrert følgende vurderinger av aktivitetsplikt.</BodyShort>
                  </div>
                  <Heading size="xsmall">Aktivitetsgrad</Heading>
                  <>
                    <Table size="small">
                      <Table.Header>
                        <Table.Row>
                          <Table.HeaderCell scope="col">Aktivitetsgrad</Table.HeaderCell>
                          <Table.HeaderCell scope="col">Fra og med</Table.HeaderCell>
                          <Table.HeaderCell scope="col">Til og med</Table.HeaderCell>
                          <Table.HeaderCell scope="col">Beskrivelse</Table.HeaderCell>
                          <Table.HeaderCell scope="col">Sist endret</Table.HeaderCell>
                          <Table.HeaderCell scope="col">Saksbehandler</Table.HeaderCell>
                        </Table.Row>
                      </Table.Header>
                      <Table.Body>
                        {!!vurdering.aktivitet?.length ? (
                          <>
                            {vurdering.aktivitet.map((aktivitet) => {
                              return (
                                <Table.Row key={aktivitet.id}>
                                  <Table.DataCell>
                                    {tekstAktivitetspliktVurderingType[aktivitet.aktivitetsgrad]}
                                  </Table.DataCell>
                                  <Table.DataCell>{aktivitet.fom ? formaterDato(aktivitet.fom) : '-'}</Table.DataCell>
                                  <Table.DataCell>{aktivitet.tom ? formaterDato(aktivitet.tom) : '-'}</Table.DataCell>
                                  <Table.DataCell>{aktivitet.beskrivelse ? aktivitet.beskrivelse : '-'}</Table.DataCell>
                                  <Table.DataCell>
                                    {aktivitet.endret && formaterDatoMedFallback(aktivitet.endret.tidspunkt, '-')}
                                  </Table.DataCell>
                                  <Table.DataCell>
                                    {aktivitet.endret.ident ? aktivitet.endret.ident : '-'}
                                  </Table.DataCell>
                                </Table.Row>
                              )
                            })}
                          </>
                        ) : (
                          <Table.Row>
                            <Table.DataCell colSpan={6}>Finner ingen aktivitetsgrad</Table.DataCell>
                          </Table.Row>
                        )}
                      </Table.Body>
                    </Table>
                  </>
                </VStack>
                <VStack gap="4">
                  <Heading size="xsmall">Unntak</Heading>
                  <>
                    <Table size="small">
                      <Table.Header>
                        <Table.Row>
                          <Table.HeaderCell scope="col">Unntak</Table.HeaderCell>
                          <Table.HeaderCell scope="col">Fra og med</Table.HeaderCell>
                          <Table.HeaderCell scope="col">Til og med</Table.HeaderCell>
                          <Table.HeaderCell scope="col">Beskrivelse</Table.HeaderCell>
                          <Table.HeaderCell scope="col">Sist endret</Table.HeaderCell>
                          <Table.HeaderCell scope="col">Saksbehandler</Table.HeaderCell>
                        </Table.Row>
                      </Table.Header>
                      <Table.Body>
                        {!!vurdering.unntak?.length ? (
                          <>
                            {vurdering.unntak.map((unntak) => {
                              return (
                                <Table.Row key={unntak.id}>
                                  <Table.DataCell>{tekstAktivitetspliktUnntakType[unntak.unntak]}</Table.DataCell>
                                  <Table.DataCell>{unntak.fom ? formaterDato(unntak.fom) : '-'}</Table.DataCell>
                                  <Table.DataCell>{unntak.tom ? formaterDato(unntak.tom) : '-'}</Table.DataCell>
                                  <Table.DataCell>{unntak.beskrivelse ? unntak.beskrivelse : '-'}</Table.DataCell>
                                  <Table.DataCell>
                                    {unntak.endret && formaterDatoMedFallback(unntak.endret.tidspunkt, '-')}
                                  </Table.DataCell>
                                  <Table.DataCell>{unntak.endret.ident ? unntak.endret.ident : '-'}</Table.DataCell>
                                </Table.Row>
                              )
                            })}
                          </>
                        ) : (
                          <Table.Row>
                            <Table.DataCell colSpan={6}>Finner ingen unntak</Table.DataCell>
                          </Table.Row>
                        )}
                      </Table.Body>
                    </Table>
                  </>
                </VStack>
              </VStack>
            ) : (
              <BodyShort>Ingen vurdering</BodyShort>
            )}
          </>
        ),
      })}
    </Box>
  )
}
