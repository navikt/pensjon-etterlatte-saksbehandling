import { Bostedsadresse } from '~shared/types/familieOpplysninger'
import { ExpansionCard, Heading, HStack, Table } from '@navikt/ds-react'
import { HouseIcon } from '@navikt/aksel-icons'
import { compareDesc } from 'date-fns'
import { BostedsadresseDataCell } from '~components/person/personopplysninger/components/BostedsadresseDataCell'
import { formaterDatoMedFallback } from '~utils/formatering/dato'

interface Props {
  bostedsadresser?: Bostedsadresse[]
  erAvdoedesAddresser?: boolean
}

export const BostedsadresserExpansionCard = ({ bostedsadresser, erAvdoedesAddresser = false }: Props) => {
  const sorterteBostedsadresse = bostedsadresser?.sort((a: Bostedsadresse, b: Bostedsadresse) => {
    if (a.gyldigFraOgMed && b.gyldigFraOgMed) {
      return compareDesc(new Date(a.gyldigFraOgMed), new Date(b.gyldigFraOgMed))
    } else if (a.gyldigFraOgMed && !b.gyldigFraOgMed) {
      return -1
    } else if (!a.gyldigFraOgMed && b.gyldigFraOgMed) {
      return 1
    }
    return 0
  })

  return (
    <ExpansionCard aria-labelledby="Bostedsadresser" size="small" defaultOpen>
      <ExpansionCard.Header>
        <HStack gap="space-4" align="center">
          <HouseIcon aria-hidden fontSize="1.5rem" />
          <ExpansionCard.Title size="small">
            {erAvdoedesAddresser ? 'Avdødes bostedsadresser' : 'Søkers bostedsadresser'}
          </ExpansionCard.Title>
        </HStack>
      </ExpansionCard.Header>
      <ExpansionCard.Content>
        <Table size="small">
          <Table.Header>
            <Table.Row>
              <Table.HeaderCell scope="col">Adresse</Table.HeaderCell>
              <Table.HeaderCell scope="col">Fra og med</Table.HeaderCell>
              <Table.HeaderCell scope="col">Til og med</Table.HeaderCell>
            </Table.Row>
          </Table.Header>
          <Table.Body>
            {!!sorterteBostedsadresse?.length ? (
              sorterteBostedsadresse.map((adresse, index) => (
                <Table.Row key={index}>
                  <BostedsadresseDataCell bostedsadresser={bostedsadresser} index={index} visAktiv />
                  <Table.DataCell>{formaterDatoMedFallback(adresse.gyldigFraOgMed)}</Table.DataCell>
                  <Table.DataCell>{formaterDatoMedFallback(adresse.gyldigTilOgMed)}</Table.DataCell>
                </Table.Row>
              ))
            ) : (
              <Table.Row>
                <Table.DataCell colSpan={3}>
                  <Heading size="small">Ingen bostedsadresser</Heading>
                </Table.DataCell>
              </Table.Row>
            )}
          </Table.Body>
        </Table>
      </ExpansionCard.Content>
    </ExpansionCard>
  )
}
