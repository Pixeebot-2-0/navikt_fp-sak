ALTER TABLE GR_YTELSES_FORDELING ADD OVERSTYRT_RETTIGHET_ID NUMBER(19)
    constraint FK_GR_YTELSES_FORDELING_5 references SO_RETTIGHET;
COMMENT ON COLUMN GR_YTELSES_FORDELING.OVERSTYRT_RETTIGHET_ID IS 'Vurdering av oppgitte rettigheter';
create index IDX_GR_YTELSES_FORDELING_20 on GR_YTELSES_FORDELING (OVERSTYRT_RETTIGHET_ID);