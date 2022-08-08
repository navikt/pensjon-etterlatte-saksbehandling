import {PersonInfoFnr} from './personinfo/PersonInfoFnr'
import {PersonBorder, PersonHeader, PersonInfoWrapper} from '../styled'
import {ChildIcon} from '../../../../../shared/icons/childIcon'
import {
    IFamilieforhold,
    IPersoninfoSoeker
} from '../../../../../store/reducers/BehandlingReducer'
import {PersonInfoAdresse} from './personinfo/PersonInfoAdresse'
import {hentAdresserEtterDoedsdato} from '../../../felles/utils'
import {Heading} from "@navikt/ds-react";
import React from "react";
import differenceInYears from "date-fns/differenceInYears";

type Props = {
    soeker: IPersoninfoSoeker
    familieforhold: IFamilieforhold
}

export const Soesken: React.FC<Props> = ({soeker, familieforhold}) => {
    const soesken = familieforhold.avdoede?.opplysning.avdoedesBarn?.filter(barn => barn.foedselsnummer !== soeker.fnr)

    const erHelsoesken = (fnr: string) => familieforhold.gjenlevende?.opplysning.familieRelasjon?.barn?.includes(fnr)

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
                            <span className={"personInfo"}>{erHelsoesken(person.foedselsnummer) ? "Helsøsken" : "Halvsøsken"}</span>
                        </PersonHeader>
                        <PersonInfoWrapper>
                            <PersonInfoFnr fnr={person.foedselsnummer}/>
                            <PersonInfoAdresse
                                adresser={hentAdresserEtterDoedsdato(person.bostedsadresse!!, familieforhold.avdoede.opplysning.doedsdato.toString())}
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
