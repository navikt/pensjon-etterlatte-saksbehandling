import { HoeyreInnhold, HoeyreKolonne, KolonneContainer, VenstreKolonne } from './styled'

export const ToKolonner = ({ children }: { children: { left: JSX.Element; right: JSX.Element | null } }) => {
  return (
    <KolonneContainer>
      <VenstreKolonne>{children.left}</VenstreKolonne>
      <HoeyreKolonne>
        <HoeyreInnhold>{children.right}</HoeyreInnhold>
      </HoeyreKolonne>
    </KolonneContainer>
  )
}
