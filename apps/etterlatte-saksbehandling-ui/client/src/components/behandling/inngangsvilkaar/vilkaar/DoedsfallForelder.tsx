import format from 'date-fns/format'
import { StatusIcon } from '../../../../shared/icons/statusIcon'
import { VilkaarVurderingsResultat } from '../../../../store/reducers/BehandlingReducer'
import { Title, VilkaarColumn, VilkaarWrapper } from '../styled'
import { OpplysningsType } from '../types'
import { IBehandlingsopplysning, IKriterie, Kriterietype } from '../../../../store/reducers/BehandlingReducer'
import { VilkaarIkkeOppfylt } from './VilkaarIkkeOppfylt'

export const DoedsFallForelder = (props: any) => {
  const vilkaar = props.vilkaar

  const avdoedDoedsdato = vilkaar.kriterier
    .find((krit: IKriterie) => krit.navn === Kriterietype.DOEDSFALL_ER_REGISTRERT_I_PDL)
    .basertPaaOpplysninger.find(
      (opplysning: IBehandlingsopplysning) => opplysning.opplysningsType === OpplysningsType.avdoed_doedsfall
    )

  const forelder = vilkaar.kriterier
    .find((krit: IKriterie) => krit.navn === Kriterietype.AVDOED_ER_FORELDER)
    .basertPaaOpplysninger.find(
      (opplysning: IBehandlingsopplysning) => opplysning.opplysningsType === OpplysningsType.relasjon_foreldre
    )

  return (
    <div style={{ borderBottom: '1px solid #ccc', padding: '1em 0' }}>
      <VilkaarWrapper>
        <VilkaarColumn>
          <Title>
            <StatusIcon status={VilkaarVurderingsResultat.OPPFYLT} /> Dødsfall forelder
          </Title>
          <div>§ 18-5</div>
          <div>En eller begge foreldrene døde</div>
        </VilkaarColumn>
        <VilkaarColumn>
          <div>
            <strong>Dødsdato</strong>
          </div>
          <div>{avdoedDoedsdato?.opplysning?.doedsdato ? format(new Date(avdoedDoedsdato.opplysning.doedsdato), 'dd.MM.yyyy') : <span className="missing">mangler</span>}</div>
        </VilkaarColumn>
        <VilkaarColumn>
          <div>
            <strong>Avdød forelder</strong>
          </div>
          <div>
            {forelder?.opplysning?.foreldre ? forelder.opplysning.foreldre : <span className="missing">mangler</span>}
          </div>
          <div>{avdoedDoedsdato.opplysning.foedselsnummer}</div>
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
          <VilkaarIkkeOppfylt
            status={props.vilkaar.resultat}
            errorText="Vi har bla bla bla fått bla bla bla som sier at bla"
          />
        </VilkaarColumn>
      </VilkaarWrapper>
    </div>
  )
}
