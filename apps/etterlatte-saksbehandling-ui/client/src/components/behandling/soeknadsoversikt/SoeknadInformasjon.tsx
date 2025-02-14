import { IDetaljertBehandling } from '~shared/types/IDetaljertBehandling'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'
import { BodyShort, Detail, Table, VStack } from '@navikt/ds-react'
import { formaterDatoMedFallback } from '~utils/formatering/dato'
import { SakType } from '~shared/types/sak'
import { formaterNavn } from '~shared/types/Person'

export const SoeknadInformasjon = ({ behandling }: { behandling: IDetaljertBehandling }) => {
  const personopplysninger = usePersonopplysninger()

  const visOmInnsenderErSoekerEllerVerge = (): string => {
    // Innsender er soeker
    if (
      personopplysninger?.innsender?.opplysning.foedselsnummer === personopplysninger?.soeker?.opplysning.foedselsnummer
    ) {
      return '(søker)'
    }

    // Innsender er verge
    if (!!personopplysninger?.soeker?.opplysning.vergemaalEllerFremtidsfullmakt?.length) {
      if (
        personopplysninger?.innsender?.opplysning.foedselsnummer ===
        personopplysninger?.soeker?.opplysning.vergemaalEllerFremtidsfullmakt?.[0].vergeEllerFullmektig
          .motpartsPersonident
      ) {
        return '(verge)'
      }
    }

    return ''
  }

  return (
    <Table size="small">
      <Table.Header>
        <Table.Row>
          <Table.HeaderCell scope="col">Søknad mottat</Table.HeaderCell>
          <Table.HeaderCell scope="col">Innsender</Table.HeaderCell>
          {behandling.sakType === SakType.BARNEPENSJON && (
            <Table.HeaderCell scope="col">Foreldreansvar</Table.HeaderCell>
          )}
          <Table.HeaderCell scope="col">Vergemål</Table.HeaderCell>
        </Table.Row>
      </Table.Header>
      <Table.Body>
        <Table.Row>
          <Table.DataCell>{formaterDatoMedFallback(behandling.soeknadMottattDato)}</Table.DataCell>
          <Table.DataCell>
            {!!personopplysninger?.innsender ? (
              <VStack>
                <BodyShort>{`{formaterNavn(personopplysninger.innsender.opplysning)} ${visOmInnsenderErSoekerEllerVerge()}`}</BodyShort>
                <Detail>{`${personopplysninger.innsender.kilde.type.toUpperCase()} ${formaterDatoMedFallback(personopplysninger.innsender.kilde.tidspunkt)}`}</Detail>
              </VStack>
            ) : (
              '-'
            )}
          </Table.DataCell>
          {behandling.sakType === SakType.BARNEPENSJON && (
            <Table.DataCell>
              <VStack>
                <BodyShort>
                  {!!personopplysninger?.soeker?.opplysning.familieRelasjon?.barn?.length
                    ? `${formaterNavn(personopplysninger?.soeker.opplysning)}`
                    : 'Ingen barn'}
                </BodyShort>
                <Detail>{`${personopplysninger?.soeker?.kilde.type.toUpperCase()} ${formaterDatoMedFallback(personopplysninger?.soeker?.kilde.tidspunkt)}`}</Detail>
              </VStack>
            </Table.DataCell>
          )}
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
