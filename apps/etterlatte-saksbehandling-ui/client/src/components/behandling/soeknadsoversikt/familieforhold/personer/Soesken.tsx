import {RelatertPersonsRolle} from '../../../types'
import {PersonInfoFnr} from './personinfo/PersonInfoFnr'
import {PersonBorder, PersonHeader, PersonInfoWrapper} from '../styled'
import {ChildIcon} from '../../../../../shared/icons/childIcon'
import {DashedBorder, TypeStatusWrap} from '../../styled'
import {IPdlPerson, IPersoninfoAvdoed, IPersoninfoSoeker} from '../../../../../store/reducers/BehandlingReducer'
import {PersonInfoAdresse} from './personinfo/PersonInfoAdresse'
import {hentAdresserEtterDoedsdato} from '../../../felles/utils'
import {Heading} from "@navikt/ds-react";
import {hentAlderVedDoedsdato} from "../../utils";
import React from "react";

type Props = {
    soeker: IPersoninfoSoeker
    avdoedesBarn: IPdlPerson[]
    avdoed: IPersoninfoAvdoed
}

export const Soesken: React.FC<Props> = ({soeker, avdoedesBarn, avdoed}) => {
    const soesken = avdoedesBarn?.filter(barn => barn.foedselsnummer !== soeker.fnr)

    return (
        <>
            <br />
            <Heading spacing size="small" level="5">
                Søsken (avdødes barn)
            </Heading>

            {soesken?.map(person => (
                <>
                    <PersonBorder key={person.foedselsnummer}>
                        <PersonHeader>
                        <span className="icon">
                          <ChildIcon/>
                        </span>
                            {`${person.fornavn} ${person.etternavn}`} <span className="personRolle">({RelatertPersonsRolle.SOESKEN})</span>
                            <TypeStatusWrap type="barn">
                                {hentAlderVedDoedsdato(person.foedselsdato.toString(), avdoed.doedsdato)} år på dødsdatoen
                            </TypeStatusWrap>
                        </PersonHeader>
                        <PersonInfoWrapper>
                            <PersonInfoFnr fnr={person.foedselsnummer}/>
                            <PersonInfoAdresse
                                adresser={hentAdresserEtterDoedsdato(person.bostedsadresse!!, avdoed.doedsdato)}
                                visHistorikk={true}/>
                        </PersonInfoWrapper>
                    </PersonBorder>
                    <DashedBorder />
                </>
            ))}

            {(!soesken || soesken.length == 0) && (
                <p>Det er ikke registrert noen andre barn på avdøde</p>
            )}
        </>
    )
}
