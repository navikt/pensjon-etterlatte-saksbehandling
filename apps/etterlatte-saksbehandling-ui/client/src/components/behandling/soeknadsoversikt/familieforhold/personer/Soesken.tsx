import {PersonInfoFnr} from './personinfo/PersonInfoFnr'
import {PersonBorder, PersonHeader, PersonInfoWrapper} from '../styled'
import {ChildIcon} from '../../../../../shared/icons/childIcon'
import {IPdlPerson, IPersoninfoAvdoed, IPersoninfoSoeker} from '../../../../../store/reducers/BehandlingReducer'
import {PersonInfoAdresse} from './personinfo/PersonInfoAdresse'
import {hentAdresserEtterDoedsdato} from '../../../felles/utils'
import {Heading} from "@navikt/ds-react";
import React from "react";
import differenceInYears from "date-fns/differenceInYears";

type Props = {
    soeker: IPersoninfoSoeker
    avdoedesBarn: IPdlPerson[]
    avdoed: IPersoninfoAvdoed
}

export const Soesken: React.FC<Props> = ({soeker, avdoedesBarn, avdoed}) => {
    const soesken = avdoedesBarn?.filter(barn => barn.foedselsnummer !== soeker.fnr)

    return (
        <>
            <br/>
            <Heading spacing size="small" level="5">
                Søsken <span style={{fontWeight: "normal"}}>(avdødes barn)</span>
            </Heading>

            {soesken?.map(person => (
                <>
                    <PersonBorder key={person.foedselsnummer}>
                        <PersonHeader>
                            <span className="icon">
                              <ChildIcon/>
                            </span>
                            {`${person.fornavn} ${person.etternavn}`} <span className={"personRolle"}>({differenceInYears(new Date(), new Date(person.foedselsdato))} år)</span>
                            <br/>
                            {/*<span className={"personInfo"}>Helsøsken/Halvsøsken</span>*/}
                        </PersonHeader>
                        <PersonInfoWrapper>
                            <PersonInfoFnr fnr={person.foedselsnummer}/>
                            <PersonInfoAdresse
                                adresser={hentAdresserEtterDoedsdato(person.bostedsadresse!!, avdoed.doedsdato)}
                                visHistorikk={true}/>
                        </PersonInfoWrapper>
                    </PersonBorder>
                </>
            ))}

            {(!soesken || soesken.length == 0) && (
                <p>Det er ikke registrert noen andre barn på avdøde</p>
            )}
        </>
    )
}
