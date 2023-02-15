CREATE TABLE omberegningTilVilkaarsvurdering
(
    id           UUID PRIMARY KEY DEFAULT (uuid_generate_v4()),
    vilkaarsvurdering_id   UUID references vilkaarsvurdering (id) ON delete cascade,
    omberegning_id UUID
);