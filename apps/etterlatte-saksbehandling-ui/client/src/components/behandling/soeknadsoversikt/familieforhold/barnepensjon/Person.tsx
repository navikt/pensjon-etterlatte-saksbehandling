import { IPdlPerson } from '~shared/types/Person'
import { PersonIcon } from '@navikt/aksel-icons'
import { PersonInfoAdresse } from '../personer/personinfo/PersonInfoAdresse'
import { CopyButton, Detail, Heading, Label, Link } from '@navikt/ds-react'
import styled from 'styled-components'
import { formaterDato } from '~utils/formatering/dato'
import { formaterFnr } from '~utils/formatering/formatering'
import { IconSize } from '~shared/types/Icon'
import { GrunnlagKilde } from '~shared/types/grunnlag'
import { Utlandsopphold } from '~components/behandling/soeknadsoversikt/familieforhold/personer/personinfo/UtvandringInnvandring'
import { StatsborgerskapVisning } from '~components/behandling/soeknadsoversikt/familieforhold/personer/personinfo/StatsborgerskapVisning'
import { ILand } from '~shared/api/trygdetid'
import { Result } from '~shared/api/apiUtils'
import { useAppSelector } from '~store/Store'

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
  mottaker?: boolean
  gjenlevende?: boolean
  landListeResult: Result<ILand[]>
}

export const Person = ({
  person,
  kilde,
  avdoed = false,
  mottaker = false,
  gjenlevende = false,
  landListeResult,
}: Props) => {
  const getHeaderTekst = () => {
    if (avdoed) {
      return 'Avdød forelder'
    } else if (mottaker) {
      return 'Mottaker'
    } else if (gjenlevende) {
      return 'Gjenlevende forelder'
    }
  }

  const saksId = useAppSelector((state) => state.behandlingReducer.behandling?.sakId)

  return (
    <PersonBorder>
      <IconWrapper>
        <PersonIcon fontSize={IconSize.DEFAULT} />
      </IconWrapper>
      <PersonInfoWrapper>
        <Heading size="small" level="3">
          {getHeaderTekst()}
        </Heading>
        <div>
          {person.fornavn} {person.etternavn}
          <FnrWrapper>
            <Link href={`/sak/${saksId}`} target="_blank" rel="noreferrer noopener">
              ({formaterFnr(person.foedselsnummer)})
            </Link>
            <CopyButton copyText={person.foedselsnummer} size="small" />
          </FnrWrapper>
        </div>
        <div>
          <PersonInfoAdresse adresser={person.bostedsadresse} visHistorikk={false} adresseDoedstidspunkt={avdoed} />
        </div>
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
      </PersonInfoWrapper>
    </PersonBorder>
  )
}

const FnrWrapper = styled.div`
  display: flex;
`
