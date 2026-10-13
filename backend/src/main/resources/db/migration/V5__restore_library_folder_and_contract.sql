-- Preserve existing rows while restoring the mappings used by the API.
ALTER TABLE public.library_items
    ADD COLUMN IF NOT EXISTS folder_id UUID REFERENCES public.library_folders(id);
CREATE INDEX IF NOT EXISTS idx_library_items_folder_id ON public.library_items(folder_id);

ALTER TABLE public.specifications
    ADD COLUMN IF NOT EXISTS contract_version VARCHAR(16);
