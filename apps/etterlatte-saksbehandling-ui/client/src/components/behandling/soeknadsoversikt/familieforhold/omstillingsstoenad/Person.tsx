import { PersonIcon } from '@navikt/aksel-icons'
import { PersonInfoAdresse } from '../personer/personinfo/PersonInfoAdresse'
import { BodyShort, CopyButton, Detail, Heading, HStack, Label } from '@navikt/ds-react'
import styled from 'styled-components'
import { IconSize } from '~shared/types/Icon'
import { GrunnlagKilde } from '~shared/types/grunnlag'
import { IPdlPerson } from '~shared/types/Person'
import { Utlandsopphold } from '~components/behandling/soeknadsoversikt/familieforhold/personer/personinfo/UtvandringInnvandring'
import { StatsborgerskapVisning } from '~components/behandling/soeknadsoversikt/familieforhold/personer/personinfo/StatsborgerskapVisning'
import { Result } from '~shared/api/apiUtils'
import { ILand } from '~shared/api/trygdetid'
import { formaterFnr } from '~utils/formatering/formatering'
import { formaterDato } from '~utils/formatering/dato'
import { NavLink } from 'react-router-dom'

const PersonBorder = styled.div`
  padding: 1.2em 1em 1em 0em;
  display: flex;
`

const IconWrapper = styled.span`
  width: 2.5rem;
`

const PersonInfoWrapper = styled.div`
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
`

type Props = {
  person: IPdlPerson
  kilde: GrunnlagKilde
  avdoed?: boolean
  landListeResult: Result<ILand[]>
}

export const Person = ({ person, kilde, avdoed = false, landListeResult }: Props) => {
  return (
    <PersonBorder>
      <IconWrapper>
        <PersonIcon fontSize={IconSize.DEFAULT} />
      </IconWrapper>
      <PersonInfoWrapper>
        <Heading size="small" level="3">
          {avdoed ? 'Avdød' : 'Gjenlevende'}
        </Heading>
        <BodyShort>
          {person.fornavn} {person.etternavn}
          <HStack>
            <NavLink to="/person" state={{ fnr: person.foedselsnummer }} target="_blank" rel="noreferrer noopener">
              ({formaterFnr(person.foedselsnummer)})
            </NavLink>
            <CopyButton copyText={person.foedselsnummer} size="small" />
          </HStack>
        </BodyShort>
        <div>
          <PersonInfoAdresse adresser={person.bostedsadresse} visHistorikk={false} adresseDoedstidspunkt={avdoed} />
        </div>
        {person.utland && <Utlandsopphold utland={person.utland} />}
        <StatsborgerskapVisning
          statsborgerskap={person.statsborgerskap}
          pdlStatsborgerskap={person.pdlStatsborgerskap}
          landListeResult={landListeResult}
        />
        <Detail>
          <Label size="small" as="p">
            Kilde
          </Label>
          {kilde.type.toUpperCase()} {formaterDato(kilde.tidspunkt)}
        </Detail>
      </PersonInfoWrapper>
    </PersonBorder>
  )
}
