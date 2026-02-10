import { Personopplysning } from '~shared/types/grunnlag'
import { ILand } from '~utils/kodeverk'
import { BodyShort, Heading, HStack, Table, VStack } from '@navikt/ds-react'
import { PersonIcon } from '@navikt/aksel-icons'
import { formaterNavn } from '~shared/types/Person'
import { DoedsdatoTag } from '~shared/tags/DoedsdatoTag'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'
import { AdresseVedDoedsfall } from '~components/behandling/soeknadsoversikt/familieforhold/AdresseVedDoedsfall'
import { finnLandSomTekst } from '~components/person/personopplysninger/utils'
import { ListeOverInnflyttingTilNorge } from '~components/behandling/soeknadsoversikt/familieforhold/ListeOverInnflyttingTilNorge'
import { ListeOverUtflyttingFraNorge } from '~components/behandling/soeknadsoversikt/familieforhold/ListeOverUtflyttingFraNorge'
interface Props {
  avdoede: Personopplysning[] | undefined
  alleLand: ILand[]
}

export const TabellOverAvdoede = ({ avdoede, alleLand }: Props) => {
  return (
    <VStack gap="space-4">
      <HStack gap="space-4" justify="start" align="center" wrap={false}>
        <PersonIcon fontSize="1.5rem" aria-hidden />
        <Heading size="small" level="3">
          Avdøde
        </Heading>
      </HStack>
      <Table size="small">
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell scope="col">Navn</Table.HeaderCell>
            <Table.HeaderCell scope="col">Fødselsnummer</Table.HeaderCell>
            <Table.HeaderCell scope="col">Bostedsadresse ved dødsdato</Table.HeaderCell>
            <Table.HeaderCell scope="col">Innflyttning</Table.HeaderCell>
            <Table.HeaderCell scope="col">Utflyttning</Table.HeaderCell>
            <Table.HeaderCell scope="col">Statsborgerskap</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {!!avdoede?.length ? (
            avdoede.map((avdoed, index) => (
              <Table.Row key={index}>
                <Table.DataCell>
                  <HStack gap="space-2" justify="start" align="center" wrap={false}>
                    <BodyShort>{formaterNavn(avdoed.opplysning)}</BodyShort>
                    <DoedsdatoTag doedsdato={avdoed.opplysning.doedsdato} />
                  </HStack>
                </Table.DataCell>
                <Table.DataCell>
                  <KopierbarVerdi value={avdoed.opplysning.foedselsnummer} iconPosition="right" />
                </Table.DataCell>
                <Table.DataCell>
                  <AdresseVedDoedsfall avdoed={avdoed} />
                </Table.DataCell>
                <Table.DataCell>
                  <ListeOverInnflyttingTilNorge
                    innflyttingTilNorge={avdoed.opplysning.utland?.innflyttingTilNorge}
                    alleLand={alleLand}
                  />
                </Table.DataCell>
                <Table.DataCell>
                  <ListeOverUtflyttingFraNorge
                    utflyttingFraNorge={avdoed.opplysning.utland?.utflyttingFraNorge}
                    alleLand={alleLand}
                  />
                </Table.DataCell>
                <Table.DataCell>
                  {!!avdoed.opplysning.statsborgerskap
                    ? finnLandSomTekst(avdoed.opplysning.statsborgerskap, alleLand)
                    : 'Ingen statsborgerskap'}
                </Table.DataCell>
              </Table.Row>
            ))
          ) : (
            <Table.Row>
              <Table.DataCell colSpan={6}>
                <Heading size="small"> Ingen avdøde</Heading>
              </Table.DataCell>
            </Table.Row>
          )}
        </Table.Body>
      </Table>
    </VStack>
  )
}
