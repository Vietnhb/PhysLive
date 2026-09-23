-- Apply only after the B2B school schema (V3). Never guess a class for legacy assignments.
ALTER TABLE public.assignments
    ADD COLUMN IF NOT EXISTS school_class_id UUID REFERENCES public.school_classes(id);
CREATE INDEX IF NOT EXISTS idx_assignments_school_class_id ON public.assignments(school_class_id);
