import styled from 'styled-components'
import { Soeknadsvurdering } from '~components/behandling/soeknadsoversikt/soeknadoversikt/SoeknadsVurdering'

const Trygdetid = () => {
  return (
    <TrygdetidWrapper>
      <Soeknadsvurdering
        tittel={'Trygdetid'}
        hjemler={[
          {
            tittel: '§ 3-5 Trygdetid ved beregning av ytelser',
            lenke: 'https://lovdata.no/lov/1997-02-28-19/§3-5',
          },
        ]}
        status={null}
      >
        <TrygdetidInfo>
          <p>
            Trygdetiden er minst 40 år som følge av faktisk trygdetid og fremtidig trygdetid. Faktisk trygdetid er den
            tiden fra avdøde fylte 16 til personen døde. Fremtidig trygdetid er tiden fra dødsfallet til og med
            kalenderåret avdøde hadde blitt 66 år. Saksbehandler bekrefter at følgende stemmer for denne behandlingen
            ved å gå videre til beregning:
          </p>
          <p>
            Trygdetid: <strong>40 år</strong>
          </p>
        </TrygdetidInfo>
      </Soeknadsvurdering>
    </TrygdetidWrapper>
  )
}

const TrygdetidWrapper = styled.form`
  padding: 0em 4em;
  max-width: 56em;
`

const TrygdetidInfo = styled.div`
  display: flex;
  flex-direction: column;
`

export default Trygdetid
