import { StatusIcon } from '../../../../shared/icons/statusIcon'
import { VilkaarVurderingsResultat } from '../../../../store/reducers/BehandlingReducer'
import { Title, VilkaarColumn } from '../styled'

export const AvdoedesForutMedlemskap = (props: any) => {
  const grunnlag = props.vilkaar.grunnlag

  return (
    <>
      <VilkaarColumn style={{ width: '100%', flex: 'auto' }}>
        <Title>
          <StatusIcon status={props.vilkaar.vilkaarDone} /> Avdødes forutgående medlemsskap
        </Title>
        <div>§ 18-5</div>
        <div>Den avdøde var medlem av trygden eller mottok pensjon/uføretrygd de siste 5 årene før dødsfallet</div>
      </VilkaarColumn>
      <VilkaarColumn>
        <Title>Bosted</Title>
        <div>{grunnlag.opplysning.doedsdato}</div>
        <div>
          <strong>Bostedsardresse</strong>
        </div>
        <div>Veien 123</div>
        <div>0088 Oslo</div>
        <div>(jan 2019 - jan 2022</div>
      </VilkaarColumn>
      <VilkaarColumn>
        <Title>Opptjening</Title>
        <div>Ola Nilsen Normann</div>
        <div>090248 54688</div>
        <div>
          <strong>Arbeidsperioder</strong>
        </div>
        <div>Tonsenhagen skole</div>
        <div>100% stilling</div>
        <div>(2015 - 2022)</div>
      </VilkaarColumn>
      <VilkaarColumn>
        <Title>Opphold utenfor Norge</Title>
        <div>
          <strong>Opphold utenfor Norge etter fylte 16 år</strong>
        </div>
        <div>Ja</div>
        <div>
          <strong>Land</strong>
        </div>
        <div>TYSKLAND</div>
        <div>(13.04.1978 til 20.08.2006)</div>
      </VilkaarColumn>
      <VilkaarColumn>
        <Title>Pensjon uføretrygd</Title>
        <div>
          <strong>Mottok pensjon/uføretrygd</strong>
        </div>
        <div>Ja (1994 til 1996)</div>
      </VilkaarColumn>
      <VilkaarColumn>
        <Title>
          Vilkår er{' '}
          {props.vilkaar.resultat === VilkaarVurderingsResultat.KAN_IKKE_VURDERE_PGA_MANGLENDE_OPPLYSNING ? (
            <> ikke oppfyllt</>
          ) : (
            <> oppfyllt</>
          )}
        </Title>
      </VilkaarColumn>
    </>
  )
}
