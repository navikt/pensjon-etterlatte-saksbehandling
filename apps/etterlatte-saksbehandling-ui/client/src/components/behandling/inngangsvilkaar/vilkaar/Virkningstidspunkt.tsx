import format from 'date-fns/format'
import {
  Innhold,
  Lovtekst,
  Title,
  VilkaarBorder,
  VilkaarColumn,
  VilkaarInfobokser,
  VilkaarVurderingColumn,
  VilkaarWrapper,
} from '../styled'
import {
  IBehandlingsType,
  IVilkaarsproving,
  KriterieOpplysningsType,
  Kriterietype,
} from '../../../../store/reducers/BehandlingReducer'
import { hentKriterierMedOpplysning } from '../../felles/utils'
import { KildeDatoOpplysning } from './KildeDatoOpplysning'

type Props = {
  id: any
  vilkaar: IVilkaarsproving | undefined
  virkningsdato: string
  mottattdato: string
  behandlingType: IBehandlingsType
}

const lovtekst: Record<IBehandlingsType, Record<'tittel' | 'tekst', string>> = {
  [IBehandlingsType.FØRSTEGANGSBEHANDLING]: {
    tittel: '§ 22-13: Virkningstidspunkt',
    tekst:
      'Barnepensjonen innvilges som hovedregel fra og med måneden etter dødsfall, men kan gis for opptil tre år før den måneden da kravet ble satt fram.',
  },
  [IBehandlingsType.REVURDERING]: {
    tittel: '§ 22-12: Virkningstidspunkt',
    tekst:
      'Det følger av paragrafens sjette ledd at utbetalingen av en ytelse som gis pr. måned, stanses ved utgangen av den måneden retten til ytelsen faller bort.',
  },
  [IBehandlingsType.MANUELT_OPPHOER]: {
    tittel: 'Virkningstidspunkt manuelt opphør',
    tekst:
      'Virkningstidspunkt for opphøret blir satt til første virkningstidspunkt fra systemet, slik at alle utbetalingene annuleres',
  },
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
              <Title>{lovtekst[props.behandlingType].tittel}</Title>
              <Lovtekst>{lovtekst[props.behandlingType].tekst}</Lovtekst>
            </VilkaarColumn>
            {props.behandlingType === IBehandlingsType.FØRSTEGANGSBEHANDLING ? (
              <>
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
              </>
            ) : null}
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
