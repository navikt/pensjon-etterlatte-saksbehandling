import format from 'date-fns/format';
import { StatusIcon } from '../../../../shared/icons/statusIcon'
import { Title, VilkaarColumn, VilkaarWrapper } from '../styled'
import { OpplysningsType, Status } from '../types'

export const DoedsFallForelder = (props: any) => {
  const vilkaar = props.vilkaar;

  const avdoedDoedsdato = vilkaar.kriterier.find((krit: any) => krit.navn === 'AVDOED_ER_FORELDER')
  .basertPaaOpplysninger.find((opplysning: any) => opplysning.opplysingType === OpplysningsType.doedsdato)

  const forelder = vilkaar.kriterier.find((krit: any) => krit.navn === 'AVDOED_ER_FORELDER')
  .basertPaaOpplysninger.find((opplysning: any) => opplysning.opplysingType === OpplysningsType.relasjon_foreldre)

  console.log(forelder.opplysning.foreldre);

  return (
    <div style={{ borderBottom: '1px solid #ccc', padding: '1em 0' }}>
      <VilkaarWrapper>
        <VilkaarColumn>
          <Title>
            <StatusIcon status={Status.NOT_DONE} /> Dødsfall forelder
          </Title>
          <div>§ 18-5</div>
          <div>En eller begge foreldrene døde</div>
        </VilkaarColumn>
        <VilkaarColumn>
          <div>
            <strong>Dødsdato</strong>
          </div>
          <div>{format(new Date(avdoedDoedsdato.opplysning.doedsdato), 'dd.MM.yyyy')}</div>
        </VilkaarColumn>
        <VilkaarColumn>
          <div>
            <strong>Avdød forelder</strong>
          </div>
          <div>{forelder.opplysning.foreldre ? forelder.opplysning.foreldre : "mangler info"}</div>
          <div>{avdoedDoedsdato.opplysning.foedselsnummer}</div>
        </VilkaarColumn>
        <VilkaarColumn>
          <Title>Vilkår er {props.vilkaar.resultat}</Title>
        </VilkaarColumn>
      </VilkaarWrapper>
    </div>
  )
}
