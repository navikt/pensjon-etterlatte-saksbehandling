import { PersonIcon } from '@navikt/aksel-icons'
import { PersonInfoAdresse } from '../personer/personinfo/PersonInfoAdresse'
import { BodyShort, CopyButton, Detail, Heading, HStack, Label, VStack } from '@navikt/ds-react'
import { IconSize } from '~shared/types/Icon'
import { GrunnlagKilde } from '~shared/types/grunnlag'
import { IPdlPerson } from '~shared/types/Person'
import { Utlandsopphold } from '~components/behandling/soeknadsoversikt/familieforhold/personer/personinfo/UtvandringInnvandring'
import { StatsborgerskapVisning } from '~components/behandling/soeknadsoversikt/familieforhold/personer/personinfo/StatsborgerskapVisning'
import { Result } from '~shared/api/apiUtils'
import { formaterFnr } from '~utils/formatering/formatering'
import { formaterDato } from '~utils/formatering/dato'
import { PersonLink } from '~components/person/lenker/PersonLink'
import { ILand } from '~utils/kodeverk'

type Props = {
  person: IPdlPerson
  kilde: GrunnlagKilde
  avdoed?: boolean
  landListeResult: Result<ILand[]>
}

export const Person = ({ person, kilde, avdoed = false, landListeResult }: Props) => {
  return (
    <HStack gap="2" margin="1">
      <PersonIcon fontSize={IconSize.DEFAULT} />

      <VStack gap="1">
        <Heading size="small" level="3">
          {avdoed ? 'Avdød' : 'Gjenlevende'}
        </Heading>
        <VStack>
          <BodyShort>
            {person.fornavn} {person.etternavn}
          </BodyShort>
          <HStack>
            <PersonLink fnr={person.foedselsnummer} target="_blank" rel="noreferrer noopener">
              ({formaterFnr(person.foedselsnummer)})
            </PersonLink>
            <CopyButton copyText={person.foedselsnummer} size="small" />
          </HStack>
        </VStack>

        <PersonInfoAdresse adresser={person.bostedsadresse} visHistorikk={false} adresseDoedstidspunkt={avdoed} />

        {person.utland && <Utlandsopphold utland={person.utland} />}

        <StatsborgerskapVisning
          statsborgerskap={person.statsborgerskap}
          pdlStatsborgerskap={person.pdlStatsborgerskap}
          landListeResult={landListeResult}
        />

        <Detail as="div">
          <Label size="small" as="p">
            Kilde
          </Label>
          {kilde.type.toUpperCase()} {formaterDato(kilde.tidspunkt)}
        </Detail>
      </VStack>
    </HStack>
  )
}
