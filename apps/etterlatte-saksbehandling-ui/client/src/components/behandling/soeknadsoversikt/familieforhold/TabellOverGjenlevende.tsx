import { Personopplysning } from '~shared/types/grunnlag'
import { ILand } from '~utils/kodeverk'
import { Heading, HStack, Table, VStack } from '@navikt/ds-react'
import { PersonIcon } from '@navikt/aksel-icons'
import { formaterAdresse, formaterNavn, formaterSivilstatusTilLesbarStreng } from '~shared/types/Person'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'
import { IAdresse } from '~shared/types/IAdresse'
import { ListeOverInnflyttingTilNorge } from '~components/behandling/soeknadsoversikt/familieforhold/ListeOverInnflyttingTilNorge'
import { ListeOverUtflyttingFraNorge } from '~components/behandling/soeknadsoversikt/familieforhold/ListeOverUtflyttingFraNorge'
import { finnLandSomTekst } from '~components/person/personopplysninger/utils'
import React from 'react'

interface Props {
  gjenlevende: Personopplysning | undefined
  alleLand: ILand[]
}

export const TabellOverGjenlevende = ({ gjenlevende, alleLand }: Props) => {
  const finnAktiveEllerSisteAdresse = (): IAdresse | undefined => {
    const aktivAdresse = gjenlevende?.opplysning.bostedsadresse?.find((adresse) => adresse.aktiv)

    if (!!aktivAdresse) {
      return aktivAdresse
    } else {
      const bostedadresser = !!gjenlevende?.opplysning.bostedsadresse?.length
        ? [...gjenlevende?.opplysning.bostedsadresse]
        : []
      return bostedadresser.sort((a, b) => (new Date(b.gyldigFraOgMed!) > new Date(a.gyldigFraOgMed!) ? 1 : -1))[0]
    }
  }

  const addresse = finnAktiveEllerSisteAdresse()

  return (
    <VStack gap="4">
      <HStack gap="4" justify="start" align="center" wrap={false}>
        <PersonIcon fontSize="1.5rem" aria-hidden />
        <Heading size="small" level="3">
          Gjenlevende
        </Heading>
      </HStack>
      <Table size="small">
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell scope="col">Navn</Table.HeaderCell>
            <Table.HeaderCell scope="col">Fødselsnummer</Table.HeaderCell>
            <Table.HeaderCell scope="col">Bostedsadresse</Table.HeaderCell>
            <Table.HeaderCell scope="col">Innflyttning</Table.HeaderCell>
            <Table.HeaderCell scope="col">Utflytning</Table.HeaderCell>
            <Table.HeaderCell scope="col">Statsborgerskap</Table.HeaderCell>
            <Table.HeaderCell scope="col">Sivilstand</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {!!gjenlevende ? (
            <Table.Row>
              <Table.DataCell>{formaterNavn(gjenlevende.opplysning)}</Table.DataCell>
              <Table.DataCell>
                <KopierbarVerdi value={gjenlevende.opplysning.foedselsnummer} iconPosition="right" />
              </Table.DataCell>
              <Table.DataCell>{!!addresse ? formaterAdresse(addresse) : 'Ingen adresse'}</Table.DataCell>
              <Table.DataCell>
                <ListeOverInnflyttingTilNorge
                  innflyttingTilNorge={gjenlevende.opplysning.utland?.innflyttingTilNorge}
                  alleLand={alleLand}
                />
              </Table.DataCell>
              <Table.DataCell>
                <ListeOverUtflyttingFraNorge
                  utflyttingFraNorge={gjenlevende.opplysning.utland?.utflyttingFraNorge}
                  alleLand={alleLand}
                />
              </Table.DataCell>
              <Table.DataCell>
                {!!gjenlevende.opplysning.statsborgerskap
                  ? finnLandSomTekst(gjenlevende.opplysning.statsborgerskap, alleLand)
                  : 'Ingen statsborgerskap'}
              </Table.DataCell>
              <Table.DataCell>{formaterSivilstatusTilLesbarStreng(gjenlevende.opplysning.sivilstatus)}</Table.DataCell>
            </Table.Row>
          ) : (
            <Table.Row>
              <Table.DataCell colSpan={7}>
                <Heading size="small"> Ingen gjenlevende</Heading>
              </Table.DataCell>
            </Table.Row>
          )}
        </Table.Body>
      </Table>
    </VStack>
  )
}
