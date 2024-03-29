import React from 'react'
import styled from 'styled-components'
import { Border } from '~components/behandling/soeknadsoversikt/styled'
import { IAvkortetYtelse } from '~shared/types/IAvkorting'
import { BodyShort, ReadMore } from '@navikt/ds-react'
import { NOK } from '~utils/formattering'

export const YtelseEtterAvkortingDetaljer = (props: { ytelse: IAvkortetYtelse }) => {
  const ytelse = props.ytelse

  return (
    <>
      <Border />
      <Wrapper>
        <ul>
          <Rad>
            <Navn>Brutto stønad før avkorting</Navn>
            <Verdi>{NOK(ytelse.ytelseFoerAvkorting)}</Verdi>
          </Rad>
          <Rad>
            <Navn>Månedlig avkortingsbeløp</Navn>
            <Operasjon>-</Operasjon>
            <Verdi>{NOK(ytelse.avkortingsbeloep)}</Verdi>
          </Rad>
          {ytelse.restanse < 0 ? (
            <Rad>
              <Navn>Månedlig restansebeløp</Navn>
              <Operasjon>+</Operasjon>
              <Verdi>{NOK(ytelse.restanse * -1)}</Verdi>
            </Rad>
          ) : (
            <Rad>
              <Navn>Månedlig restansebeløp</Navn>
              <Operasjon>-</Operasjon>
              <Verdi>{NOK(ytelse.restanse)} kr</Verdi>
            </Rad>
          )}
          <Border />
          <Rad>
            <Navn>Brutto stønad etter avkorting og restanse</Navn>
            <Operasjon>=</Operasjon>
            <Verdi>
              <strong>{NOK(ytelse.ytelseEtterAvkorting)}</strong>
            </Verdi>
          </Rad>
        </ul>
        <Beskrivelse>
          <li>
            <h4>Hvordan fungerer avkorting av en omstillingstønad?</h4>
            <ReadMore header="Les mer">
              <BodyShort size="small">
                Omstillingsstønaden avkortes med mottakers forventede årsinntekt/12. Er forventet inntekt satt for
                lavt/høyt, og en eventuell restanse (se under) ikke klarer å hente inn igjen for mye/lite utbetalt
                stønad, vil dette bli behandlet i et etteroppgjør.
              </BodyShort>
            </ReadMore>
          </li>
          <li>
            <h4>Hva er restanse?</h4>
            <ReadMore header="Les mer">
              <BodyShort spacing={true} size="small">
                Hvis man fastsetter en ny forventet inntekt for inneværende år, oppstår det en
                feilutbetaling/etterbetaling, en restanse. Restansen skal fordeles ut over resterende utbetalingsmåneder
                for inneværende år. Dette gjøres for å minimere etteroppgjøret.
              </BodyShort>
              <BodyShort size="small">
                Det er antall gjenstående måneder som avgjør hvor stort restansebeløpet blir. Har man f.eks. endret
                forventet inntekt fra 01.07.2023 og det foreligger en feilutbetaling på 15 000 kroner, skal beløpet
                fordeles på 6 måneder, og restansebeløpet blir 2 500 kr.
              </BodyShort>
            </ReadMore>
          </li>
        </Beskrivelse>
      </Wrapper>
    </>
  )
}

const Wrapper = styled.div`
  margin-top: 2em;
  display: flex;

  font-size: 0.9em;
`
const Rad = styled.li`
  margin-bottom: 1.25em;
  overflow: hidden;
`
const Navn = styled.div`
  float: left;
  width: 18em;
`
const Operasjon = styled.div`
  float: left;
  width: 5em;
`
const Verdi = styled.div`
  float: right;
  width: 5em;
`
const Beskrivelse = styled.div`
  flex-grow: 1;
  margin-left: 1em;
  list-style-type: none;
  width: 10em;

  h4 {
    margin: 0;
  }

  li {
    margin-bottom: 2em;
  }
`
