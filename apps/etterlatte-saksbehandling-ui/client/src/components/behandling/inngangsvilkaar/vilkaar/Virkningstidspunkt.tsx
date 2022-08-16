import format from 'date-fns/format'
import {
  Innhold,
  Lovtekst,
  Title,
  VilkaarBorder,
  VilkaarColumn,
  VilkaarVurderingColumn,
  VilkaarInfobokser,
  VilkaarWrapper,
} from '../styled'
import { IVilkaarsproving, KriterieOpplysningsType, Kriterietype } from '../../../../store/reducers/BehandlingReducer'
import { hentKriterierMedOpplysning } from '../../felles/utils'
import { KildeDatoOpplysning } from './KildeDatoOpplysning'

type Props = {
  id: any
  vilkaar: IVilkaarsproving | undefined
  virkningsdato: string
  mottattdato: string
}

export const Virkningstidspunkt = (props: Props) => {
  const vilkaar = props.vilkaar

  const avdoedDoedsdato = hentKriterierMedOpplysning(
    vilkaar,
    Kriterietype.SOEKER_ER_UNDER_20_PAA_VIRKNINGSDATO,
    KriterieOpplysningsType.DOEDSDATO
  )

  return (
    <VilkaarBorder id={props.id}>
      <Innhold>
        <VilkaarWrapper>
          <VilkaarInfobokser>
            <VilkaarColumn>
              <Title>§ 22-13: Virkningstidspunkt</Title>
              <Lovtekst>
                Barnepensjonen innvilges som hovedregel fra og med måneden etter dødsfall, men kan gis for opptil tre år
                før den måneden da kravet ble satt fram.
              </Lovtekst>
            </VilkaarColumn>
            <VilkaarColumn>
              <div>
                <strong>Dødsdato</strong>
              </div>
              <div>
                {avdoedDoedsdato ? (
                  <>
                    <div>{format(new Date(avdoedDoedsdato?.opplysning.doedsdato), 'dd.MM.yyyy')}</div>
                    <KildeDatoOpplysning
                      type={avdoedDoedsdato?.kilde.type}
                      dato={avdoedDoedsdato?.kilde.tidspunktForInnhenting}
                    />
                  </>
                ) : (
                  <span className="missing">mangler</span>
                )}
              </div>
            </VilkaarColumn>
            <VilkaarColumn>
              <div>
                <strong>Søknadstidspunkt</strong>
              </div>
              <div>{format(new Date(props.mottattdato), 'dd.MM.yyyy')}</div>
            </VilkaarColumn>
            <VilkaarColumn>
              <div>
                <strong>Virkningstidspunkt</strong>
              </div>
              <div>{format(new Date(props.virkningsdato), 'dd.MM.yyyy')}</div>
            </VilkaarColumn>
          </VilkaarInfobokser>
          <VilkaarVurderingColumn></VilkaarVurderingColumn>
        </VilkaarWrapper>
      </Innhold>
    </VilkaarBorder>
  )
}
