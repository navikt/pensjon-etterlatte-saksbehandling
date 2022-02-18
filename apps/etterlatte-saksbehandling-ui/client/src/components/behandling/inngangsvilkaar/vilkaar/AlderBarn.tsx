import { StatusIcon } from '../../../../shared/icons/statusIcon'
import differenceInYears from 'date-fns/differenceInYears'
import format from 'date-fns/format'
import { Title, VilkaarColumn, VilkaarWrapper } from '../styled'
import { OpplysningsType, Status } from '../types'

export const AlderBarn = (props: any) => {
  const vilkaar = props.vilkaar

  const barnetsFoedselsdato = vilkaar.kriterier
    .find((krit: any) => krit.navn === 'SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO')
    .basertPaaOpplysninger.find((opplysning: any) => opplysning.opplysingType === OpplysningsType.soeker_foedselsdato)

  const avdoedDoedsdato = vilkaar.kriterier
    .find((krit: any) => krit.navn === 'SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO')
    .basertPaaOpplysninger.find((opplysning: any) => opplysning.opplysingType === OpplysningsType.doedsdato)

  const barnetsAlder = differenceInYears(
    new Date(avdoedDoedsdato.opplysning.doedsdato),
    new Date(barnetsFoedselsdato.opplysning.foedselsdato)
  )

  return (
    <div style={{ borderBottom: '1px solid #ccc', padding: '1em 0' }}>
      <VilkaarWrapper>
        <VilkaarColumn>
          <Title>
            <StatusIcon status={props.vilkaar.resultat === 'OPPFYLT' ? Status.DONE : Status.NOT_DONE} /> Alder barn
          </Title>
          <div>§ 18-5</div>
          <div>Barnet er under 20 år</div>
        </VilkaarColumn>
        <VilkaarColumn>
          <div>
            <strong>Barnets fødselsdato</strong>
          </div>
          <div>{format(new Date(barnetsFoedselsdato.opplysning.foedselsdato), 'dd.MM.yyyy')}</div>
        </VilkaarColumn>
        <VilkaarColumn>
          <div>
            <strong>Alder ved dødsfall</strong>
          </div>
          <div>{barnetsAlder ? `${barnetsAlder} år` : 'mangler info'}</div>
        </VilkaarColumn>
        <VilkaarColumn>
          <Title>Vilkår er {props.vilkaar.resultat}</Title>
        </VilkaarColumn>
      </VilkaarWrapper>
    </div>
  )
}
