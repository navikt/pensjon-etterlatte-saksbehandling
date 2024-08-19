import { isSuccess, mapResult, Result } from '~shared/api/apiUtils'
import { SakMedBehandlinger } from '~components/person/typer'
import React, { ReactNode, useEffect } from 'react'
import { BodyShort, Box, Heading, Table, VStack } from '@navikt/ds-react'
import { tekstAktivitetspliktUnntakType, tekstAktivitetspliktVurderingType } from '~shared/types/Aktivitetsplikt'
import { useApiCall } from '~shared/hooks/useApiCall'
import { hentAktivitspliktVurderingForSak } from '~shared/api/aktivitetsplikt'
import { formaterDato, formaterDatoMedFallback } from '~utils/formatering/dato'
import Spinner from '~shared/Spinner'
import { ApiErrorAlert } from '~ErrorBoundary'
import { AktivitetspliktTidslinje } from '~components/behandling/aktivitetsplikt/AktivitetspliktTidslinje'
import { hentFamilieOpplysninger } from '~shared/api/pdltjenester'
import { Familiemedlem } from '~shared/types/familieOpplysninger'

const velgDoedsdato = (avdoede: Familiemedlem[] | []): Date => {
  if (avdoede.length === 0) return new Date()
  else if (avdoede.length === 1) return avdoede[0].doedsdato!!
  else
    return avdoede.reduce((foersteAvdoed, andreAvdoed) =>
      foersteAvdoed.doedsdato!! < andreAvdoed.doedsdato!! ? foersteAvdoed : andreAvdoed
    ).doedsdato!!
}

export const Aktivitet = ({ fnr, sakResult }: { fnr: string; sakResult: Result<SakMedBehandlinger> }): ReactNode => {
  const [hentetVurdering, hentVurdering] = useApiCall(hentAktivitspliktVurderingForSak)
  const [familieOpplysningerResult, familieOpplysningerFetch] = useApiCall(hentFamilieOpplysninger)

  useEffect(() => {
    if (isSuccess(sakResult)) {
      hentVurdering({ sakId: sakResult.data.sak.id })
      familieOpplysningerFetch({ ident: fnr, sakType: sakResult.data.sak.sakType })
    }
  }, [])

  return (
    <Box padding="8" maxWidth="70rem">
      <VStack gap="8">
        <VStack gap="4">
          <Heading size="medium">Aktivitetsplikt</Heading>
          <BodyShort>Gjenlevende sin tidslinje</BodyShort>

          {isSuccess(sakResult) &&
            mapResult(familieOpplysningerResult, {
              pending: <Spinner label="Henter opplysninger om avdød" />,
              error: (error) => (
                <ApiErrorAlert>{error.detail || 'Kunne ikke hente opplysninger om avdød'}</ApiErrorAlert>
              ),
              success: ({ avdoede }) => (
                <>
                  {avdoede && (
                    <AktivitetspliktTidslinje doedsdato={velgDoedsdato(avdoede)} sakId={sakResult.data.sak.id} />
                  )}
                </>
              ),
            })}
        </VStack>

        <hr style={{ width: '100%' }} />

        {mapResult(hentetVurdering, {
          pending: <Spinner label="Henter vurderinger..." />,
          error: (error) => <ApiErrorAlert>{error.detail || 'Kunne ikke hente vurderinger'}</ApiErrorAlert>,
          success: (vurdering) => (
            <>
              {vurdering ? (
                <VStack gap="4">
                  <VStack gap="4">
                    <div>
                      <Heading size="small">Vurderinger</Heading>
                      <BodyShort>Det er registrert følgende vurderinger av aktivitetsplikt.</BodyShort>
                    </div>
                    <Heading size="xsmall">Aktivitetsgrad</Heading>
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
                            {vurdering.aktivitet.map((aktivitet) => (
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
                                <Table.DataCell>{aktivitet.endret.ident ? aktivitet.endret.ident : '-'}</Table.DataCell>
                              </Table.Row>
                            ))}
                          </>
                        ) : (
                          <Table.Row>
                            <Table.DataCell colSpan={6}>Finner ingen aktivitetsgrad</Table.DataCell>
                          </Table.Row>
                        )}
                      </Table.Body>
                    </Table>
                  </VStack>
                  <VStack gap="4">
                    <Heading size="xsmall">Unntak</Heading>
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
                            {vurdering.unntak.map((unntak) => (
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
                            ))}
                          </>
                        ) : (
                          <Table.Row>
                            <Table.DataCell colSpan={6}>Finner ingen unntak</Table.DataCell>
                          </Table.Row>
                        )}
                      </Table.Body>
                    </Table>
                  </VStack>
                </VStack>
              ) : (
                <BodyShort>Ingen vurdering</BodyShort>
              )}
            </>
          ),
        })}
      </VStack>
    </Box>
  )
}
