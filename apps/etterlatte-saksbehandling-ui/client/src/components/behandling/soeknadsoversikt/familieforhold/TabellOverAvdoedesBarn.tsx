import { formaterNavn, hentLevendeSoeskenFraAvdoedeForSoeker, IPdlPerson } from '~shared/types/Person'
import { BodyShort, Heading, HStack, Table, VStack } from '@navikt/ds-react'
import { ChildHairEyesIcon } from '@navikt/aksel-icons'
import React from 'react'
import { hentAlderForDato } from '~components/behandling/felles/utils'
import { KopierbarVerdi } from '~shared/statusbar/KopierbarVerdi'
import { PdlPersonAktivEllerSisteAdresse } from '~components/behandling/soeknadsoversikt/familieforhold/AktivEllerSisteAdresse'
import { BarnAddressePeriode } from '~components/behandling/soeknadsoversikt/familieforhold/BarnAddressePeriode'
import { Personopplysninger } from '~shared/types/grunnlag'
import { SakType } from '~shared/types/sak'
import { DoedsdatoTag } from '~shared/tags/DoedsdatoTag'
import { usePersonopplysninger } from '~components/person/usePersonopplysninger'

interface Props {
  sakType: SakType
}

function erBarnTilPerson(person: IPdlPerson, barnIdent: string): boolean {
  return !!(person.familieRelasjon?.barn ?? []).find((ident) => ident == barnIdent)
}

function hvemErBarnetsForeldre(personopplysninger: Personopplysninger, barn: IPdlPerson): string {
  if (personopplysninger.avdoede.length >= 2) {
    const [avdoedEn, avdoedTo] = personopplysninger.avdoede
    const harAvdoedEnSomForelder = erBarnTilPerson(avdoedEn.opplysning, barn.foedselsnummer)
    const harAvdoedToSomForelder = erBarnTilPerson(avdoedTo.opplysning, barn.foedselsnummer)

    if (harAvdoedEnSomForelder && harAvdoedToSomForelder) {
      return 'Begge avdøde'
    } else if (harAvdoedEnSomForelder) {
      return `Kun ${formaterNavn(avdoedEn.opplysning)}`
    } else {
      return `Kun ${formaterNavn(avdoedTo.opplysning)}`
    }
  } else {
    const harGjenlevendeSomForelder = personopplysninger.gjenlevende.some((gjenlevende) =>
      erBarnTilPerson(gjenlevende.opplysning, barn.foedselsnummer)
    )
    if (harGjenlevendeSomForelder) {
      return 'Avdød og gjenlevende'
    } else {
      return 'Kun avdød'
    }
  }
}

export const TabellOverAvdoedesBarn = ({ sakType }: Props) => {
  const opplysninger = usePersonopplysninger()
  if (!opplysninger) {
    return null
  }
  const avdoedesBarn = hentLevendeSoeskenFraAvdoedeForSoeker(
    opplysninger.avdoede,
    opplysninger.soeker?.opplysning?.foedselsnummer
  )

  return (
    <VStack gap="4">
      <HStack gap="4" justify="start" align="center" wrap={false}>
        <ChildHairEyesIcon fontSize="1.75rem" aria-hidden />
        <Heading size="small" level="3">
          {sakType === SakType.OMSTILLINGSSTOENAD ? 'Avdødes barn' : 'Avdødes barn (søsken)'}
        </Heading>
      </HStack>
      <Table size="small">
        <Table.Header>
          <Table.Row>
            <Table.HeaderCell scope="col">Navn</Table.HeaderCell>
            <Table.HeaderCell scope="col">Fødselsnummer</Table.HeaderCell>
            <Table.HeaderCell scope="col">Bostedsadresse</Table.HeaderCell>
            <Table.HeaderCell scope="col">Periode</Table.HeaderCell>
            <Table.HeaderCell scope="col">Foreldre</Table.HeaderCell>
          </Table.Row>
        </Table.Header>
        <Table.Body>
          {!!avdoedesBarn.length ? (
            avdoedesBarn.map((barn, index) => (
              <Table.Row key={index}>
                <Table.DataCell>
                  <HStack gap="2" justify="start" align="center" wrap={false}>
                    <BodyShort>{`${formaterNavn(barn)} (${hentAlderForDato(barn.foedselsdato)} år)`}</BodyShort>
                    <DoedsdatoTag doedsdato={barn.doedsdato} />
                  </HStack>
                </Table.DataCell>
                <Table.DataCell>
                  <KopierbarVerdi value={barn.foedselsnummer} iconPosition="right" />
                </Table.DataCell>
                <Table.DataCell>
                  <PdlPersonAktivEllerSisteAdresse person={barn} />
                </Table.DataCell>
                <Table.DataCell>
                  <BarnAddressePeriode barn={barn} />
                </Table.DataCell>
                <Table.DataCell>{hvemErBarnetsForeldre(opplysninger, barn)}</Table.DataCell>
              </Table.Row>
            ))
          ) : (
            <Table.Row>
              <Table.DataCell colSpan={5}>
                <Heading size="small">
                  {sakType === SakType.OMSTILLINGSSTOENAD
                    ? 'Avdøde har ingen barn'
                    : 'Avdøde har ingen andre barn enn søker'}
                </Heading>
              </Table.DataCell>
            </Table.Row>
          )}
        </Table.Body>
      </Table>
    </VStack>
  )
}
