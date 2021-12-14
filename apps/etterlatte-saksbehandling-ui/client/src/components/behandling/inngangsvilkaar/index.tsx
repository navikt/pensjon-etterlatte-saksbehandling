import styled from "styled-components";
import { Content } from "../../../shared/styled";
import { Vilkaar, VilkaarStatus } from "./vilkaar";

export const Inngangsvilkaar = () => {
    return (
        <Content>
            <VilkaarListe>
                <Vilkaar vilkaarType="Dødsfall" vilkaarStatus={VilkaarStatus.IKKE_OPPFYLT} />
                <Vilkaar vilkaarType="Alder" vilkaarStatus={VilkaarStatus.OPPFYLT} />
                <Vilkaar vilkaarType="Foreldreansvar" vilkaarStatus={VilkaarStatus.IKKE_OPPFYLT} />
                <Vilkaar vilkaarType="Familieforhold" vilkaarStatus={VilkaarStatus.IKKE_OPPFYLT} />
                <Vilkaar vilkaarType="Bostedsadresse" vilkaarStatus={VilkaarStatus.OPPFYLT} />
                <Vilkaar vilkaarType="Yrkesskade" vilkaarStatus={VilkaarStatus.IKKE_OPPFYLT} />
                <Vilkaar vilkaarType="Medlemsskap" vilkaarStatus={VilkaarStatus.IKKE_OPPFYLT} />
            </VilkaarListe>
        </Content>
    );
};

const VilkaarListe = styled.div`
    padding: 0 2em;
`;
