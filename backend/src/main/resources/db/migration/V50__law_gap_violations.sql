-- A model a curriculum law still refuses after the repair loop reaches the reviewers too: either the model was
-- wrong, or the law is too narrow for what teachers describe.
ALTER TABLE law_gaps DROP CONSTRAINT IF EXISTS law_gaps_kind_check;
ALTER TABLE law_gaps ADD CONSTRAINT law_gaps_kind_check CHECK (kind IN ('UNCHECKED', 'NO_ROLE', 'AMBIGUOUS', 'VIOLATION'));
