CREATE TABLE IF NOT EXISTS vilkaarsvurdering_kilde (
    vilkaarsvurdering_id UUID REFERENCES vilkaarsvurdering(id) PRIMARY KEY,
    kopiert_fra_vilkaarsvurdering_id UUID REFERENCES vilkaarsvurdering(id)
)