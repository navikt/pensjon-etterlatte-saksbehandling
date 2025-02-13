import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { BodyShort, Detail, Table, VStack } from '@navikt/ds-react'
import { formaterDatoMedFallback } from '~utils/formatering/dato'

export const SoeknadInformasjon = ({ behandling }: { behandling: IDetaljertBehandling }) => {
  const personopplysninger = usePersonopplysninger()

  const visOmInnsenderErGjenlevendeEllerVerge = (): string => {
    // Innsender er gjenlevende
    if (
      personopplysninger?.innsender?.opplysning.foedselsnummer === personopplysninger?.soeker?.opplysning.foedselsnummer
    ) {
      return '(gjenlevende)'
    }

    // Innsender er verge
    if (
      personopplysninger?.innsender?.opplysning.foedselsnummer ===
      personopplysninger?.soeker?.opplysning.vergemaalEllerFremtidsfullmakt?.[0].vergeEllerFullmektig
        .motpartsPersonident
    ) {
      return '(verge)'
    }

    return ''
  }

  return (
    <Table size="small">
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell scope="col">Søknad mottat</Table.HeaderCell>
          <Table.HeaderCell scope="col">Innsender</Table.HeaderCell>
          <Table.HeaderCell scope="col">Foreldreansvar</Table.HeaderCell>
          <Table.HeaderCell scope="col">Vergemål</Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        <Table.Row>
          <Table.DataCell>{formaterDatoMedFallback(behandling.soeknadMottattDato)}</Table.DataCell>
          <Table.DataCell>
            {!!personopplysninger?.innsender ? (
              <VStack>
                <BodyShort>{`${personopplysninger.innsender.opplysning.fornavn} ${personopplysninger.innsender.opplysning.etternavn} ${visOmInnsenderErGjenlevendeEllerVerge()}`}</BodyShort>
                <Detail>{`${personopplysninger.innsender.kilde.type.toUpperCase()} ${formaterDatoMedFallback(personopplysninger.innsender.kilde.tidspunkt)}`}</Detail>
              </VStack>
            ) : (
              '-'
            )}
          </Table.DataCell>
          <Table.DataCell>
            <VStack>
              <BodyShort>
                {!!personopplysninger?.soeker?.opplysning.familieRelasjon?.barn?.length
                  ? `${personopplysninger?.soeker.opplysning.fornavn} ${personopplysninger?.soeker.opplysning.etternavn}`
                  : 'Ingen barn'}
              </BodyShort>
              <Detail>{`${personopplysninger?.soeker?.kilde.type.toUpperCase()} ${formaterDatoMedFallback(personopplysninger?.soeker?.kilde.tidspunkt)}`}</Detail>
            </VStack>
          </Table.DataCell>
          <Table.DataCell>
            <VStack>
              {!!personopplysninger?.soeker?.opplysning.vergemaalEllerFremtidsfullmakt?.length ? (
                <VStack>
                  <BodyShort>
                    {personopplysninger?.soeker.opplysning.vergemaalEllerFremtidsfullmakt.map(
                      (vergemaal) => vergemaal.vergeEllerFullmektig.navn ?? 'Har ikke navn på verge'
                    )}
                  </BodyShort>
                </VStack>
              ) : (
                <BodyShort>Ingen vergemål</BodyShort>
              )}
              <Detail>{`${personopplysninger?.soeker?.kilde.type.toUpperCase()} ${formaterDatoMedFallback(personopplysninger?.soeker?.kilde.tidspunkt)}`}</Detail>
            </VStack>
          </Table.DataCell>
        </Table.Row>
      </Table.Body>
    </Table>
  )
}
